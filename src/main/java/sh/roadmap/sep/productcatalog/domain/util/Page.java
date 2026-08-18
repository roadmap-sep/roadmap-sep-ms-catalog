package sh.roadmap.sep.productcatalog.domain.util;


import java.util.List;

public record Page<T>(
        List<T> data,
        int pageNumber,
        int pageSize,
        long totalElements,
        int totalPages,
        boolean hasNext) {
    public <U> PageBuilder<U> toBuilder() {
        return new PageBuilder<U>()
                .pageNumber(this.pageNumber)
                .pageSize(this.pageSize)
                .totalElements(this.totalElements)
                .totalPages(this.totalPages)
                .hasNext(this.hasNext);
    }

    public record Request(int pageNumber, int pageSize) {
    }

    public static <T> PageBuilder<T> builder() {
        return new PageBuilder<>();
    }

    public static class PageBuilder<T> {
        private List<T> data;
        private int pageNumber;
        private int pageSize;
        private long totalElements;
        private int totalPages;
        private boolean hasNext;

        public PageBuilder<T> data(List<T> data) {
            this.data = data;
            return this;
        }

        public PageBuilder<T> pageNumber(int pageNumber) {
            this.pageNumber = pageNumber;
            return this;
        }

        public PageBuilder<T> pageSize(int pageSize) {
            this.pageSize = pageSize;
            return this;
        }

        public PageBuilder<T> totalElements(long totalElements) {
            this.totalElements = totalElements;
            return this;
        }

        public PageBuilder<T> totalPages(int totalPages) {
            this.totalPages = totalPages;
            return this;
        }

        public PageBuilder<T> hasNext(boolean hasNext) {
            this.hasNext = hasNext;
            return this;
        }

        public Page<T> build() {
            return new Page<>(this.data, this.pageNumber, this.pageSize,
                    this.totalElements, this.totalPages, this.hasNext);
        }
    }
}