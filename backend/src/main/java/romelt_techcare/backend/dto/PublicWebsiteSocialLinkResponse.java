package romelt_techcare.backend.dto;

/**
 * ================================================================
 * ROMELT TECHCARE — PUBLIC WEBSITE SOCIAL LINK RESPONSE
 * ================================================================
 *
 * Purpose:
 * Provides the public React website with one active social or external
 * business-profile link.
 *
 * Security:
 * Internal identifiers, administrator attribution, timestamps, and
 * optimistic-lock information are excluded.
 * ================================================================
 */
public record PublicWebsiteSocialLinkResponse(

        String platform,

        String label,

        String profileUrl,

        String iconKey,

        Integer displayOrder
) {
}