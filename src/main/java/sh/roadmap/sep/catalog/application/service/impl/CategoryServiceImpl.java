package sh.roadmap.sep.catalog.application.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sh.roadmap.sep.catalog.application.dto.request.CategoryRequest;
import sh.roadmap.sep.catalog.application.dto.response.CategoryResponse;
import sh.roadmap.sep.catalog.application.exception.CategoryServiceException;
import sh.roadmap.sep.catalog.application.mapper.CategoryDtoMapper;
import sh.roadmap.sep.catalog.application.service.CategoryService;
import sh.roadmap.sep.catalog.domain.model.Category;
import sh.roadmap.sep.catalog.domain.model.CategoryFilter;
import sh.roadmap.sep.catalog.domain.port.in.CategoryPortIn;
import sh.roadmap.sep.catalog.domain.util.Page;

import java.text.Normalizer;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryServiceImpl implements CategoryService {
    private final CategoryPortIn categoryPortIn;
    private final CategoryDtoMapper categoryDtoMapper;

    @Override
    public Page<CategoryResponse> searchCategories(CategoryFilter categoryFilter, Page.Request pageRequest) {
        return categoryDtoMapper.toDto(categoryPortIn.searchCategories(categoryFilter, pageRequest));
    }

    @Override
    public CategoryResponse getById(long categoryId) {
        return categoryDtoMapper.toDto(categoryPortIn.getById(categoryId));
    }

    @Override
    @Transactional
    public void create(CategoryRequest categoryRequest) {
        Long parentId = categoryRequest.parentId();
        if (parentId != null) {
            categoryPortIn.getById(parentId);
        }
        Category category = Category.builder()
                .name(categoryRequest.name())
                .slug(generateSlug(categoryRequest))
                .parentId(parentId)
                .active(true)
                .build();
        categoryPortIn.create(category);
    }

    @Override
    @Transactional
    public void update(CategoryRequest categoryRequest, long categoryId) {
        Category oldCategory = categoryPortIn.getById(categoryId);

        Long parentId = categoryRequest.parentId();
        if (parentId != null) {
            if (parentId == categoryId) {
                throw new CategoryServiceException("A category cannot be the parent of itself.");
            }
            categoryPortIn.getById(parentId);
        }
        boolean hasCustomSlug = categoryRequest.slug() != null && !categoryRequest.slug().isBlank();
        String finalSlug = hasCustomSlug ? categoryRequest.slug() : oldCategory.slug();
        Category updatedCategory = oldCategory
                .toBuilder()
                .name(categoryRequest.name())
                .slug(finalSlug)
                .parentId(parentId)
                .build();
        categoryPortIn.update(updatedCategory);
    }

    @Override
    @Transactional
    public void toggleActiveStatus(long categoryId) {
        Category oldCategory = categoryPortIn.getById(categoryId);
        Category updatedCategory = oldCategory.toBuilder()
                .active(!oldCategory.active())
                .build();
        categoryPortIn.update(updatedCategory);
    }

    private String generateSlug(CategoryRequest categoryRequest) {
        if (categoryRequest.slug() != null && !categoryRequest.slug().isBlank()) {
            return categoryRequest.slug();
        }
        String noWhitespace = categoryRequest.name()
                .trim()
                .toLowerCase(Locale.ENGLISH)
                .replaceAll("\\s+", "-");

        String normalized = Normalizer.normalize(noWhitespace, Normalizer.Form.NFD);

        String cleanSlug = normalized.replaceAll("[^\\w-]", "");
        return cleanSlug.replaceAll("(^-|-$)", "");
    }
}
