package romelt_techcare.backend.dto;

import jakarta.validation.constraints.Size;
import romelt_techcare.backend.enums.NotificationCategory;
import romelt_techcare.backend.enums.NotificationChannel;
import romelt_techcare.backend.enums.NotificationSuppressionReason;

import java.util.UUID;

/**
 * ================================================================
 * ROMELT TECHCARE — NOTIFICATION SUPPRESSION SEARCH REQUEST
 * ================================================================
 */
public record NotificationSuppressionSearchRequest(

        @Size(
                max = 500,
                message = "Notification suppression search keyword cannot exceed 500 characters."
        )
        String keyword,

        UUID customerId,

        NotificationChannel channel,

        NotificationCategory notificationCategory,

        NotificationSuppressionReason suppressionReason,

        Boolean active,

        Boolean effective
) {
}