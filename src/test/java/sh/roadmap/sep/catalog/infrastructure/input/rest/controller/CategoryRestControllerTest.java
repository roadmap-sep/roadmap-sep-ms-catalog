package sh.roadmap.sep.catalog.infrastructure.input.rest.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import sh.roadmap.sep.catalog.application.dto.request.CategoryRequest;
import sh.roadmap.sep.catalog.application.dto.response.CategoryResponse;
import sh.roadmap.sep.catalog.application.service.CategoryService;
import sh.roadmap.sep.catalog.domain.exception.CategoryNotFoundException;
import sh.roadmap.sep.catalog.domain.exception.ProductAlreadyExistsException;
import sh.roadmap.sep.catalog.domain.model.CategoryFilter;
import sh.roadmap.sep.catalog.domain.util.Page;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CategoryRestController.class)
class CategoryRestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CategoryService categoryService;

    private static final String BASE_URL = "/v1.0/categories";

    private final CategoryResponse categoryResponse = new CategoryResponse(1L, "Electronics",
            "electronics", null, true);
    private final CategoryRequest validRequest = new CategoryRequest("Electronics",
            "electronics", null);

    @Nested
    @DisplayName("GET " + BASE_URL)
    class SearchCategoriesTests {

        @Test
        @DisplayName("Should return 200 OK and paged categories using default parameters when no filters are provided")
        void shouldReturnPagedCategoriesWithDefaultFilters() throws Exception {
            var defaultFilter = new CategoryFilter(null, null, null, true);
            var pageRequest = new Page.Request(0, 10);
            var categoryPage = Page.<CategoryResponse>builder()
                    .data(List.of(categoryResponse))
                    .pageNumber(0)
                    .pageSize(10)
                    .totalElements(1)
                    .totalPages(1)
                    .hasNext(false)
                    .build();

            given(categoryService.searchCategories(defaultFilter, pageRequest)).willReturn(categoryPage);

            mockMvc.perform(get(BASE_URL)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data[0].id").value(1))
                    .andExpect(jsonPath("$.data[0].name").value("Electronics"))
                    .andExpect(jsonPath("$.data[0].slug").value("electronics"))
                    .andExpect(jsonPath("$.data[0].active").value(true))
                    .andExpect(jsonPath("$.pageNumber").value(0))
                    .andExpect(jsonPath("$.pageSize").value(10))
                    .andExpect(jsonPath("$.totalElements").value(1));

            then(categoryService).should().searchCategories(defaultFilter, pageRequest);
        }

        @Test
        @DisplayName("Should return 200 OK and filtered categories when query parameters are provided")
        void shouldReturnFilteredCategoriesWhenQueryParamsAreProvided() throws Exception {
            String name = "Elec";
            String slug = "electronics";
            Long parentId = 2L;
            Boolean isActive = false;
            int page = 1;
            int size = 5;

            var filter = new CategoryFilter(name, slug, parentId, isActive);
            var pageRequest = new Page.Request(page, size);
            var categoryPage = Page.<CategoryResponse>builder()
                    .data(List.of(categoryResponse))
                    .pageNumber(page)
                    .pageSize(size)
                    .totalElements(1)
                    .totalPages(1)
                    .hasNext(false)
                    .build();

            given(categoryService.searchCategories(filter, pageRequest)).willReturn(categoryPage);

            mockMvc.perform(get(BASE_URL)
                            .param("name", name)
                            .param("slug", slug)
                            .param("parent_id", String.valueOf(parentId))
                            .param("is_active", String.valueOf(isActive))
                            .param("page", String.valueOf(page))
                            .param("size", String.valueOf(size))
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data[0].id").value(1))
                    .andExpect(jsonPath("$.data[0].name").value("Electronics"))
                    .andExpect(jsonPath("$.pageNumber").value(1))
                    .andExpect(jsonPath("$.pageSize").value(5));

            then(categoryService).should().searchCategories(filter, pageRequest);
        }
    }

    @Nested
    @DisplayName("GET " + BASE_URL + "/{category_id}")
    class GetCategoryByIdTests {

        @Test
        @DisplayName("Should return 200 OK and category detail when category exists")
        void shouldReturnCategoryById() throws Exception {
            long categoryId = 1L;
            given(categoryService.getById(categoryId)).willReturn(categoryResponse);

            mockMvc.perform(get(BASE_URL + "/{category_id}", categoryId)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.name").value("Electronics"))
                    .andExpect(jsonPath("$.slug").value("electronics"))
                    .andExpect(jsonPath("$.active").value(true));

            then(categoryService).should().getById(categoryId);
        }

        @Test
        @DisplayName("Should throw CategoryNotFoundException when ID not found")
        void shouldReturnCategoryNotFoundExceptionWhenIdNotFound() throws Exception {
            long categoryId = 1L;
            given(categoryService.getById(categoryId))
                    .willThrow(new CategoryNotFoundException(categoryId));

            mockMvc.perform(get(BASE_URL + "/{product_id}", categoryId)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.detail").exists());
        }
    }

    @Nested
    @DisplayName("POST " + BASE_URL)
    class CreateTests {

        @Test
        @DisplayName("Should return 201 Created when request payload is valid")
        void shouldCreateCategorySuccessfully() throws Exception {
            willDoNothing().given(categoryService).create(validRequest);

            mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest)))
                    .andExpect(status().isCreated());

            then(categoryService).should().create(validRequest);
        }

        @Test
        @DisplayName("Should throw ProductAlreadyExistsException")
        void shouldReturnProductAlreadyExistsException() throws Exception {

            doThrow(new ProductAlreadyExistsException(validRequest.slug()))
                    .when(categoryService).create(any(CategoryRequest.class));

            mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.detail").exists());
        }

        @Test
        @DisplayName("Should return 400 Bad Request when request name is blank")
        void shouldReturn400WhenNameIsBlank() throws Exception {
            var invalidRequest = new CategoryRequest("", "electronics", null);

            mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest());

            then(categoryService).should(never()).create(any());
        }

        @Test
        @DisplayName("Should return 400 Bad Request when request slug violates pattern")
        void shouldReturn400WhenSlugIsInvalid() throws Exception {
            var invalidRequest = new CategoryRequest("Electronics", "Invalid_Slug!", null);

            mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest());

            then(categoryService).should(never()).create(any());
        }

        @Test
        @DisplayName("Should return 400 Bad Request when parentId is negative")
        void shouldReturn400WhenParentIdIsNegative() throws Exception {
            var invalidRequest = new CategoryRequest("Electronics", "electronics", -5L);

            mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest());

            then(categoryService).should(never()).create(any());
        }
    }

    @Nested
    @DisplayName("PUT " + BASE_URL + "/{category_id}")
    class UpdateTests {

        @Test
        @DisplayName("Should return 204 No Content when request payload is valid")
        void shouldUpdateCategorySuccessfully() throws Exception {
            long categoryId = 1L;
            willDoNothing().given(categoryService).update(validRequest, categoryId);

            mockMvc.perform(put(BASE_URL + "/{category_id}", categoryId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest)))
                    .andExpect(status().isNoContent());

            then(categoryService).should().update(validRequest, categoryId);
        }

        @Test
        @DisplayName("Should return 400 Bad Request when payload fails validation")
        void shouldReturn400WhenUpdatePayloadIsInvalid() throws Exception {
            long categoryId = 1L;
            var invalidRequest = new CategoryRequest("   ", "electronics", null);

            mockMvc.perform(put(BASE_URL + "/{category_id}", categoryId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest());

            then(categoryService).should(never()).update(any(), eq(categoryId));
        }
    }

    @Nested
    @DisplayName("PATCH " + BASE_URL + "/{category_id}")
    class ToggleActiveStatusTests {

        @Test
        @DisplayName("Should return 204 No Content when toggling active status")
        void shouldToggleActiveStatusSuccessfully() throws Exception {
            long categoryId = 1L;
            willDoNothing().given(categoryService).toggleActiveStatus(categoryId);

            mockMvc.perform(patch(BASE_URL + "/{category_id}", categoryId))
                    .andExpect(status().isNoContent());

            then(categoryService).should().toggleActiveStatus(categoryId);
        }
    }
}