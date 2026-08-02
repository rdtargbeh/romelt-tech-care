package romelt_techcare.backend.dto;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.Instant;
import java.util.UUID;

/**
 * ================================================================
 * ROMELT TECHCARE — WEBSITE SETTING RESPONSE
 * ================================================================
 *
 * Purpose:
 * Returns complete administrator-facing website-setting information.
 *
 * Responsibilities:
 * - Returns setting identity, group, key, and JSON value.
 * - Returns public and sensitive states.
 * - Returns administrator attribution.
 * - Returns timestamps and optimistic-lock information.
 *
 * Security:
 * This response may contain sensitive setting values and must only be
 * returned by protected administrator endpoints.
 * ================================================================
 */
public record WebsiteSettingResponse(

        UUID websiteSettingId,

        String settingGroup,

        String settingKey,

        JsonNode settingValue,

        String description,

        Boolean isPublic,

        Boolean isSensitive,

        Boolean publiclyAvailable,

        UUID createdByAdminUserId,

        String createdByAdminUserDisplayName,

        UUID updatedByAdminUserId,

        String updatedByAdminUserDisplayName,

        Instant createdAt,

        Instant updatedAt,

        Long rowVersion
) {
}