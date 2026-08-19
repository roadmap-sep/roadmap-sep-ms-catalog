package sh.roadmap.sep.catalog.infrastructure.output.jpa.adapter;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import sh.roadmap.sep.catalog.domain.exception.CategoryAlreadyExistsException;
import sh.roadmap.sep.catalog.domain.exception.CategoryNotFoundException;
import sh.roadmap.sep.catalog.domain.model.Category;
import sh.roadmap.sep.catalog.domain.port.out.CategoryPortOut;
import sh.roadmap.sep.catalog.domain.util.Page;
import sh.roadmap.sep.catalog.infrastructure.output.jpa.entity.CategoryEntity;
import sh.roadmap.sep.catalog.infrastructure.output.jpa.mapper.CategoryJpaMapper;
import sh.roadmap.sep.catalog.infrastructure.output.jpa.repository.CategoryJpaRepository;

import java.util.stream.Collectors;

@RequiredArgsConstructor
public class CategoryJpaAdapter implements CategoryPortOut {
    private final CategoryJpaRepository categoryJpaRepository;
    private final CategoryJpaMapper categoryJpaMapper;

    @Override
    public Page<Category> getAll(Page.Request pageRequest) {
        return this.toCategoryPage(categoryJpaRepository.findAll(PageRequest.of(pageRequest.pageNumber(),
                pageRequest.pageSize())));
    }

    @Override
    public Page<Category> getByName(String name, Page.Request pageRequest) {
        return this.toCategoryPage(categoryJpaRepository.findByNameContainingIgnoreCase(name,
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
}
