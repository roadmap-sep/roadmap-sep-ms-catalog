package sh.roadmap.sep.productcatalog.application.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import org.hibernate.validator.constraints.URL;

import java.math.BigDecimal;
import java.util.Set;

@Builder
public record ProductRequest(
        @NotBlank(message = "SKU cannot be blank")
        @Size(min = 3, max = 50, message = "SKU must be between 3 and 50 characters")
        @Pattern(regexp = "^[A-Za-z0-9\\-_]+$",
                message = "SKU can only contain letters, numbers, hyphens, and underscores")
        String sku,

        @NotBlank(message = "Product name cannot be blank")
        @Size(min = 2, max = 100, message = "Product name must be between 2 and 100 characters")
        String name,

        @NotBlank(message = "Description cannot be blank")
        @Size(max = 500, message = "Description cannot exceed 500 characters")
        String description,

        @NotEmpty(message = "Product must have at least one category")
        Set<@NotNull(message = "Categories id cannot be empty") Long> categoryIds,

        @NotNull(message = "Price cannot be null")
        @DecimalMin(value = "0.01", message = "Price must be greater than zero")
        @Digits(integer = 8, fraction = 2, message = "Invalid price format (maximum 8 integers and 2 fractions)")
        BigDecimal price,

        @Min(value = 0, message = "Stock cannot be negative")
        @NotNull(message = "Stock cannot be null")
        Integer stock,

        @NotBlank(message = "Main image URL cannot be blank")
        @URL(message = "Must be a valid URL")
        String mainImageUrl,

        @NotNull(message = "Weight cannot be null")
        @Positive(message = "Weight must be greater than zero")
        Double weight) {
}
