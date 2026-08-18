package sh.roadmap.sep.productcatalog.domain.usecase;

import sh.roadmap.sep.productcatalog.domain.model.Category;
import sh.roadmap.sep.productcatalog.domain.port.in.CategoryPortIn;
import sh.roadmap.sep.productcatalog.domain.port.out.CategoryPortOut;
import sh.roadmap.sep.productcatalog.domain.util.Page;

public class CategoryUseCase implements CategoryPortIn {
    private final CategoryPortOut categoryPortOut;

    public CategoryUseCase(CategoryPortOut categoryPortOut) {
        this.categoryPortOut = categoryPortOut;
    }

    @Override
    public Page<Category> getAll(Page.Request pageRequest) {
        return categoryPortOut.getAll(pageRequest);
    }

    @Override
    public Page<Category> getByName(String name, Page.Request pageRequest) {
        return categoryPortOut.getByName(name, pageRequest);
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
