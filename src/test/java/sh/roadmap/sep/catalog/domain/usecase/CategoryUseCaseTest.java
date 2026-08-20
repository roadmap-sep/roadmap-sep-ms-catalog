package sh.roadmap.sep.catalog.domain.usecase;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sh.roadmap.sep.catalog.domain.model.Category;
import sh.roadmap.sep.catalog.domain.model.CategoryFilter;
import sh.roadmap.sep.catalog.domain.port.out.CategoryPortOut;
import sh.roadmap.sep.catalog.domain.util.Page;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willDoNothing;

@ExtendWith(MockitoExtension.class)
class CategoryUseCaseTest {

    @Mock
    private CategoryPortOut categoryPortOut;

    @InjectMocks
    private CategoryUseCase categoryUseCase;

    private final Category category = new Category(1L, "Electronics",
            "electronics", null, true);
    private final Page.Request pageRequest = new Page.Request(0, 10);

    @Nested
    @DisplayName("searchCategories() tests")
    class SearchCategoriesTests {

        @Test
        @DisplayName("Should delegate category search to portOut and return a page of categories")
        void shouldDelegateSearchCategories() {
            var filter = new CategoryFilter("Elec", "electronics", null, true);
            var categoryPage = Page.<Category>builder()
                    .data(List.of(category))
                    .pageNumber(0)
                    .pageSize(10)
                    .totalElements(1)
                    .totalPages(1)
                    .hasNext(false)
                    .build();

            given(categoryPortOut.searchCategories(filter, pageRequest)).willReturn(categoryPage);

            var result = categoryUseCase.searchCategories(filter, pageRequest);

            assertThat(result).isNotNull();
            assertThat(result).isEqualTo(categoryPage);
            then(categoryPortOut).should().searchCategories(filter, pageRequest);
        }
    }

    @Nested
    @DisplayName("getById() tests")
    class GetByIdTests {

        @Test
        @DisplayName("Should delegate to portOut and return the category")
        void shouldDelegateGetById() {
            long categoryId = 1L;
            given(categoryPortOut.getById(categoryId)).willReturn(category);

            var result = categoryUseCase.getById(categoryId);

            assertThat(result).isEqualTo(category);
            then(categoryPortOut).should().getById(categoryId);
        }
    }

    @Nested
    @DisplayName("create() tests")
    class CreateTests {

        @Test
        @DisplayName("Should delegate creation to portOut")
        void shouldDelegateCreate() {
            willDoNothing().given(categoryPortOut).create(category);

            categoryUseCase.create(category);

            then(categoryPortOut).should().create(category);
        }
    }

    @Nested
    @DisplayName("update() tests")
    class UpdateTests {

        @Test
        @DisplayName("Should delegate update to portOut")
        void shouldDelegateUpdate() {
            willDoNothing().given(categoryPortOut).update(category);

            categoryUseCase.update(category);

            then(categoryPortOut).should().update(category);
        }
    }
}