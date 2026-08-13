package romelt_techcare.backend.dto;

import romelt_techcare.backend.enums.NotificationResourceType;

import java.time.Instant;
import java.util.UUID;

/**
 * ================================================================
 * ROMELT TECHCARE — ADMIN IN-APP NOTIFICATION RESPONSE
 * ================================================================
 *
 * Purpose:
 * Provides the administrator portal with the information required to
 * display notification-bell entries, toast messages, unread state,
 * and links to the related booking or contact inquiry.
 * ================================================================
 */
public record AdminInAppNotificationResponse(

        UUID notificationId,

        NotificationResourceType resourceType,

        UUID resourceId,

        String title,

        String message,

        String portalPath,

        boolean read,

        boolean dismissed,

        Instant readAt,

        Instant dismissedAt,

        Instant createdAt
) {
}