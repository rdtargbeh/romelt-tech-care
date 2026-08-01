package romelt_techcare.backend.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import romelt_techcare.backend.api.ApiErrorResponse;
import romelt_techcare.backend.api.ApiResponse;
import romelt_techcare.backend.dto.AdminCreateUserRequest;
import romelt_techcare.backend.dto.AdminJwtPrincipal;
import romelt_techcare.backend.dto.AdminResetPasswordRequest;
import romelt_techcare.backend.dto.AdminUpdateUserRequest;
import romelt_techcare.backend.dto.AdminUserResponse;
import romelt_techcare.backend.dto.AdminUserStatusRequest;
import romelt_techcare.backend.enums.AdminRole;
import romelt_techcare.backend.enums.AdminStatus;
import romelt_techcare.backend.exception.AdminUserManagementException;
import romelt_techcare.backend.service.implement.AdminUserManagementService;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * ================================================================
 * ROMELT TECHCARE — ADMIN USER MANAGEMENT CONTROLLER
 * ================================================================
 *
 * Purpose:
 * Exposes protected API endpoints used by SUPER_ADMIN users to manage
 * administrator and employee accounts.
 *
 * Responsibilities:
 * - Creates administrator employee accounts.
 * - Lists and filters administrator accounts.
 * - Retrieves one administrator account.
 * - Updates administrator profile and role information.
 * - Changes administrator account status.
 * - Resets temporary passwords.
 * - Deletes administrator accounts when permitted.
 *
 * Authorization:
 * Every endpoint requires ROLE_SUPER_ADMIN.
 *
 * Endpoint base path:
 * /api/v1/admin/users
 *
 * Security rules:
 * - Passwords and password hashes are never returned.
 * - Temporary passwords are accepted only over protected requests.
 * - The final active SUPER_ADMIN is protected.
 * ================================================================
 */
@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class AdminUserController {

    private final AdminUserManagementService
            adminUserManagementService;

    /**
     * Creates a new administrator employee account.
     */
    @PostMapping
    public ResponseEntity<ApiResponse<AdminUserResponse>> createAdministrator(
            @AuthenticationPrincipal
            AdminJwtPrincipal principal,

            @Valid
            @RequestBody
            AdminCreateUserRequest request,

            HttpServletRequest httpRequest
    ) {
        AdminUserResponse response =
                adminUserManagementService
                        .createAdministrator(
                                principal,
                                request
                        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        ApiResponse.success(
                                "Administrator account created successfully.",
                                response,
                                httpRequest.getRequestURI()
                        )
                );
    }

    /**
     * Lists administrator accounts.
     *
     * Optional filters:
     * - role=SUPER_ADMIN|ADMIN|STAFF
     * - status=ACTIVE|INACTIVE|LOCKED
     */
    @GetMapping
    public ResponseEntity<
            ApiResponse<List<AdminUserResponse>>
            > getAdministrators(
            @AuthenticationPrincipal
            AdminJwtPrincipal principal,

            @RequestParam(required = false)
            AdminRole role,

            @RequestParam(required = false)
            AdminStatus status,

            HttpServletRequest httpRequest
    ) {
        List<AdminUserResponse> response =
                adminUserManagementService
                        .getAdministrators(
                                principal,
                                role,
                                status
                        );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Administrator accounts retrieved successfully.",
                        response,
                        httpRequest.getRequestURI()
                )
        );
    }

    /**
     * Retrieves one administrator account.
     */
    @GetMapping("/{adminUserId}")
    public ResponseEntity<ApiResponse<AdminUserResponse>>
    getAdministrator(
            @AuthenticationPrincipal
            AdminJwtPrincipal principal,

            @PathVariable
            UUID adminUserId,

            HttpServletRequest httpRequest
    ) {
        AdminUserResponse response =
                adminUserManagementService
                        .getAdministrator(
                                principal,
                                adminUserId
                        );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Administrator account retrieved successfully.",
                        response,
                        httpRequest.getRequestURI()
                )
        );
    }

    /**
     * Updates an administrator profile and role.
     */
    @PutMapping("/{adminUserId}")
    public ResponseEntity<ApiResponse<AdminUserResponse>>
    updateAdministrator(
            @AuthenticationPrincipal
            AdminJwtPrincipal principal,

            @PathVariable
            UUID adminUserId,

            @Valid
            @RequestBody
            AdminUpdateUserRequest request,

            HttpServletRequest httpRequest
    ) {
        AdminUserResponse response =
                adminUserManagementService
                        .updateAdministrator(
                                principal,
                                adminUserId,
                                request
                        );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Administrator account updated successfully.",
                        response,
                        httpRequest.getRequestURI()
                )
        );
    }

    /**
     * Activates, disables, locks, or unlocks an account.
     */
    @PatchMapping("/{adminUserId}/status")
    public ResponseEntity<ApiResponse<AdminUserResponse>>
    changeAdministratorStatus(
            @AuthenticationPrincipal
            AdminJwtPrincipal principal,

            @PathVariable
            UUID adminUserId,

            @Valid
            @RequestBody
            AdminUserStatusRequest request,

            HttpServletRequest httpRequest
    ) {
        AdminUserResponse response =
                adminUserManagementService
                        .changeAdministratorStatus(
                                principal,
                                adminUserId,
                                request
                        );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Administrator account status updated successfully.",
                        response,
                        httpRequest.getRequestURI()
                )
        );
    }

    /**
     * Assigns another administrator a temporary password.
     */
    @PostMapping("/{adminUserId}/reset-password")
    public ResponseEntity<ApiResponse<AdminUserResponse>>
    resetAdministratorPassword(
            @AuthenticationPrincipal
            AdminJwtPrincipal principal,

            @PathVariable
            UUID adminUserId,

            @Valid
            @RequestBody
            AdminResetPasswordRequest request,

            HttpServletRequest httpRequest
    ) {
        AdminUserResponse response =
                adminUserManagementService
                        .resetAdministratorPassword(
                                principal,
                                adminUserId,
                                request
                        );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Temporary password assigned successfully. "
                                + "The administrator must change it after signing in.",
                        response,
                        httpRequest.getRequestURI()
                )
        );
    }

    /**
     * Deletes an administrator account.
     */
    @DeleteMapping("/{adminUserId}")
    public ResponseEntity<Void> deleteAdministrator(
            @AuthenticationPrincipal
            AdminJwtPrincipal principal,

            @PathVariable
            UUID adminUserId
    ) {
        adminUserManagementService
                .deleteAdministrator(
                        principal,
                        adminUserId
                );

        return ResponseEntity.noContent().build();
    }

    /**
     * Converts controlled user-management failures into the same error
     * shape used by the rest of the backend.
     *
     * This local handler avoids requiring changes to the already
     * completed GlobalExceptionHandler.
     */
    @org.springframework.web.bind.annotation.ExceptionHandler(
            AdminUserManagementException.class
    )
    public ResponseEntity<ApiErrorResponse>
    handleAdminUserManagementException(
            AdminUserManagementException exception,
            HttpServletRequest request
    ) {
        ApiErrorResponse response =
                new ApiErrorResponse(
                        false,
                        exception.getStatus().value(),
                        exception.getStatus().getReasonPhrase(),
                        exception.getMessage(),
                        request.getRequestURI(),
                        Instant.now(),
                        List.of(),
                        Map.of(
                                "errorCode",
                                exception.getErrorCode()
                        )
                );

        return ResponseEntity
                .status(exception.getStatus())
                .body(response);
    }
}