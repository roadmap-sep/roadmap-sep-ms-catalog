package sh.roadmap.sep.catalog.application.strategy;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import sh.roadmap.sep.catalog.application.exception.ProductImportStrategyException;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ProductImportStrategyResolverImpl implements ProductImportStrategyResolver {
    private final List<ProductImportStrategy> strategies;

    @Override
    public ProductImportStrategy resolve(String fileExtension) {
        return strategies.stream()
                .filter(strategy -> strategy.supports(fileExtension))
                .findFirst()
                .orElseThrow(() -> new ProductImportStrategyException(fileExtension));
    }
}
