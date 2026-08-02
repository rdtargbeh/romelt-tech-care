package romelt_techcare.backend.dto;

import jakarta.validation.constraints.NotNull;

/**
 * ================================================================
 * ROMELT TECHCARE — WEBSITE SETTING VISIBILITY REQUEST
 * ================================================================
 *
 * Purpose:
 * Updates the public and sensitive state of one website setting.
 *
 * Security rule:
 * A sensitive setting cannot be public. When isSensitive is true,
 * isPublic is automatically forced to false.
 * ================================================================
 */
public record WebsiteSettingVisibilityRequest(

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