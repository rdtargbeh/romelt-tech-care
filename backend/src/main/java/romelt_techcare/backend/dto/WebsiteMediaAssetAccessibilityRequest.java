package romelt_techcare.backend.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * ================================================================
 * ROMELT TECHCARE — MEDIA ASSET ACCESSIBILITY REQUEST
 * ================================================================
 *
 * Purpose:
 * Carries accessibility metadata for a website media asset.
 *
 * Accessibility rules:
 * - Informative images require meaningful alternative text.
 * - Decorative images may use empty alternative text.
 * - The backend validates the combination before persistence.
 * ================================================================
 */
public record WebsiteMediaAssetAccessibilityRequest(

        @Size(
                max = 500,
                message = "Alternative text must not exceed 500 characters."
        )
        String altText,

        @NotNull(
                message = "Decorative status is required."
        )
        Boolean isDecorative
) {
}