package sh.roadmap.sep.productcatalog.infrastructure.output.jpa.adapter;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import sh.roadmap.sep.productcatalog.domain.exception.CategoryAlreadyExistsException;
import sh.roadmap.sep.productcatalog.domain.exception.CategoryNotFoundException;
import sh.roadmap.sep.productcatalog.domain.model.Category;
import sh.roadmap.sep.productcatalog.domain.port.out.CategoryPortOut;
import sh.roadmap.sep.productcatalog.domain.util.Page;
import sh.roadmap.sep.productcatalog.infrastructure.output.jpa.entity.CategoryEntity;
import sh.roadmap.sep.productcatalog.infrastructure.output.jpa.mapper.CategoryJpaMapper;
import sh.roadmap.sep.productcatalog.infrastructure.output.jpa.repository.CategoryJpaRepository;

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
        int pageNumber = page.getNumber();
        int pageSize = page.getSize();
        long totalElements = page.getTotalElements();
        int totalPages = page.getTotalPages();
        boolean hasNextPage = page.hasNext();
        return page.get()
                .map(categoryJpaMapper::toModel)
                .collect(Collectors.collectingAndThen(Collectors.toList(), categories ->
                        Page.<Category>builder()
                                .data(categories)
                                .pageNumber(pageNumber)
                                .pageSize(pageSize)
                                .totalElements(totalElements)
                                .totalPages(totalPages)
                                .hasNext(hasNextPage)
                                .build()));
    }
}
