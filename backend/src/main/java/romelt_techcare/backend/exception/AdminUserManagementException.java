package romelt_techcare.backend.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * ================================================================
 * ROMELT TECHCARE — ADMIN USER MANAGEMENT EXCEPTION
 * ================================================================
 *
 * Purpose:
 * Represents a controlled administrator user-management failure.
 *
 * Responsibilities:
 * - Carries the HTTP response status.
 * - Carries a stable application error code.
 * - Carries a safe client-facing message.
 * - Prevents expected business-rule failures from becoming HTTP 500
 *   responses.
 *
 * Examples:
 * - Administrator not found.
 * - Duplicate administrator email.
 * - Unsafe self-management operation.
 * - Attempt to remove the final active SUPER_ADMIN.
 * - Invalid temporary password.
 *
 * Security rules:
 * - Passwords and password hashes must never appear in messages.
 * - Internal database or infrastructure details must not be exposed.
 * ================================================================
 */
@Getter
public class AdminUserManagementException extends RuntimeException {

    private final HttpStatus status;
    private final String errorCode;

    public AdminUserManagementException(
            HttpStatus status,
            String errorCode,
            String message
    ) {
        super(requireMessage(message));

        if (status == null) {
            throw new IllegalArgumentException(
                    "HTTP status must not be null."
            );
        }

        if (errorCode == null || errorCode.isBlank()) {
            throw new IllegalArgumentException(
                    "Error code must not be blank."
            );
        }

        this.status = status;
        this.errorCode = errorCode.trim();
    }

    public static AdminUserManagementException notFound() {
        return new AdminUserManagementException(
                HttpStatus.NOT_FOUND,
                "ADMIN_USER_NOT_FOUND",
                "The requested administrator account could not be found."
        );
    }

    public static AdminUserManagementException duplicateEmail() {
        return new AdminUserManagementException(
                HttpStatus.CONFLICT,
                "ADMIN_EMAIL_ALREADY_EXISTS",
                "An administrator account already uses this email address."
        );
    }

    public static AdminUserManagementException selfStatusChangeNotAllowed() {
        return new AdminUserManagementException(
                HttpStatus.BAD_REQUEST,
                "ADMIN_SELF_STATUS_CHANGE_NOT_ALLOWED",
                "You cannot disable or lock your own administrator account."
        );
    }

    public static AdminUserManagementException selfDeleteNotAllowed() {
        return new AdminUserManagementException(
                HttpStatus.BAD_REQUEST,
                "ADMIN_SELF_DELETE_NOT_ALLOWED",
                "You cannot delete your own administrator account."
        );
    }

    public static AdminUserManagementException selfPasswordResetNotAllowed() {
        return new AdminUserManagementException(
                HttpStatus.BAD_REQUEST,
                "ADMIN_SELF_PASSWORD_RESET_NOT_ALLOWED",
                "Use the authenticated password-change page to change your own password."
        );
    }

    public static AdminUserManagementException finalSuperAdminRequired() {
        return new AdminUserManagementException(
                HttpStatus.CONFLICT,
                "FINAL_ACTIVE_SUPER_ADMIN_REQUIRED",
                "At least one active SUPER_ADMIN account must remain available."
        );
    }

    public static AdminUserManagementException invalidTemporaryPassword(
            String message
    ) {
        return new AdminUserManagementException(
                HttpStatus.BAD_REQUEST,
                "TEMPORARY_PASSWORD_INVALID",
                requireMessage(message)
        );
    }

    public static AdminUserManagementException unauthorizedManager() {
        return new AdminUserManagementException(
                HttpStatus.FORBIDDEN,
                "SUPER_ADMIN_REQUIRED",
                "Only a SUPER_ADMIN may manage administrator accounts."
        );
    }

    private static String requireMessage(
            String message
    ) {
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException(
                    "Exception message must not be blank."
            );
        }

        return message.trim();
    }
}