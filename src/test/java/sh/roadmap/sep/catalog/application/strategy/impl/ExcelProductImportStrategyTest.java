package sh.roadmap.sep.catalog.application.strategy.impl;

import com.alibaba.excel.EasyExcelFactory;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.read.builder.ExcelReaderBuilder;
import com.alibaba.excel.read.builder.ExcelReaderSheetBuilder;
import com.alibaba.excel.read.listener.ReadListener;
import com.alibaba.excel.read.metadata.holder.ReadRowHolder;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Path;
import jakarta.validation.Validator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import sh.roadmap.sep.catalog.application.dto.request.ProductImportRequest;
import sh.roadmap.sep.catalog.application.dto.request.ProductRequest;
import sh.roadmap.sep.catalog.application.exception.ProductImportException;
import sh.roadmap.sep.catalog.domain.exception.CategoryNotFoundException;
import sh.roadmap.sep.catalog.domain.model.Category;
import sh.roadmap.sep.catalog.domain.port.in.CategoryPortIn;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ExcelProductImportStrategyTest {

    @Mock
    private Validator validator;

    @Mock
    private CategoryPortIn categoryPortIn;

    @InjectMocks
    private ExcelProductImportStrategy strategy;

    @Nested
    @DisplayName("supports() tests")
    class SupportsTests {

        @Test
        @DisplayName("Should return true for valid Excel file extensions")
        void shouldSupportValidExtensions() {
            assertThat(strategy.supports("xlsx")).isTrue();
            assertThat(strategy.supports("XLS")).isTrue();
            assertThat(strategy.supports("xlsm")).isTrue();
            assertThat(strategy.supports(" xlsx ")).isTrue();
        }

        @Test
        @DisplayName("Should return false for invalid extensions or null")
        void shouldNotSupportInvalidExtensions() {
            assertThat(strategy.supports("ods")).isFalse();
            assertThat(strategy.supports("txt")).isFalse();
            assertThat(strategy.supports(null)).isFalse();
        }
    }

    @Nested
    @DisplayName("process() and ProductExcelListener tests")
    class ProcessTests {

        @Test
        @DisplayName("Should throw exception when InputStream is empty")
        void shouldThrowExceptionWhenInputStreamIsEmpty() throws IOException {
            InputStream inputStream = mock(InputStream.class);
            given(inputStream.available()).willReturn(0);
            ProductImportRequest request = new ProductImportRequest("xlsx", "empty.xlsx", inputStream);

            assertThatThrownBy(() -> strategy.process(request))
                    .isInstanceOf(ProductImportException.class)
                    .hasMessageContaining("empty.xlsx")
                    .hasMessageContaining("The file is empty");
        }

        @Test
        @DisplayName("Should throw exception if reading ends with an empty product list")
        void shouldThrowExceptionWhenNoProductsExtracted() throws IOException {
            InputStream inputStream = mock(InputStream.class);
            given(inputStream.available()).willReturn(10);
            ProductImportRequest request = new ProductImportRequest("xlsx", "file.xlsx", inputStream);

            try (MockedStatic<EasyExcelFactory> easyExcelMock = mockStatic(EasyExcelFactory.class)) {
                mockEasyExcel(easyExcelMock, listener -> {
                });

                assertThatThrownBy(() -> strategy.process(request))
                        .isInstanceOf(ProductImportException.class)
                        .hasMessageContaining("The file is empty");
            }
        }

        @Test
        @DisplayName("Should process valid rows perfectly and hit Category Port caches (Valid Cache)")
        void shouldProcessValidProductsAndUseCategoryCache() throws IOException {
            InputStream inputStream = mock(InputStream.class);
            given(inputStream.available()).willReturn(10);
            ProductImportRequest request = new ProductImportRequest("xlsx", "file.xlsx", inputStream);

            AnalysisContext contextMock = mock(AnalysisContext.class);
            ReadRowHolder rowHolderMock = mock(ReadRowHolder.class);
            given(contextMock.readRowHolder()).willReturn(rowHolderMock);
            given(rowHolderMock.getRowIndex()).willReturn(0, 1); // Fila 1 y Fila 2

            given(validator.validate(any())).willReturn(Collections.emptySet());
            given(categoryPortIn.getById(1L)).willReturn(mock(Category.class));

            try (MockedStatic<EasyExcelFactory> easyExcelMock = mockStatic(EasyExcelFactory.class)) {
                mockEasyExcel(easyExcelMock, listener -> {
                    listener.invoke(createDto("SKU-1", "1", "$100.50", "10 units", "1.5 kg"), contextMock);
                    listener.invoke(createDto("SKU-2", "1", "200", "5", "2.0"), contextMock);
                    listener.doAfterAllAnalysed(contextMock);
                });

                List<ProductRequest> result = strategy.process(request);

                assertThat(result).hasSize(2);
                assertThat(result.getFirst().sku()).isEqualTo("SKU-1");
                assertThat(result.getFirst().price()).isEqualTo(new BigDecimal("100.50"));
                assertThat(result.getFirst().stock()).isEqualTo(10);
                assertThat(result.getFirst().weight()).isEqualTo(1.5);

                verify(categoryPortIn, times(1)).getById(1L);
            }
        }

        @Test
        @DisplayName("Should accumulate multiple violations: DTO constraints, invalid caches, and NumberFormats")
        @SuppressWarnings("unchecked")
        void shouldAccumulateViolationsForInvalidDataAndCaches() throws IOException {
            InputStream inputStream = mock(InputStream.class);
            given(inputStream.available()).willReturn(10);
            ProductImportRequest request = new ProductImportRequest("xlsx", "errors.xlsx", inputStream);

            AnalysisContext contextMock = mock(AnalysisContext.class);
            ReadRowHolder rowHolderMock = mock(ReadRowHolder.class);
            given(contextMock.readRowHolder()).willReturn(rowHolderMock);
            given(rowHolderMock.getRowIndex()).willReturn(0, 1, 2);

            ConstraintViolation<Object> violationMock = mock(ConstraintViolation.class);
            Path pathMock = mock(Path.class);
            given(pathMock.toString()).willReturn("sku");
            given(violationMock.getPropertyPath()).willReturn(pathMock);
            given(violationMock.getMessage()).willReturn("must not be blank");

            given(validator.validate(any())).willReturn(Set.of(violationMock),
                    Collections.emptySet(), Collections.emptySet());

            // Mock CategoryNotFound para la ID 99
            given(categoryPortIn.getById(99L)).willThrow(new CategoryNotFoundException(99L));

            try (MockedStatic<EasyExcelFactory> easyExcelMock = mockStatic(EasyExcelFactory.class)) {
                mockEasyExcel(easyExcelMock, listener -> {
                    listener.invoke(createDto("", "99", "10", "10", "1.0"), contextMock);

                    listener.invoke(createDto("SKU-2", "99", "10", "10", "1.0"), contextMock);

                    listener.invoke(createDto("SKU-3", "123", "abc", "xyz", "abc"), contextMock);
                });

                assertThatThrownBy(() -> strategy.process(request))
                        .isInstanceOf(ProductImportException.class)
                        .satisfies(e -> {
                            ProductImportException pie = (ProductImportException) e;
                            List<String> violations = pie.getViolations();
                            assertThat(violations).hasSizeGreaterThan(0);

                            String errors = violations.toString();
                            assertThat(errors).contains("must not be blank");
                            assertThat(errors).contains("Category with id: 99 not found");
                            assertThat(errors).contains("Invalid numeric format");
                        });

                verify(categoryPortIn, times(1)).getById(99L);
            }
        }
    }

    private void mockEasyExcel(MockedStatic<EasyExcelFactory> easyExcelMock,
                               Consumer<ReadListener<ExcelProductImportStrategy.ProductExcelDto>> listenerAction) {
        ExcelReaderBuilder readerBuilderMock = mock(ExcelReaderBuilder.class);
        ExcelReaderSheetBuilder sheetBuilderMock = mock(ExcelReaderSheetBuilder.class);

        easyExcelMock.when(() -> EasyExcelFactory.read(any(InputStream.class),
                        eq(ExcelProductImportStrategy.ProductExcelDto.class),
                        any(ReadListener.class)))
                .thenAnswer(invocation -> {
                    ReadListener<ExcelProductImportStrategy.ProductExcelDto> listener = invocation.getArgument(2);

                    doAnswer(i -> {
                        listenerAction.accept(listener);
                        return null;
                    }).when(sheetBuilderMock).doRead();

                    return readerBuilderMock;
                });

        given(readerBuilderMock.sheet()).willReturn(sheetBuilderMock);
        given(sheetBuilderMock.headRowNumber(1)).willReturn(sheetBuilderMock);
    }

    private ExcelProductImportStrategy.ProductExcelDto createDto(
            String sku, String cat, String price, String stock, String weight) {
        ExcelProductImportStrategy.ProductExcelDto dto = new ExcelProductImportStrategy.ProductExcelDto();
        dto.setSku(sku);
        dto.setName("Name");
        dto.setDescription("Desc");
        dto.setCategories(cat);
        dto.setPrice(price);
        dto.setStock(stock);
        dto.setWeight(weight);
        dto.setMainImageUrl("http://image.url");
        return dto;
    }
}