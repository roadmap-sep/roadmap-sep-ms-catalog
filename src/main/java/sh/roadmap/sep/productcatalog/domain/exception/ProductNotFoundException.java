package sh.roadmap.sep.productcatalog.domain.exception;

import java.util.UUID;

public class ProductNotFoundException extends RuntimeException {
    private static final String ERROR_MESSAGE = "Product with %s not found";

    public ProductNotFoundException(UUID productId) {
        super(String.format(ERROR_MESSAGE, "id: ".concat(productId.toString())));
    }

    public ProductNotFoundException(String sku) {
        super(String.format(ERROR_MESSAGE, "sku: ".concat(sku)));
    }
}
