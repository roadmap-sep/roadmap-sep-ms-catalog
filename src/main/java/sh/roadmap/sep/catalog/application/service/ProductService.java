package sh.roadmap.sep.catalog.application.service;


import sh.roadmap.sep.catalog.application.dto.request.ProductImportRequest;
import sh.roadmap.sep.catalog.application.dto.request.ProductRequest;
import sh.roadmap.sep.catalog.application.dto.response.ProductResponse;
import sh.roadmap.sep.catalog.domain.model.ProductFilter;
import sh.roadmap.sep.catalog.domain.util.Page;

import java.util.UUID;

public interface ProductService {
    Page<ProductResponse> searchProducts(ProductFilter productFilter, Page.Request pageRequest);

    ProductResponse getById(UUID productId);

    ProductResponse getBySku(String sku);

    void save(ProductRequest productRequest);

    void saveBatch(ProductImportRequest file);

    void update(ProductRequest productRequest, UUID productId);

    void toggleActiveStatus(UUID productId);
}
