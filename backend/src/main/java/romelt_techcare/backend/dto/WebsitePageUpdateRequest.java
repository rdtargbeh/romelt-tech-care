package romelt_techcare.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import romelt_techcare.backend.enums.WebsitePageType;

/**
 * ================================================================
 * ROMELT TECHCARE — WEBSITE PAGE UPDATE REQUEST
 * ================================================================
 *
 * Purpose:
 * Carries the complete editable identity and routing state of one
 * website page.
 *
 * Version pointers are updated through dedicated endpoints and are not
 * included in this request.
 * ================================================================
 */
public record WebsitePageUpdateRequest(

        @NotBlank(message = "Page key is required.")
        @Size(
                max = 120,
                message = "Page key must not exceed 120 characters."
        )
        String pageKey,

        @NotBlank(message = "Page name is required.")
        @Size(
                max = 180,
                message = "Page name must not exceed 180 characters."
        )
        String pageName,

        @NotBlank(message = "Route path is required.")
        @Size(
                max = 500,
                message = "Route path must not exceed 500 characters."
        )
        String routePath,

        @NotNull(message = "Page type is required.")
        WebsitePageType pageType,

        @NotNull(message = "Content schema version is required.")
        @Positive(
                message = "Content schema version must be greater than zero."
        )
        Integer contentSchemaVersion,

        @NotNull(message = "System-page status is required.")
        Boolean isSystemPage,

        @NotNull(message = "Active status is required.")
        Boolean isActive
) {
}