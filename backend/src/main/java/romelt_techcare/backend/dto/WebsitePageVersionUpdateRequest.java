package romelt_techcare.backend.dto;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * ================================================================
 * ROMELT TECHCARE — PAGE VERSION UPDATE REQUEST
 * ================================================================
 *
 * Purpose:
 * Carries the complete editable state of an existing draft.
 *
 * Rules:
 * - Only DRAFT versions may be updated.
 * - Page ownership, version number, lifecycle status, publication
 *   information, and archive information are backend-managed.
 * ================================================================
 */
public record WebsitePageVersionUpdateRequest(

        @NotNull(
                message = "Content schema version is required."
        )
        @Positive(
                message = "Content schema version must be greater than zero."
        )
        Integer contentSchemaVersion,

        @NotNull(
                message = "Page content is required."
        )
        JsonNode contentJson,

        @Size(
                max = 255,
                message = "SEO title must not exceed 255 characters."
        )
        String seoTitle,

        @Size(
                max = 500,
                message = "SEO description must not exceed 500 characters."
        )
        String seoDescription,

        @Size(
                max = 255,
                message = "Social title must not exceed 255 characters."
        )
        String socialTitle,

        @Size(
                max = 500,
                message = "Social description must not exceed 500 characters."
        )
        String socialDescription,

        UUID socialImageMediaId,

        @Size(
                max = 1000,
                message = "Canonical URL must not exceed 1000 characters."
        )
        String canonicalUrl,

        @NotNull(
                message = "Robots index status is required."
        )
        Boolean robotsIndex,

        @NotNull(
                message = "Robots follow status is required."
        )
        Boolean robotsFollow,

        @Size(
                max = 1000,
                message = "Change summary must not exceed 1000 characters."
        )
        String changeSummary
) {
}