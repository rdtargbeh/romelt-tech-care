package romelt_techcare.backend.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import romelt_techcare.backend.entity.AdminUser;
import romelt_techcare.backend.enums.AdminRole;
import romelt_techcare.backend.enums.AdminStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * ================================================================
 * ROMELT TECHCARE — ADMINISTRATOR USER REPOSITORY
 * ================================================================
 *
 * Purpose:
 * Provides database access for Romelt TechCare administrator
 * accounts.
 *
 * Responsibilities:
 * - Finds administrators by unique email address.
 * - Checks whether an administrator email already exists.
 * - Retrieves administrator accounts by role and status.
 * - Supports locked administrator retrieval during authentication.
 * - Prevents concurrent login attempts from incorrectly updating
 *   failed-attempt and lockout information.
 *
 * Security rules:
 * - Authentication lookups use case-insensitive email matching.
 * - Password hashes must never be returned through API DTOs.
 * - Pessimistic locking should be used when login attempts modify
 *   account security state.
 *
 * Real-data integration:
 * Used by:
 * - AdminUserSeeder
 * - AdminAuthenticationService
 * - Administrator management services
 * - Password change and account recovery services
 * ================================================================
 */
@Repository
public interface AdminUserRepository
        extends JpaRepository<AdminUser, UUID> {

    /**
     * Finds an administrator using a case-insensitive email lookup.
     *
     * Use this method for read-only account lookups.
     *
     * @param email administrator email address
     * @return matching administrator when present
     */
    Optional<AdminUser> findByEmailIgnoreCase(String email);

    /**
     * Checks whether an administrator email already exists.
     *
     * @param email administrator email address
     * @return true when the email already exists
     */
    boolean existsByEmailIgnoreCase(String email);

    /**
     * Checks whether another administrator already uses an email.
     *
     * This method is useful when updating an existing administrator.
     *
     * @param email administrator email address
     * @param adminUserId administrator being excluded
     * @return true when another account uses the email
     */
    boolean existsByEmailIgnoreCaseAndAdminUserIdNot(
            String email,
            UUID adminUserId
    );

    /**
     * Finds an administrator and obtains a pessimistic database lock.
     *
     * Authentication services should use this method before modifying:
     * - failedLoginAttempts
     * - lockedUntil
     * - lastLoginAt
     *
     * The lock helps prevent simultaneous login requests from
     * overwriting each other's account-security updates.
     *
     * @param email normalized administrator email
     * @return locked administrator record when present
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select administrator
            from AdminUser administrator
            where lower(administrator.email) = lower(:email)
            """)
    Optional<AdminUser> findByEmailIgnoreCaseForAuthentication(
            @Param("email") String email
    );

    /**
     * Finds administrators by account status.
     *
     * @param status account status
     * @return matching administrators
     */
    List<AdminUser> findAllByStatusOrderByCreatedAtDesc(
            AdminStatus status
    );

    /**
     * Finds administrators by assigned role.
     *
     * @param role administrator role
     * @return matching administrators
     */
    List<AdminUser> findAllByRoleOrderByCreatedAtDesc(
            AdminRole role
    );

    /**
     * Finds administrators by role and status.
     *
     * @param role administrator role
     * @param status account status
     * @return matching administrators
     */
    List<AdminUser> findAllByRoleAndStatusOrderByCreatedAtDesc(
            AdminRole role,
            AdminStatus status
    );

    /**
     * Counts administrators assigned to a role.
     *
     * This can later prevent removal of the final SUPER_ADMIN.
     *
     * @param role administrator role
     * @return number of matching administrators
     */
    long countByRole(AdminRole role);

    /**
     * Counts active administrators assigned to a role.
     *
     * @param role administrator role
     * @param status administrator status
     * @return number of matching administrators
     */
    long countByRoleAndStatus(
            AdminRole role,
            AdminStatus status
    );
}