package sh.roadmap.sep.catalog.application.strategy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sh.roadmap.sep.catalog.application.exception.ProductImportStrategyException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class ProductImportStrategyResolverImplTest {

    @Mock
    private ProductImportStrategy csvStrategy;

    @Mock
    private ProductImportStrategy excelStrategy;

    @Nested
    @DisplayName("resolve() tests")
    class ResolveTests {

        @Test
        @DisplayName("Should return the correct strategy that supports the given file extension")
        void shouldReturnMatchingStrategy() {
            String extension = "";
            given(excelStrategy.supports(extension)).willReturn(false);
            given(csvStrategy.supports(extension)).willReturn(true);

            var resolver = new ProductImportStrategyResolverImpl(List.of(excelStrategy, csvStrategy));

            var result = resolver.resolve(extension);

            assertThat(result).isEqualTo(csvStrategy);

            then(excelStrategy).should().supports(extension);
            then(csvStrategy).should().supports(extension);
        }

        @Test
        @DisplayName("Should short-circuit and return the first matching strategy")
        void shouldReturnFirstMatchingStrategy() {
            String extension = "csv";

            given(excelStrategy.supports(extension)).willReturn(true);

            var resolver = new ProductImportStrategyResolverImpl(List.of(excelStrategy, csvStrategy));

            var result = resolver.resolve(extension);

            assertThat(result).isEqualTo(excelStrategy);

            then(excelStrategy).should().supports(extension);
            then(csvStrategy).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("Should throw ProductImportStrategyException when no strategy supports the extension")
        void shouldThrowExceptionWhenNoStrategyMatches() {
            String extension = "xml";
            given(excelStrategy.supports(extension)).willReturn(false);
            given(csvStrategy.supports(extension)).willReturn(false);

            var resolver = new ProductImportStrategyResolverImpl(List.of(excelStrategy, csvStrategy));

            assertThatThrownBy(() -> resolver.resolve(extension))
                    .isInstanceOf(ProductImportStrategyException.class)
                    .hasMessageContaining(extension);
        }

        @Test
        @DisplayName("Should throw ProductImportStrategyException when the strategies list is empty")
        void shouldThrowExceptionWhenListIsEmpty() {
            String extension = "csv";
            var resolver = new ProductImportStrategyResolverImpl(List.of());

            assertThatThrownBy(() -> resolver.resolve(extension))
                    .isInstanceOf(ProductImportStrategyException.class);
        }
    }
}