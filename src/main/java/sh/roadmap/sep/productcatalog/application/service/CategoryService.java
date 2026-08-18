package sh.roadmap.sep.productcatalog.application.service;

import sh.roadmap.sep.productcatalog.application.dto.request.CategoryRequest;
import sh.roadmap.sep.productcatalog.application.dto.response.CategoryResponse;
import sh.roadmap.sep.productcatalog.domain.util.Page;

public interface CategoryService {
    Page<CategoryResponse> getAll(Page.Request pageRequest);

    Page<CategoryResponse> getByName(String name, Page.Request pageRequest);

    CategoryResponse getById(long categoryId);

    void create(CategoryRequest categoryRequest);

    void update(CategoryRequest categoryRequest, long categoryId);

    void toggleActiveStatus(long categoryId);
}
