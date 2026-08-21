package sh.roadmap.sep.catalog.infrastructure.output.jpa.adapter;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.RowMapper;
import sh.roadmap.sep.catalog.application.exception.ProductImportException;
import sh.roadmap.sep.catalog.domain.exception.ProductAlreadyExistsException;
import sh.roadmap.sep.catalog.domain.exception.ProductNotFoundException;
import sh.roadmap.sep.catalog.domain.model.Product;
import sh.roadmap.sep.catalog.domain.model.ProductFilter;
import sh.roadmap.sep.catalog.domain.util.Page;
import sh.roadmap.sep.catalog.infrastructure.output.jpa.entity.ProductEntity;
import sh.roadmap.sep.catalog.infrastructure.output.jpa.mapper.ProductJpaMapper;
import sh.roadmap.sep.catalog.infrastructure.output.jpa.repository.ProductJpaRepository;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class ProductJpaAdapterTest {

    @Mock
    private ProductJpaRepository productJpaRepository;

    @Mock
    private ProductJpaMapper productJpaMapper;

    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private ProductJpaAdapter productJpaAdapter;

    @Captor
    private ArgumentCaptor<ProductEntity> productEntityCaptor;

    @Captor
    private ArgumentCaptor<Specification<ProductEntity>> specificationCaptor;

    @Captor
    private ArgumentCaptor<BatchPreparedStatementSetter> batchSetterCaptor;

    private final UUID productId = UUID.randomUUID();
    private final String sku = "PROD-123";

    private final Product product = Product.builder()
            .id(productId)
            .sku(sku)
            .name("Smartphone")
            .price(new BigDecimal("999.00"))
            .categoryIds(Set.of(1L, 2L))
            .active(true)
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();

    private final ProductEntity productEntity = new ProductEntity(
            productId, sku, "Smartphone", "Desc", new BigDecimal("999.00"),
            "url", 10, 0.5, true, Set.of(1L, 2L),
            Instant.now(), Instant.now(), true);

    @Nested
    @DisplayName("searchProducts() tests")
    class SearchProductsTests {

        @Test
        @DisplayName("Should return mapped page of products using dynamic filters")
        @SuppressWarnings("unchecked")
        void shouldReturnPagedProducts() {
            var filter = new ProductFilter("Smart", 1L, new BigDecimal("100"),
                    new BigDecimal("2000"), true, true);
            var pageRequest = new Page.Request(0, 10);
            var pageable = PageRequest.of(pageRequest.pageNumber(), pageRequest.pageSize());
            var entityPage = new PageImpl<>(List.of(productEntity), PageRequest.of(0, 10), 1);

            given(productJpaRepository.findAll(specificationCaptor.capture(), eq(pageable)))
                    .willReturn(entityPage);
            given(productJpaMapper.toDomain(productEntity)).willReturn(product);

            Page<Product> result = productJpaAdapter.searchProducts(filter, pageRequest);
            Specification<ProductEntity> capturedSpec = specificationCaptor.getValue();
            Root<ProductEntity> rootMock = mock(Root.class);
            CriteriaQuery<?> queryMock = mock(CriteriaQuery.class);
            CriteriaBuilder cbMock = mock(CriteriaBuilder.class);
            Path<?> pathMock = mock(Path.class);
            given(rootMock.get(anyString())).willReturn((Path) pathMock);

            lenient().when(cbMock.lower(any())).thenReturn(mock(Expression.class));
            lenient().when(cbMock.like(any(), anyString())).thenReturn(mock(Predicate.class));
            lenient().when(cbMock.equal(any(), any())).thenReturn(mock(Predicate.class));
            lenient().when(cbMock.and(any(Predicate[].class))).thenReturn(mock(Predicate.class));

            capturedSpec.toPredicate(rootMock, queryMock, cbMock);

            assertThat(result.data()).hasSize(1);
            assertThat(result.data().getFirst().id()).isEqualTo(productId);
            assertThat(result.totalElements()).isEqualTo(1);

            then(productJpaRepository).should().findAll(any(Specification.class), any(Pageable.class));
        }
    }

    @Nested
    @DisplayName("getById() & getBySku() tests")
    class GetTests {

        @Test
        @DisplayName("Should return Product domain model by ID")
        void shouldReturnProductById() {
            given(productJpaRepository.findById(productId)).willReturn(Optional.of(productEntity));
            given(productJpaMapper.toDomain(productEntity)).willReturn(product);

            Product result = productJpaAdapter.getById(productId);

            assertThat(result).isNotNull();
            assertThat(result.id()).isEqualTo(productId);
        }

        @Test
        @DisplayName("Should throw ProductNotFoundException when ID not found")
        void shouldThrowExceptionWhenIdNotFound() {
            given(productJpaRepository.findById(productId)).willReturn(Optional.empty());

            assertThatThrownBy(() -> productJpaAdapter.getById(productId))
                    .isInstanceOf(ProductNotFoundException.class);
        }

        @Test
        @DisplayName("Should return Product domain model by SKU")
        void shouldReturnProductBySku() {
            given(productJpaRepository.findBySku(sku)).willReturn(Optional.of(productEntity));
            given(productJpaMapper.toDomain(productEntity)).willReturn(product);

            Product result = productJpaAdapter.getBySku(sku);

            assertThat(result).isNotNull();
            assertThat(result.sku()).isEqualTo(sku);
        }
    }

    @Nested
    @DisplayName("save() tests")
    class SaveTests {

        @Test
        @DisplayName("Should save and flush product entity successfully")
        void shouldSaveSuccessfully() {
            given(productJpaMapper.toEntity(product)).willReturn(productEntity);
            given(productJpaRepository.saveAndFlush(productEntity)).willReturn(productEntity);

            productJpaAdapter.save(product);

            then(productJpaRepository).should().saveAndFlush(productEntity);
        }

        @Test
        @DisplayName("Should map DB constraint exception to ProductAlreadyExistsException (SKU)")
        void shouldThrowAlreadyExistsForSku() {
            given(productJpaMapper.toEntity(product)).willReturn(productEntity);

            SQLException sqlException = new SQLException("duplicate key violates unique constraint 'sku'");
            DataIntegrityViolationException dbException = new DataIntegrityViolationException("Error", sqlException);

            given(productJpaRepository.saveAndFlush(productEntity)).willThrow(dbException);

            assertThatThrownBy(() -> productJpaAdapter.save(product))
                    .isInstanceOf(ProductAlreadyExistsException.class)
                    .hasMessageContaining(sku);
        }

        @Test
        @DisplayName("Should map DB constraint exception to ProductAlreadyExistsException (ID)")
        void shouldThrowAlreadyExistsForId() {
            given(productJpaMapper.toEntity(product)).willReturn(productEntity);

            SQLException sqlException = new SQLException("duplicate key violates unique constraint 'id'");
            DataIntegrityViolationException dbException = new DataIntegrityViolationException("Error", sqlException);

            given(productJpaRepository.saveAndFlush(productEntity)).willThrow(dbException);

            assertThatThrownBy(() -> productJpaAdapter.save(product))
                    .isInstanceOf(ProductAlreadyExistsException.class)
                    .hasMessageContaining(productId.toString());
        }
    }

    @Nested
    @DisplayName("saveBatch() tests")
    class SaveBatchTests {

        @Test
        @DisplayName("Should do nothing if product list is null or empty")
        void shouldDoNothingOnEmptyList() {
            productJpaAdapter.saveBatch(null);
            productJpaAdapter.saveBatch(Collections.emptyList());

            then(jdbcTemplate).should(never()).batchUpdate(anyString(), any(BatchPreparedStatementSetter.class));
        }

        @Test
        @DisplayName("Should execute two batch updates and map properties correctly in BatchPreparedStatementSetter")
        @SuppressWarnings("unchecked")
        void shouldSaveBatchSuccessfully() throws SQLException {
            List<Product> products = List.of(product);

            productJpaAdapter.saveBatch(products);

            then(jdbcTemplate).should().batchUpdate(anyString(), batchSetterCaptor.capture());
            then(jdbcTemplate).should().batchUpdate(anyString(), any(List.class));

            BatchPreparedStatementSetter capturedSetter = batchSetterCaptor.getValue();

            assertThat(capturedSetter.getBatchSize()).isEqualTo(1);

            PreparedStatement psMock = mock(PreparedStatement.class);
            capturedSetter.setValues(psMock, 0); // 0 es el índice del producto en la lista

            then(psMock).should().setObject(1, product.id().toString());
            then(psMock).should().setString(2, product.sku());
            then(psMock).should().setString(3, product.name());
            then(psMock).should().setString(4, product.description());
            then(psMock).should().setBigDecimal(5, product.price());
            then(psMock).should().setString(6, product.mainImageUrl());
            then(psMock).should().setInt(7, product.stock());
            then(psMock).should().setDouble(8, product.weight());
            then(psMock).should().setBoolean(9, product.active());

            then(psMock).should().setTimestamp(eq(10), any(java.sql.Timestamp.class));
            then(psMock).should().setTimestamp(eq(11), any(java.sql.Timestamp.class));
        }

        @Test
        @DisplayName("Should check for existing SKUs first and throw ProductImportException without inserting")
        @SuppressWarnings("unchecked")
        void shouldHandleDuplicateSkuInBatchAndThrow() {
            List<Product> products = List.of(product);
            String duplicateSku = product.sku();

            given(jdbcTemplate.query(any(org.springframework.jdbc.core.PreparedStatementCreator.class),
                    any(org.springframework.jdbc.core.RowMapper.class)))
                    .willReturn(List.of(duplicateSku));

            assertThatThrownBy(() -> productJpaAdapter.saveBatch(products))
                    .isInstanceOf(ProductImportException.class)
                    .hasMessageContaining(duplicateSku);

            then(jdbcTemplate).should(never()).batchUpdate(anyString(),
                    any(org.springframework.jdbc.core.BatchPreparedStatementSetter.class));
            then(jdbcTemplate).should(never()).batchUpdate(anyString(), any(List.class));
        }
    }

    @Nested
    @DisplayName("update() tests")
    class UpdateTests {

        @Test
        @DisplayName("Should mark entity as not new and save")
        void shouldUpdateProductSuccessfully() {
            given(productJpaMapper.toEntity(product)).willReturn(productEntity);

            productJpaAdapter.update(product);

            then(productJpaRepository).should().save(productEntityCaptor.capture());

            ProductEntity capturedEntity = productEntityCaptor.getValue();
            assertThat(capturedEntity.isNew()).isFalse();
        }
    }
}