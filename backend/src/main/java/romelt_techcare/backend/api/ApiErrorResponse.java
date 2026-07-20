package romelt_techcare.backend.api;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * ================================================================
 * ROMELT TECHCARE — STANDARD API ERROR RESPONSE
 * ================================================================
 *
 * Purpose:
 * Provides one consistent error structure for validation, business,
 * authorization, and unexpected backend failures.
 *
 * Responsibilities:
 * - Provides the HTTP status and error message.
 * - Identifies the request path.
 * - Provides field-level validation failures.
 * - Supports the frontend ApiError implementation.
 * ================================================================
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record ApiErrorResponse(
        boolean success,
        int status,
        String error,
        String message,
        String path,
        Instant timestamp,
        List<ApiFieldError> fieldErrors,
        Map<String, String> validationErrors
) {
}