package romelt_techcare.backend.dto;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.Size;

/**
 * ================================================================
 * ROMELT TECHCARE — NOTIFICATION TEMPLATE UPDATE REQUEST
 * ================================================================
 *
 * Purpose:
 * Updates editable content and activation state for one existing
 * notification-template version.
 *
 * Responsibilities:
 * - Updates the administrator-readable template name.
 * - Updates subject, text-body, and HTML-body content.
 * - Updates required rendering variables.
 * - Activates or deactivates the template version.
 *
 * Important:
 * This request does not modify:
 * - templateKey
 * - channel
 * - locale
 * - templateVersion
 *
 * Those fields form the template-version identity and should remain
 * stable after creation.
 *
 * Null handling:
 * - Null means preserve the existing value.
 * - An empty string clears an optional text field.
 *
 * Channel-specific content validation is performed by the service
 * after applying the requested changes to the existing entity.
 * ================================================================
 */
public record NotificationTemplateUpdateRequest(

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

        JsonNode requiredVariablesJson,

        Boolean active
) {

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

    public boolean hasChanges() {
        return templateName != null
                || subjectTemplate != null
                || bodyTextTemplate != null
                || bodyHtmlTemplate != null
                || requiredVariablesJson != null
                || active != null;
    }
}