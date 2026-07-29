package romelt_techcare.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import romelt_techcare.backend.enums.AdminRole;
import romelt_techcare.backend.enums.AdminStatus;

import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

/**
 * ================================================================
 * ROMELT TECHCARE — ADMINISTRATOR USER ENTITY
 * ================================================================
 *
 * Purpose:
 * Stores authenticated administrative accounts that manage Romelt
 * TechCare operations.
 *
 * Responsibilities:
 * - Stores administrator identity and profile information.
 * - Stores only an encoded password hash.
 * - Stores administrator role and account status.
 * - Tracks failed authentication attempts and account lockout.
 * - Tracks successful login and password-change timestamps.
 * - Supports temporary-password and forced-password-change workflows.
 *
 * Password security:
 * - A plain-text password must never be stored in this entity.
 * - passwordHash contains only a BCrypt or compatible encoded value.
 * - Password encoding must happen in the service layer before the
 *   password hash is passed into this entity.
 * - Password verification must use PasswordEncoder.matches(...).
 *
 * Real-data integration:
 * AdminAuthenticationService will use this entity for login,
 * password changes, lockout management, and administrator profiles.
 * ================================================================
 */
@Entity
@Table(
        name = "admin_users",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_admin_users_email",
                        columnNames = "email"
                )
        },
        indexes = {
                @Index(
                        name = "idx_admin_users_email",
                        columnList = "email"
                ),
                @Index(
                        name = "idx_admin_users_status",
                        columnList = "status"
                ),
                @Index(
                        name = "idx_admin_users_role",
                        columnList = "role"
                ),
                @Index(
                        name = "idx_admin_users_locked_until",
                        columnList = "locked_until"
                )
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminUser {

    /**
     * Primary database identifier.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Setter(AccessLevel.NONE)
    @Column(
            name = "admin_user_id",
            nullable = false,
            updatable = false
    )
    private UUID adminUserId;

    /**
     * Unique administrator login email.
     */
    @Column(
            name = "email",
            nullable = false,
            length = 254
    )
    private String email;

    /**
     * Administrator first name.
     */
    @Column(
            name = "first_name",
            nullable = false,
            length = 100
    )
    private String firstName;

    /**
     * Administrator last name.
     */
    @Column(
            name = "last_name",
            nullable = false,
            length = 100
    )
    private String lastName;

    /**
     * Optional business or administrative title.
     */
    @Column(
            name = "job_title",
            length = 150
    )
    private String jobTitle;

    /**
     * BCrypt or another approved one-way password hash.
     *
     * This column must never contain a plain-text password.
     */
    @Setter(AccessLevel.NONE)
    @Column(
            name = "password_hash",
            nullable = false,
            length = 255
    )
    private String passwordHash;

    /**
     * Authorization role assigned to the administrator.
     */
    @Enumerated(EnumType.STRING)
    @Column(
            name = "role",
            nullable = false,
            length = 40
    )
    private AdminRole role;

    /**
     * Current account status.
     */
    @Enumerated(EnumType.STRING)
    @Column(
            name = "status",
            nullable = false,
            length = 40
    )
    private AdminStatus status;

    /**
     * Indicates that the administrator must replace a temporary
     * password before normal administrative access is allowed.
     */
    @Builder.Default
    @Column(
            name = "must_change_password",
            nullable = false
    )
    private Boolean mustChangePassword = true;

    /**
     * Consecutive failed login attempts.
     */
    @Builder.Default
    @Column(
            name = "failed_login_attempts",
            nullable = false
    )
    private Integer failedLoginAttempts = 0;

    /**
     * Time until which the account is temporarily locked.
     */
    @Column(name = "locked_until")
    private Instant lockedUntil;

    /**
     * Most recent successful login time.
     */
    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    /**
     * Most recent time the password hash was changed.
     */
    @Column(name = "password_changed_at")
    private Instant passwordChangedAt;

    /**
     * Account creation timestamp.
     */
    @Setter(AccessLevel.NONE)
    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private Instant createdAt;

    /**
     * Most recent account update timestamp.
     */
    @Setter(AccessLevel.NONE)
    @Column(
            name = "updated_at",
            nullable = false
    )
    private Instant updatedAt;

    /**
     * Applies normalized values and defaults before insertion.
     */
    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();

        normalizeValues();

        if (role == null) {
            role = AdminRole.STAFF;
        }

        if (status == null) {
            status = AdminStatus.ACTIVE;
        }

        if (mustChangePassword == null) {
            mustChangePassword = true;
        }

        if (failedLoginAttempts == null || failedLoginAttempts < 0) {
            failedLoginAttempts = 0;
        }

        if (createdAt == null) {
            createdAt = now;
        }

        if (passwordChangedAt == null && passwordHash != null) {
            passwordChangedAt = now;
        }

        updatedAt = now;
    }

    /**
     * Applies normalized values before an update.
     */
    @PreUpdate
    protected void onUpdate() {
        normalizeValues();

        if (mustChangePassword == null) {
            mustChangePassword = false;
        }

        if (failedLoginAttempts == null || failedLoginAttempts < 0) {
            failedLoginAttempts = 0;
        }

        updatedAt = Instant.now();
    }

    /**
     * Replaces the stored password hash.
     *
     * Important:
     * encodedPassword must already be encoded by PasswordEncoder.
     *
     * @param encodedPassword encoded BCrypt password
     * @param requirePasswordChange whether another change is required
     */
    public void changePassword(
            String encodedPassword,
            boolean requirePasswordChange
    ) {
        String normalizedHash = trimToNull(encodedPassword);

        if (normalizedHash == null) {
            throw new IllegalArgumentException(
                    "Encoded password must not be blank."
            );
        }

        this.passwordHash = normalizedHash;
        this.passwordChangedAt = Instant.now();
        this.mustChangePassword = requirePasswordChange;
        this.failedLoginAttempts = 0;
        this.lockedUntil = null;
    }

    /**
     * Records a successful administrator login.
     */
    public void recordSuccessfulLogin() {
        this.lastLoginAt = Instant.now();
        this.failedLoginAttempts = 0;
        this.lockedUntil = null;
    }

    /**
     * Increments the failed login attempt counter.
     */
    public void recordFailedLoginAttempt() {
        if (failedLoginAttempts == null || failedLoginAttempts < 0) {
            failedLoginAttempts = 0;
        }

        failedLoginAttempts++;
    }

    /**
     * Temporarily locks the administrator account.
     *
     * @param lockExpiration time when the temporary lock expires
     */
    public void lockUntil(Instant lockExpiration) {
        if (lockExpiration == null || !lockExpiration.isAfter(Instant.now())) {
            throw new IllegalArgumentException(
                    "Lock expiration must be in the future."
            );
        }

        this.lockedUntil = lockExpiration;
    }

    /**
     * Permanently locks the administrator until manually unlocked.
     */
    public void lockAccount() {
        this.status = AdminStatus.LOCKED;
        this.lockedUntil = null;
    }

    /**
     * Unlocks the administrator account.
     */
    public void unlockAccount() {
        this.status = AdminStatus.ACTIVE;
        this.lockedUntil = null;
        this.failedLoginAttempts = 0;
    }

    /**
     * Returns the administrator display name.
     */
    public String getFullName() {
        String normalizedFirstName = trimToNull(firstName);
        String normalizedLastName = trimToNull(lastName);

        if (normalizedFirstName == null && normalizedLastName == null) {
            return email;
        }

        if (normalizedFirstName == null) {
            return normalizedLastName;
        }

        if (normalizedLastName == null) {
            return normalizedFirstName;
        }

        return normalizedFirstName + " " + normalizedLastName;
    }

    /**
     * Returns true when the account status is ACTIVE.
     */
    public boolean isActive() {
        return AdminStatus.ACTIVE.equals(status);
    }

    /**
     * Returns true when the account is permanently or temporarily
     * locked.
     */
    public boolean isLocked() {
        if (AdminStatus.LOCKED.equals(status)) {
            return true;
        }

        return lockedUntil != null
                && lockedUntil.isAfter(Instant.now());
    }

    /**
     * Returns true when authentication should be permitted.
     */
    public boolean canAuthenticate() {
        return isActive() && !isLocked();
    }

    /**
     * Normalizes persisted values.
     */
    private void normalizeValues() {
        email = normalizeEmail(email);
        firstName = trimToNull(firstName);
        lastName = trimToNull(lastName);
        jobTitle = trimToNull(jobTitle);
        passwordHash = trimToNull(passwordHash);
    }

    /**
     * Normalizes email addresses for case-insensitive authentication.
     */
    private String normalizeEmail(String value) {
        String normalizedValue = trimToNull(value);

        return normalizedValue == null
                ? null
                : normalizedValue.toLowerCase(Locale.ROOT);
    }

    /**
     * Trims a value and converts blank strings to null.
     */
    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }

        String normalizedValue = value.trim();

        return normalizedValue.isEmpty()
                ? null
                : normalizedValue;
    }
}