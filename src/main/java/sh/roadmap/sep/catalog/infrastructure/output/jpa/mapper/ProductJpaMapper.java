package sh.roadmap.sep.catalog.infrastructure.output.jpa.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import sh.roadmap.sep.catalog.domain.model.Product;
import sh.roadmap.sep.catalog.infrastructure.output.jpa.entity.ProductEntity;

@Mapper(componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        unmappedSourcePolicy = ReportingPolicy.IGNORE)
public interface ProductJpaMapper {
    ProductEntity toEntity(Product product);

    Product toDomain(ProductEntity productEntity);
}
