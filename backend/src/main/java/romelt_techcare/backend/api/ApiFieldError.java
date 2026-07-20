package romelt_techcare.backend.api;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * ================================================================
 * ROMELT TECHCARE — API FIELD ERROR
 * ================================================================
 *
 * Purpose:
 * Represents one field-level validation failure returned to the
 * frontend.
 *
 * Responsibilities:
 * - Identifies the rejected request field.
 * - Provides a user-readable validation message.
 * - Optionally exposes the rejected value when safe.
 *
 * Security:
 * Rejected passwords, secrets, payment information, and other
 * sensitive values must never be included.
 * ================================================================
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiFieldError(
        String field,
        String message,
        Object rejectedValue
) {
}