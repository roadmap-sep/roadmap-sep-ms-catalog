package sh.roadmap.sep.productcatalog.domain.exception;

public class CategoryNotFoundException extends RuntimeException {
    private static final String ERROR_MESSAGE = "Category with id: %d not found";

    public CategoryNotFoundException(long categoryId) {
        super(String.format(ERROR_MESSAGE, categoryId));
    }
}
