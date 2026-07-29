package romelt_techcare.backend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import romelt_techcare.backend.dto.AdminCreateUserRequest;
import romelt_techcare.backend.dto.AdminJwtPrincipal;
import romelt_techcare.backend.dto.AdminResetPasswordRequest;
import romelt_techcare.backend.dto.AdminUpdateUserRequest;
import romelt_techcare.backend.dto.AdminUserResponse;
import romelt_techcare.backend.dto.AdminUserStatusRequest;
import romelt_techcare.backend.entity.AdminUser;
import romelt_techcare.backend.enums.AdminRole;
import romelt_techcare.backend.enums.AdminStatus;
import romelt_techcare.backend.exception.AdminUserManagementException;
import romelt_techcare.backend.repository.AdminUserRepository;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * ================================================================
 * ROMELT TECHCARE — ADMIN USER MANAGEMENT SERVICE
 * ================================================================
 *
 * Purpose:
 * Implements protected administrator and employee account-management
 * operations.
 *
 * Responsibilities:
 * - Creates administrator employee accounts.
 * - Lists and retrieves administrator accounts.
 * - Updates profile information and assigned roles.
 * - Activates, disables, locks, and unlocks accounts.
 * - Resets temporary passwords.
 * - Deletes administrator accounts when permitted.
 * - Protects the final active SUPER_ADMIN account.
 * - Prevents unsafe self-management operations.
 *
 * Authorization:
 * Every operation requires an authenticated SUPER_ADMIN principal.
 * Controller method security also enforces ROLE_SUPER_ADMIN.
 *
 * Security rules:
 * - Plain-text passwords are never persisted.
 * - Passwords are validated and encoded by
 *   AdminPasswordPolicyService.
 * - Password hashes are never returned.
 * - Existing administrator identities are checked against the JWT
 *   principal before management operations continue.
 * ================================================================
 */
@Service
@RequiredArgsConstructor
public class AdminUserManagementService {

    private final AdminUserRepository adminUserRepository;
    private final romelt_techcare.backend.service.AdminPasswordPolicyService passwordPolicyService;

    /**
     * Creates a new administrator account with a temporary password.
     */
    @Transactional
    public AdminUserResponse createAdministrator(
            AdminJwtPrincipal principal,
            AdminCreateUserRequest request
    ) {
        requireSuperAdministrator(principal);

        String email = normalizeEmail(request.email());
        String firstName = normalizeRequiredText(
                request.firstName(),
                "First name is required."
        );
        String lastName = normalizeRequiredText(
                request.lastName(),
                "Last name is required."
        );
        String jobTitle = trimToNull(request.jobTitle());

        if (adminUserRepository.existsByEmailIgnoreCase(email)) {
            throw AdminUserManagementException.duplicateEmail();
        }

        String encodedPassword;

        try {
            encodedPassword =
                    passwordPolicyService.encodeNewPassword(
                            request.temporaryPassword(),
                            email,
                            firstName,
                            lastName
                    );
        } catch (IllegalArgumentException exception) {
            throw AdminUserManagementException
                    .invalidTemporaryPassword(
                            exception.getMessage()
                    );
        }

        AdminUser administrator = AdminUser.builder()
                .email(email)
                .firstName(firstName)
                .lastName(lastName)
                .jobTitle(jobTitle)
                .role(request.role())
                .status(AdminStatus.ACTIVE)
                .mustChangePassword(true)
                .failedLoginAttempts(0)
                .lockedUntil(null)
                .lastLoginAt(null)
                .build();

        administrator.changePassword(
                encodedPassword,
                true
        );

        AdminUser savedAdministrator =
                adminUserRepository.save(administrator);

        return AdminUserResponse.from(savedAdministrator);
    }

    /**
     * Lists administrator accounts with optional role and status
     * filters.
     */
    @Transactional(readOnly = true)
    public List<AdminUserResponse> getAdministrators(
            AdminJwtPrincipal principal,
            AdminRole role,
            AdminStatus status
    ) {
        requireSuperAdministrator(principal);

        List<AdminUser> administrators;

        if (role != null && status != null) {
            administrators =
                    adminUserRepository
                            .findAllByRoleAndStatusOrderByCreatedAtDesc(
                                    role,
                                    status
                            );
        } else if (role != null) {
            administrators =
                    adminUserRepository
                            .findAllByRoleOrderByCreatedAtDesc(
                                    role
                            );
        } else if (status != null) {
            administrators =
                    adminUserRepository
                            .findAllByStatusOrderByCreatedAtDesc(
                                    status
                            );
        } else {
            administrators =
                    adminUserRepository.findAll(
                            Sort.by(
                                    Sort.Direction.DESC,
                                    "createdAt"
                            )
                    );
        }

        return administrators.stream()
                .map(AdminUserResponse::from)
                .toList();
    }

    /**
     * Retrieves one administrator account.
     */
    @Transactional(readOnly = true)
    public AdminUserResponse getAdministrator(
            AdminJwtPrincipal principal,
            UUID adminUserId
    ) {
        requireSuperAdministrator(principal);

        return AdminUserResponse.from(
                findAdministrator(adminUserId)
        );
    }

    /**
     * Updates administrator identity and role information.
     */
    @Transactional
    public AdminUserResponse updateAdministrator(
            AdminJwtPrincipal principal,
            UUID adminUserId,
            AdminUpdateUserRequest request
    ) {
        requireSuperAdministrator(principal);

        AdminUser administrator =
                findAdministrator(adminUserId);

        String normalizedEmail =
                normalizeEmail(request.email());

        if (
                adminUserRepository
                        .existsByEmailIgnoreCaseAndAdminUserIdNot(
                                normalizedEmail,
                                adminUserId
                        )
        ) {
            throw AdminUserManagementException.duplicateEmail();
        }

        boolean isDemotingActiveSuperAdministrator =
                administrator.getRole() == AdminRole.SUPER_ADMIN
                        && administrator.getStatus() == AdminStatus.ACTIVE
                        && request.role() != AdminRole.SUPER_ADMIN;

        if (isDemotingActiveSuperAdministrator) {
            ensureAnotherActiveSuperAdministratorExists(
                    administrator
            );
        }

        administrator.setEmail(normalizedEmail);
        administrator.setFirstName(
                normalizeRequiredText(
                        request.firstName(),
                        "First name is required."
                )
        );
        administrator.setLastName(
                normalizeRequiredText(
                        request.lastName(),
                        "Last name is required."
                )
        );
        administrator.setJobTitle(
                trimToNull(request.jobTitle())
        );
        administrator.setRole(request.role());

        AdminUser savedAdministrator =
                adminUserRepository.save(administrator);

        return AdminUserResponse.from(savedAdministrator);
    }

    /**
     * Changes account status.
     */
    @Transactional
    public AdminUserResponse changeAdministratorStatus(
            AdminJwtPrincipal principal,
            UUID adminUserId,
            AdminUserStatusRequest request
    ) {
        requireSuperAdministrator(principal);

        AdminUser administrator =
                findAdministrator(adminUserId);

        AdminStatus requestedStatus =
                request.status();

        boolean isSelf =
                principal.adminUserId().equals(
                        administrator.getAdminUserId()
                );

        if (
                isSelf
                        && requestedStatus != AdminStatus.ACTIVE
        ) {
            throw AdminUserManagementException
                    .selfStatusChangeNotAllowed();
        }

        boolean isRemovingActiveSuperAdministrator =
                administrator.getRole() == AdminRole.SUPER_ADMIN
                        && administrator.getStatus() == AdminStatus.ACTIVE
                        && requestedStatus != AdminStatus.ACTIVE;

        if (isRemovingActiveSuperAdministrator) {
            ensureAnotherActiveSuperAdministratorExists(
                    administrator
            );
        }

        switch (requestedStatus) {
            case ACTIVE -> administrator.unlockAccount();

            case INACTIVE -> {
                administrator.setStatus(AdminStatus.INACTIVE);
                administrator.setLockedUntil(null);
                administrator.setFailedLoginAttempts(0);
            }

            case LOCKED -> administrator.lockAccount();
        }

        AdminUser savedAdministrator =
                adminUserRepository.save(administrator);

        return AdminUserResponse.from(savedAdministrator);
    }

    /**
     * Replaces another administrator's password with a temporary
     * password and requires a password change after login.
     */
    @Transactional
    public AdminUserResponse resetAdministratorPassword(
            AdminJwtPrincipal principal,
            UUID adminUserId,
            AdminResetPasswordRequest request
    ) {
        requireSuperAdministrator(principal);

        AdminUser administrator =
                findAdministrator(adminUserId);

        if (
                principal.adminUserId().equals(
                        administrator.getAdminUserId()
                )
        ) {
            throw AdminUserManagementException
                    .selfPasswordResetNotAllowed();
        }

        String encodedPassword;

        try {
            encodedPassword =
                    passwordPolicyService.encodeNewPassword(
                            request.temporaryPassword(),
                            administrator.getEmail(),
                            administrator.getFirstName(),
                            administrator.getLastName()
                    );
        } catch (IllegalArgumentException exception) {
            throw AdminUserManagementException
                    .invalidTemporaryPassword(
                            exception.getMessage()
                    );
        }

        administrator.changePassword(
                encodedPassword,
                true
        );

        /*
         * A password reset does not automatically reactivate an
         * administratively disabled account.
         *
         * It does clear temporary login lock information through
         * AdminUser.changePassword(...).
         */
        AdminUser savedAdministrator =
                adminUserRepository.save(administrator);

        return AdminUserResponse.from(savedAdministrator);
    }

    /**
     * Deletes an administrator account when all safety requirements
     * are satisfied.
     */
    @Transactional
    public void deleteAdministrator(
            AdminJwtPrincipal principal,
            UUID adminUserId
    ) {
        requireSuperAdministrator(principal);

        AdminUser administrator =
                findAdministrator(adminUserId);

        if (
                principal.adminUserId().equals(
                        administrator.getAdminUserId()
                )
        ) {
            throw AdminUserManagementException
                    .selfDeleteNotAllowed();
        }

        if (
                administrator.getRole() == AdminRole.SUPER_ADMIN
                        && administrator.getStatus() == AdminStatus.ACTIVE
        ) {
            ensureAnotherActiveSuperAdministratorExists(
                    administrator
            );
        }

        adminUserRepository.delete(administrator);
    }

    /**
     * Ensures that the authenticated principal represents an existing,
     * active SUPER_ADMIN account.
     */
    private void requireSuperAdministrator(
            AdminJwtPrincipal principal
    ) {
        if (
                principal == null
                        || principal.adminUserId() == null
                        || principal.role() != AdminRole.SUPER_ADMIN
        ) {
            throw AdminUserManagementException
                    .unauthorizedManager();
        }

        AdminUser manager = adminUserRepository
                .findById(principal.adminUserId())
                .orElseThrow(
                        AdminUserManagementException
                                ::unauthorizedManager
                );

        if (
                manager.getRole() != AdminRole.SUPER_ADMIN
                        || manager.getStatus() != AdminStatus.ACTIVE
                        || manager.isLocked()
        ) {
            throw AdminUserManagementException
                    .unauthorizedManager();
        }
    }

    private AdminUser findAdministrator(
            UUID adminUserId
    ) {
        if (adminUserId == null) {
            throw AdminUserManagementException.notFound();
        }

        return adminUserRepository
                .findById(adminUserId)
                .orElseThrow(
                        AdminUserManagementException
                                ::notFound
                );
    }

    /**
     * Prevents removal, deactivation, or demotion of the final active
     * SUPER_ADMIN.
     */
    private void ensureAnotherActiveSuperAdministratorExists(
            AdminUser affectedAdministrator
    ) {
        long activeSuperAdministratorCount =
                adminUserRepository.countByRoleAndStatus(
                        AdminRole.SUPER_ADMIN,
                        AdminStatus.ACTIVE
                );

        boolean affectedAccountIsActiveSuperAdministrator =
                affectedAdministrator.getRole()
                        == AdminRole.SUPER_ADMIN
                        && affectedAdministrator.getStatus()
                        == AdminStatus.ACTIVE;

        long remainingCount =
                affectedAccountIsActiveSuperAdministrator
                        ? activeSuperAdministratorCount - 1
                        : activeSuperAdministratorCount;

        if (remainingCount < 1) {
            throw AdminUserManagementException
                    .finalSuperAdminRequired();
        }
    }

    private String normalizeEmail(
            String value
    ) {
        String normalizedValue =
                normalizeRequiredText(
                        value,
                        "Email is required."
                );

        return normalizedValue.toLowerCase(
                Locale.ROOT
        );
    }

    private String normalizeRequiredText(
            String value,
            String message
    ) {
        String normalizedValue =
                trimToNull(value);

        if (normalizedValue == null) {
            throw new AdminUserManagementException(
                    org.springframework.http.HttpStatus.BAD_REQUEST,
                    "REQUIRED_VALUE_MISSING",
                    message
            );
        }

        return normalizedValue;
    }

    private String trimToNull(
            String value
    ) {
        if (value == null) {
            return null;
        }

        String normalizedValue =
                value.trim();

        return normalizedValue.isEmpty()
                ? null
                : normalizedValue;
    }
}