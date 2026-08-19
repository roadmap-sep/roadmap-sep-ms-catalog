package sh.roadmap.sep.catalog.infrastructure.output.jpa.adapter;

import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import sh.roadmap.sep.catalog.domain.exception.CategoryAlreadyExistsException;
import sh.roadmap.sep.catalog.domain.exception.CategoryNotFoundException;
import sh.roadmap.sep.catalog.domain.model.Category;
import sh.roadmap.sep.catalog.domain.model.CategoryFilter;
import sh.roadmap.sep.catalog.domain.port.out.CategoryPortOut;
import sh.roadmap.sep.catalog.domain.util.Page;
import sh.roadmap.sep.catalog.infrastructure.output.jpa.entity.CategoryEntity;
import sh.roadmap.sep.catalog.infrastructure.output.jpa.mapper.CategoryJpaMapper;
import sh.roadmap.sep.catalog.infrastructure.output.jpa.repository.CategoryJpaRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class CategoryJpaAdapter implements CategoryPortOut {
    private final CategoryJpaRepository categoryJpaRepository;
    private final CategoryJpaMapper categoryJpaMapper;

    @Override
    public Page<Category> searchCategories(CategoryFilter categoryFilter, Page.Request pageRequest) {
        return this.toCategoryPage(categoryJpaRepository.findAll(getDynamicFilters(categoryFilter),
                PageRequest.of(pageRequest.pageNumber(), pageRequest.pageSize())));
    }

    @Override
    public Category getById(long categoryId) {
        return categoryJpaRepository.findById(categoryId)
                .map(categoryJpaMapper::toModel)
                .orElseThrow(() -> new CategoryNotFoundException(categoryId));
    }

    @Override
    public void create(Category category) {
        try {
            categoryJpaRepository.save(categoryJpaMapper.toEntity(category));
        } catch (DataIntegrityViolationException e) {
            String dbMessage = e.getMostSpecificCause().getMessage();
            if (dbMessage.contains("id")) {
                throw new CategoryAlreadyExistsException(category.id());
            }
            if (dbMessage.contains("slug")) {
                throw new CategoryAlreadyExistsException(category.slug());
            }
            throw e;
        }
    }

    @Override
    public void update(Category category) {
        if (!categoryJpaRepository.existsById(category.id())) {
            throw new CategoryNotFoundException(category.id());
        }
        categoryJpaRepository.save(categoryJpaMapper.toEntity(category));
    }

    private Page<Category> toCategoryPage(org.springframework.data.domain.Page<CategoryEntity> page) {
        Page.PageBuilder<Category> builder = Page.<Category>builder()
                .pageNumber(page.getNumber())
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .hasNext(page.hasNext());
        return page.get()
                .map(categoryJpaMapper::toModel)
                .collect(Collectors.collectingAndThen(Collectors.toList(), builder::data))
                .build();
    }

    private Specification<CategoryEntity> getDynamicFilters(CategoryFilter filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (filter.name() != null && !filter.name().isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("name")), "%" + filter.name().toLowerCase() + "%"));
            }

            if (filter.slug() != null && !filter.slug().isBlank()) {
                predicates.add(cb.equal(root.get("slug"), filter.slug()));
            }

            if (filter.parentId() != null) {
                predicates.add(cb.equal(root.get("parentId"), filter.parentId()));
            }

            if (filter.isActive() != null) {
                predicates.add(cb.equal(root.get("active"), filter.isActive()));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
