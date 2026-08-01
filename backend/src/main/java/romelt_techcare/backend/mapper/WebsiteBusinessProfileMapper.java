package romelt_techcare.backend.mapper;

import org.springframework.stereotype.Component;
import romelt_techcare.backend.dto.PublicWebsiteBusinessProfileResponse;
import romelt_techcare.backend.dto.PublicWebsiteMediaAssetResponse;
import romelt_techcare.backend.dto.WebsiteBusinessProfileCreateRequest;
import romelt_techcare.backend.dto.WebsiteBusinessProfileResponse;
import romelt_techcare.backend.dto.WebsiteBusinessProfileUpdateRequest;
import romelt_techcare.backend.entity.AdminUser;
import romelt_techcare.backend.entity.WebsiteBusinessProfile;
import romelt_techcare.backend.entity.WebsiteMediaAsset;

import java.util.UUID;

/**
 * ================================================================
 * ROMELT TECHCARE — WEBSITE BUSINESS PROFILE MAPPER
 * ================================================================
 *
 * Purpose:
 * Converts profile request DTOs into service-compatible entities and
 * converts persisted profiles into administrator and public responses.
 * ================================================================
 */
@Component
public class WebsiteBusinessProfileMapper {

    public WebsiteBusinessProfile toEntity(
            WebsiteBusinessProfileCreateRequest request
    ) {
        if (request == null) {
            return null;
        }

        return WebsiteBusinessProfile.builder()
                .businessName(request.businessName())
                .legalBusinessName(request.legalBusinessName())
                .tagline(request.tagline())
                .secondaryTagline(request.secondaryTagline())
                .shortDescription(request.shortDescription())
                .fullDescription(request.fullDescription())
                .publicEmail(request.publicEmail())
                .publicPhone(request.publicPhone())
                .streetAddress(request.streetAddress())
                .addressLine2(request.addressLine2())
                .city(request.city())
                .stateRegion(request.stateRegion())
                .postalCode(request.postalCode())
                .countryCode(request.countryCode())
                .serviceArea(request.serviceArea())
                .appointmentOnly(
                        request.appointmentOnly() == null
                                || request.appointmentOnly()
                )
                .defaultLocale(request.defaultLocale())
                .defaultTimezone(request.defaultTimezone())
                .primaryDomain(request.primaryDomain())
                .primaryLogoMedia(
                        mediaReference(request.primaryLogoMediaId())
                )
                .lightLogoMedia(
                        mediaReference(request.lightLogoMediaId())
                )
                .darkLogoMedia(
                        mediaReference(request.darkLogoMediaId())
                )
                .faviconMedia(
                        mediaReference(request.faviconMediaId())
                )
                .defaultSocialImageMedia(
                        mediaReference(
                                request.defaultSocialImageMediaId()
                        )
                )
                .isActive(
                        request.isActive() == null
                                || request.isActive()
                )
                .build();
    }

    public WebsiteBusinessProfile toUpdateEntity(
            WebsiteBusinessProfileUpdateRequest request
    ) {
        if (request == null) {
            return null;
        }

        return WebsiteBusinessProfile.builder()
                .businessName(request.businessName())
                .legalBusinessName(request.legalBusinessName())
                .tagline(request.tagline())
                .secondaryTagline(request.secondaryTagline())
                .shortDescription(request.shortDescription())
                .fullDescription(request.fullDescription())
                .publicEmail(request.publicEmail())
                .publicPhone(request.publicPhone())
                .streetAddress(request.streetAddress())
                .addressLine2(request.addressLine2())
                .city(request.city())
                .stateRegion(request.stateRegion())
                .postalCode(request.postalCode())
                .countryCode(request.countryCode())
                .serviceArea(request.serviceArea())
                .appointmentOnly(request.appointmentOnly())
                .defaultLocale(request.defaultLocale())
                .defaultTimezone(request.defaultTimezone())
                .primaryDomain(request.primaryDomain())
                .primaryLogoMedia(
                        mediaReference(request.primaryLogoMediaId())
                )
                .lightLogoMedia(
                        mediaReference(request.lightLogoMediaId())
                )
                .darkLogoMedia(
                        mediaReference(request.darkLogoMediaId())
                )
                .faviconMedia(
                        mediaReference(request.faviconMediaId())
                )
                .defaultSocialImageMedia(
                        mediaReference(
                                request.defaultSocialImageMediaId()
                        )
                )
                .isActive(request.isActive())
                .build();
    }

    public WebsiteBusinessProfileResponse toResponse(
            WebsiteBusinessProfile profile
    ) {
        if (profile == null) {
            return null;
        }

        return new WebsiteBusinessProfileResponse(
                profile.getBusinessProfileId(),
                profile.getBusinessName(),
                profile.getLegalBusinessName(),
                profile.getTagline(),
                profile.getSecondaryTagline(),
                profile.getShortDescription(),
                profile.getFullDescription(),
                profile.getPublicEmail(),
                profile.getPublicPhone(),
                profile.getStreetAddress(),
                profile.getAddressLine2(),
                profile.getCity(),
                profile.getStateRegion(),
                profile.getPostalCode(),
                profile.getCountryCode(),
                profile.getServiceArea(),
                profile.getAppointmentOnly(),
                profile.getDefaultLocale(),
                profile.getDefaultTimezone(),
                profile.getPrimaryDomain(),
                publicMedia(profile.getPrimaryLogoMedia()),
                publicMedia(profile.getLightLogoMedia()),
                publicMedia(profile.getDarkLogoMedia()),
                publicMedia(profile.getFaviconMedia()),
                publicMedia(profile.getDefaultSocialImageMedia()),
                profile.getIsActive(),
                adminId(profile.getCreatedByAdminUser()),
                adminDisplayName(profile.getCreatedByAdminUser()),
                adminId(profile.getUpdatedByAdminUser()),
                adminDisplayName(profile.getUpdatedByAdminUser()),
                profile.getCreatedAt(),
                profile.getUpdatedAt(),
                profile.getRowVersion()
        );
    }

    public PublicWebsiteBusinessProfileResponse toPublicResponse(
            WebsiteBusinessProfile profile
    ) {
        if (profile == null) {
            return null;
        }

        return new PublicWebsiteBusinessProfileResponse(
                profile.getBusinessProfileId(),
                profile.getBusinessName(),
                profile.getLegalBusinessName(),
                profile.getTagline(),
                profile.getSecondaryTagline(),
                profile.getShortDescription(),
                profile.getFullDescription(),
                profile.getPublicEmail(),
                profile.getPublicPhone(),
                profile.getStreetAddress(),
                profile.getAddressLine2(),
                profile.getCity(),
                profile.getStateRegion(),
                profile.getPostalCode(),
                profile.getCountryCode(),
                profile.getServiceArea(),
                profile.getAppointmentOnly(),
                profile.getDefaultLocale(),
                profile.getDefaultTimezone(),
                profile.getPrimaryDomain(),
                publicMedia(profile.getPrimaryLogoMedia()),
                publicMedia(profile.getLightLogoMedia()),
                publicMedia(profile.getDarkLogoMedia()),
                publicMedia(profile.getFaviconMedia()),
                publicMedia(profile.getDefaultSocialImageMedia())
        );
    }

    private WebsiteMediaAsset mediaReference(UUID mediaAssetId) {
        if (mediaAssetId == null) {
            return null;
        }

        return WebsiteMediaAsset.builder()
                .mediaAssetId(mediaAssetId)
                .build();
    }

    private PublicWebsiteMediaAssetResponse publicMedia(
            WebsiteMediaAsset mediaAsset
    ) {
        if (mediaAsset == null) {
            return null;
        }

        return PublicWebsiteMediaAssetResponse.from(mediaAsset);
    }

    private UUID adminId(AdminUser adminUser) {
        return adminUser == null
                ? null
                : adminUser.getAdminUserId();
    }

    private String adminDisplayName(AdminUser adminUser) {
        if (adminUser == null) {
            return null;
        }

        String firstName = normalize(adminUser.getFirstName());
        String lastName = normalize(adminUser.getLastName());

        if (firstName != null && lastName != null) {
            return firstName + " " + lastName;
        }

        if (firstName != null) {
            return firstName;
        }

        if (lastName != null) {
            return lastName;
        }

        return normalize(adminUser.getEmail());
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }

        String normalized = value.trim();

        return normalized.isEmpty()
                ? null
                : normalized;
    }
}