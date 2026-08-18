package sh.roadmap.sep.productcatalog.domain.exception;

import java.util.UUID;

public class ProductAlreadyExistsException extends RuntimeException {
    private static final String ERROR_MESSAGE = "Product with %s is already exists";

    public ProductAlreadyExistsException(UUID productId) {
        super(String.format(ERROR_MESSAGE, "id: ".concat(productId.toString())));
    }

    public ProductAlreadyExistsException(String sku) {
        super(String.format(ERROR_MESSAGE, "sku: ".concat(sku)));
    }
}
