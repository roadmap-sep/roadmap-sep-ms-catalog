package sh.roadmap.sep.productcatalog.application.strategy;

public interface ProductImportStrategyResolver {
    ProductImportStrategy resolve(String fileExtension);
}
