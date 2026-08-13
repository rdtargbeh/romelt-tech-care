package romelt_techcare.backend.dto;

/**
 * ================================================================
 * ROMELT TECHCARE — ADMIN NOTIFICATION COUNT RESPONSE
 * ================================================================
 *
 * Purpose:
 * Returns the administrator's unread notification count for the
 * portal notification bell.
 * ================================================================
 */
public record AdminNotificationCountResponse(

        long unreadCount
) {
}