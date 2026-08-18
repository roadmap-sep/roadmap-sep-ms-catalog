package sh.roadmap.sep.catalog.application.exception;

import lombok.Getter;

import java.util.List;

@Getter
public class ProductImportException extends RuntimeException {
    private static final String ERROR_MESSAGE = "Error importing the product from the %s file: %s";
    private static final String FILE_EMPTY = "The file is empty";

    private final List<String> violations;

    public ProductImportException(String fileName, List<String> violations) {
        super(String.format(ERROR_MESSAGE, fileName, violations.toString()));
        this.violations = violations;
    }

    public ProductImportException(List<String> violations) {
        throw new ProductImportException("", violations);
    }

    public ProductImportException(String fileName) {
        throw new ProductImportException(fileName, List.of(FILE_EMPTY));
    }
}
