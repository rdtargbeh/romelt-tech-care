package romelt_techcare.backend.dto;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import romelt_techcare.backend.enums.NotificationChannel;

import java.time.Instant;
import java.util.UUID;

/**
 * ================================================================
 * ROMELT TECHCARE — NOTIFICATION DELIVERY CREATE REQUEST
 * ================================================================
 *
 * Purpose:
 * Internal request used by the notification worker after a
 * NotificationEvent has been committed.
 *
 * One NotificationEvent may generate one or more deliveries:
 * - EMAIL
 * - SMS
 * - IN_APP
 *
 * This DTO is NOT intended for public APIs.
 * ================================================================
 */
public record NotificationDeliveryCreateRequest(

        @NotNull
        UUID notificationEventId,

        UUID customerId,

        UUID recipientAdminUserId,

        @NotNull
        NotificationChannel channel,

        @NotBlank
        @Size(max = 500)
        String recipientAddress,

        @Size(max = 180)
        String recipientName,

        @Size(max = 500)
        String senderAddress,

        @Size(max = 180)
        String senderName,

        @Size(max = 500)
        String subject,

        String renderedBodyText,

        String renderedBodyHtml,

        @Size(max = 100)
        String providerName,

        @Size(max = 255)
        String providerMessageId,

        JsonNode providerResponseJson,

        @Min(1)
        Integer maximumAttempts
) {

    public boolean isEmail() {
        return channel == NotificationChannel.EMAIL;
    }

    public boolean isSms() {
        return channel == NotificationChannel.SMS;
    }

    public boolean isInApp() {
        return channel == NotificationChannel.IN_APP;
    }

    public boolean hasValidContent() {

        if (channel == null) {
            return false;
        }

        return switch (channel) {

            case EMAIL ->
                    hasText(subject)
                            &&
                            (
                                    hasText(renderedBodyText)
                                            || hasText(renderedBodyHtml)
                            );

            case SMS ->
                    hasText(renderedBodyText);

            case IN_APP ->
                    recipientAdminUserId != null
                            &&
                            hasText(subject)
                            &&
                            hasText(renderedBodyText);
        };
    }

    private boolean hasText(
            String value
    ) {
        return value != null
                && !value.trim().isEmpty();
    }

}