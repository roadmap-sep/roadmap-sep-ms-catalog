package sh.roadmap.sep.catalog.infrastructure.output.jpa.adapter;

import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import sh.roadmap.sep.catalog.application.exception.ProductImportException;
import sh.roadmap.sep.catalog.domain.exception.ProductAlreadyExistsException;
import sh.roadmap.sep.catalog.domain.exception.ProductNotFoundException;
import sh.roadmap.sep.catalog.domain.model.Product;
import sh.roadmap.sep.catalog.domain.model.ProductFilter;
import sh.roadmap.sep.catalog.domain.port.out.ProductPortOut;
import sh.roadmap.sep.catalog.domain.util.Page;
import sh.roadmap.sep.catalog.infrastructure.output.jpa.entity.ProductEntity;
import sh.roadmap.sep.catalog.infrastructure.output.jpa.mapper.ProductJpaMapper;
import sh.roadmap.sep.catalog.infrastructure.output.jpa.repository.ProductJpaRepository;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class ProductJpaAdapter implements ProductPortOut {
    private final ProductJpaRepository productJpaRepository;
    private final ProductJpaMapper productJpaMapper;
    private final JdbcTemplate jdbcTemplate;

    @Override
    public Page<Product> searchProducts(ProductFilter productFilter, Page.Request pageRequest) {
        return this.toProductPage(productJpaRepository.findAll(getDynamicFilters(productFilter),
                PageRequest.of(pageRequest.pageNumber(),
                        pageRequest.pageSize())));
    }

    @Override
    public Product getById(UUID productId) {
        return productJpaRepository.findById(productId)
                .map(productJpaMapper::toDomain)
                .orElseThrow(() -> new ProductNotFoundException(productId));
    }

    @Override
    public Product getBySku(String sku) {
        return productJpaRepository.findBySku(sku)
                .map(productJpaMapper::toDomain)
                .orElseThrow(() -> new ProductNotFoundException(sku));
    }

    @Override
    public void save(Product product) {
        try {
            productJpaRepository.saveAndFlush(productJpaMapper.toEntity(product));
        } catch (DataIntegrityViolationException e) {
            String dbMessage = e.getMostSpecificCause().getMessage();

            if (dbMessage.contains("sku")) {
                throw new ProductAlreadyExistsException(product.sku());
            }
            if (dbMessage.contains("id")) {
                throw new ProductAlreadyExistsException(product.id());
            }
            throw e;
        }
    }

    @Override
    public void saveBatch(List<Product> products) {
        if (products == null || products.isEmpty()) {
            return;
        }
        try {
            insertProducts(products);
        } catch (DataIntegrityViolationException e) {
            handleDuplicateSkuAndThrow(products, e);
        }
        insertCategoryRelations(products);
    }

    @Override
    public void update(Product product) {
        ProductEntity productEntity = productJpaMapper.toEntity(product);
        productEntity.markNotNew();
        productJpaRepository.save(productEntity);
    }

    private Page<Product> toProductPage(org.springframework.data.domain.Page<ProductEntity> page) {
        Page.PageBuilder<Product> builder = Page.<Product>builder()
                .pageNumber(page.getNumber())
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .hasNext(page.hasNext());

        return page.get()
                .map(productJpaMapper::toDomain)
                .collect(Collectors.collectingAndThen(Collectors.toList(), builder::data))
                .build();
    }

    private Specification<ProductEntity> getDynamicFilters(ProductFilter filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (filter.name() != null && !filter.name().isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("name")), "%" + filter.name().toLowerCase() + "%"));
            }
            if (filter.categoryId() != null) {
                predicates.add(cb.isMember(filter.categoryId(), root.get("categoryIds")));
            }
            if (filter.minPrice() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("price"), filter.minPrice()));
            }
            if (filter.maxPrice() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("price"), filter.maxPrice()));
            }
            if (Boolean.TRUE.equals(filter.inStock())) {
                predicates.add(cb.greaterThan(root.get("stock"), 0));
            }
            if (filter.isActive() != null) {
                predicates.add(cb.equal(root.get("active"), filter.isActive()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private void insertProducts(List<Product> products) {
        String sqlProduct = """
                  INSERT INTO product (id, sku, name, description, price,\s
                  main_image_url, stock, weight, active, created_at, updated_at)\s
                  VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)\s
                """;

        jdbcTemplate.batchUpdate(sqlProduct, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(@NonNull PreparedStatement ps, int i) throws SQLException {
                Product product = products.get(i);
                String productId = product.id() != null ? product.id().toString() : null;

                ps.setObject(1, productId);
                ps.setString(2, product.sku());
                ps.setString(3, product.name());
                ps.setString(4, product.description());
                ps.setBigDecimal(5, product.price());
                ps.setString(6, product.mainImageUrl());
                ps.setInt(7, product.stock());
                ps.setDouble(8, product.weight());
                ps.setBoolean(9, product.active());
                ps.setTimestamp(10, Timestamp.from(product.createdAt()));
                ps.setTimestamp(11, Timestamp.from(product.updatedAt()));
            }

            @Override
            public int getBatchSize() {
                return products.size();
            }
        });
    }

    private void handleDuplicateSkuAndThrow(List<Product> products, DataIntegrityViolationException e) {
        if (e.getMostSpecificCause().getMessage().contains("sku")) {
            MapSqlParameterSource parameters = products.stream()
                    .map(Product::sku)
                    .collect(Collectors.collectingAndThen(Collectors.toList(),
                            incomingSkus -> new MapSqlParameterSource("skus", incomingSkus)));

            String sqlCheck = "SELECT sku FROM product WHERE sku IN (:skus)";

            throw new NamedParameterJdbcTemplate(jdbcTemplate)
                    .queryForList(sqlCheck, parameters, String.class)
                    .stream()
                    .collect(Collectors.collectingAndThen(Collectors.toList(), ProductImportException::new));
        }
        throw e;
    }

    private void insertCategoryRelations(List<Product> products) {
        List<Object[]> productCategoryArgs = new ArrayList<>(products.size() * 2);

        for (Product product : products) {
            if (product.categoryIds() != null && !product.categoryIds().isEmpty()) {
                String productId = product.id() != null ? product.id().toString() : null;
                for (Long categoryId : product.categoryIds()) {
                    productCategoryArgs.add(new Object[]{productId, categoryId});
                }
            }
        }

        if (!productCategoryArgs.isEmpty()) {
            String sqlCategory = "INSERT INTO product_category (product_id, category_id) VALUES (?, ?)";
            jdbcTemplate.batchUpdate(sqlCategory, productCategoryArgs);
        }
    }
}
