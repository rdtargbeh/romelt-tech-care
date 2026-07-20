package romelt_techcare.backend.api;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;

/**
 * ================================================================
 * ROMELT TECHCARE — STANDARD API RESPONSE
 * ================================================================
 *
 * Purpose:
 * Provides one consistent success-response structure for all backend
 * endpoints consumed by the React frontend.
 *
 * Responsibilities:
 * - Indicates whether the operation succeeded.
 * - Provides a user-readable response message.
 * - Contains the endpoint-specific response data.
 * - Records the response timestamp and request path.
 *
 * Real-data integration:
 * All public, customer, and administrative controllers should return
 * this structure unless an endpoint has a specialized response format.
 * ================================================================
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(
        boolean success,
        String message,
        T data,
        Instant timestamp,
        String path
) {

    public static <T> ApiResponse<T> success(
            String message,
            T data,
            String path
    ) {
        return new ApiResponse<>(
                true,
                message,
                data,
                Instant.now(),
                path
        );
    }
}