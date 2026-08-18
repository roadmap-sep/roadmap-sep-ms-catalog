package sh.roadmap.sep.catalog.application.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import sh.roadmap.sep.catalog.application.dto.response.CategoryResponse;
import sh.roadmap.sep.catalog.domain.model.Category;
import sh.roadmap.sep.catalog.domain.util.Page;

@Mapper(componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        unmappedSourcePolicy = ReportingPolicy.IGNORE)
public interface CategoryDtoMapper {
    CategoryResponse toDto(Category category);

    Page<CategoryResponse> toDto(Page<Category> categories);
}
