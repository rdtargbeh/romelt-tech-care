package romelt_techcare.backend.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * ================================================================
 * ROMELT TECHCARE — WEBSITE BUSINESS PROFILE RESPONSE
 * ================================================================
 *
 * Purpose:
 * Returns complete administrator-facing business-profile information
 * without serializing JPA relationships directly.
 * ================================================================
 */
public record WebsiteBusinessProfileResponse(

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
        PublicWebsiteMediaAssetResponse defaultSocialImage,

        Boolean isActive,

        UUID createdByAdminUserId,
        String createdByAdminUserDisplayName,

        UUID updatedByAdminUserId,
        String updatedByAdminUserDisplayName,

        Instant createdAt,
        Instant updatedAt,

        Long rowVersion
) {
}