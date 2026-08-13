package romelt_techcare.backend.dto;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import romelt_techcare.backend.enums.NotificationChannel;
import romelt_techcare.backend.enums.NotificationRecipientType;
import romelt_techcare.backend.enums.NotificationResourceType;

import java.util.UUID;

/**
 * ================================================================
 * ROMELT TECHCARE — NOTIFICATION CREATE REQUEST
 * ================================================================
 *
 * Purpose:
 * Internal request used by booking, contact, email, SMS, and in-app
 * notification services to create one notification record.
 *
 * This DTO is not intended for public customer APIs.
 *
 * Channel rules:
 *
 * EMAIL:
 * - recipientAddress is required.
 * - title is the email subject.
 * - messageText or messageHtml contains the email body.
 *
 * SMS:
 * - recipientAddress is required.
 * - messageText contains the SMS message.
 * - messageHtml must remain null.
 *
 * IN_APP:
 * - adminUserId is required.
 * - title is the portal notification title.
 * - messageText is the portal notification message.
 * - messageHtml must remain null.
 * ================================================================
 */
public record NotificationCreateRequest(

        @NotNull
        NotificationChannel channel,

        @NotNull
        NotificationRecipientType recipientType,

        UUID customerId,

        UUID adminUserId,

        @NotNull
        NotificationResourceType resourceType,

        @NotNull
        UUID resourceId,

        @Size(max = 500)
        String recipientAddress,

        @Size(max = 180)
        String recipientName,

        @NotBlank
        @Size(max = 500)
        String title,

        @NotBlank
        @Size(max = 100000)
        String messageText,

        @Size(max = 250000)
        String messageHtml,

        @Size(max = 100)
        String providerName,

        @Size(max = 255)
        String providerMessageId,

        JsonNode providerResponseJson
) {

    public boolean hasValidRecipient() {
        if (channel == null || recipientType == null) {
            return false;
        }

        return switch (channel) {
            case EMAIL, SMS ->
                    hasText(recipientAddress);

            case IN_APP ->
                    recipientType
                            == NotificationRecipientType.ADMIN
                            && adminUserId != null;
        };
    }

    public boolean hasValidContent() {
        if (
                channel == null
                        || !hasText(title)
                        || !hasText(messageText)
        ) {
            return false;
        }

        return switch (channel) {
            case EMAIL -> true;

            case SMS, IN_APP ->
                    !hasText(messageHtml);
        };
    }

    private boolean hasText(
            String value
    ) {
        return value != null
                && !value.trim().isEmpty();
    }
}