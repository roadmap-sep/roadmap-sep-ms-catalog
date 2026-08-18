package sh.roadmap.sep.catalog.domain.port.in;

import sh.roadmap.sep.catalog.domain.model.Category;
import sh.roadmap.sep.catalog.domain.util.Page;

public interface CategoryPortIn {
    Page<Category> getAll(Page.Request pageRequest);

    Page<Category> getByName(String name, Page.Request pageRequest);

    Category getById(long categoryId);

    void create(Category category);

    void update(Category category);
}
