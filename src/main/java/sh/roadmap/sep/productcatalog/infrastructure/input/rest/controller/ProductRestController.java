package sh.roadmap.sep.productcatalog.infrastructure.input.rest.controller;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Valid;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import sh.roadmap.sep.productcatalog.application.dto.request.ProductImportRequest;
import sh.roadmap.sep.productcatalog.application.dto.request.ProductRequest;
import sh.roadmap.sep.productcatalog.application.dto.response.ProductResponse;
import sh.roadmap.sep.productcatalog.application.exception.ProductImportException;
import sh.roadmap.sep.productcatalog.application.service.ProductService;
import sh.roadmap.sep.productcatalog.domain.model.ProductFilter;
import sh.roadmap.sep.productcatalog.domain.util.Page;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/{version}/products")
@RequiredArgsConstructor
public class ProductRestController {
    private final ProductService productService;
    private final Validator validator;

    @GetMapping(version = "1.0")
    public ResponseEntity<Page<ProductResponse>> searchProducts(
            @RequestParam(name = "name", required = false) String name,
            @RequestParam(name = "category_id", required = false) Long categoryId,
            @RequestParam(name = "min_price", required = false) BigDecimal minPrice,
            @RequestParam(name = "max_price", required = false) BigDecimal maxPrice,
            @RequestParam(name = "in_stock", required = false) Boolean inStock,
            @RequestParam(name = "is_active", defaultValue = "true") Boolean isActive,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size) {
        return ResponseEntity.ok(productService.searchProducts(new ProductFilter(name,
                categoryId, minPrice, maxPrice, inStock, isActive), new Page.Request(page, size)));
    }

    @GetMapping(value = "/{product_id}", version = "1.0")
    public ResponseEntity<ProductResponse> getProductById(@PathVariable("product_id") String productId) {
        return ResponseEntity.ok(productService.getById(UUID.fromString(productId)));
    }

    @GetMapping(value = "/sku/{sku}", version = "1.0")
    public ResponseEntity<ProductResponse> getProductBySku(@PathVariable("sku") String sku) {
        return ResponseEntity.ok(productService.getBySku(sku));
    }

    @PostMapping(version = "1.0")
    public ResponseEntity<Void> createProduct(@RequestBody @Valid ProductRequest productRequest) {
        productService.save(productRequest);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping(value = "/batch", version = "1.0")
    public ResponseEntity<Void> createBatchProduct(@RequestParam("file") MultipartFile file) {
        String originalFilename = file.getOriginalFilename();
        try (InputStream inputStream = file.getInputStream()) {
            ProductImportRequest dto = new ProductImportRequest(
                    StringUtils.getFilenameExtension(originalFilename),
                    originalFilename,
                    inputStream
            );
            validateDto(dto);
            productService.saveBatch(dto);
        } catch (IOException e) {
            throw new ProductImportException(originalFilename, List.of(e.getMessage()));
        }
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PutMapping(value = "/{product_id}", version = "1.0")
    public ResponseEntity<Void> updateProduct(@RequestBody @Valid ProductRequest productRequest,
                                              @PathVariable("product_id") String productId) {
        productService.update(productRequest, UUID.fromString(productId));
        return ResponseEntity.noContent().build();
    }

    @PatchMapping(value = "/{product_id}", version = "1.0")
    public ResponseEntity<Void> toggleActiveStatus(@PathVariable("product_id") String productId) {
        productService.toggleActiveStatus(UUID.fromString(productId));
        return ResponseEntity.noContent().build();
    }

    private void validateDto(ProductImportRequest dto) {
        Set<ConstraintViolation<ProductImportRequest>> violations = validator.validate(dto);
        if (!violations.isEmpty()) {
            throw new ConstraintViolationException(violations);
        }
    }
}
