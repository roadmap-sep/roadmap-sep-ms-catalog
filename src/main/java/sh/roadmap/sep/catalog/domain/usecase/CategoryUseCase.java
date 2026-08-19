package sh.roadmap.sep.catalog.domain.usecase;

import sh.roadmap.sep.catalog.domain.model.Category;
import sh.roadmap.sep.catalog.domain.model.CategoryFilter;
import sh.roadmap.sep.catalog.domain.port.in.CategoryPortIn;
import sh.roadmap.sep.catalog.domain.port.out.CategoryPortOut;
import sh.roadmap.sep.catalog.domain.util.Page;

public class CategoryUseCase implements CategoryPortIn {
    private final CategoryPortOut categoryPortOut;

    public CategoryUseCase(CategoryPortOut categoryPortOut) {
        this.categoryPortOut = categoryPortOut;
    }

    @Override
    public Page<Category> searchCategories(CategoryFilter categoryFilter, Page.Request pageRequest) {
        return categoryPortOut.searchCategories(categoryFilter, pageRequest);
    }

    @Override
    public Category getById(long categoryId) {
        return categoryPortOut.getById(categoryId);
    }

    @Override
    public void create(Category category) {
        categoryPortOut.create(category);
    }

    @Override
    public void update(Category category) {
        categoryPortOut.update(category);
    }
}
