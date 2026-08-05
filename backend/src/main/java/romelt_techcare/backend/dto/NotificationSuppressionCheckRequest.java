package romelt_techcare.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import romelt_techcare.backend.enums.NotificationCategory;
import romelt_techcare.backend.enums.NotificationChannel;

/**
 * ================================================================
 * ROMELT TECHCARE — NOTIFICATION SUPPRESSION CHECK REQUEST
 * ================================================================
 *
 * Purpose:
 * Checks whether a proposed EMAIL or SMS delivery is currently
 * suppressed.
 * ================================================================
 */
public record NotificationSuppressionCheckRequest(

        @NotNull(
                message = "Notification channel is required."
        )
        NotificationChannel channel,

        NotificationCategory notificationCategory,

        @NotBlank(
                message = "Notification recipient address is required."
        )
        @Size(
                max = 500,
                message = "Notification recipient address cannot exceed 500 characters."
        )
        String recipientAddress
) {
}