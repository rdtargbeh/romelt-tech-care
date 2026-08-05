package romelt_techcare.backend.dto;

import romelt_techcare.backend.enums.NotificationChannel;

import java.time.Instant;
import java.util.UUID;

/**
 * ================================================================
 * ROMELT TECHCARE — NOTIFICATION TEMPLATE SUMMARY RESPONSE
 * ================================================================
 *
 * Purpose:
 * Returns compact notification-template information for administrator
 * lists, selectors, and search results.
 *
 * Excluded:
 * - Complete text body
 * - Complete HTML body
 * - Required-variable JSON
 * - Administrator attribution
 * ================================================================
 */
public record NotificationTemplateSummaryResponse(

        UUID notificationTemplateId,

        String templateKey,

        NotificationChannel channel,

        String templateName,

        String locale,

        Integer templateVersion,

        boolean active,

        boolean hasTextBody,

        boolean hasHtmlBody,

        Instant createdAt,

        Instant updatedAt
) {
}