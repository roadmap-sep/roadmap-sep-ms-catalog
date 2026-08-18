package sh.roadmap.sep.productcatalog.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record Product(
        UUID id,
        String sku,
        String name,
        String description,
        Set<Long> categoryIds,
        BigDecimal price,
        String mainImageUrl,
        int stock,
        double weight,
        boolean active,
        Instant createdAt,
        Instant updatedAt) {
    public ProductBuilder toBuilder() {
        return new ProductBuilder(this);
    }

    public static ProductBuilder builder() {
        return new ProductBuilder();
    }

    public static class ProductBuilder {
        private UUID id;
        private String sku;
        private String name;
        private String description;
        private Set<Long> categoryIds;
        private String mainImageUrl;
        private BigDecimal price;
        private int stock;
        private double weight;
        private boolean active;
        private Instant createdAt;
        private Instant updatedAt;

        public ProductBuilder() {
        }

        public ProductBuilder(Product product) {
            this.id = product.id;
            this.sku = product.sku;
            this.name = product.name;
            this.description = product.description;
            this.categoryIds = product.categoryIds;
            this.mainImageUrl = product.mainImageUrl;
            this.price = product.price;
            this.stock = product.stock;
            this.weight = product.weight;
            this.active = product.active;
            this.createdAt = product.createdAt;
            this.updatedAt = product.updatedAt;
        }

        public ProductBuilder id(UUID id) {
            this.id = id;
            return this;
        }

        public ProductBuilder sku(String sku) {
            this.sku = sku;
            return this;
        }


        public ProductBuilder categoryIds(Set<Long> categoryIds) {
            this.categoryIds = categoryIds;
            return this;
        }

        public ProductBuilder name(String name) {
            this.name = name;
            return this;
        }

        public ProductBuilder description(String description) {
            this.description = description;
            return this;
        }

        public ProductBuilder mainImageUrl(String mainImageUrl) {
            this.mainImageUrl = mainImageUrl;
            return this;
        }

        public ProductBuilder price(BigDecimal price) {
            this.price = price;
            return this;
        }

        public ProductBuilder stock(int stock) {
            this.stock = stock;
            return this;
        }


        public ProductBuilder updatedAt(Instant updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        public ProductBuilder createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public ProductBuilder active(boolean active) {
            this.active = active;
            return this;
        }

        public ProductBuilder weight(double weight) {
            this.weight = weight;
            return this;
        }

        public Product build() {
            return new Product(id, sku, name, description, categoryIds,
                    price, mainImageUrl, stock, weight, active,
                    createdAt, updatedAt);
        }
    }
}
