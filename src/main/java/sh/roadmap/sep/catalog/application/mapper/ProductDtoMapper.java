package sh.roadmap.sep.catalog.application.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import sh.roadmap.sep.catalog.application.dto.request.ProductRequest;
import sh.roadmap.sep.catalog.application.dto.response.ProductResponse;
import sh.roadmap.sep.catalog.domain.model.Product;
import sh.roadmap.sep.catalog.domain.util.Page;

@Mapper(componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        unmappedSourcePolicy = ReportingPolicy.IGNORE)
public interface ProductDtoMapper {
    ProductResponse toDto(Product product);

    Page<ProductResponse> toDto(Page<Product> products);

    Product toDomain(ProductRequest productRequest);
}
