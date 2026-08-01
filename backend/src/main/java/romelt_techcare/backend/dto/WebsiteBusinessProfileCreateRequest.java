package romelt_techcare.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * ================================================================
 * ROMELT TECHCARE — WEBSITE BUSINESS PROFILE CREATE REQUEST
 * ================================================================
 *
 * Purpose:
 * Carries validated administrator input for creating the initial
 * Romelt TechCare public business profile.
 * ================================================================
 */
public record WebsiteBusinessProfileCreateRequest(

        @NotBlank(message = "Business name is required.")
        @Size(max = 180)
        String businessName,

        @Size(max = 220)
        String legalBusinessName,

        @Size(max = 255)
        String tagline,

        @Size(max = 255)
        String secondaryTagline,

        @Size(max = 500)
        String shortDescription,

        String fullDescription,

        @Email(message = "Public email must be valid.")
        @Size(max = 254)
        String publicEmail,

        @Size(max = 40)
        String publicPhone,

        @Size(max = 180)
        String streetAddress,

        @Size(max = 180)
        String addressLine2,

        @Size(max = 100)
        String city,

        @Size(max = 100)
        String stateRegion,

        @Size(max = 30)
        String postalCode,

        @NotBlank(message = "Country code is required.")
        @Pattern(
                regexp = "^[A-Za-z]{2}$",
                message = "Country code must contain exactly two letters."
        )
        String countryCode,

        @Size(max = 500)
        String serviceArea,

        Boolean appointmentOnly,

        @NotBlank(message = "Default locale is required.")
        @Size(max = 20)
        String defaultLocale,

        @NotBlank(message = "Default timezone is required.")
        @Size(max = 80)
        String defaultTimezone,

        @Size(max = 255)
        String primaryDomain,

        UUID primaryLogoMediaId,

        UUID lightLogoMediaId,

        UUID darkLogoMediaId,

        UUID faviconMediaId,

        UUID defaultSocialImageMediaId,

        Boolean isActive
) {
}