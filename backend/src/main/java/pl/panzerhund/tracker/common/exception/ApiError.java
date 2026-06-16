package pl.panzerhund.tracker.common.exception;

import java.time.Instant;
import java.util.List;

/** Uniform error response body. {@code fieldErrors} is null for non-validation errors. */
public record ApiError(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path,
        List<FieldError> fieldErrors
) {
    public record FieldError(String field, String message) {
    }
}
