package sh.roadmap.sep.productcatalog.application.dto.response;

public record CategoryResponse(
        Long id,
        String name,
        String slug,
        Long parentId,
        boolean active) {
}
