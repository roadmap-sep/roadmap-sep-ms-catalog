package sh.roadmap.sep.productcatalog.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CategoryRequest(
        @NotBlank(message = "Category name is required")
        @Size(max = 100, message = "Category name must not exceed 100 characters")
        String name,

        @Size(max = 150, message = "Slug must not exceed 150 characters")
        @Pattern(regexp = "^[a-z0-9-]*$", message = "Slug can only contain lowercase letters, numbers, and hyphens")
        String slug,

        @Positive(message = "Parent ID must be a positive number")
        Long parentId) {
}
