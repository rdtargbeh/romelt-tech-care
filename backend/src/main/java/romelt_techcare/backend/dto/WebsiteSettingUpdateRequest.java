package romelt_techcare.backend.dto;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * ================================================================
 * ROMELT TECHCARE — WEBSITE SETTING UPDATE REQUEST
 * ================================================================
 *
 * Purpose:
 * Carries the complete editable state of one website setting.
 *
 * Update model:
 * This request is intended for a full PUT operation.
 *
 * Security:
 * If isSensitive is true, the service forces isPublic to false.
 * ================================================================
 */
public record WebsiteSettingUpdateRequest(

        @NotBlank(
                message = "Setting group is required."
        )
        @Size(
                max = 100,
                message = "Setting group must not exceed 100 characters."
        )
        String settingGroup,

        @NotBlank(
                message = "Setting key is required."
        )
        @Size(
                max = 160,
                message = "Setting key must not exceed 160 characters."
        )
        String settingKey,

        @NotNull(
                message = "Setting value is required."
        )
        JsonNode settingValue,

        @Size(
                max = 500,
                message = "Description must not exceed 500 characters."
        )
        String description,

        @NotNull(
                message = "Public status is required."
        )
        Boolean isPublic,

        @NotNull(
                message = "Sensitive status is required."
        )
        Boolean isSensitive
) {
}