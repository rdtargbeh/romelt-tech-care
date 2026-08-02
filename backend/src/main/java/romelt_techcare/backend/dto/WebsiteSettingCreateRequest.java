package romelt_techcare.backend.dto;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * ================================================================
 * ROMELT TECHCARE — WEBSITE SETTING CREATE REQUEST
 * ================================================================
 *
 * Purpose:
 * Carries validated administrator input for creating a website or CMS
 * setting.
 *
 * JSON value examples:
 *
 * String:
 * "Book a Service"
 *
 * Boolean:
 * true
 *
 * Number:
 * 300
 *
 * Object:
 * {
 *   "primary": "#1976D2",
 *   "secondary": "#D4AF37"
 * }
 *
 * Array:
 * ["HOME", "SERVICES", "CONTACT"]
 *
 * Security:
 * Sensitive settings are automatically forced private.
 * ================================================================
 */
public record WebsiteSettingCreateRequest(

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

        Boolean isPublic,

        Boolean isSensitive
) {
}