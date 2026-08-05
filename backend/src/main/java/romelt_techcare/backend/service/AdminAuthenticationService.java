package romelt_techcare.backend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import romelt_techcare.backend.dto.AdminChangePasswordRequest;
import romelt_techcare.backend.dto.AdminJwtPrincipal;
import romelt_techcare.backend.dto.AdminJwtToken;
import romelt_techcare.backend.dto.AdminLoginRequest;
import romelt_techcare.backend.dto.AdminLoginResponse;
import romelt_techcare.backend.dto.AdminProfileResponse;
import romelt_techcare.backend.entity.AdminUser;
import romelt_techcare.backend.enums.AdminStatus;
import romelt_techcare.backend.exception.AdminAuthenticationException;
import romelt_techcare.backend.repository.AdminUserRepository;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;

/**
 * ================================================================
 * ROMELT TECHCARE — ADMIN AUTHENTICATION SERVICE
 * ================================================================
 *
 * Purpose:
 * Implements administrator authentication and password-management
 * operations for the Romelt TechCare administration portal.
 *
 * Responsibilities:
 * - Authenticates administrators by email and password.
 * - Tracks failed login attempts.
 * - Temporarily locks accounts after repeated failures.
 * - Rejects inactive and permanently locked accounts.
 * - Clears expired temporary account locks.
 * - Records successful administrator logins.
 * - Generates signed JWT access tokens.
 * - Returns the authenticated administrator profile.
 * - Changes administrator passwords securely.
 * - Upgrades older password hashes when appropriate.
 *
 * Security rules:
 * - Plain-text passwords are never stored or returned.
 * - Unknown email and incorrect password use the same response.
 * - Login lookups use a pessimistic database lock.
 * - Failed-attempt counters are updated transactionally.
 * - JWTs are generated only after successful authentication.
 * - The current password must be verified before password changes.
 * - The new password must satisfy AdminPasswordPolicyService.
 *
 * Real-data integration:
 * Used by AdminAuthenticationController for:
 * - POST /api/v1/admin/auth/login
 * - GET  /api/v1/admin/auth/me
 * - POST /api/v1/admin/auth/change-password
 * ================================================================
 */
@Service
@RequiredArgsConstructor
public class AdminAuthenticationService {

    private static final int MAXIMUM_FAILED_LOGIN_ATTEMPTS = 5;

    private static final Duration TEMPORARY_LOCK_DURATION =
            Duration.ofMinutes(15);

    /**
     * Valid BCrypt hash used only to reduce timing differences when
     * an administrator email does not exist.
     *
     * It does not represent a usable application password.
     */
    private static final String DUMMY_PASSWORD_HASH =
            "$2a$12$K4YfLzZPqhjskQmHbnGIXuDqfVKPqv9Bzn6u9LnUVk5VdWYfEcE3i";

    private final AdminUserRepository adminUserRepository;
    private final AdminPasswordPolicyService passwordPolicyService;
    private final JwtService jwtService;

    /**
     * Authenticates an administrator and generates an access token.
     */
    @Transactional
    public AdminLoginResponse login(
            AdminLoginRequest request
    ) {
        requireLoginRequest(request);

        String normalizedEmail =
                normalizeEmail(request.email());

        AdminUser administrator = adminUserRepository
                .findByEmailIgnoreCaseForAuthentication(
                        normalizedEmail
                )
                .orElse(null);

        if (administrator == null) {
            performDummyPasswordCheck(request.password());

            throw AdminAuthenticationException
                    .invalidCredentials();
        }

        clearExpiredTemporaryLock(administrator);
        validateAccountAvailability(administrator);

        boolean passwordMatches =
                passwordPolicyService.matches(
                        request.password(),
                        administrator.getPasswordHash()
                );

        if (!passwordMatches) {
            processFailedLogin(administrator);

            adminUserRepository.save(administrator);

            /*
             * When the failed attempt caused a temporary lock, return
             * the locked response immediately rather than presenting
             * another generic credential failure.
             */
            if (administrator.getLockedUntil() != null
                    && administrator.getLockedUntil()
                    .isAfter(Instant.now())) {

                throw AdminAuthenticationException
                        .temporarilyLocked();
            }

            throw AdminAuthenticationException
                    .invalidCredentials();
        }

        upgradePasswordHashWhenRequired(
                administrator,
                request.password()
        );

        administrator.recordSuccessfulLogin();

        AdminUser savedAdministrator =
                adminUserRepository.save(administrator);

        AdminJwtToken jwtToken =
                jwtService.generateAccessToken(
                        savedAdministrator
                );

        AdminProfileResponse profile =
                AdminProfileResponse.from(
                        savedAdministrator
                );

        return AdminLoginResponse.success(
                jwtToken.value(),
                jwtToken.expiresAt(),
                jwtToken.expiresInSeconds(),
                profile
        );
    }

    /**
     * Returns the currently authenticated administrator profile.
     */
    @Transactional(readOnly = true)
    public AdminProfileResponse getCurrentProfile(
            AdminJwtPrincipal principal
    ) {
        AdminUser administrator =
                findAuthenticatedAdministrator(principal);

        validateAccountAvailability(administrator);

        return AdminProfileResponse.from(administrator);
    }

    /**
     * Changes the authenticated administrator password.
     */
    @Transactional
    public AdminProfileResponse changePassword(
            AdminJwtPrincipal principal,
            AdminChangePasswordRequest request
    ) {
        requirePrincipal(principal);
        requireChangePasswordRequest(request);

        AdminUser administrator = adminUserRepository
                .findById(principal.adminUserId())
                .orElseThrow(
                        AdminAuthenticationException
                                ::accountNotFound
                );

        validatePrincipalMatchesAccount(
                principal,
                administrator
        );

        validateAccountAvailability(administrator);

        boolean currentPasswordMatches =
                passwordPolicyService.matches(
                        request.currentPassword(),
                        administrator.getPasswordHash()
                );

        if (!currentPasswordMatches) {
            throw AdminAuthenticationException
                    .incorrectCurrentPassword();
        }

        if (!request.passwordsMatch()) {
            throw AdminAuthenticationException
                    .passwordConfirmationMismatch();
        }

        boolean sameAsCurrentPassword =
                passwordPolicyService.matches(
                        request.newPassword(),
                        administrator.getPasswordHash()
                );

        if (sameAsCurrentPassword) {
            throw AdminAuthenticationException
                    .passwordUnchanged();
        }

        final String encodedPassword;

        try {
            encodedPassword =
                    passwordPolicyService.encodeNewPassword(
                            request.newPassword(),
                            administrator.getEmail(),
                            administrator.getFirstName(),
                            administrator.getLastName()
                    );
        } catch (IllegalArgumentException exception) {
            throw new AdminAuthenticationException(
                    org.springframework.http.HttpStatus.BAD_REQUEST,
                    "NEW_PASSWORD_INVALID",
                    exception.getMessage()
            );
        }

        administrator.changePassword(
                encodedPassword,
                false
        );

        AdminUser savedAdministrator =
                adminUserRepository.save(administrator);

        return AdminProfileResponse.from(
                savedAdministrator
        );
    }

    /**
     * Reloads the administrator represented by a JWT principal.
     */
    private AdminUser findAuthenticatedAdministrator(
            AdminJwtPrincipal principal
    ) {
        requirePrincipal(principal);

        AdminUser administrator = adminUserRepository
                .findById(principal.adminUserId())
                .orElseThrow(
                        AdminAuthenticationException
                                ::accountNotFound
                );

        validatePrincipalMatchesAccount(
                principal,
                administrator
        );

        return administrator;
    }

    /**
     * Ensures the JWT identity still matches the database account.
     *
     * Existing access tokens become unusable when the administrator
     * email or role changes.
     */
    private void validatePrincipalMatchesAccount(
            AdminJwtPrincipal principal,
            AdminUser administrator
    ) {
        if (administrator.getAdminUserId() == null
                || !administrator.getAdminUserId()
                .equals(principal.adminUserId())) {

            throw AdminAuthenticationException
                    .staleAuthentication();
        }

        String accountEmail =
                normalizeEmail(administrator.getEmail());

        String principalEmail =
                normalizeEmail(principal.email());

        if (!accountEmail.equals(principalEmail)) {
            throw AdminAuthenticationException
                    .staleAuthentication();
        }

        if (administrator.getRole() == null
                || administrator.getRole()
                != principal.role()) {

            throw AdminAuthenticationException
                    .staleAuthentication();
        }
    }

    /**
     * Rejects inactive, permanent-lock, and active temporary-lock
     * accounts.
     */
    private void validateAccountAvailability(
            AdminUser administrator
    ) {
        if (administrator == null) {
            throw AdminAuthenticationException
                    .accountNotFound();
        }

        if (AdminStatus.INACTIVE.equals(
                administrator.getStatus()
        )) {
            throw AdminAuthenticationException
                    .inactiveAccount();
        }

        if (AdminStatus.LOCKED.equals(
                administrator.getStatus()
        )) {
            throw AdminAuthenticationException
                    .lockedAccount();
        }

        Instant lockedUntil =
                administrator.getLockedUntil();

        if (lockedUntil != null
                && lockedUntil.isAfter(Instant.now())) {

            throw AdminAuthenticationException
                    .temporarilyLocked();
        }

        if (!administrator.isActive()) {
            throw AdminAuthenticationException
                    .inactiveAccount();
        }
    }

    /**
     * Clears a temporary lock after its expiration.
     */
    private void clearExpiredTemporaryLock(
            AdminUser administrator
    ) {
        Instant lockedUntil =
                administrator.getLockedUntil();

        if (lockedUntil == null) {
            return;
        }

        if (!lockedUntil.isAfter(Instant.now())) {
            administrator.setLockedUntil(null);
            administrator.setFailedLoginAttempts(0);
        }
    }

    /**
     * Records an unsuccessful login and applies a temporary lock after
     * the configured failure threshold.
     */
    private void processFailedLogin(
            AdminUser administrator
    ) {
        administrator.recordFailedLoginAttempt();

        int failedAttempts =
                administrator.getFailedLoginAttempts() == null
                        ? 0
                        : administrator.getFailedLoginAttempts();

        if (failedAttempts
                >= MAXIMUM_FAILED_LOGIN_ATTEMPTS) {

            administrator.lockUntil(
                    Instant.now().plus(
                            TEMPORARY_LOCK_DURATION
                    )
            );
        }
    }

    /**
     * Upgrades a successfully verified password hash when required by
     * the configured PasswordEncoder.
     */
    private void upgradePasswordHashWhenRequired(
            AdminUser administrator,
            String verifiedRawPassword
    ) {
        if (!passwordPolicyService.needsUpgrade(
                administrator.getPasswordHash()
        )) {
            return;
        }

        String upgradedHash =
                passwordPolicyService
                        .reencodeVerifiedPassword(
                                verifiedRawPassword
                        );

        administrator.changePassword(
                upgradedHash,
                Boolean.TRUE.equals(
                        administrator.getMustChangePassword()
                )
        );
    }

    /**
     * Performs a BCrypt comparison for unknown accounts to reduce
     * observable account-enumeration timing differences.
     */
    private void performDummyPasswordCheck(
            String rawPassword
    ) {
        passwordPolicyService.matches(
                rawPassword,
                DUMMY_PASSWORD_HASH
        );
    }

    private void requireLoginRequest(
            AdminLoginRequest request
    ) {
        if (request == null
                || request.email() == null
                || request.email().isBlank()
                || request.password() == null
                || request.password().isEmpty()) {

            throw AdminAuthenticationException
                    .invalidCredentials();
        }
    }

    private void requireChangePasswordRequest(
            AdminChangePasswordRequest request
    ) {
        if (request == null) {
            throw new AdminAuthenticationException(
                    org.springframework.http.HttpStatus.BAD_REQUEST,
                    "PASSWORD_CHANGE_REQUEST_REQUIRED",
                    "Password-change information is required."
            );
        }

        if (request.currentPassword() == null
                || request.currentPassword().isEmpty()) {

            throw new AdminAuthenticationException(
                    org.springframework.http.HttpStatus.BAD_REQUEST,
                    "CURRENT_PASSWORD_REQUIRED",
                    "Current password is required."
            );
        }

        if (request.newPassword() == null
                || request.newPassword().isEmpty()) {

            throw new AdminAuthenticationException(
                    org.springframework.http.HttpStatus.BAD_REQUEST,
                    "NEW_PASSWORD_REQUIRED",
                    "New password is required."
            );
        }

        if (request.confirmNewPassword() == null
                || request.confirmNewPassword().isEmpty()) {

            throw new AdminAuthenticationException(
                    org.springframework.http.HttpStatus.BAD_REQUEST,
                    "PASSWORD_CONFIRMATION_REQUIRED",
                    "Password confirmation is required."
            );
        }
    }

    private void requirePrincipal(
            AdminJwtPrincipal principal
    ) {
        if (principal == null
                || principal.adminUserId() == null
                || principal.email() == null
                || principal.email().isBlank()
                || principal.role() == null) {

            throw AdminAuthenticationException
                    .staleAuthentication();
        }
    }

    /**
     * Normalizes an administrator email for case-insensitive
     * comparison.
     */
    private String normalizeEmail(
            String email
    ) {
        if (email == null || email.isBlank()) {
            throw AdminAuthenticationException
                    .staleAuthentication();
        }

        return email
                .trim()
                .toLowerCase(Locale.ROOT);
    }
}