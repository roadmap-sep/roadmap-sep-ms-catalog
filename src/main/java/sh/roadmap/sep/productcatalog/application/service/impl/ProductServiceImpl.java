package sh.roadmap.sep.productcatalog.application.service.impl;

import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sh.roadmap.sep.productcatalog.application.dto.request.ProductImportRequest;
import sh.roadmap.sep.productcatalog.application.dto.request.ProductRequest;
import sh.roadmap.sep.productcatalog.application.dto.response.ProductResponse;
import sh.roadmap.sep.productcatalog.application.mapper.ProductDtoMapper;
import sh.roadmap.sep.productcatalog.application.service.ProductService;
import sh.roadmap.sep.productcatalog.application.strategy.ProductImportStrategyResolver;
import sh.roadmap.sep.productcatalog.domain.model.Category;
import sh.roadmap.sep.productcatalog.domain.model.Product;
import sh.roadmap.sep.productcatalog.domain.model.ProductFilter;
import sh.roadmap.sep.productcatalog.domain.port.in.CategoryPortIn;
import sh.roadmap.sep.productcatalog.domain.port.in.ProductPortIn;
import sh.roadmap.sep.productcatalog.domain.util.Page;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductServiceImpl implements ProductService {
    private final ProductPortIn productPortIn;
    private final ProductDtoMapper productDtoMapper;
    private final ProductImportStrategyResolver productImportStrategyResolver;
    private final CategoryPortIn categoryPortIn;

    @Override
    public Page<ProductResponse> searchProducts(ProductFilter productFilter, Page.Request pageRequest) {
        Page<Product> productPage = productPortIn.searchProducts(productFilter, pageRequest);
        return productPage.<ProductResponse>toBuilder()
                .data(productPage.data()
                        .stream()
                        .map(this::toDto)
                        .toList())
                .build();
    }

    @Override
    public ProductResponse getById(UUID productId) {
        return toDto(productPortIn.getById(productId));
    }

    @Override
    public ProductResponse getBySku(String sku) {
        return toDto(productPortIn.getBySku(sku));
    }

    @Override
    @Transactional
    public void save(@NonNull ProductRequest productRequest) {

        Instant instant = Instant.now();
        validateCategories(productRequest.categoryIds());
        Product product = productDtoMapper.toDomain(productRequest)
                .toBuilder()
                .id(UUID.randomUUID())
                .active(true)
                .createdAt(instant)
                .updatedAt(instant)
                .build();
        productPortIn.save(product);
    }

    @Override
    @Transactional
    public void saveBatch(@NonNull ProductImportRequest file) {
        List<Product> productList = productImportStrategyResolver.resolve(file.fileExtension())
                .process(file)
                .stream()
                .map(dto -> {
                    Instant instant = Instant.now();
                    return productDtoMapper.toDomain(dto).toBuilder()
                            .id(UUID.randomUUID())
                            .active(true)
                            .createdAt(instant)
                            .updatedAt(instant)
                            .build();
                })
                .toList();
        productPortIn.saveBatch(productList);
    }

    @Override
    @Transactional
    public void update(@NonNull ProductRequest productRequest, UUID productId) {
        validateCategories(productRequest.categoryIds());
        Product productUpdated = productPortIn.getById(productId)
                .toBuilder()
                .name(productRequest.name())
                .sku(productRequest.sku())
                .description(productRequest.description())
                .price(productRequest.price())
                .categoryIds(productRequest.categoryIds())
                .stock(productRequest.stock())
                .weight(productRequest.weight())
                .mainImageUrl(productRequest.mainImageUrl())
                .updatedAt(Instant.now())
                .build();
        productPortIn.update(productUpdated);
    }

    @Override
    @Transactional
    public void toggleActiveStatus(UUID productId) {
        Product oldProduct = productPortIn.getById(productId);
        Product newProduct = oldProduct.toBuilder()
                .active(!oldProduct.active())
                .updatedAt(Instant.now())
                .build();
        productPortIn.update(newProduct);
    }

    private ProductResponse toDto(Product product) {
        return productDtoMapper.toDto(product)
                .toBuilder()
                .categories(product.categoryIds()
                        .stream()
                        .map(categoryPortIn::getById)
                        .map(Category::slug)
                        .collect(Collectors.toSet()))
                .build();
    }

    private void validateCategories(@NonNull Set<Long> categories) {
        categories.forEach(categoryPortIn::getById);
    }
}
