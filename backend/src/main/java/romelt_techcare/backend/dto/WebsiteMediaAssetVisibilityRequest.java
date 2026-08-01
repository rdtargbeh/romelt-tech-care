package romelt_techcare.backend.dto;

import jakarta.validation.constraints.NotNull;

/**
 * ================================================================
 * ROMELT TECHCARE — MEDIA ASSET VISIBILITY REQUEST
 * ================================================================
 *
 * Purpose:
 * Carries the requested public-visibility state for a website media
 * asset.
 *
 * Rules:
 * - Only an ACTIVE asset may become public.
 * - A public informative image must have alternative text.
 * - A decorative image may remain public without descriptive alt text.
 * - Archived, failed, processing, uploading, or deleted assets cannot
 *   be made public.
 * ================================================================
 */
public record WebsiteMediaAssetVisibilityRequest(

        @NotNull(
                message = "Public visibility is required."
        )
        Boolean isPublic
) {
}