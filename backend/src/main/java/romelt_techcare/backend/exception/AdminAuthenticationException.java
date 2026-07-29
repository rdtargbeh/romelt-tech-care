package romelt_techcare.backend.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * ================================================================
 * ROMELT TECHCARE — ADMIN AUTHENTICATION EXCEPTION
 * ================================================================
 *
 * Purpose:
 * Represents a controlled administrator authentication or account
 * security failure.
 *
 * Responsibilities:
 * - Carries the correct HTTP status for the failure.
 * - Provides a stable application error code.
 * - Provides a safe client-facing message.
 * - Prevents expected authentication failures from becoming HTTP 500
 *   responses.
 *
 * Examples:
 * - Invalid administrator credentials.
 * - Inactive administrator account.
 * - Permanently locked administrator account.
 * - Temporarily locked administrator account.
 * - Invalid or stale authenticated administrator identity.
 * - Incorrect current password.
 *
 * Security rules:
 * - Messages must not reveal whether an unknown email exists.
 * - Passwords, hashes, JWT values, and internal exception details must
 *   never be included.
 * - Unexpected infrastructure failures must not use this exception.
 *
 * Real-data integration:
 * Thrown by AdminAuthenticationService and converted into a standard
 * ApiErrorResponse by GlobalExceptionHandler.
 * ================================================================
 */
@Getter
public class AdminAuthenticationException extends RuntimeException {

    /**
     * HTTP status returned to the API client.
     */
    private final HttpStatus status;

    /**
     * Stable frontend-readable error identifier.
     */
    private final String errorCode;

    /**
     * Creates a controlled administrator authentication exception.
     *
     * @param status HTTP response status
     * @param errorCode stable application error code
     * @param message safe client-facing message
     */
    public AdminAuthenticationException(
            HttpStatus status,
            String errorCode,
            String message
    ) {
        super(requireMessage(message));

        if (status == null) {
            throw new IllegalArgumentException(
                    "Authentication exception status must not be null."
            );
        }

        if (errorCode == null || errorCode.isBlank()) {
            throw new IllegalArgumentException(
                    "Authentication exception error code must not be blank."
            );
        }

        this.status = status;
        this.errorCode = errorCode.trim();
    }

    /**
     * Creates an invalid-credentials exception.
     *
     * The same response is used for an unknown email and an incorrect
     * password to reduce account-enumeration risk.
     */
    public static AdminAuthenticationException invalidCredentials() {
        return new AdminAuthenticationException(
                HttpStatus.UNAUTHORIZED,
                "INVALID_ADMIN_CREDENTIALS",
                "The email address or password is incorrect."
        );
    }

    /**
     * Creates an inactive-account exception.
     */
    public static AdminAuthenticationException inactiveAccount() {
        return new AdminAuthenticationException(
                HttpStatus.FORBIDDEN,
                "ADMIN_ACCOUNT_INACTIVE",
                "This administrator account is inactive."
        );
    }

    /**
     * Creates a permanently locked-account exception.
     */
    public static AdminAuthenticationException lockedAccount() {
        return new AdminAuthenticationException(
                HttpStatus.LOCKED,
                "ADMIN_ACCOUNT_LOCKED",
                "This administrator account is locked."
        );
    }

    /**
     * Creates a temporarily locked-account exception.
     */
    public static AdminAuthenticationException temporarilyLocked() {
        return new AdminAuthenticationException(
                HttpStatus.LOCKED,
                "ADMIN_ACCOUNT_TEMPORARILY_LOCKED",
                "This administrator account is temporarily locked. Please try again later."
        );
    }

    /**
     * Creates an exception for an administrator account that no
     * longer exists.
     */
    public static AdminAuthenticationException accountNotFound() {
        return new AdminAuthenticationException(
                HttpStatus.UNAUTHORIZED,
                "ADMIN_ACCOUNT_NOT_FOUND",
                "The authenticated administrator account is no longer available."
        );
    }

    /**
     * Creates an exception when token identity information no longer
     * matches the administrator account.
     */
    public static AdminAuthenticationException staleAuthentication() {
        return new AdminAuthenticationException(
                HttpStatus.UNAUTHORIZED,
                "ADMIN_AUTHENTICATION_STALE",
                "The administrator account has changed. Please sign in again."
        );
    }

    /**
     * Creates an exception when the current password is incorrect.
     */
    public static AdminAuthenticationException incorrectCurrentPassword() {
        return new AdminAuthenticationException(
                HttpStatus.BAD_REQUEST,
                "CURRENT_PASSWORD_INCORRECT",
                "The current password is incorrect."
        );
    }

    /**
     * Creates an exception when the new password confirmation does not
     * match.
     */
    public static AdminAuthenticationException passwordConfirmationMismatch() {
        return new AdminAuthenticationException(
                HttpStatus.BAD_REQUEST,
                "PASSWORD_CONFIRMATION_MISMATCH",
                "The new password and password confirmation do not match."
        );
    }

    /**
     * Creates an exception when the selected password matches the
     * current password.
     */
    public static AdminAuthenticationException passwordUnchanged() {
        return new AdminAuthenticationException(
                HttpStatus.BAD_REQUEST,
                "PASSWORD_UNCHANGED",
                "The new password must be different from the current password."
        );
    }

    /**
     * Ensures that exception messages are safe and non-empty.
     */
    private static String requireMessage(String message) {
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException(
                    "Authentication exception message must not be blank."
            );
        }

        return message.trim();
    }
}