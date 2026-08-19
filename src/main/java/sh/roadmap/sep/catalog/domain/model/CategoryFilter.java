package sh.roadmap.sep.catalog.domain.model;

public record CategoryFilter(
        String name,
        String slug,
        Long parentId,
        Boolean isActive) {
}
