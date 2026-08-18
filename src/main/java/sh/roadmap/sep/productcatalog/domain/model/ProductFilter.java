package sh.roadmap.sep.productcatalog.domain.model;

import java.math.BigDecimal;

public record ProductFilter(
        String name,
        Long categoryId,
        BigDecimal minPrice,
        BigDecimal maxPrice,
        Boolean inStock,
        Boolean isActive) {
    public ProductFilterBuilder toBuilder() {
        return new ProductFilterBuilder(this);
    }

    public static ProductFilterBuilder builder() {
        return new ProductFilterBuilder();
    }

    public static class ProductFilterBuilder {
        private String name;
        private Long categoryId;
        private BigDecimal minPrice;
        private BigDecimal maxPrice;
        private Boolean inStock;
        private Boolean isActive;

        public ProductFilterBuilder() {
        }

        public ProductFilterBuilder(ProductFilter productFilter) {
            this.name = productFilter.name;
            this.categoryId = productFilter.categoryId;
            this.minPrice = productFilter.minPrice;
            this.maxPrice = productFilter.maxPrice;
            this.inStock = productFilter.inStock;
            this.isActive = productFilter.isActive;
        }

        public ProductFilterBuilder name(String name) {
            this.name = name;
            return this;
        }

        public ProductFilterBuilder categoryId(Long categoryId) {
            this.categoryId = categoryId;
            return this;
        }

        public ProductFilterBuilder minPrice(BigDecimal minPrice) {
            this.minPrice = minPrice;
            return this;
        }

        public ProductFilterBuilder maxPrice(BigDecimal maxPrice) {
            this.maxPrice = maxPrice;
            return this;
        }

        public ProductFilterBuilder inStock(Boolean inStock) {
            this.inStock = inStock;
            return this;
        }

        public ProductFilterBuilder active(Boolean active) {
            isActive = active;
            return this;
        }

        public ProductFilter build() {
            return new ProductFilter(this.name, this.categoryId, this.minPrice,
                    this.maxPrice, this.inStock, this.isActive);
        }
    }
}
