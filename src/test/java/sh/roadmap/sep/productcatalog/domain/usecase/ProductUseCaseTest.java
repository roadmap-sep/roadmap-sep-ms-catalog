package sh.roadmap.sep.productcatalog.domain.usecase;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sh.roadmap.sep.productcatalog.domain.model.Product;
import sh.roadmap.sep.productcatalog.domain.model.ProductFilter;
import sh.roadmap.sep.productcatalog.domain.port.out.ProductPortOut;
import sh.roadmap.sep.productcatalog.domain.util.Page;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willDoNothing;

@ExtendWith(MockitoExtension.class)
class ProductUseCaseTest {

    @Mock
    private ProductPortOut productPortOut;

    @InjectMocks
    private ProductUseCase productUseCase;

    private final UUID productId = UUID.randomUUID();
    private final String productSku = "SKU-12345";
    private final Product product = Product.builder()
            .id(productId)
            .sku(productSku)
            .name("Gaming Laptop")
            .description("High performance laptop")
            .categoryIds(Set.of(1L, 2L))
            .price(new BigDecimal("1500.00"))
            .mainImageUrl("http://image.url/laptop.png")
            .stock(50)
            .weight(2.5)
            .active(true)
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();

    private final Page.Request pageRequest = new Page.Request(0, 10);
    private final ProductFilter filter = ProductFilter.builder()
            .name("Laptop")
            .categoryId(1L)
            .inStock(true)
            .active(true)
            .build();

    @Nested
    @DisplayName("searchProducts() tests")
    class SearchProductsTests {

        @Test
        @DisplayName("Should delegate to portOut and return a page of products matching the filter")
        void shouldDelegateSearchProducts() {
            var productPage = Page.<Product>builder()
                    .data(List.of(product))
                    .pageNumber(0)
                    .pageSize(10)
                    .totalElements(1)
                    .totalPages(1)
                    .hasNext(false)
                    .build();

            given(productPortOut.searchProducts(filter, pageRequest)).willReturn(productPage);

            var result = productUseCase.searchProducts(filter, pageRequest);

            assertThat(result).isNotNull();
            assertThat(result.data()).containsExactly(product);
            then(productPortOut).should().searchProducts(filter, pageRequest);
        }
    }

    @Nested
    @DisplayName("getById() tests")
    class GetByIdTests {

        @Test
        @DisplayName("Should delegate to portOut and return the product by UUID")
        void shouldDelegateGetById() {
            given(productPortOut.getById(productId)).willReturn(product);

            var result = productUseCase.getById(productId);

            assertThat(result).isEqualTo(product);
            then(productPortOut).should().getById(productId);
        }
    }

    @Nested
    @DisplayName("getBySku() tests")
    class GetBySkuTests {

        @Test
        @DisplayName("Should delegate to portOut and return the product by SKU")
        void shouldDelegateGetBySku() {
            given(productPortOut.getBySku(productSku)).willReturn(product);

            var result = productUseCase.getBySku(productSku);

            assertThat(result).isEqualTo(product);
            then(productPortOut).should().getBySku(productSku);
        }
    }

    @Nested
    @DisplayName("save() tests")
    class SaveTests {

        @Test
        @DisplayName("Should delegate product creation to portOut")
        void shouldDelegateSave() {
            willDoNothing().given(productPortOut).save(product);

            productUseCase.save(product);

            then(productPortOut).should().save(product);
        }
    }

    @Nested
    @DisplayName("saveBatch() tests")
    class SaveBatchTests {

        @Test
        @DisplayName("Should delegate batch product creation to portOut")
        void shouldDelegateSaveBatch() {
            List<Product> products = List.of(product, product.toBuilder().id(UUID.randomUUID()).sku("SKU-999").build());
            willDoNothing().given(productPortOut).saveBatch(products);

            productUseCase.saveBatch(products);

            then(productPortOut).should().saveBatch(products);
        }
    }

    @Nested
    @DisplayName("update() tests")
    class UpdateTests {

        @Test
        @DisplayName("Should delegate product update to portOut")
        void shouldDelegateUpdate() {
            willDoNothing().given(productPortOut).update(product);

            productUseCase.update(product);

            then(productPortOut).should().update(product);
        }
    }
}