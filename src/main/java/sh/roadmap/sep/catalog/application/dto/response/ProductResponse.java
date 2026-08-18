package sh.roadmap.sep.catalog.application.dto.response;

import lombok.Builder;

import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;

@Builder(toBuilder = true)
public record ProductResponse(
        UUID id,
        String sku,
        String name,
        Set<String> categories,
        String description,
        BigDecimal price,
        String mainImageUrl,
        Integer stock,
        Double weight,
        boolean active) {
}
