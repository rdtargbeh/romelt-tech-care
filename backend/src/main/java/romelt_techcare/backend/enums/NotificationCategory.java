package romelt_techcare.backend.enums;

/**
 * ================================================================
 * ROMELT TECHCARE — NOTIFICATION CATEGORY
 * ================================================================
 *
 * Purpose:
 * Classifies notifications for suppression, compliance, reporting,
 * and delivery behavior.
 *
 * Important:
 * Marketing opt-out must not automatically suppress required
 * transactional, service, or security notifications.
 * ================================================================
 */
public enum NotificationCategory {

    TRANSACTIONAL,

    SERVICE,

    SECURITY,

    MARKETING,

    INTERNAL
}