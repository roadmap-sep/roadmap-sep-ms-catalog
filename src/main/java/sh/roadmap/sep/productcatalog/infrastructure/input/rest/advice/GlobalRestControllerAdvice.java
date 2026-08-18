package sh.roadmap.sep.productcatalog.infrastructure.input.rest.advice;

import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.accept.InvalidApiVersionException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import sh.roadmap.sep.productcatalog.application.exception.ProductImportException;
import sh.roadmap.sep.productcatalog.application.exception.ProductImportStrategyException;
import sh.roadmap.sep.productcatalog.domain.exception.CategoryAlreadyExistsException;
import sh.roadmap.sep.productcatalog.domain.exception.CategoryNotFoundException;
import sh.roadmap.sep.productcatalog.domain.exception.ProductAlreadyExistsException;
import sh.roadmap.sep.productcatalog.domain.exception.ProductNotFoundException;

import java.time.Instant;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalRestControllerAdvice {
    @Value("${spring.servlet.multipart.max-file-size}")
    private String maxFileSize;
    @Value("${spring.mvc.apiversion.supported}")
    private String apiVersionSupported;
    private static final String UNKNOWN_ERROR = "Unknown Error";
    private static final String MESSAGE_FORMAT_RESPONSE = "traceId: %s | %s | {%s}";

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> genericException(Exception exception) {
        return buildResponse(UNKNOWN_ERROR, exception, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler({CategoryNotFoundException.class, ProductNotFoundException.class})
    public ResponseEntity<ErrorResponse> notFound(RuntimeException exception) {
        return buildResponse(exception, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler({CategoryAlreadyExistsException.class, ProductAlreadyExistsException.class})
    public ResponseEntity<ErrorResponse> alreadyExist(RuntimeException exception) {
        return buildResponse(exception, HttpStatus.CONFLICT);
    }

    @ExceptionHandler({ProductImportException.class, ProductImportStrategyException.class})
    public ResponseEntity<ErrorResponse> badRequest(RuntimeException exception) {
        return buildResponse(exception, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> methodArgumentNotValidException(MethodArgumentNotValidException exception) {
        UUID uuid = UUID.randomUUID();
        String msg = String.format(MESSAGE_FORMAT_RESPONSE, uuid, exception.getMessage(), exception);
        log.error(msg);
        HttpStatus status = HttpStatus.BAD_REQUEST;
        return exception.getFieldErrors()
                .stream()
                .collect(Collectors.collectingAndThen(Collectors.toMap(FieldError::getField,
                                FieldError::getDefaultMessage, (o, o2) -> o + ", " + o2),
                        map -> ResponseEntity.status(status)
                                .body(ErrorResponse.builder()
                                        .title(status.getReasonPhrase())
                                        .status(status.value())
                                        .detail(map.toString())
                                        .traceId(uuid)
                                        .timestamp(Instant.now())
                                        .build())));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> maxUploadSizeExceededException(MaxUploadSizeExceededException exception) {
        return buildResponse(String.format("The maximum file size is %s.", maxFileSize),
                exception, HttpStatus.CONTENT_TOO_LARGE);
    }

    @ExceptionHandler(InvalidApiVersionException.class)
    public ResponseEntity<ErrorResponse> invalidApiVersionException(InvalidApiVersionException exception) {
        String msg = String.format("Version %s is not available. Supported versions: %s.",
                exception.getVersion(), apiVersionSupported);
        return buildResponse(msg, exception, HttpStatus.BAD_REQUEST);
    }

    private ResponseEntity<ErrorResponse> buildResponse(Exception exception, HttpStatus status) {
        return this.buildResponse(exception.getMessage(), exception, status);
    }

    private ResponseEntity<ErrorResponse> buildResponse(String detail, Exception exception, HttpStatus status) {
        UUID uuid = UUID.randomUUID();
        String msg = String.format(MESSAGE_FORMAT_RESPONSE, uuid, exception.getMessage(), exception);
        log.error(msg);
        return ResponseEntity.status(status)
                .body(ErrorResponse.builder()
                        .title(status.getReasonPhrase())
                        .status(status.value())
                        .detail(detail)
                        .traceId(uuid)
                        .timestamp(Instant.now())
                        .build());
    }

    @Builder
    public record ErrorResponse(
            String title,
            Integer status,
            String detail,
            UUID traceId,
            Instant timestamp) {
    }
}
