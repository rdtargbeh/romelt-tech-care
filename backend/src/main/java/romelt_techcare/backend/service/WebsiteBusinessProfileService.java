package romelt_techcare.backend.service;

import romelt_techcare.backend.entity.WebsiteBusinessProfile;

import java.util.UUID;

/**
 * ================================================================
 * ROMELT TECHCARE — WEBSITE BUSINESS PROFILE SERVICE
 * ================================================================
 *
 * Purpose:
 * Defines business operations for managing the Romelt TechCare public
 * business profile.
 *
 * Responsibilities:
 * - Creates the initial business profile.
 * - Updates public identity, contact, location, and branding data.
 * - Retrieves administrator and public profile information.
 * - Activates and deactivates the profile.
 * - Enforces the single-active-profile rule.
 * - Validates and assigns website media assets.
 * - Synchronizes website media-usage records.
 * ================================================================
 */
public interface WebsiteBusinessProfileService {

    WebsiteBusinessProfile createBusinessProfile(
            WebsiteBusinessProfile businessProfile,
            UUID administratorId
    );

    WebsiteBusinessProfile updateBusinessProfile(
            UUID businessProfileId,
            WebsiteBusinessProfile requestedUpdate,
            UUID administratorId
    );

    WebsiteBusinessProfile getBusinessProfile(
            UUID businessProfileId
    );

    WebsiteBusinessProfile getActiveBusinessProfile();

    WebsiteBusinessProfile activateBusinessProfile(
            UUID businessProfileId,
            UUID administratorId
    );

    WebsiteBusinessProfile deactivateBusinessProfile(
            UUID businessProfileId,
            UUID administratorId
    );
}