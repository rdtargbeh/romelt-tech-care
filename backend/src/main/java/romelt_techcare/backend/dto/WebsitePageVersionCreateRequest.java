package romelt_techcare.backend.dto;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * ================================================================
 * ROMELT TECHCARE — PAGE VERSION CREATE REQUEST
 * ================================================================
 *
 * Purpose:
 * Creates the current editable draft for a website page.
 *
 * Version numbering:
 * The backend calculates the next version number. Clients must never
 * submit or calculate version numbers.
 *
 * Draft rule:
 * A page may have only one current draft.
 * ================================================================
 */
public record WebsitePageVersionCreateRequest(

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

        Boolean robotsIndex,

        Boolean robotsFollow,

        @Size(
                max = 1000,
                message = "Change summary must not exceed 1000 characters."
        )
        String changeSummary
) {
}