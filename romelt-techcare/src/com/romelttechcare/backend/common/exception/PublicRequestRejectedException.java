package com.romelttechcare.backend.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * ================================================================
 * ROMELT TECHCARE — PUBLIC REQUEST REJECTED EXCEPTION
 * ================================================================
 *
 * Purpose:
 * Represents a controlled rejection of a public website request.
 *
 * Examples:
 * - Duplicate or abusive submissions.
 * - Unsupported service selections.
 * - Requests blocked by application business rules.
 * - Rate-limit or availability restrictions.
 * ================================================================
 */
@Getter
public class PublicRequestRejectedException
        extends RuntimeException {

    private final HttpStatus status;

    public PublicRequestRejectedException(
            HttpStatus status,
            String message
    ) {
        super(message);
        this.status = status;
    }
}