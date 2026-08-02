package romelt_techcare.backend.dto;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotNull;

/**
 * ================================================================
 * ROMELT TECHCARE — WEBSITE SETTING VALUE UPDATE REQUEST
 * ================================================================
 *
 * Purpose:
 * Updates only the JSON value of an existing website setting.
 *
 * Usage:
 * Useful for quick setting changes where the group, key, description,
 * visibility, and sensitivity must remain unchanged.
 * ================================================================
 */
public record WebsiteSettingValueUpdateRequest(

        @NotNull(
                message = "Setting value is required."
        )
        JsonNode settingValue
) {
}