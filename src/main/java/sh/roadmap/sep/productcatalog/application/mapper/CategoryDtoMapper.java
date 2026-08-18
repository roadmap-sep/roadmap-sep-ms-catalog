package sh.roadmap.sep.productcatalog.application.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import sh.roadmap.sep.productcatalog.application.dto.response.CategoryResponse;
import sh.roadmap.sep.productcatalog.domain.model.Category;
import sh.roadmap.sep.productcatalog.domain.util.Page;

@Mapper(componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        unmappedSourcePolicy = ReportingPolicy.IGNORE)
public interface CategoryDtoMapper {
    CategoryResponse toDto(Category category);

    Page<CategoryResponse> toDto(Page<Category> categories);
}
