package sh.roadmap.sep.catalog.application.strategy;

import sh.roadmap.sep.catalog.application.dto.request.ProductImportRequest;
import sh.roadmap.sep.catalog.application.dto.request.ProductRequest;

import java.util.List;

public interface ProductImportStrategy {
    boolean supports(String fileExtension);

    List<ProductRequest> process(ProductImportRequest file);
}
