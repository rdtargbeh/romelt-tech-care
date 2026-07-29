package romelt_techcare.backend.enums;

/**
 * ================================================================
 * ROMELT TECHCARE — ADMINISTRATOR ROLE
 * ================================================================
 *
 * Purpose:
 * Defines the authorization level assigned to a Romelt TechCare
 * administrative user.
 *
 * Responsibilities:
 * - SUPER_ADMIN owns the complete administrative platform.
 * - ADMIN manages normal administrative operations.
 * - STAFF performs assigned day-to-day operational tasks.
 *
 * Security integration:
 * These values will later be converted into Spring Security
 * authorities such as ROLE_SUPER_ADMIN and ROLE_ADMIN.
 * ================================================================
 */
public enum AdminRole {

    /**
     * Full platform access, including administrator management,
     * security settings, service configuration, and reporting.
     */
    SUPER_ADMIN,

    /**
     * General administrative access to bookings, contact inquiries,
     * customers, services, and operational reports.
     */
    ADMIN,

    /**
     * Restricted operational access for assigned support personnel.
     */
    STAFF
}