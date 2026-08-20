package sh.roadmap.sep.catalog.application.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sh.roadmap.sep.catalog.application.dto.request.ProductImportRequest;
import sh.roadmap.sep.catalog.application.dto.request.ProductRequest;
import sh.roadmap.sep.catalog.application.dto.response.ProductResponse;
import sh.roadmap.sep.catalog.application.exception.ProductImportStrategyException;
import sh.roadmap.sep.catalog.application.mapper.ProductDtoMapper;
import sh.roadmap.sep.catalog.application.strategy.ProductImportStrategy;
import sh.roadmap.sep.catalog.application.strategy.ProductImportStrategyResolver;
import sh.roadmap.sep.catalog.application.strategy.impl.ProductImportStrategyResolverImpl;
import sh.roadmap.sep.catalog.domain.model.Category;
import sh.roadmap.sep.catalog.domain.model.Product;
import sh.roadmap.sep.catalog.domain.model.ProductFilter;
import sh.roadmap.sep.catalog.domain.port.in.CategoryPortIn;
import sh.roadmap.sep.catalog.domain.port.in.ProductPortIn;
import sh.roadmap.sep.catalog.domain.util.Page;

import java.io.InputStream;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class ProductServiceImplTest {

    @Mock
    private ProductPortIn productPortIn;

    @Mock
    private ProductDtoMapper productDtoMapper;

    @Mock
    private ProductImportStrategyResolver productImportStrategyResolver;

    @Mock
    private CategoryPortIn categoryPortIn;

    @InjectMocks
    private ProductServiceImpl productService;

    @Captor
    private ArgumentCaptor<Product> productCaptor;

    @Captor
    private ArgumentCaptor<List<Product>> productListCaptor;
    @Mock
    private ProductImportStrategy csvStrategy;

    @Mock
    private ProductImportStrategy excelStrategy;

    private ProductImportStrategyResolverImpl resolver;

    private final UUID productId = UUID.randomUUID();
    private final Page.Request pageRequest = new Page.Request(0, 10);

    private final Category category = new Category(1L, "Electronics", "electronics", null, true);

    private final Product product = Product.builder()
            .id(productId)
            .sku("PROD-123")
            .name("Smartphone")
            .categoryIds(Set.of(1L))
            .active(true)
            .build();

    private final ProductRequest productRequest = ProductRequest.builder()
            .sku("PROD-123")
            .name("Smartphone")
            .description("Desc")
            .price(new BigDecimal("999.00"))
            .stock(10)
            .weight(0.5)
            .mainImageUrl("http://image.com/1.jpg")
            .categoryIds(Set.of(1L))
            .build();

    private final ProductResponse baseResponse = ProductResponse.builder()
            .id(productId)
            .sku("PROD-123")
            .name("Smartphone")
            .build();

    private void mockToDtoBehavior() {
        given(productDtoMapper.toDto(product)).willReturn(baseResponse);
        given(categoryPortIn.getById(1L)).willReturn(category);
    }

    @BeforeEach
    void setUp() {
        resolver = new ProductImportStrategyResolverImpl(List.of(csvStrategy, excelStrategy));
    }

    @Test
    @DisplayName("Should return the correct strategy when extension is supported")
    void shouldResolveCorrectStrategy() {
        String extension = "csv";
        given(csvStrategy.supports(extension)).willReturn(true);

        ProductImportStrategy resolvedStrategy = resolver.resolve(extension);

        assertThat(resolvedStrategy).isEqualTo(csvStrategy);
    }

    @Test
    @DisplayName("Should throw ProductImportStrategyException when extension is not supported")
    void shouldThrowExceptionWhenStrategyNotFound() {
        String extension = "xml";
        given(csvStrategy.supports(extension)).willReturn(false);
        given(excelStrategy.supports(extension)).willReturn(false);

        assertThatThrownBy(() -> resolver.resolve(extension))
                .isInstanceOf(ProductImportStrategyException.class);
    }

    @Nested
    @DisplayName("searchProducts() tests")
    class SearchProductsTests {

        @Test
        @DisplayName("Should return paged products and map category ids to slugs")
        void shouldReturnPagedProducts() {
            var filter = new ProductFilter("Smart", null, null,
                    null, true, true);
            var productPage = Page.<Product>builder().data(List.of(product)).build();

            given(productPortIn.searchProducts(filter, pageRequest)).willReturn(productPage);
            mockToDtoBehavior();

            var result = productService.searchProducts(filter, pageRequest);

            assertThat(result.data()).hasSize(1);
            assertThat(result.data().getFirst().categories()).containsExactly("electronics");

            then(productPortIn).should().searchProducts(filter, pageRequest);
            then(categoryPortIn).should().getById(1L);
        }
    }

    @Nested
    @DisplayName("getById() tests")
    class GetByIdTests {

        @Test
        @DisplayName("Should return mapped product response with resolved categories")
        void shouldReturnProductById() {
            given(productPortIn.getById(productId)).willReturn(product);
            mockToDtoBehavior();

            var result = productService.getById(productId);

            assertThat(result.id()).isEqualTo(productId);
            assertThat(result.categories()).containsExactly("electronics");

            then(productPortIn).should().getById(productId);
            then(categoryPortIn).should().getById(1L);
        }
    }

    @Nested
    @DisplayName("getBySku() tests")
    class GetBySkuTests {

        @Test
        @DisplayName("Should return mapped product response by SKU")
        void shouldReturnProductBySku() {
            String sku = "PROD-123";
            given(productPortIn.getBySku(sku)).willReturn(product);
            mockToDtoBehavior();

            var result = productService.getBySku(sku);

            assertThat(result.sku()).isEqualTo(sku);
            assertThat(result.categories()).containsExactly("electronics");

            then(productPortIn).should().getBySku(sku);
            then(categoryPortIn).should().getById(1L);
        }
    }

    @Nested
    @DisplayName("save() tests")
    class SaveTests {

        @Test
        @DisplayName("Should validate categories, enhance product with UUID/timestamps, and save")
        void shouldSaveProductSuccessfully() {
            given(categoryPortIn.getById(1L)).willReturn(category);
            given(productDtoMapper.toDomain(productRequest))
                    .willReturn(Product.builder().sku("PROD-123").name("Smartphone").categoryIds(Set.of(1L)).build());

            willDoNothing().given(productPortIn).save(any(Product.class));

            productService.save(productRequest);

            then(categoryPortIn).should().getById(1L); // Verifica que validó la categoría
            then(productPortIn).should().save(productCaptor.capture());

            Product savedProduct = productCaptor.getValue();
            assertThat(savedProduct.id()).isNotNull();
            assertThat(savedProduct.active()).isTrue();
            assertThat(savedProduct.createdAt()).isNotNull();
            assertThat(savedProduct.updatedAt()).isNotNull();
            assertThat(savedProduct.sku()).isEqualTo("PROD-123");
        }
    }

    @Nested
    @DisplayName("saveBatch() tests")
    class SaveBatchTests {

        @Test
        @DisplayName("Should resolve strategy, process file, map to domain, and save batch")
        void shouldSaveBatchSuccessfully() {
            var importRequest = new ProductImportRequest("csv", "products.csv",
                    InputStream.nullInputStream());

            ProductImportStrategy strategyMock = mock(ProductImportStrategy.class);

            given(productImportStrategyResolver.resolve("csv")).willReturn(strategyMock);
            given(strategyMock.process(importRequest)).willReturn(List.of(productRequest));

            given(productDtoMapper.toDomain(productRequest))
                    .willReturn(Product.builder().sku("PROD-123").categoryIds(Set.of(1L)).build());
            willDoNothing().given(productPortIn).saveBatch(any());

            productService.saveBatch(importRequest);

            then(productImportStrategyResolver).should().resolve("csv");
            then(strategyMock).should().process(importRequest);
            then(productPortIn).should().saveBatch(productListCaptor.capture());

            List<Product> savedBatch = productListCaptor.getValue();
            assertThat(savedBatch).hasSize(1);

            Product batchProduct = savedBatch.getFirst();
            assertThat(batchProduct.id()).isNotNull();
            assertThat(batchProduct.active()).isTrue();
            assertThat(batchProduct.createdAt()).isNotNull();
            assertThat(batchProduct.updatedAt()).isNotNull();
            assertThat(batchProduct.sku()).isEqualTo("PROD-123");
        }
    }

    @Nested
    @DisplayName("update() tests")
    class UpdateTests {

        @Test
        @DisplayName("Should validate categories and update fields retaining the ID")
        void shouldUpdateProductSuccessfully() {
            given(categoryPortIn.getById(1L)).willReturn(category);
            given(productPortIn.getById(productId)).willReturn(product);
            willDoNothing().given(productPortIn).update(any(Product.class));

            productService.update(productRequest, productId);

            then(categoryPortIn).should().getById(1L);
            then(productPortIn).should().update(productCaptor.capture());

            Product updatedProduct = productCaptor.getValue();
            assertThat(updatedProduct.id()).isEqualTo(productId); // ID no debe mutar
            assertThat(updatedProduct.name()).isEqualTo(productRequest.name());
            assertThat(updatedProduct.price()).isEqualTo(productRequest.price());
            assertThat(updatedProduct.updatedAt()).isNotNull();
        }
    }

    @Nested
    @DisplayName("toggleActiveStatus() tests")
    class ToggleActiveStatusTests {

        @Test
        @DisplayName("Should toggle active status from true to false and update timestamp")
        void shouldToggleFromTrueToFalse() {
            given(productPortIn.getById(productId)).willReturn(product); // El original tiene active=true
            willDoNothing().given(productPortIn).update(any(Product.class));

            productService.toggleActiveStatus(productId);

            then(productPortIn).should().update(productCaptor.capture());

            Product toggledProduct = productCaptor.getValue();
            assertThat(toggledProduct.id()).isEqualTo(productId);
            assertThat(toggledProduct.active()).isFalse(); // Verificación del toggle
            assertThat(toggledProduct.updatedAt()).isNotNull();
        }

        @Test
        @DisplayName("Should toggle active status from false to true")
        void shouldToggleFromFalseToTrue() {
            var inactiveProduct = product.toBuilder().active(false).build();
            given(productPortIn.getById(productId)).willReturn(inactiveProduct);

            productService.toggleActiveStatus(productId);

            then(productPortIn).should().update(productCaptor.capture());
            assertThat(productCaptor.getValue().active()).isTrue();
        }
    }
}