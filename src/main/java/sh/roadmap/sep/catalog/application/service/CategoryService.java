package sh.roadmap.sep.catalog.application.service;

import sh.roadmap.sep.catalog.application.dto.request.CategoryRequest;
import sh.roadmap.sep.catalog.application.dto.response.CategoryResponse;
import sh.roadmap.sep.catalog.domain.model.CategoryFilter;
import sh.roadmap.sep.catalog.domain.util.Page;

public interface CategoryService {
    Page<CategoryResponse> searchCategories(CategoryFilter categoryFilter, Page.Request pageRequest);

    CategoryResponse getById(long categoryId);

    void create(CategoryRequest categoryRequest);

    void update(CategoryRequest categoryRequest, long categoryId);

    void toggleActiveStatus(long categoryId);
}
