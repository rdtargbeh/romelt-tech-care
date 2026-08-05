package romelt_techcare.backend.dto;

import java.util.UUID;

/**
 * ================================================================
 * ROMELT TECHCARE — PUBLIC WEBSITE BUSINESS PROFILE RESPONSE
 * ================================================================
 *
 * Purpose:
 * Returns the active public business identity and branding information
 * required by the React website.
 *
 * Security:
 * Administrator attribution, timestamps, internal identifiers other
 * than the profile ID, and optimistic-lock information are excluded.
 * ================================================================
 */
public record PublicWebsiteBusinessProfileResponse(

        UUID businessProfileId,

        String businessName,
        String legalBusinessName,

        String tagline,
        String secondaryTagline,

        String shortDescription,
        String fullDescription,

        String publicEmail,
        String publicPhone,

        String streetAddress,
        String addressLine2,
        String city,
        String stateRegion,
        String postalCode,
        String countryCode,

        String serviceArea,

        Boolean appointmentOnly,

        String defaultLocale,
        String defaultTimezone,

        String primaryDomain,

        PublicWebsiteMediaAssetResponse primaryLogo,
        PublicWebsiteMediaAssetResponse lightLogo,
        PublicWebsiteMediaAssetResponse darkLogo,
        PublicWebsiteMediaAssetResponse favicon,
        PublicWebsiteMediaAssetResponse defaultSocialImage
) {
}