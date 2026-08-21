package sh.roadmap.sep.catalog.application.strategy.impl;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Path;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.odftoolkit.odfdom.doc.OdfSpreadsheetDocument;
import org.odftoolkit.odfdom.doc.table.OdfTable;
import org.odftoolkit.odfdom.doc.table.OdfTableRow;
import sh.roadmap.sep.catalog.application.dto.request.ProductImportRequest;
import sh.roadmap.sep.catalog.application.dto.request.ProductRequest;
import sh.roadmap.sep.catalog.application.exception.ProductImportException;
import sh.roadmap.sep.catalog.domain.exception.CategoryNotFoundException;
import sh.roadmap.sep.catalog.domain.model.Category;
import sh.roadmap.sep.catalog.domain.port.in.CategoryPortIn;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
@DisplayName("OdsProductImportStrategy Unit Tests")
class OdsProductImportStrategyTest {

    @Mock
    private Validator validator;

    @Mock
    private CategoryPortIn categoryPortIn;

    @InjectMocks
    private OdsProductImportStrategy strategy;

    @Nested
    @DisplayName("supports() Tests")
    class SupportsTests {

        @Test
        @DisplayName("Should return true when file extension is valid ODS or OTS")
        void shouldReturnTrueForValidExtensions() {
            assertThat(strategy.supports("ods")).isTrue();
            assertThat(strategy.supports("ots")).isTrue();
            assertThat(strategy.supports(" ODS ")).isTrue();
        }

        @Test
        @DisplayName("Should return false when file extension is invalid or null")
        void shouldReturnFalseForInvalidExtensions() {
            assertThat(strategy.supports("xlsx")).isFalse();
            assertThat(strategy.supports("csv")).isFalse();
            assertThat(strategy.supports("")).isFalse();
            assertThat(strategy.supports(null)).isFalse();
        }
    }

    @Nested
    @DisplayName("process() File Stream Validation Tests")
    class FileValidationTests {

        @Test
        @DisplayName("Should throw ProductImportException when InputStream is empty")
        void shouldThrowExceptionWhenInputStreamIsEmpty() throws Exception {
            InputStream emptyStream = mock(InputStream.class);
            given(emptyStream.available()).willReturn(0);

            ProductImportRequest fileRequest = new ProductImportRequest("ods",
                    "empty.ods", emptyStream);

            assertThatThrownBy(() -> strategy.process(fileRequest))
                    .isInstanceOf(ProductImportException.class);
        }

        @Test
        @DisplayName("Should throw ProductImportException when file is corrupted or invalid ODS format")
        void shouldThrowExceptionWhenFileIsCorrupted() {
            InputStream invalidStream = new ByteArrayInputStream("invalid content".getBytes());
            ProductImportRequest fileRequest = new ProductImportRequest("ods",
                    "corrupt.ods", invalidStream);

            assertThatThrownBy(() -> strategy.process(fileRequest))
                    .isInstanceOf(ProductImportException.class);
        }

        @Test
        @DisplayName("Should throw ProductImportException when file has no data rows")
        void shouldThrowExceptionWhenFileHasOnlyHeaders() throws Exception {
            List<List<String>> rows = List.of(
                    List.of("SKU", "Name", "Description", "Categories", "Price", "Stock", "Image", "Weight")
            );
            InputStream inputStream = createOdsInputStream(rows);
            ProductImportRequest fileRequest = new ProductImportRequest("ods",
                    "empty_data.ods", inputStream);

            assertThatThrownBy(() -> strategy.process(fileRequest))
                    .isInstanceOf(ProductImportException.class);
        }
    }

    @Nested
    @DisplayName("process() Data Processing & Private Method Coverage")
    class ProcessingDataTests {

        @BeforeEach
        void setupDefaultValidator() {
            given(validator.validate(any())).willReturn(Collections.emptySet());
        }

        @Test
        @DisplayName("Should successfully process valid file and populate DTOs")
        void shouldProcessValidOdsFileSuccessfully() throws Exception {
            List<List<String>> rows = List.of(
                    List.of("SKU", "Name", "Description", "Categories", "Price", "Stock", "Image", "Weight"),
                    List.of("SKU-100", "Laptop", "Gaming Laptop", "1, 2", "$1,200.50", "15", "http://img.png", "2.5")
            );

            InputStream inputStream = createOdsInputStream(rows);
            ProductImportRequest fileRequest = new ProductImportRequest("ods",
                    "products.ods", inputStream);

            List<ProductRequest> result = strategy.process(fileRequest);

            assertThat(result).hasSize(1);
            ProductRequest dto = result.getFirst();
            assertThat(dto.sku()).isEqualTo("SKU-100");
            assertThat(dto.name()).isEqualTo("Laptop");
            assertThat(dto.categoryIds()).containsExactlyInAnyOrder(1L, 2L);
            assertThat(dto.price()).isEqualTo(new BigDecimal("1200.50"));
            assertThat(dto.stock()).isEqualTo(15);
            assertThat(dto.weight()).isEqualTo(2.5);

            then(categoryPortIn).should(times(1)).getById(1L);
            then(categoryPortIn).should(times(1)).getById(2L);
        }

        @Test
        @DisplayName("Should stop parsing after encountering more than 10 consecutive empty rows")
        void shouldBreakLoopWhenExceedingMaxConsecutiveEmptyRows() throws Exception {
            List<List<String>> rows = List.of(
                    List.of("SKU", "Name", "Description", "Categories", "Price", "Stock", "Image", "Weight"),
                    List.of("SKU-1", "Prod 1", "Desc 1", "10", "100", "5", "url", "1.0"),
                    // 11 Empty Rows
                    List.of("", "", "", "", "", "", "", ""),
                    List.of("", "", "", "", "", "", "", ""),
                    List.of("", "", "", "", "", "", "", ""),
                    List.of("", "", "", "", "", "", "", ""),
                    List.of("", "", "", "", "", "", "", ""),
                    List.of("", "", "", "", "", "", "", ""),
                    List.of("", "", "", "", "", "", "", ""),
                    List.of("", "", "", "", "", "", "", ""),
                    List.of("", "", "", "", "", "", "", ""),
                    List.of("", "", "", "", "", "", "", ""),
                    List.of("", "", "", "", "", "", "", ""),
                    // This row should be skipped due to break
                    List.of("SKU-99", "Prod 99", "Desc 99", "10", "100", "5", "url", "1.0")
            );

            InputStream inputStream = createOdsInputStream(rows);
            ProductImportRequest fileRequest = new ProductImportRequest("ods",
                    "products.ods", inputStream);

            List<ProductRequest> result = strategy.process(fileRequest);

            assertThat(result).hasSize(1);
            assertThat(result.getFirst().sku()).isEqualTo("SKU-1");
        }

        @Test
        @DisplayName("Should leverage category caching (valid & invalid caches) and record category violations")
        void shouldUtilizeCategoryCachingForValidAndInvalidCategories() throws Exception {
            given(categoryPortIn.getById(99L)).willThrow(new CategoryNotFoundException(99L));
            given(categoryPortIn.getById(1L)).willReturn(mock(Category.class));

            List<List<String>> rows = List.of(
                    List.of("SKU", "Name", "Description", "Categories", "Price", "Stock", "Image", "Weight"),
                    List.of("SKU-1", "Prod 1", "Desc", "1, 99", "10.00", "1", "url", "1.0"),
                    List.of("SKU-2", "Prod 2", "Desc", "1, 99", "20.00", "2", "url", "2.0")
            );

            InputStream inputStream = createOdsInputStream(rows);
            ProductImportRequest fileRequest = new ProductImportRequest("ods",
                    "products.ods", inputStream);

            assertThatThrownBy(() -> strategy.process(fileRequest))
                    .isInstanceOf(ProductImportException.class);

            then(categoryPortIn).should(times(1)).getById(1L);
            then(categoryPortIn).should(times(1)).getById(99L);
        }

        @Test
        @DisplayName("Should collect violations for numeric conversion and DTO bean validation failures")
        @SuppressWarnings("unchecked")
        void shouldCollectViolationsOnInvalidNumericFormatsAndBeanValidationErrors() throws Exception {
            ConstraintViolation<ProductRequest> mockViolation = mock(ConstraintViolation.class);
            Path mockPath = mock(Path.class);
            given(mockPath.toString()).willReturn("name");
            given(mockViolation.getPropertyPath()).willReturn(mockPath);
            given(mockViolation.getMessage()).willReturn("must not be blank");

            given(validator.validate(any(ProductRequest.class)))
                    .willReturn(Set.of(mockViolation));

            List<List<String>> rows = List.of(
                    List.of("SKU", "Name", "Description", "Categories", "Price", "Stock", "Image", "Weight"),
                    List.of("SKU-1", "", "Desc", "abc-invalid-cat", "invalid-price", "invalid-stock", "url",
                            "invalid-weight")
            );

            InputStream inputStream = createOdsInputStream(rows);
            ProductImportRequest fileRequest = new ProductImportRequest("ods",
                    "products.ods", inputStream);

            assertThatThrownBy(() -> strategy.process(fileRequest))
                    .isInstanceOf(ProductImportException.class);

            then(validator).should(atLeastOnce()).validate(any());
        }
    }

    private InputStream createOdsInputStream(List<List<String>> rowData) throws Exception {
        OdfSpreadsheetDocument doc = OdfSpreadsheetDocument.newSpreadsheetDocument();
        OdfTable table = doc.getSpreadsheetTables().getFirst();

        for (int r = 0; r < rowData.size(); r++) {
            List<String> rowValues = rowData.get(r);
            OdfTableRow row = table.getRowByIndex(r);
            for (int c = 0; c < rowValues.size(); c++) {
                String val = rowValues.get(c);
                if (val != null && !val.isEmpty()) {
                    row.getCellByIndex(c).setStringValue(val);
                }
            }
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        doc.save(out);
        return new ByteArrayInputStream(out.toByteArray());
    }
}