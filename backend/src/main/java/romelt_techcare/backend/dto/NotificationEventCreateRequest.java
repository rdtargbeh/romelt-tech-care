package romelt_techcare.backend.dto;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import romelt_techcare.backend.enums.NotificationCategory;
import romelt_techcare.backend.enums.NotificationChannel;
import romelt_techcare.backend.enums.NotificationEventType;
import romelt_techcare.backend.enums.NotificationPriority;
import romelt_techcare.backend.enums.NotificationRecipientType;
import romelt_techcare.backend.enums.NotificationResourceType;

import java.time.Instant;
import java.util.UUID;

/**
 * ================================================================
 * ROMELT TECHCARE — NOTIFICATION EVENT CREATE REQUEST
 * ================================================================
 *
 * Purpose:
 * Defines the internal command used by business services to create a
 * transactional EMAIL, SMS, or IN_APP notification event.
 *
 * Channel rules:
 *
 * EMAIL:
 * - recipientEmail is required.
 *
 * SMS:
 * - recipientPhone is required.
 *
 * IN_APP:
 * - recipientType must be ADMIN.
 * - recipientAdminUserId is required.
 *
 * This request must not be exposed through a public controller.
 * ================================================================
 */
public record NotificationEventCreateRequest(

        @NotNull(
                message = "Notification event type is required."
        )
        NotificationEventType eventType,

        @NotNull(
                message = "Notification category is required."
        )
        NotificationCategory notificationCategory,

        @NotNull(
                message = "Notification channel is required."
        )
        NotificationChannel channel,

        @NotNull(
                message = "Notification resource type is required."
        )
        NotificationResourceType resourceType,

        UUID resourceId,

        @Size(
                max = 255,
                message = "Correlation key cannot exceed 255 characters."
        )
        String correlationKey,

        @Size(
                max = 255,
                message = "Idempotency key cannot exceed 255 characters."
        )
        String idempotencyKey,

        @NotNull(
                message = "Notification recipient type is required."
        )
        NotificationRecipientType recipientType,

        UUID customerId,

        UUID recipientAdminUserId,

        @Email(
                message = "Recipient email must be valid."
        )
        @Size(
                max = 254,
                message = "Recipient email cannot exceed 254 characters."
        )
        String recipientEmail,

        @Pattern(
                regexp = "^$|^[0-9+()\\-\\.\\s]{7,40}$",
                message = "Recipient telephone number is invalid."
        )
        String recipientPhone,

        @Size(
                max = 180,
                message = "Recipient name cannot exceed 180 characters."
        )
        String recipientName,

        UUID notificationTemplateId,

        @NotBlank(
                message = "Notification template key is required."
        )
        @Size(
                max = 160,
                message = "Notification template key cannot exceed 160 characters."
        )
        String templateKey,

        @Min(
                value = 1,
                message = "Notification template version must be greater than zero."
        )
        Integer templateVersion,

        @Size(
                max = 20,
                message = "Notification template locale cannot exceed 20 characters."
        )
        String templateLocale,

        @NotNull(
                message = "Notification template data is required."
        )
        JsonNode templateDataJson,

        NotificationPriority priority,

        Instant scheduledFor,

        Instant availableAt,

        @Min(
                value = 1,
                message = "Maximum attempts must be greater than zero."
        )
        Integer maximumAttempts,

        UUID createdByAdminUserId
) {

    public boolean hasRecipient() {
        return customerId != null
                || recipientAdminUserId != null
                || hasText(recipientEmail)
                || hasText(recipientPhone);
    }

    public boolean isCustomerRecipientValid() {
        if (recipientType != NotificationRecipientType.CUSTOMER) {
            return true;
        }

        return customerId != null
                || hasText(recipientEmail)
                || hasText(recipientPhone);
    }

    public boolean isAdminRecipientValid() {
        if (recipientType != NotificationRecipientType.ADMIN) {
            return true;
        }

        return recipientAdminUserId != null
                || hasText(recipientEmail);
    }

    public boolean hasValidChannelRecipient() {
        if (channel == null) {
            return false;
        }

        return switch (channel) {
            case EMAIL ->
                    hasText(recipientEmail);

            case SMS ->
                    hasText(recipientPhone);

            case IN_APP ->
                    recipientType == NotificationRecipientType.ADMIN
                            && recipientAdminUserId != null;
        };
    }

    public boolean hasObjectTemplateData() {
        return templateDataJson != null
                && templateDataJson.isObject();
    }

    public boolean hasValidSchedule() {
        return scheduledFor == null
                || availableAt == null
                || !availableAt.isBefore(scheduledFor);
    }

    private boolean hasText(
            String value
    ) {
        return value != null
                && !value.trim().isEmpty();
    }
}