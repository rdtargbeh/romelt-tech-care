package romelt_techcare.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import romelt_techcare.backend.enums.WebsitePageType;

import java.util.UUID;

/**
 * ================================================================
 * ROMELT TECHCARE — WEBSITE PAGE CREATE REQUEST
 * ================================================================
 *
 * Purpose:
 * Carries validated administrator input for creating a stable website
 * page identity.
 *
 * Version identifiers are optional because a page can be registered
 * before its first draft or publication is created.
 * ================================================================
 */
public record WebsitePageCreateRequest(

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

        WebsitePageType pageType,

        UUID draftVersionId,

        UUID publishedVersionId,

        @Positive(
                message = "Content schema version must be greater than zero."
        )
        Integer contentSchemaVersion,

        Boolean isSystemPage,

        Boolean isActive
) {
}