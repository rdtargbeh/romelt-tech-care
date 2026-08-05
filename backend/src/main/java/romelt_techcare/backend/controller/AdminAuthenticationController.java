package romelt_techcare.backend.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import romelt_techcare.backend.dto.AdminChangePasswordRequest;
import romelt_techcare.backend.dto.AdminJwtPrincipal;
import romelt_techcare.backend.dto.AdminLoginRequest;
import romelt_techcare.backend.dto.AdminLoginResponse;
import romelt_techcare.backend.dto.AdminProfileResponse;
import romelt_techcare.backend.service.AdminAuthenticationService;

/**
 * ================================================================
 * ROMELT TECHCARE — ADMIN AUTHENTICATION CONTROLLER
 * ================================================================
 *
 * Purpose:
 * Exposes administrator authentication endpoints for the Romelt
 * TechCare administration portal.
 *
 * Responsibilities:
 * - Accepts administrator login requests.
 * - Returns JWT access tokens after successful authentication.
 * - Returns the currently authenticated administrator profile.
 * - Allows authenticated administrators to change passwords.
 * - Applies request validation before service-layer processing.
 *
 * Security behavior:
 * - POST /login is publicly accessible.
 * - GET /me requires a valid administrator JWT.
 * - POST /change-password requires a valid administrator JWT.
 * - Password values are never returned by this controller.
 * - AuthenticationPrincipal contains the validated JWT identity.
 *
 * Endpoint base path:
 * /api/v1/admin/auth
 *
 * Real-data integration:
 * Requests are delegated to AdminAuthenticationService, which performs
 * database authentication, lockout tracking, JWT generation, account
 * verification, and password updates.
 * ================================================================
 */
@RestController
@RequestMapping("/api/v1/admin/auth")
@RequiredArgsConstructor
public class AdminAuthenticationController {

    private final AdminAuthenticationService authenticationService;

    /**
     * Authenticates an administrator and returns a signed JWT.
     *
     * Endpoint:
     * POST /api/v1/admin/auth/login
     *
     * @param request administrator login credentials
     * @return JWT access token and administrator profile
     */
    @PostMapping("/login")
    public ResponseEntity<AdminLoginResponse> login(
            @Valid @RequestBody AdminLoginRequest request
    ) {
        AdminLoginResponse response =
                authenticationService.login(request);

        return ResponseEntity.ok(response);
    }

    /**
     * Returns the currently authenticated administrator profile.
     *
     * Endpoint:
     * GET /api/v1/admin/auth/me
     *
     * @param principal authenticated JWT principal
     * @return current administrator profile
     */
    @GetMapping("/me")
    public ResponseEntity<AdminProfileResponse> getCurrentAdministrator(
            @AuthenticationPrincipal AdminJwtPrincipal principal
    ) {
        AdminProfileResponse response =
                authenticationService.getCurrentProfile(principal);

        return ResponseEntity.ok(response);
    }

    /**
     * Changes the authenticated administrator's password.
     *
     * Endpoint:
     * POST /api/v1/admin/auth/change-password
     *
     * @param principal authenticated JWT principal
     * @param request password-change request
     * @return updated administrator profile
     */
    @PostMapping("/change-password")
    public ResponseEntity<AdminProfileResponse> changePassword(
            @AuthenticationPrincipal AdminJwtPrincipal principal,
            @Valid @RequestBody AdminChangePasswordRequest request
    ) {
        AdminProfileResponse response =
                authenticationService.changePassword(
                        principal,
                        request
                );

        return ResponseEntity.ok(response);
    }
}