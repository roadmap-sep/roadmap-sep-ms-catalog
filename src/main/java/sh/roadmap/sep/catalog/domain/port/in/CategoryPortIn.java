package sh.roadmap.sep.catalog.domain.port.in;

import sh.roadmap.sep.catalog.domain.model.Category;
import sh.roadmap.sep.catalog.domain.model.CategoryFilter;
import sh.roadmap.sep.catalog.domain.util.Page;

public interface CategoryPortIn {
    Page<Category> searchCategories(CategoryFilter categoryFilter, Page.Request pageRequest);

    Category getById(long categoryId);

    void create(Category category);

    void update(Category category);
}
