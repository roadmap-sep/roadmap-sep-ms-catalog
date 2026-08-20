package sh.roadmap.sep.catalog.application.service.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sh.roadmap.sep.catalog.application.dto.request.CategoryRequest;
import sh.roadmap.sep.catalog.application.dto.response.CategoryResponse;
import sh.roadmap.sep.catalog.application.exception.CategoryServiceException;
import sh.roadmap.sep.catalog.application.mapper.CategoryDtoMapper;
import sh.roadmap.sep.catalog.domain.model.Category;
import sh.roadmap.sep.catalog.domain.model.CategoryFilter;
import sh.roadmap.sep.catalog.domain.port.in.CategoryPortIn;
import sh.roadmap.sep.catalog.domain.util.Page;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class CategoryServiceImplTest {

    @Mock
    private CategoryPortIn categoryPortIn;

    @Mock
    private CategoryDtoMapper categoryDtoMapper;

    @InjectMocks
    private CategoryServiceImpl categoryService;

    @Captor
    private ArgumentCaptor<Category> categoryCaptor;

    private final Page.Request pageRequest = new Page.Request(0, 10);
    private final Category category = new Category(1L, "Electronics",
            "electronics", null, true);
    private final CategoryResponse categoryResponse = new CategoryResponse(1L, "Electronics",
            "electronics", null, true);

    @Nested
    @DisplayName("searchCategories() tests")
    class SearchCategoriesTests {

        @Test
        @DisplayName("Should delegate category search to port and map results to DTO page")
        void shouldReturnMappedCategoriesByFilter() {
            var filter = new CategoryFilter("Elec", "electronics", null, true);
            var categoryPage = Page.<Category>builder().data(List.of(category)).build();
            var responsePage = Page.<CategoryResponse>builder().data(List.of(categoryResponse)).build();

            given(categoryPortIn.searchCategories(filter, pageRequest)).willReturn(categoryPage);
            given(categoryDtoMapper.toDto(categoryPage)).willReturn(responsePage);

            var result = categoryService.searchCategories(filter, pageRequest);

            assertThat(result).isEqualTo(responsePage);
            then(categoryPortIn).should().searchCategories(filter, pageRequest);
            then(categoryDtoMapper).should().toDto(categoryPage);
        }
    }

    @Nested
    @DisplayName("getById() tests")
    class GetByIdTests {

        @Test
        @DisplayName("Should return a mapped category by id")
        void shouldReturnCategoryById() {
            long categoryId = 1L;
            given(categoryPortIn.getById(categoryId)).willReturn(category);
            given(categoryDtoMapper.toDto(category)).willReturn(categoryResponse);

            var result = categoryService.getById(categoryId);

            assertThat(result).isEqualTo(categoryResponse);
            then(categoryPortIn).should().getById(categoryId);
        }
    }

    @Nested
    @DisplayName("create() tests")
    class CreateTests {

        @Test
        @DisplayName("Should create category using provided custom slug")
        void shouldCreateWithCustomSlug() {
            var request = new CategoryRequest("Smartphones", "custom-slug", null);

            categoryService.create(request);

            then(categoryPortIn).should().create(categoryCaptor.capture());
            Category capturedCategory = categoryCaptor.getValue();

            assertThat(capturedCategory.name()).isEqualTo("Smartphones");
            assertThat(capturedCategory.slug()).isEqualTo("custom-slug");
            assertThat(capturedCategory.parentId()).isNull();
            assertThat(capturedCategory.active()).isTrue();
        }

        @Test
        @DisplayName("Should create category and generate slug when slug is null")
        void shouldCreateAndGenerateSlugWhenNull() {
            var request = new CategoryRequest("  Smart TVs & Audio  ", null, null);

            categoryService.create(request);

            then(categoryPortIn).should().create(categoryCaptor.capture());
            assertThat(categoryCaptor.getValue().slug()).isEqualTo("smart-tvs--audio");
        }

        @Test
        @DisplayName("Should correctly normalize special characters for slug generation")
        void shouldNormalizeCharactersForSlug() {
            var request = new CategoryRequest("Electrónica y Más!", "  ", null);

            categoryService.create(request);

            then(categoryPortIn).should().create(categoryCaptor.capture());
            assertThat(categoryCaptor.getValue().slug()).isEqualTo("electronica-y-mas");
        }

        @Test
        @DisplayName("Should verify parent category exists when parentId is provided")
        void shouldVerifyParentExistsWhenProvided() {
            long parentId = 2L;
            var request = new CategoryRequest("Laptops", "laptops", parentId);
            given(categoryPortIn.getById(parentId)).willReturn(new Category(parentId, "Computers",
                    "computers", null, true));

            categoryService.create(request);

            then(categoryPortIn).should().getById(parentId);
            then(categoryPortIn).should().create(categoryCaptor.capture());
            assertThat(categoryCaptor.getValue().parentId()).isEqualTo(parentId);
        }
    }

    @Nested
    @DisplayName("update() tests")
    class UpdateTests {

        @Test
        @DisplayName("Should update category and replace slug if a new one is provided")
        void shouldUpdateWithNewSlug() {
            long categoryId = 1L;
            var request = new CategoryRequest("New Electronics", "new-slug", null);
            given(categoryPortIn.getById(categoryId)).willReturn(category);

            categoryService.update(request, categoryId);

            then(categoryPortIn).should().update(categoryCaptor.capture());
            Category updatedCategory = categoryCaptor.getValue();

            assertThat(updatedCategory.name()).isEqualTo("New Electronics");
            assertThat(updatedCategory.slug()).isEqualTo("new-slug");
            assertThat(updatedCategory.id()).isEqualTo(1L);
        }

        @Test
        @DisplayName("Should retain old slug if update request slug is null or blank")
        void shouldRetainOldSlugIfRequestSlugIsBlank() {
            long categoryId = 1L;
            var request = new CategoryRequest("New Electronics", "   ", null);
            given(categoryPortIn.getById(categoryId)).willReturn(category); // old slug is "electronics"

            categoryService.update(request, categoryId);

            then(categoryPortIn).should().update(categoryCaptor.capture());
            assertThat(categoryCaptor.getValue().slug()).isEqualTo("electronics");
        }

        @Test
        @DisplayName("Should throw CategoryServiceException if parentId equals categoryId")
        void shouldThrowExceptionWhenSelfParenting() {
            long categoryId = 1L;
            var request = new CategoryRequest("Name", "slug", categoryId); // parentId = categoryId
            given(categoryPortIn.getById(categoryId)).willReturn(category);

            assertThatThrownBy(() -> categoryService.update(request, categoryId))
                    .isInstanceOf(CategoryServiceException.class)
                    .hasMessage("A category cannot be the parent of itself.");

            then(categoryPortIn).should(never()).update(any(Category.class));
        }

        @Test
        @DisplayName("Should verify parent category when a valid parentId is provided")
        void shouldVerifyParentOnUpdate() {
            long categoryId = 1L;
            long parentId = 2L;
            var request = new CategoryRequest("Name", "slug", parentId);

            given(categoryPortIn.getById(categoryId)).willReturn(category);
            given(categoryPortIn.getById(parentId)).willReturn(new Category(parentId, "Parent",
                    "parent", null, true));

            categoryService.update(request, categoryId);

            then(categoryPortIn).should().getById(parentId);
            then(categoryPortIn).should().update(categoryCaptor.capture());
            assertThat(categoryCaptor.getValue().parentId()).isEqualTo(parentId);
        }
    }

    @Nested
    @DisplayName("toggleActiveStatus() tests")
    class ToggleActiveStatusTests {

        @Test
        @DisplayName("Should toggle active status from true to false")
        void shouldToggleFromTrueToFalse() {
            long categoryId = 1L;
            var activeCategory = new Category(1L, "Name", "slug", null, true);
            given(categoryPortIn.getById(categoryId)).willReturn(activeCategory);

            categoryService.toggleActiveStatus(categoryId);

            then(categoryPortIn).should().update(categoryCaptor.capture());
            assertThat(categoryCaptor.getValue().active()).isFalse();
        }

        @Test
        @DisplayName("Should toggle active status from false to true")
        void shouldToggleFromFalseToTrue() {
            long categoryId = 1L;
            var inactiveCategory = new Category(1L, "Name", "slug", null, false);
            given(categoryPortIn.getById(categoryId)).willReturn(inactiveCategory);

            categoryService.toggleActiveStatus(categoryId);

            then(categoryPortIn).should().update(categoryCaptor.capture());
            assertThat(categoryCaptor.getValue().active()).isTrue();
        }
    }
}