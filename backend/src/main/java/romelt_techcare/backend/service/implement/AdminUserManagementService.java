package romelt_techcare.backend.service.implement;

import com.fasterxml.jackson.databind.JsonNode;
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
import romelt_techcare.backend.enums.WebsiteContentAuditAction;
import romelt_techcare.backend.enums.WebsiteContentAuditResourceType;
import romelt_techcare.backend.exception.AdminUserManagementException;
import romelt_techcare.backend.repository.AdminUserRepository;
import romelt_techcare.backend.service.AdminPasswordPolicyService;
import romelt_techcare.backend.service.WebsiteContentAuditLogService;
import romelt_techcare.backend.service.WebsiteContentAuditSnapshotService;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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
 * - Records immutable audit events for all administrator-account
 *   mutations.
 *
 * Security:
 * Passwords and password hashes are never included in audit snapshots.
 * ================================================================
 */
@Service
@RequiredArgsConstructor
public class AdminUserManagementService {

    private final AdminUserRepository
            adminUserRepository;

    private final AdminPasswordPolicyService
            passwordPolicyService;

    private final WebsiteContentAuditLogService
            websiteContentAuditLogService;

    private final WebsiteContentAuditSnapshotService
            websiteContentAuditSnapshotService;

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

        String jobTitle =
                trimToNull(request.jobTitle());

        if (
                adminUserRepository
                        .existsByEmailIgnoreCase(email)
        ) {
            throw AdminUserManagementException
                    .duplicateEmail();
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

        AdminUser administrator =
                AdminUser.builder()
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
                adminUserRepository.saveAndFlush(
                        administrator
                );

        websiteContentAuditLogService.recordAudit(
                principal.adminUserId(),
                WebsiteContentAuditAction.CREATE,
                WebsiteContentAuditResourceType.ADMIN_USER,
                savedAdministrator.getAdminUserId(),
                createAdministratorResourceName(
                        savedAdministrator
                ),
                null,
                createAdministratorSnapshot(
                        savedAdministrator
                ),
                "Administrator account created.",
                null
        );

        return AdminUserResponse.from(
                savedAdministrator
        );
    }

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

    @Transactional
    public AdminUserResponse updateAdministrator(
            AdminJwtPrincipal principal,
            UUID adminUserId,
            AdminUpdateUserRequest request
    ) {
        requireSuperAdministrator(principal);

        AdminUser administrator =
                findAdministrator(adminUserId);

        JsonNode beforeSnapshot =
                createAdministratorSnapshot(
                        administrator
                );

        String normalizedEmail =
                normalizeEmail(request.email());

        if (
                adminUserRepository
                        .existsByEmailIgnoreCaseAndAdminUserIdNot(
                                normalizedEmail,
                                adminUserId
                        )
        ) {
            throw AdminUserManagementException
                    .duplicateEmail();
        }

        boolean isDemotingActiveSuperAdministrator =
                administrator.getRole()
                        == AdminRole.SUPER_ADMIN
                        && administrator.getStatus()
                        == AdminStatus.ACTIVE
                        && request.role()
                        != AdminRole.SUPER_ADMIN;

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
                adminUserRepository.saveAndFlush(
                        administrator
                );

        websiteContentAuditLogService.recordAudit(
                principal.adminUserId(),
                WebsiteContentAuditAction.UPDATE,
                WebsiteContentAuditResourceType.ADMIN_USER,
                savedAdministrator.getAdminUserId(),
                createAdministratorResourceName(
                        savedAdministrator
                ),
                beforeSnapshot,
                createAdministratorSnapshot(
                        savedAdministrator
                ),
                "Administrator identity or role information updated.",
                null
        );

        return AdminUserResponse.from(
                savedAdministrator
        );
    }

    @Transactional
    public AdminUserResponse changeAdministratorStatus(
            AdminJwtPrincipal principal,
            UUID adminUserId,
            AdminUserStatusRequest request
    ) {
        requireSuperAdministrator(principal);

        AdminUser administrator =
                findAdministrator(adminUserId);

        JsonNode beforeSnapshot =
                createAdministratorSnapshot(
                        administrator
                );

        AdminStatus previousStatus =
                administrator.getStatus();

        AdminStatus requestedStatus =
                request.status();

        boolean isSelf =
                principal.adminUserId().equals(
                        administrator.getAdminUserId()
                );

        if (
                isSelf
                        && requestedStatus
                        != AdminStatus.ACTIVE
        ) {
            throw AdminUserManagementException
                    .selfStatusChangeNotAllowed();
        }

        boolean isRemovingActiveSuperAdministrator =
                administrator.getRole()
                        == AdminRole.SUPER_ADMIN
                        && administrator.getStatus()
                        == AdminStatus.ACTIVE
                        && requestedStatus
                        != AdminStatus.ACTIVE;

        if (isRemovingActiveSuperAdministrator) {
            ensureAnotherActiveSuperAdministratorExists(
                    administrator
            );
        }

        switch (requestedStatus) {
            case ACTIVE ->
                    administrator.unlockAccount();

            case INACTIVE -> {
                administrator.setStatus(
                        AdminStatus.INACTIVE
                );

                administrator.setLockedUntil(null);
                administrator.setFailedLoginAttempts(0);
            }

            case LOCKED ->
                    administrator.lockAccount();
        }

        AdminUser savedAdministrator =
                adminUserRepository.saveAndFlush(
                        administrator
                );

        websiteContentAuditLogService.recordAudit(
                principal.adminUserId(),
                WebsiteContentAuditAction.UPDATE,
                WebsiteContentAuditResourceType.ADMIN_USER,
                savedAdministrator.getAdminUserId(),
                createAdministratorResourceName(
                        savedAdministrator
                ),
                beforeSnapshot,
                createAdministratorSnapshot(
                        savedAdministrator
                ),
                "Administrator status changed from "
                        + previousStatus
                        + " to "
                        + savedAdministrator.getStatus()
                        + ".",
                null
        );

        return AdminUserResponse.from(
                savedAdministrator
        );
    }

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

        JsonNode beforeSnapshot =
                createAdministratorSnapshot(
                        administrator
                );

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

        AdminUser savedAdministrator =
                adminUserRepository.saveAndFlush(
                        administrator
                );

        websiteContentAuditLogService.recordAudit(
                principal.adminUserId(),
                WebsiteContentAuditAction.UPDATE,
                WebsiteContentAuditResourceType.ADMIN_USER,
                savedAdministrator.getAdminUserId(),
                createAdministratorResourceName(
                        savedAdministrator
                ),
                beforeSnapshot,
                createAdministratorSnapshot(
                        savedAdministrator
                ),
                "Administrator temporary password reset. "
                        + "No password value or hash was recorded.",
                null
        );

        return AdminUserResponse.from(
                savedAdministrator
        );
    }

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
                administrator.getRole()
                        == AdminRole.SUPER_ADMIN
                        && administrator.getStatus()
                        == AdminStatus.ACTIVE
        ) {
            ensureAnotherActiveSuperAdministratorExists(
                    administrator
            );
        }

        JsonNode beforeSnapshot =
                createAdministratorSnapshot(
                        administrator
                );

        String resourceName =
                createAdministratorResourceName(
                        administrator
                );

        UUID deletedAdministratorId =
                administrator.getAdminUserId();

        adminUserRepository.delete(administrator);
        adminUserRepository.flush();

        websiteContentAuditLogService.recordAudit(
                principal.adminUserId(),
                WebsiteContentAuditAction.DELETE,
                WebsiteContentAuditResourceType.ADMIN_USER,
                deletedAdministratorId,
                resourceName,
                beforeSnapshot,
                null,
                "Administrator account deleted.",
                null
        );
    }

    private JsonNode createAdministratorSnapshot(
            AdminUser administrator
    ) {
        Map<String, Object> fields =
                new LinkedHashMap<>();

        fields.put(
                "adminUserId",
                administrator.getAdminUserId()
        );

        fields.put(
                "email",
                administrator.getEmail()
        );

        fields.put(
                "firstName",
                administrator.getFirstName()
        );

        fields.put(
                "lastName",
                administrator.getLastName()
        );

        fields.put(
                "jobTitle",
                administrator.getJobTitle()
        );

        fields.put(
                "role",
                administrator.getRole()
        );

        fields.put(
                "status",
                administrator.getStatus()
        );

        fields.put(
                "mustChangePassword",
                administrator.getMustChangePassword()
        );

        fields.put(
                "failedLoginAttempts",
                administrator.getFailedLoginAttempts()
        );

        fields.put(
                "lockedUntil",
                administrator.getLockedUntil()
        );

        return websiteContentAuditSnapshotService
                .createSnapshot(fields);
    }

    private String createAdministratorResourceName(
            AdminUser administrator
    ) {
        String firstName =
                trimToNull(
                        administrator.getFirstName()
                );

        String lastName =
                trimToNull(
                        administrator.getLastName()
                );

        String fullName =
                String.join(
                        " ",
                        firstName == null ? "" : firstName,
                        lastName == null ? "" : lastName
                ).trim();

        if (!fullName.isBlank()) {
            return fullName;
        }

        return administrator.getEmail();
    }

    private void requireSuperAdministrator(
            AdminJwtPrincipal principal
    ) {
        if (
                principal == null
                        || principal.adminUserId() == null
                        || principal.role()
                        != AdminRole.SUPER_ADMIN
        ) {
            throw AdminUserManagementException
                    .unauthorizedManager();
        }

        AdminUser manager =
                adminUserRepository
                        .findById(principal.adminUserId())
                        .orElseThrow(
                                AdminUserManagementException
                                        ::unauthorizedManager
                        );

        if (
                manager.getRole()
                        != AdminRole.SUPER_ADMIN
                        || manager.getStatus()
                        != AdminStatus.ACTIVE
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
            throw AdminUserManagementException
                    .notFound();
        }

        return adminUserRepository
                .findById(adminUserId)
                .orElseThrow(
                        AdminUserManagementException
                                ::notFound
                );
    }

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