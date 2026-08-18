package sh.roadmap.sep.catalog.application.strategy;

public interface ProductImportStrategyResolver {
    ProductImportStrategy resolve(String fileExtension);
}
