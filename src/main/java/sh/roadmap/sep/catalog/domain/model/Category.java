package sh.roadmap.sep.catalog.domain.model;

public record Category(
        Long id,
        String name,
        String slug,
        Long parentId,
        boolean active) {
    public CategoryBuilder toBuilder() {
        return new CategoryBuilder(this);
    }

    public static CategoryBuilder builder() {
        return new CategoryBuilder();
    }

    public static class CategoryBuilder {
        private Long id;
        private String name;
        private String slug;
        private Long parentId;
        private boolean active;

        public CategoryBuilder() {
        }

        public CategoryBuilder(Category category) {
            this.id = category.id;
            this.name = category.name;
            this.slug = category.slug;
            this.parentId = category.parentId;
            this.active = category.active;
        }

        public CategoryBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public CategoryBuilder name(String name) {
            this.name = name;
            return this;
        }

        public CategoryBuilder slug(String slug) {
            this.slug = slug;
            return this;
        }

        public CategoryBuilder parentId(Long parentId) {
            this.parentId = parentId;
            return this;
        }

        public CategoryBuilder active(boolean active) {
            this.active = active;
            return this;
        }

        public Category build() {
            return new Category(id, name, slug, parentId, active);
        }
    }
}
