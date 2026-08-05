package romelt_techcare.backend.dto;

import com.fasterxml.jackson.databind.JsonNode;
import romelt_techcare.backend.enums.NotificationChannel;

import java.time.Instant;
import java.util.UUID;

/**
 * ================================================================
 * ROMELT TECHCARE — NOTIFICATION TEMPLATE RESPONSE
 * ================================================================
 *
 * Purpose:
 * Returns the complete notification-template version to authenticated
 * administrators.
 *
 * Responsibilities:
 * - Returns template identity and channel.
 * - Returns complete subject and body content.
 * - Returns locale and version.
 * - Returns required rendering variables.
 * - Returns activation state.
 * - Returns administrator attribution.
 * - Returns audit timestamps and optimistic-locking data.
 *
 * Security:
 * This response is administrator-only.
 * ================================================================
 */
public record NotificationTemplateResponse(

        UUID notificationTemplateId,

        String templateKey,

        NotificationChannel channel,

        String templateName,

        String subjectTemplate,

        String bodyTextTemplate,

        String bodyHtmlTemplate,

        String locale,

        Integer templateVersion,

        JsonNode requiredVariablesJson,

        boolean active,

        UUID createdByAdminUserId,

        UUID updatedByAdminUserId,

        Instant createdAt,

        Instant updatedAt,

        Long rowVersion
) {
}