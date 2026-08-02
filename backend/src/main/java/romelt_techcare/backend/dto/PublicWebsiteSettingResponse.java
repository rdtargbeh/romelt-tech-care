package romelt_techcare.backend.dto;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * ================================================================
 * ROMELT TECHCARE — PUBLIC WEBSITE SETTING RESPONSE
 * ================================================================
 *
 * Purpose:
 * Returns one explicitly public, non-sensitive website setting to the
 * React public website.
 *
 * Security:
 * - Sensitive settings are never mapped to this response.
 * - Administrator information is excluded.
 * - Internal identifiers and optimistic-lock fields are excluded.
 * ================================================================
 */
public record PublicWebsiteSettingResponse(

        String settingGroup,

        String settingKey,

        JsonNode settingValue,

        String description
) {
}