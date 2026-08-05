package romelt_techcare.backend.dto;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import romelt_techcare.backend.enums.NotificationChannel;

/**
 * ================================================================
 * ROMELT TECHCARE — NOTIFICATION TEMPLATE CREATE REQUEST
 * ================================================================
 *
 * Purpose:
 * Creates a new versioned EMAIL, SMS, or IN_APP notification
 * template.
 *
 * Responsibilities:
 * - Identifies the logical template key.
 * - Identifies the notification channel.
 * - Stores administrator-readable template naming.
 * - Stores subject, text-body, and HTML-body templates.
 * - Stores locale and template version.
 * - Stores required template variables.
 * - Determines whether the new template version is active.
 *
 * Channel rules:
 *
 * EMAIL:
 * - subjectTemplate is required.
 * - bodyTextTemplate or bodyHtmlTemplate is required.
 *
 * SMS:
 * - bodyTextTemplate is required.
 * - subjectTemplate and bodyHtmlTemplate are not used.
 *
 * IN_APP:
 * - subjectTemplate is required as the notification title.
 * - bodyTextTemplate is required as the notification message.
 * - bodyHtmlTemplate is not used.
 *
 * Required variables:
 * requiredVariablesJson must be a JSON array containing non-blank
 * strings.
 *
 * Example:
 *
 * [
 *   "customerName",
 *   "referenceNumber",
 *   "scheduledStartAt"
 * ]
 * ================================================================
 */
public record NotificationTemplateCreateRequest(

        @NotBlank(
                message = "Notification template key is required."
        )
        @Size(
                max = 160,
                message = "Notification template key cannot exceed 160 characters."
        )
        String templateKey,

        @NotNull(
                message = "Notification template channel is required."
        )
        NotificationChannel channel,

        @NotBlank(
                message = "Notification template name is required."
        )
        @Size(
                max = 180,
                message = "Notification template name cannot exceed 180 characters."
        )
        String templateName,

        @Size(
                max = 500,
                message = "Notification subject template cannot exceed 500 characters."
        )
        String subjectTemplate,

        @Size(
                max = 50000,
                message = "Notification text-body template cannot exceed 50,000 characters."
        )
        String bodyTextTemplate,

        @Size(
                max = 100000,
                message = "Notification HTML-body template cannot exceed 100,000 characters."
        )
        String bodyHtmlTemplate,

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

        JsonNode requiredVariablesJson,

        Boolean active
) {

    /**
     * Validates that requiredVariablesJson is either absent or a JSON
     * array containing only non-blank string values.
     */
    public boolean hasValidRequiredVariables() {
        if (requiredVariablesJson == null) {
            return true;
        }

        if (!requiredVariablesJson.isArray()) {
            return false;
        }

        for (JsonNode variable : requiredVariablesJson) {
            if (
                    variable == null
                            || !variable.isTextual()
                            || variable.asText().trim().isEmpty()
            ) {
                return false;
            }
        }

        return true;
    }

    /**
     * Validates the content required by the selected channel.
     */
    public boolean hasValidChannelContent() {
        if (channel == null) {
            return false;
        }

        return switch (channel) {
            case EMAIL ->
                    hasText(subjectTemplate)
                            && (
                            hasText(bodyTextTemplate)
                                    || hasText(bodyHtmlTemplate)
                    );

            case SMS ->
                    hasText(bodyTextTemplate);

            case IN_APP ->
                    hasText(subjectTemplate)
                            && hasText(bodyTextTemplate);
        };
    }

    /**
     * Ensures channel-incompatible content is not supplied.
     */
    public boolean hasValidChannelSpecificFields() {
        if (channel == null) {
            return false;
        }

        return switch (channel) {
            case EMAIL -> true;

            case SMS ->
                    !hasText(subjectTemplate)
                            && !hasText(bodyHtmlTemplate);

            case IN_APP ->
                    !hasText(bodyHtmlTemplate);
        };
    }

    private boolean hasText(
            String value
    ) {
        return value != null
                && !value.trim().isEmpty();
    }
}