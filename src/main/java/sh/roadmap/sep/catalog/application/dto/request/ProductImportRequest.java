package sh.roadmap.sep.catalog.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.io.InputStream;

public record ProductImportRequest(
        @NotBlank(message = "File extension cannot be blank")
        String fileExtension,
        @NotBlank(message = "File name cannot be blank")
        String fileName,
        @NotNull(message = "Input stream cannot be null")
        InputStream inputStream) {
}
