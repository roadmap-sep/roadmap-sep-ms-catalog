package sh.roadmap.sep.catalog.application.strategy.impl;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.read.listener.ReadListener;
import com.alibaba.excel.support.ExcelTypeEnum;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
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
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class ExcelProductImportStrategy implements ProductImportStrategy {
    private static final Pattern CATEGORY_SPLITTER = Pattern.compile("\\D+");
    private final Validator validator;
    private final CategoryPortIn categoryPortIn;
    private static final Set<String> SUPPORTED_EXTENSIONS;

    static {
        SUPPORTED_EXTENSIONS = Arrays.stream(ExcelTypeEnum.values())
                .map(ExcelTypeEnum::getValue)
                .map(extension -> extension.substring(1))
                .map(String::toLowerCase)
                .collect(Collectors.toSet());
        SUPPORTED_EXTENSIONS.add("xlsm");
    }

    @Override
    public boolean supports(String fileExtension) {
        return fileExtension != null && SUPPORTED_EXTENSIONS.contains(fileExtension.toLowerCase().trim());
    }

    @Override
    public List<ProductRequest> process(ProductImportRequest file) {
        List<ProductRequest> productsDto = new ArrayList<>();
        List<String> violations = new ArrayList<>();

        try (InputStream inputStream = file.inputStream()) {
            if (inputStream.available() == 0) {
                throw new ProductImportException(file.fileName());
            }

            ProductExcelListener listener = new ProductExcelListener(validator, categoryPortIn, productsDto, violations);

            EasyExcel.read(inputStream, ProductExcelDto.class, listener)
                    .sheet()
                    .headRowNumber(1)
                    .doRead();
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

    @Getter
    @Setter
    @NoArgsConstructor
    public static class ProductExcelDto {
        @ExcelProperty(index = 0)
        private String sku;

        @ExcelProperty(index = 1)
        private String name;

        @ExcelProperty(index = 2)
        private String description;

        @ExcelProperty(index = 3)
        private String categories;

        @ExcelProperty(index = 4)
        private String price;

        @ExcelProperty(index = 5)
        private String stock;

        @ExcelProperty(index = 6)
        private String mainImageUrl;

        @ExcelProperty(index = 7)
        private String weight;
    }

    @RequiredArgsConstructor
    private static class ProductExcelListener implements ReadListener<ProductExcelDto> {
        private static final String VIOLATION_MESSAGE_TEMPLATE = "Row: %s  { %s }";
        private final Validator validator;
        private final CategoryPortIn categoryPortIn;
        private final List<ProductRequest> productsDto;
        private final List<String> violations;
        private final Set<Long> validCategoriesCache = new HashSet<>();
        private final Set<Long> invalidCategoriesCache = new HashSet<>();

        @Override
        public void invoke(ProductExcelDto data, AnalysisContext context) {
            int rowNumber = context.readRowHolder().getRowIndex() + 1;
            Set<Long> categorySet = new HashSet<>();
            if (data.getCategories() != null && !data.getCategories().isBlank()) {
                try {
                    categorySet = Arrays.stream(CATEGORY_SPLITTER.split(data.getCategories()))
                            .map(String::trim)
                            .map(Long::parseLong)
                            .collect(Collectors.toSet());
                } catch (NumberFormatException e) {
                    violations.add(String.format(VIOLATION_MESSAGE_TEMPLATE, rowNumber,
                            "Only numbers are allowed in the categories Id column"));
                }
            }
            ProductRequest.ProductRequestBuilder builder = ProductRequest.builder();

            builder.sku(data.getSku())
                    .name(data.getName())
                    .description(data.getDescription())
                    .categoryIds(categorySet)
                    .mainImageUrl(data.getMainImageUrl());
            try {
                builder
                        .price(validateString(data.getPrice())
                                ? new BigDecimal(data.getPrice().replaceAll("[^0-9.-]", ""))
                                : null)
                        .stock(validateString(data.getStock())
                                ? Integer.valueOf(data.getStock().replaceAll("[^0-9-]", ""))
                                : null)
                        .weight(validateString(data.getWeight())
                                ? Double.valueOf(data.getWeight().replaceAll("[^0-9.-]", ""))
                                : null);
            } catch (NumberFormatException e) {
                violations.add(String.format(VIOLATION_MESSAGE_TEMPLATE, rowNumber, "Invalid numeric format"));
            }
            ProductRequest dto = builder.build();
            validateCategories(dto, rowNumber).ifPresent(violations::add);
            validateDto(dto, rowNumber).ifPresent(violations::add);
            productsDto.add(dto);
        }

        @Override
        public void doAfterAllAnalysed(AnalysisContext context) {
        }

        private boolean validateString(String str) {
            return str != null && !str.isBlank();
        }

        private Optional<String> validateDto(ProductRequest dto, int rowNumber) {
            Set<ConstraintViolation<ProductRequest>> constraintViolations = validator.validate(dto);
            if (!constraintViolations.isEmpty()) {
                return constraintViolations.stream()
                        .map(violation ->
                                String.format("'%s': %s", violation.getPropertyPath(), violation.getMessage()))
                        .collect(Collectors.collectingAndThen(
                                Collectors.joining(" | ", "Row: " + rowNumber + " {", "}"),
                                Optional::of));
            }
            return Optional.empty();
        }

        private Optional<String> validateCategories(ProductRequest dto, int rowNumber) {
            return dto.categoryIds().stream()
                    .map(categoryId -> resolveCategoryValidation(categoryId, rowNumber))
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .findFirst();
        }

        private Optional<String> resolveCategoryValidation(Long categoryId, int rowNumber) {
            if (validCategoriesCache.contains(categoryId)) {
                return Optional.empty();
            }

            if (invalidCategoriesCache.contains(categoryId)) {
                return Optional.of(String.format(VIOLATION_MESSAGE_TEMPLATE, rowNumber,
                        String.format("Category with id: %d not found", categoryId)));
            }
            try {
                categoryPortIn.getById(categoryId);
                validCategoriesCache.add(categoryId);
                return Optional.empty();
            } catch (CategoryNotFoundException e) {
                invalidCategoriesCache.add(categoryId);
                return Optional.of(String.format(VIOLATION_MESSAGE_TEMPLATE, rowNumber, e.getMessage()));
            }
        }
    }
}
