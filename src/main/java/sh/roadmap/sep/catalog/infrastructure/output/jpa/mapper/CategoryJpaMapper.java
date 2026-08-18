package sh.roadmap.sep.catalog.infrastructure.output.jpa.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import sh.roadmap.sep.catalog.domain.model.Category;
import sh.roadmap.sep.catalog.infrastructure.output.jpa.entity.CategoryEntity;

@Mapper(componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        unmappedSourcePolicy = ReportingPolicy.IGNORE)
public interface CategoryJpaMapper {
    CategoryEntity toEntity(Category category);

    Category toModel(CategoryEntity category);
}
