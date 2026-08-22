package sh.roadmap.sep.catalog.application.strategy.impl;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.odftoolkit.odfdom.doc.OdfDocument;
import org.odftoolkit.odfdom.doc.OdfSpreadsheetDocument;
import org.odftoolkit.odfdom.doc.table.OdfTable;
import org.odftoolkit.odfdom.doc.table.OdfTableCell;
import org.odftoolkit.odfdom.doc.table.OdfTableRow;
import org.springframework.stereotype.Component;
import sh.roadmap.sep.catalog.application.dto.request.ProductImportRequest;
import sh.roadmap.sep.catalog.application.dto.request.ProductRequest;
import sh.roadmap.sep.catalog.application.exception.ProductImportException;
import sh.roadmap.sep.catalog.application.strategy.ProductImportStrategy;
import sh.roadmap.sep.catalog.domain.exception.CategoryNotFoundException;
import sh.roadmap.sep.catalog.domain.port.in.CategoryPortIn;

import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class OdsProductImportStrategy implements ProductImportStrategy {
    private static final Pattern CATEGORY_SPLITTER = Pattern.compile("\\D+");
    private static final String VIOLATION_MESSAGE_TEMPLATE = "Row: %s  { %s }";
    private final Validator validator;
    private final CategoryPortIn categoryPortIn;

    @Override
    public boolean supports(String fileExtension) {
        return fileExtension != null
                && (OdfDocument.OdfMediaType.SPREADSHEET.getSuffix().equalsIgnoreCase(fileExtension.trim())
                || OdfDocument.OdfMediaType.SPREADSHEET_TEMPLATE.getSuffix().equalsIgnoreCase(fileExtension.trim()));
    }

    @Override
    public List<ProductRequest> process(ProductImportRequest file) {
        List<ProductRequest> productsDto = new ArrayList<>();
        List<String> violations = new ArrayList<>();

        Set<Long> validCategoriesCache = new HashSet<>();
        Set<Long> invalidCategoriesCache = new HashSet<>();
        try (InputStream inputStream = file.inputStream()) {
            if (inputStream.available() == 0) {
                throw new ProductImportException(file.fileName());
            }

            try (OdfSpreadsheetDocument document = OdfSpreadsheetDocument.loadDocument(inputStream)) {
                List<OdfTable> tables = document.getSpreadsheetTables();
                if (tables == null || tables.isEmpty()) {
                    throw new ProductImportException(file.fileName());
                }

                OdfTable table = tables.getFirst();
                int rowCount = table.getRowCount();
                int emptyRowCount = 0;

                for (int i = 1; i < rowCount; i++) {
                    OdfTableRow row = table.getRowByIndex(i);

                    if (isRowEmpty(row)) {
                        emptyRowCount++;
                        if (emptyRowCount > 10) {
                            break;
                        }
                        continue;
                    }
                    emptyRowCount = 0;

                    int rowNumber = i + 1;

                    String sku = getCellValue(row.getCellByIndex(0));
                    String name = getCellValue(row.getCellByIndex(1));
                    String description = getCellValue(row.getCellByIndex(2));
                    String categoriesStr = getCellValue(row.getCellByIndex(3));
                    String priceStr = getCellValue(row.getCellByIndex(4));
                    String stockStr = getCellValue(row.getCellByIndex(5));
                    String mainImageUrl = getCellValue(row.getCellByIndex(6));
                    String weightStr = getCellValue(row.getCellByIndex(7));

                    Set<Long> categorySet = new HashSet<>();
                    if (categoriesStr != null && !categoriesStr.isBlank()) {
                        try {
                            categorySet = Arrays.stream(CATEGORY_SPLITTER.split(categoriesStr))
                                    .map(String::trim)
                                    .map(Long::parseLong)
                                    .collect(Collectors.toSet());
                        } catch (NumberFormatException e) {
                            violations.add(String.format(VIOLATION_MESSAGE_TEMPLATE, rowNumber,
                                    "Only numbers are allowed in the categories Id column"));
                        }
                    }

                    ProductRequest.ProductRequestBuilder builder = ProductRequest.builder()
                            .sku(sku)
                            .name(name)
                            .description(description)
                            .categoryIds(categorySet)
                            .mainImageUrl(mainImageUrl);

                    try {
                        builder.price(validateString(priceStr)
                                        ? new BigDecimal(priceStr.replaceAll("[^0-9.-]", "")) : null)
                                .stock(validateString(stockStr)
                                        ? Integer.valueOf(stockStr.replaceAll("[^0-9-]", "")) : null)
                                .weight(validateString(weightStr)
                                        ? Double.valueOf(weightStr.replaceAll("[^0-9.-]", "")) : null);
                    } catch (NumberFormatException e) {
                        violations.add(String.format(VIOLATION_MESSAGE_TEMPLATE, rowNumber, "Invalid numeric format"));
                    }

                    ProductRequest dto = builder.build();

                    validateCategories(dto, rowNumber, violations, validCategoriesCache, invalidCategoriesCache);
                    validateDto(dto, rowNumber, violations);

                    productsDto.add(dto);
                }
            }
        } catch (Exception e) {
            if (e instanceof ProductImportException) {
                throw (ProductImportException) e;
            }
            log.error(e.getMessage(), e);
            violations.add("The information could not be extracted from the file; "
                    + "please ensure it is in the correct format.");
            throw new ProductImportException(file.fileName(), violations);
        }

        if (productsDto.isEmpty()) {
            throw new ProductImportException(file.fileName());
        }
        if (!violations.isEmpty()) {
            throw new ProductImportException(file.fileName(), violations);
        }

        return productsDto;
    }

    private boolean isRowEmpty(OdfTableRow row) {
        for (int i = 0; i <= 7; i++) {
            String value = getCellValue(row.getCellByIndex(i));
            if (validateString(value)) {
                return false;
            }
        }
        return true;
    }

    private String getCellValue(OdfTableCell cell) {
        if (cell == null || cell.getOdfElement() == null) {
            return null;
        }
        String content = cell.getDisplayText();
        return validateString(content) ? content.trim() : null;
    }

    private boolean validateString(String str) {
        return str != null && !str.isBlank();
    }

    private void validateDto(ProductRequest dto, int rowNumber, List<String> violations) {
        Set<ConstraintViolation<ProductRequest>> constraintViolations = validator.validate(dto);
        if (!constraintViolations.isEmpty()) {
            String validationErrors = constraintViolations.stream()
                    .map(violation ->
                            String.format("'%s': %s", violation.getPropertyPath(), violation.getMessage()))
                    .collect(Collectors.joining(" | ", "Row: " + rowNumber + " {", "}"));
            violations.add(validationErrors);
        }
    }

    private void validateCategories(ProductRequest dto, int rowNumber, List<String> violations,
                                    Set<Long> validCache, Set<Long> invalidCache) {
        for (Long categoryId : dto.categoryIds()) {
            if (validCache.contains(categoryId)) {
                continue;
            }

            if (invalidCache.contains(categoryId)) {
                violations.add(String.format(VIOLATION_MESSAGE_TEMPLATE, rowNumber,
                        String.format("Category with id: %d not found", categoryId)));
                continue;
            }

            try {
                categoryPortIn.getById(categoryId);
                validCache.add(categoryId);
            } catch (CategoryNotFoundException e) {
                invalidCache.add(categoryId);
                violations.add(String.format(VIOLATION_MESSAGE_TEMPLATE, rowNumber, e.getMessage()));
            }
        }
    }
}
