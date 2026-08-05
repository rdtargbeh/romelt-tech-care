package romelt_techcare.backend.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import romelt_techcare.backend.enums.NotificationChannel;

/**
 * ================================================================
 * ROMELT TECHCARE — NOTIFICATION TEMPLATE SEARCH REQUEST
 * ================================================================
 *
 * Purpose:
 * Defines optional administrator filters for notification-template
 * list and search operations.
 *
 * Supported filters:
 * - keyword
 * - templateKey
 * - channel
 * - locale
 * - templateVersion
 * - active
 *
 * Keyword search may include:
 * - template key
 * - template name
 * - locale
 * ================================================================
 */
public record NotificationTemplateSearchRequest(

        @Size(
                max = 180,
                message = "Notification template keyword cannot exceed 180 characters."
        )
        String keyword,

        @Size(
                max = 160,
                message = "Notification template key cannot exceed 160 characters."
        )
        String templateKey,

        NotificationChannel channel,

        @Size(
                max = 20,
                message = "Notification template locale cannot exceed 20 characters."
        )
        String locale,

        @Min(
                value = 1,
                message = "Notification template version must be greater than zero."
        )
        Integer templateVersion,

        Boolean active
) {
}