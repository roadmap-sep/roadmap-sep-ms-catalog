package sh.roadmap.sep.productcatalog.application.strategy.impl;

import org.odftoolkit.odfdom.doc.OdfDocument;
import org.springframework.stereotype.Component;
import sh.roadmap.sep.productcatalog.application.dto.request.ProductImportRequest;
import sh.roadmap.sep.productcatalog.application.dto.request.ProductRequest;
import sh.roadmap.sep.productcatalog.application.strategy.ProductImportStrategy;

import java.util.List;

@Component
public class OdsProductImportStrategy implements ProductImportStrategy {

    @Override
    public boolean supports(String fileExtension) {
        return OdfDocument.OdfMediaType.SPREADSHEET.getSuffix().equalsIgnoreCase(fileExtension.trim())
                || OdfDocument.OdfMediaType.SPREADSHEET_TEMPLATE.getSuffix().equalsIgnoreCase(fileExtension.trim());
    }

    @Override
    public List<ProductRequest> process(ProductImportRequest file) {
        return List.of();
    }
}
