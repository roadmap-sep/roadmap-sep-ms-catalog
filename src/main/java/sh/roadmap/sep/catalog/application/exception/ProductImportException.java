package sh.roadmap.sep.catalog.application.exception;

import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

@Getter
public class ProductImportException extends RuntimeException {
    private static final String ERROR_MESSAGE = "Error importing the product from the %s file: %s";
    private final List<String> violations;

    public ProductImportException(List<String> violations) {
        super(String.format(ERROR_MESSAGE, StringUtils.EMPTY,
                "The following sku list already exist: " + violations));
        this.violations = violations;
    }

    public ProductImportException(String fileName, List<String> violations) {
        super(String.format(ERROR_MESSAGE, fileName, violations.toString()));
        this.violations = violations;
    }

    public ProductImportException(String fileName) {
        throw new ProductImportException(fileName, List.of("The file is empty"));
    }
}
