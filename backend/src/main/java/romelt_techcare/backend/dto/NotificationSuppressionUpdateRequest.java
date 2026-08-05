package romelt_techcare.backend.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Size;
import romelt_techcare.backend.enums.NotificationSuppressionReason;

import java.time.Instant;

/**
 * ================================================================
 * ROMELT TECHCARE — NOTIFICATION SUPPRESSION UPDATE REQUEST
 * ================================================================
 *
 * Purpose:
 * Updates editable suppression reason, details, or expiration.
 *
 * Immutable fields:
 * - customerId
 * - channel
 * - notificationCategory
 * - recipientAddress
 * - normalizedRecipientAddress
 * ================================================================
 */
public record NotificationSuppressionUpdateRequest(

        NotificationSuppressionReason suppressionReason,

        @Size(
                max = 1000,
                message = "Notification suppression reason details cannot exceed 1,000 characters."
        )
        String reasonDetails,

        @Future(
                message = "Notification suppression expiration must be in the future."
        )
        Instant expiresAt,

        Boolean clearExpiration
) {

    public boolean hasChanges() {
        return suppressionReason != null
                || reasonDetails != null
                || expiresAt != null
                || Boolean.TRUE.equals(clearExpiration);
    }

    public Instant resolveExpiration(
            Instant existingExpiration
    ) {
        if (Boolean.TRUE.equals(clearExpiration)) {
            return null;
        }

        return expiresAt != null
                ? expiresAt
                : existingExpiration;
    }
}