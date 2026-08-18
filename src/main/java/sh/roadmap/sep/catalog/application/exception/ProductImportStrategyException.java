package sh.roadmap.sep.catalog.application.exception;

public class ProductImportStrategyException extends RuntimeException {
    private static final String ERROR_MESSAGE = "The file whit extension: %s is not supported";

    public ProductImportStrategyException(String fileExtension) {
        super(String.format(ERROR_MESSAGE, fileExtension));
    }
}
