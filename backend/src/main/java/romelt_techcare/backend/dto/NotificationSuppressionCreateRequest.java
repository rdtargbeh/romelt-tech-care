package romelt_techcare.backend.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import romelt_techcare.backend.enums.NotificationCategory;
import romelt_techcare.backend.enums.NotificationChannel;
import romelt_techcare.backend.enums.NotificationSuppressionReason;

import java.time.Instant;
import java.util.UUID;

/**
 * ================================================================
 * ROMELT TECHCARE — NOTIFICATION SUPPRESSION CREATE REQUEST
 * ================================================================
 *
 * Purpose:
 * Creates an EMAIL or SMS suppression record.
 *
 * Category:
 * - Null means suppress all categories.
 * - A supplied category suppresses only that category.
 * ================================================================
 */
public record NotificationSuppressionCreateRequest(

        UUID customerId,

        @NotNull(
                message = "Notification suppression channel is required."
        )
        NotificationChannel channel,

        NotificationCategory notificationCategory,

        @NotBlank(
                message = "Notification suppression recipient address is required."
        )
        @Size(
                max = 500,
                message = "Notification suppression recipient address cannot exceed 500 characters."
        )
        String recipientAddress,

        @NotNull(
                message = "Notification suppression reason is required."
        )
        NotificationSuppressionReason suppressionReason,

        @Size(
                max = 1000,
                message = "Notification suppression reason details cannot exceed 1,000 characters."
        )
        String reasonDetails,

        @Future(
                message = "Notification suppression expiration must be in the future."
        )
        Instant expiresAt
) {

    public boolean hasSupportedChannel() {
        return channel == NotificationChannel.EMAIL
                || channel == NotificationChannel.SMS;
    }

    public boolean hasValidRecipientAddress() {
        if (channel == null || recipientAddress == null) {
            return false;
        }

        String normalized = recipientAddress.trim();

        if (normalized.isEmpty()) {
            return false;
        }

        return switch (channel) {
            case EMAIL ->
                    normalized.contains("@")
                            && !normalized.startsWith("@")
                            && !normalized.endsWith("@");

            case SMS -> {
                String digitsOnly =
                        normalized.replaceAll("\\D", "");

                yield digitsOnly.length() >= 7
                        && digitsOnly.length() <= 30;
            }

            case IN_APP -> false;
        };
    }
}