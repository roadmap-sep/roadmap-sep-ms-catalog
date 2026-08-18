package sh.roadmap.sep.productcatalog.application.strategy;

import sh.roadmap.sep.productcatalog.application.dto.request.ProductImportRequest;
import sh.roadmap.sep.productcatalog.application.dto.request.ProductRequest;

import java.util.List;

public interface ProductImportStrategy {
    boolean supports(String fileExtension);

    List<ProductRequest> process(ProductImportRequest file);
}
