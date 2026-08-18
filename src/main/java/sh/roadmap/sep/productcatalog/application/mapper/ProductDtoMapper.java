package sh.roadmap.sep.productcatalog.application.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import sh.roadmap.sep.productcatalog.application.dto.request.ProductRequest;
import sh.roadmap.sep.productcatalog.application.dto.response.ProductResponse;
import sh.roadmap.sep.productcatalog.domain.model.Product;
import sh.roadmap.sep.productcatalog.domain.util.Page;

@Mapper(componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        unmappedSourcePolicy = ReportingPolicy.IGNORE)
public interface ProductDtoMapper {
    ProductResponse toDto(Product product);

    Page<ProductResponse> toDto(Page<Product> products);

    Product toDomain(ProductRequest productRequest);
}
