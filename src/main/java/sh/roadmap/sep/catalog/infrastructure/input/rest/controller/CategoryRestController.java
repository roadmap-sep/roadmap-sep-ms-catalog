package sh.roadmap.sep.catalog.infrastructure.input.rest.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import sh.roadmap.sep.catalog.application.dto.request.CategoryRequest;
import sh.roadmap.sep.catalog.application.dto.response.CategoryResponse;
import sh.roadmap.sep.catalog.application.service.CategoryService;
import sh.roadmap.sep.catalog.domain.model.CategoryFilter;
import sh.roadmap.sep.catalog.domain.util.Page;

@RestController
@RequestMapping("/{version}/categories")
@RequiredArgsConstructor
public class CategoryRestController {
    private final CategoryService categoryService;

    @GetMapping(version = "1.0")
    public ResponseEntity<Page<CategoryResponse>> getAll(
            @RequestParam(name = "name", required = false) String name,
            @RequestParam(name = "slug", required = false) String slug,
            @RequestParam(name = "parent_id", required = false) Long parentId,
            @RequestParam(name = "is_active", defaultValue = "true") Boolean isActive,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size) {
        return ResponseEntity.ok(categoryService.searchCategories(new CategoryFilter(name, slug, parentId, isActive),
                new Page.Request(page, size)));
    }

    @GetMapping(value = "/{category_id}", version = "1.0")
    public ResponseEntity<CategoryResponse> getCategoryById(@PathVariable("category_id") long categoryId) {
        return ResponseEntity.ok(categoryService.getById(categoryId));
    }

    @PostMapping(version = "1.0")
    public ResponseEntity<CategoryResponse> create(@RequestBody @Valid CategoryRequest categoryRequest) {
        categoryService.create(categoryRequest);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PutMapping(value = "/{category_id}", version = "1.0")
    public ResponseEntity<CategoryResponse> update(@RequestBody @Valid CategoryRequest categoryRequest,
                                                   @PathVariable("category_id") long categoryId) {
        categoryService.update(categoryRequest, categoryId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping(value = "/{category_id}", version = "1.0")
    public ResponseEntity<CategoryResponse> toggleActiveStatus(@PathVariable("category_id") long categoryId) {
        categoryService.toggleActiveStatus(categoryId);
        return ResponseEntity.noContent().build();
    }
}
