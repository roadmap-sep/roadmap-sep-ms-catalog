package sh.roadmap.sep.catalog.domain.exception;

public class CategoryAlreadyExistsException extends RuntimeException {
    private static final String ERROR_MESSAGE = "Product with %s is already exists";

    public CategoryAlreadyExistsException(String slug) {
        super(String.format(ERROR_MESSAGE, "slug: ".concat(slug)));
    }

    public CategoryAlreadyExistsException(Long id) {
        super(String.format(ERROR_MESSAGE, "id: ".concat(id.toString())));
    }
}
