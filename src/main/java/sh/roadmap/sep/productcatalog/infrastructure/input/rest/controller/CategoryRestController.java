package sh.roadmap.sep.productcatalog.infrastructure.input.rest.controller;

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
import sh.roadmap.sep.productcatalog.application.dto.request.CategoryRequest;
import sh.roadmap.sep.productcatalog.application.dto.response.CategoryResponse;
import sh.roadmap.sep.productcatalog.application.service.CategoryService;
import sh.roadmap.sep.productcatalog.domain.util.Page;

@RestController
@RequestMapping("/{version}/categories")
@RequiredArgsConstructor
public class CategoryRestController {
    private final CategoryService categoryService;

    @GetMapping(version = "1.0")
    public ResponseEntity<Page<CategoryResponse>> getAll(@RequestParam(name = "page", defaultValue = "0")
                                                            int page,
                                                         @RequestParam(name = "size", defaultValue = "10")
                                                            int size) {
        return ResponseEntity.ok(categoryService.getAll(new Page.Request(page, size)));
    }

    @GetMapping(version = "1.0", params = "category_name")
    public ResponseEntity<Page<CategoryResponse>> getByName(@RequestParam(name = "category_name")
                                                               String categoryName,
                                                            @RequestParam(name = "page", defaultValue = "0")
                                                               int page,
                                                            @RequestParam(name = "size", defaultValue = "10")
                                                               int size) {
        return ResponseEntity.ok(categoryService.getByName(categoryName, new Page.Request(page, size)));
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
