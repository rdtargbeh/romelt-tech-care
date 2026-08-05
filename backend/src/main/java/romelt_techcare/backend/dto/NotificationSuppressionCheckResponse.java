package romelt_techcare.backend.dto;

import romelt_techcare.backend.enums.NotificationCategory;
import romelt_techcare.backend.enums.NotificationChannel;
import romelt_techcare.backend.enums.NotificationSuppressionReason;

import java.time.Instant;
import java.util.UUID;

/**
 * ================================================================
 * ROMELT TECHCARE — NOTIFICATION SUPPRESSION CHECK RESPONSE
 * ================================================================
 */
public record NotificationSuppressionCheckResponse(

        boolean suppressed,

        UUID notificationSuppressionId,

        NotificationChannel channel,

        NotificationCategory requestedCategory,

        NotificationCategory matchedCategory,

        NotificationSuppressionReason suppressionReason,

        Instant expiresAt
) {

    public static NotificationSuppressionCheckResponse notSuppressed(
            NotificationChannel channel,
            NotificationCategory requestedCategory
    ) {
        return new NotificationSuppressionCheckResponse(
                false,
                null,
                channel,
                requestedCategory,
                null,
                null,
                null
        );
    }
}