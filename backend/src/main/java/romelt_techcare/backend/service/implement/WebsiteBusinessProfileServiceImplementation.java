package romelt_techcare.backend.service.implement;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import romelt_techcare.backend.entity.AdminUser;
import romelt_techcare.backend.entity.WebsiteBusinessProfile;
import romelt_techcare.backend.entity.WebsiteMediaAsset;
import romelt_techcare.backend.enums.WebsiteContentAuditAction;
import romelt_techcare.backend.enums.WebsiteContentAuditResourceType;
import romelt_techcare.backend.enums.WebsiteMediaAssetStatus;
import romelt_techcare.backend.enums.WebsiteMediaUsageResourceType;
import romelt_techcare.backend.repository.AdminUserRepository;
import romelt_techcare.backend.repository.WebsiteBusinessProfileRepository;
import romelt_techcare.backend.repository.WebsiteMediaAssetRepository;
import romelt_techcare.backend.service.WebsiteBusinessProfileService;
import romelt_techcare.backend.service.WebsiteContentAuditLogService;
import romelt_techcare.backend.service.WebsiteContentAuditSnapshotService;
import romelt_techcare.backend.service.WebsiteMediaUsageService;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * ================================================================
 * ROMELT TECHCARE — WEBSITE BUSINESS PROFILE SERVICE IMPLEMENTATION
 * ================================================================
 *
 * Purpose:
 * Implements production business rules for the Romelt TechCare public
 * business profile.
 *
 * Responsibilities:
 * - Creates and updates the shared business profile.
 * - Enforces one active profile.
 * - Loads and validates branding media assets.
 * - Prevents deleted, archived, failed, or incomplete media from being
 *   assigned to the public business profile.
 * - Synchronizes media-usage references transactionally.
 * - Attributes all write operations to an authenticated administrator.
 * - Records immutable audit events for all profile changes.
 *
 * Publishing behavior:
 * Profile changes are immediately visible through the public endpoint.
 *
 * Audit security:
 * Audit snapshots intentionally omit:
 * - public email;
 * - public phone;
 * - street address;
 * - address line 2;
 * - postal code;
 * - long business descriptions.
 * ================================================================
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WebsiteBusinessProfileServiceImplementation
        implements WebsiteBusinessProfileService {

    private static final String PRIMARY_LOGO_USAGE =
            "PRIMARY_LOGO";

    private static final String LIGHT_LOGO_USAGE =
            "LIGHT_LOGO";

    private static final String DARK_LOGO_USAGE =
            "DARK_LOGO";

    private static final String FAVICON_USAGE =
            "FAVICON";

    private static final String DEFAULT_SOCIAL_IMAGE_USAGE =
            "DEFAULT_SOCIAL_IMAGE";

    private final WebsiteBusinessProfileRepository
            websiteBusinessProfileRepository;

    private final WebsiteMediaAssetRepository
            websiteMediaAssetRepository;

    private final WebsiteMediaUsageService
            websiteMediaUsageService;

    private final AdminUserRepository
            adminUserRepository;

    private final WebsiteContentAuditLogService
            websiteContentAuditLogService;

    private final WebsiteContentAuditSnapshotService
            websiteContentAuditSnapshotService;

    @Override
    @Transactional
    public WebsiteBusinessProfile createBusinessProfile(
            WebsiteBusinessProfile businessProfile,
            UUID administratorId
    ) {
        if (businessProfile == null) {
            throw badRequest(
                    "Website business profile information is required."
            );
        }

        AdminUser administrator =
                getRequiredAdministrator(administratorId);

        validateRequiredFields(businessProfile);

        if (
                Boolean.TRUE.equals(
                        businessProfile.getIsActive()
                )
                        && websiteBusinessProfileRepository
                        .existsByIsActiveTrue()
        ) {
            throw conflict(
                    "An active website business profile already exists."
            );
        }

        WebsiteBusinessProfile profileToCreate =
                WebsiteBusinessProfile.builder()
                        .businessName(
                                businessProfile.getBusinessName()
                        )
                        .legalBusinessName(
                                businessProfile.getLegalBusinessName()
                        )
                        .tagline(
                                businessProfile.getTagline()
                        )
                        .secondaryTagline(
                                businessProfile.getSecondaryTagline()
                        )
                        .shortDescription(
                                businessProfile.getShortDescription()
                        )
                        .fullDescription(
                                businessProfile.getFullDescription()
                        )
                        .publicEmail(
                                businessProfile.getPublicEmail()
                        )
                        .publicPhone(
                                businessProfile.getPublicPhone()
                        )
                        .streetAddress(
                                businessProfile.getStreetAddress()
                        )
                        .addressLine2(
                                businessProfile.getAddressLine2()
                        )
                        .city(
                                businessProfile.getCity()
                        )
                        .stateRegion(
                                businessProfile.getStateRegion()
                        )
                        .postalCode(
                                businessProfile.getPostalCode()
                        )
                        .countryCode(
                                businessProfile.getCountryCode()
                        )
                        .serviceArea(
                                businessProfile.getServiceArea()
                        )
                        .appointmentOnly(
                                businessProfile.getAppointmentOnly()
                        )
                        .defaultLocale(
                                businessProfile.getDefaultLocale()
                        )
                        .defaultTimezone(
                                businessProfile.getDefaultTimezone()
                        )
                        .primaryDomain(
                                businessProfile.getPrimaryDomain()
                        )
                        .primaryLogoMedia(
                                validatePublicMedia(
                                        businessProfile
                                                .getPrimaryLogoMedia(),
                                        "Primary logo"
                                )
                        )
                        .lightLogoMedia(
                                validatePublicMedia(
                                        businessProfile
                                                .getLightLogoMedia(),
                                        "Light logo"
                                )
                        )
                        .darkLogoMedia(
                                validatePublicMedia(
                                        businessProfile
                                                .getDarkLogoMedia(),
                                        "Dark logo"
                                )
                        )
                        .faviconMedia(
                                validatePublicMedia(
                                        businessProfile
                                                .getFaviconMedia(),
                                        "Favicon"
                                )
                        )
                        .defaultSocialImageMedia(
                                validatePublicMedia(
                                        businessProfile
                                                .getDefaultSocialImageMedia(),
                                        "Default social image"
                                )
                        )
                        .isActive(
                                businessProfile.getIsActive() == null
                                        || businessProfile.getIsActive()
                        )
                        .createdByAdminUser(administrator)
                        .updatedByAdminUser(administrator)
                        .build();

        WebsiteBusinessProfile savedProfile =
                saveProfile(profileToCreate);

        synchronizeMediaUsages(savedProfile);

        websiteContentAuditLogService.recordAudit(
                administratorId,
                WebsiteContentAuditAction.CREATE,
                WebsiteContentAuditResourceType.BUSINESS_PROFILE,
                savedProfile.getBusinessProfileId(),
                savedProfile.getBusinessName(),
                null,
                createProfileSnapshot(savedProfile),
                "Website business profile created.",
                null
        );

        return getBusinessProfile(
                savedProfile.getBusinessProfileId()
        );
    }

    @Override
    @Transactional
    public WebsiteBusinessProfile updateBusinessProfile(
            UUID businessProfileId,
            WebsiteBusinessProfile requestedUpdate,
            UUID administratorId
    ) {
        requireIdentifier(
                businessProfileId,
                "Business profile ID"
        );

        if (requestedUpdate == null) {
            throw badRequest(
                    "Updated business profile information is required."
            );
        }

        AdminUser administrator =
                getRequiredAdministrator(administratorId);

        WebsiteBusinessProfile existingProfile =
                getProfileForUpdate(businessProfileId);

        JsonNode beforeSnapshot =
                createProfileSnapshot(existingProfile);

        validateRequiredFields(requestedUpdate);

        existingProfile.setBusinessName(
                requestedUpdate.getBusinessName()
        );

        existingProfile.setLegalBusinessName(
                requestedUpdate.getLegalBusinessName()
        );

        existingProfile.setTagline(
                requestedUpdate.getTagline()
        );

        existingProfile.setSecondaryTagline(
                requestedUpdate.getSecondaryTagline()
        );

        existingProfile.setShortDescription(
                requestedUpdate.getShortDescription()
        );

        existingProfile.setFullDescription(
                requestedUpdate.getFullDescription()
        );

        existingProfile.setPublicEmail(
                requestedUpdate.getPublicEmail()
        );

        existingProfile.setPublicPhone(
                requestedUpdate.getPublicPhone()
        );

        existingProfile.setStreetAddress(
                requestedUpdate.getStreetAddress()
        );

        existingProfile.setAddressLine2(
                requestedUpdate.getAddressLine2()
        );

        existingProfile.setCity(
                requestedUpdate.getCity()
        );

        existingProfile.setStateRegion(
                requestedUpdate.getStateRegion()
        );

        existingProfile.setPostalCode(
                requestedUpdate.getPostalCode()
        );

        existingProfile.setCountryCode(
                requestedUpdate.getCountryCode()
        );

        existingProfile.setServiceArea(
                requestedUpdate.getServiceArea()
        );

        existingProfile.setAppointmentOnly(
                requestedUpdate.getAppointmentOnly()
        );

        existingProfile.setDefaultLocale(
                requestedUpdate.getDefaultLocale()
        );

        existingProfile.setDefaultTimezone(
                requestedUpdate.getDefaultTimezone()
        );

        existingProfile.setPrimaryDomain(
                requestedUpdate.getPrimaryDomain()
        );

        existingProfile.setPrimaryLogoMedia(
                validatePublicMedia(
                        requestedUpdate.getPrimaryLogoMedia(),
                        "Primary logo"
                )
        );

        existingProfile.setLightLogoMedia(
                validatePublicMedia(
                        requestedUpdate.getLightLogoMedia(),
                        "Light logo"
                )
        );

        existingProfile.setDarkLogoMedia(
                validatePublicMedia(
                        requestedUpdate.getDarkLogoMedia(),
                        "Dark logo"
                )
        );

        existingProfile.setFaviconMedia(
                validatePublicMedia(
                        requestedUpdate.getFaviconMedia(),
                        "Favicon"
                )
        );

        existingProfile.setDefaultSocialImageMedia(
                validatePublicMedia(
                        requestedUpdate.getDefaultSocialImageMedia(),
                        "Default social image"
                )
        );

        if (requestedUpdate.getIsActive() != null) {
            if (
                    requestedUpdate.getIsActive()
                            && websiteBusinessProfileRepository
                            .existsByIsActiveTrueAndBusinessProfileIdNot(
                                    businessProfileId
                            )
            ) {
                throw conflict(
                        "Another active website business profile already exists."
                );
            }

            existingProfile.setIsActive(
                    requestedUpdate.getIsActive()
            );
        }

        existingProfile.setUpdatedByAdminUser(
                administrator
        );

        WebsiteBusinessProfile savedProfile =
                saveProfile(existingProfile);

        synchronizeMediaUsages(savedProfile);

        websiteContentAuditLogService.recordAudit(
                administratorId,
                WebsiteContentAuditAction.UPDATE,
                WebsiteContentAuditResourceType.BUSINESS_PROFILE,
                savedProfile.getBusinessProfileId(),
                savedProfile.getBusinessName(),
                beforeSnapshot,
                createProfileSnapshot(savedProfile),
                "Website business profile updated.",
                null
        );

        return getBusinessProfile(
                savedProfile.getBusinessProfileId()
        );
    }

    @Override
    public WebsiteBusinessProfile getBusinessProfile(
            UUID businessProfileId
    ) {
        requireIdentifier(
                businessProfileId,
                "Business profile ID"
        );

        return websiteBusinessProfileRepository
                .findByBusinessProfileId(
                        businessProfileId
                )
                .orElseThrow(() -> notFound(
                        "Website business profile was not found."
                ));
    }

    @Override
    public WebsiteBusinessProfile getActiveBusinessProfile() {
        return websiteBusinessProfileRepository
                .findByIsActiveTrue()
                .orElseThrow(() -> notFound(
                        "An active website business profile was not found."
                ));
    }

    @Override
    @Transactional
    public WebsiteBusinessProfile activateBusinessProfile(
            UUID businessProfileId,
            UUID administratorId
    ) {
        AdminUser administrator =
                getRequiredAdministrator(administratorId);

        WebsiteBusinessProfile profile =
                getProfileForUpdate(businessProfileId);

        if (Boolean.TRUE.equals(profile.getIsActive())) {
            return getBusinessProfile(businessProfileId);
        }

        if (
                websiteBusinessProfileRepository
                        .existsByIsActiveTrueAndBusinessProfileIdNot(
                                businessProfileId
                        )
        ) {
            throw conflict(
                    "Another active website business profile already exists."
            );
        }

        JsonNode beforeSnapshot =
                createProfileSnapshot(profile);

        profile.activate(administrator);

        WebsiteBusinessProfile savedProfile =
                saveProfile(profile);

        websiteContentAuditLogService.recordAudit(
                administratorId,
                WebsiteContentAuditAction.UPDATE,
                WebsiteContentAuditResourceType.BUSINESS_PROFILE,
                savedProfile.getBusinessProfileId(),
                savedProfile.getBusinessName(),
                beforeSnapshot,
                createProfileSnapshot(savedProfile),
                "Website business profile activated.",
                null
        );

        return getBusinessProfile(businessProfileId);
    }

    @Override
    @Transactional
    public WebsiteBusinessProfile deactivateBusinessProfile(
            UUID businessProfileId,
            UUID administratorId
    ) {
        AdminUser administrator =
                getRequiredAdministrator(administratorId);

        WebsiteBusinessProfile profile =
                getProfileForUpdate(businessProfileId);

        if (!Boolean.TRUE.equals(profile.getIsActive())) {
            return getBusinessProfile(businessProfileId);
        }

        JsonNode beforeSnapshot =
                createProfileSnapshot(profile);

        profile.deactivate(administrator);

        WebsiteBusinessProfile savedProfile =
                saveProfile(profile);

        websiteContentAuditLogService.recordAudit(
                administratorId,
                WebsiteContentAuditAction.UPDATE,
                WebsiteContentAuditResourceType.BUSINESS_PROFILE,
                savedProfile.getBusinessProfileId(),
                savedProfile.getBusinessName(),
                beforeSnapshot,
                createProfileSnapshot(savedProfile),
                "Website business profile deactivated.",
                null
        );

        return getBusinessProfile(businessProfileId);
    }

    private JsonNode createProfileSnapshot(
            WebsiteBusinessProfile profile
    ) {
        Map<String, Object> fields =
                new LinkedHashMap<>();

        fields.put(
                "businessProfileId",
                profile.getBusinessProfileId()
        );

        fields.put(
                "businessName",
                profile.getBusinessName()
        );

        fields.put(
                "legalBusinessName",
                profile.getLegalBusinessName()
        );

        fields.put(
                "tagline",
                profile.getTagline()
        );

        fields.put(
                "secondaryTagline",
                profile.getSecondaryTagline()
        );

        fields.put(
                "city",
                profile.getCity()
        );

        fields.put(
                "stateRegion",
                profile.getStateRegion()
        );

        fields.put(
                "countryCode",
                profile.getCountryCode()
        );

        fields.put(
                "serviceArea",
                profile.getServiceArea()
        );

        fields.put(
                "appointmentOnly",
                profile.getAppointmentOnly()
        );

        fields.put(
                "defaultLocale",
                profile.getDefaultLocale()
        );

        fields.put(
                "defaultTimezone",
                profile.getDefaultTimezone()
        );

        fields.put(
                "primaryDomain",
                profile.getPrimaryDomain()
        );

        fields.put(
                "primaryLogoMediaId",
                getMediaAssetId(
                        profile.getPrimaryLogoMedia()
                )
        );

        fields.put(
                "lightLogoMediaId",
                getMediaAssetId(
                        profile.getLightLogoMedia()
                )
        );

        fields.put(
                "darkLogoMediaId",
                getMediaAssetId(
                        profile.getDarkLogoMedia()
                )
        );

        fields.put(
                "faviconMediaId",
                getMediaAssetId(
                        profile.getFaviconMedia()
                )
        );

        fields.put(
                "defaultSocialImageMediaId",
                getMediaAssetId(
                        profile.getDefaultSocialImageMedia()
                )
        );

        fields.put(
                "isActive",
                profile.getIsActive()
        );

        return websiteContentAuditSnapshotService
                .createSnapshot(fields);
    }

    private void synchronizeMediaUsages(
            WebsiteBusinessProfile profile
    ) {
        UUID resourceId =
                profile.getBusinessProfileId();

        websiteMediaUsageService.replaceUsage(
                getMediaAssetId(
                        profile.getPrimaryLogoMedia()
                ),
                WebsiteMediaUsageResourceType.BUSINESS_PROFILE,
                resourceId,
                PRIMARY_LOGO_USAGE,
                "Primary Romelt TechCare website logo."
        );

        websiteMediaUsageService.replaceUsage(
                getMediaAssetId(
                        profile.getLightLogoMedia()
                ),
                WebsiteMediaUsageResourceType.BUSINESS_PROFILE,
                resourceId,
                LIGHT_LOGO_USAGE,
                "Logo variant used on dark website backgrounds."
        );

        websiteMediaUsageService.replaceUsage(
                getMediaAssetId(
                        profile.getDarkLogoMedia()
                ),
                WebsiteMediaUsageResourceType.BUSINESS_PROFILE,
                resourceId,
                DARK_LOGO_USAGE,
                "Logo variant used on light website backgrounds."
        );

        websiteMediaUsageService.replaceUsage(
                getMediaAssetId(
                        profile.getFaviconMedia()
                ),
                WebsiteMediaUsageResourceType.BUSINESS_PROFILE,
                resourceId,
                FAVICON_USAGE,
                "Browser favicon for the public website."
        );

        websiteMediaUsageService.replaceUsage(
                getMediaAssetId(
                        profile.getDefaultSocialImageMedia()
                ),
                WebsiteMediaUsageResourceType.BUSINESS_PROFILE,
                resourceId,
                DEFAULT_SOCIAL_IMAGE_USAGE,
                "Default social-sharing and search-preview image."
        );
    }

    private WebsiteMediaAsset validatePublicMedia(
            WebsiteMediaAsset requestedMedia,
            String fieldName
    ) {
        if (
                requestedMedia == null
                        || requestedMedia.getMediaAssetId() == null
        ) {
            return null;
        }

        WebsiteMediaAsset mediaAsset =
                websiteMediaAssetRepository
                        .findByMediaAssetIdAndDeletedAtIsNull(
                                requestedMedia.getMediaAssetId()
                        )
                        .orElseThrow(() -> notFound(
                                fieldName
                                        + " media asset was not found."
                        ));

        if (
                mediaAsset.getAssetStatus()
                        != WebsiteMediaAssetStatus.ACTIVE
        ) {
            throw conflict(
                    fieldName
                            + " must reference an active media asset."
            );
        }

        if (!Boolean.TRUE.equals(mediaAsset.getIsPublic())) {
            throw conflict(
                    fieldName
                            + " must reference a public media asset."
            );
        }

        if (!mediaAsset.isPubliclyAvailable()) {
            throw conflict(
                    fieldName
                            + " is not publicly available."
            );
        }

        return mediaAsset;
    }

    private WebsiteBusinessProfile getProfileForUpdate(
            UUID businessProfileId
    ) {
        requireIdentifier(
                businessProfileId,
                "Business profile ID"
        );

        return websiteBusinessProfileRepository
                .findByIdForUpdate(businessProfileId)
                .orElseThrow(() -> notFound(
                        "Website business profile was not found."
                ));
    }

    private AdminUser getRequiredAdministrator(
            UUID administratorId
    ) {
        requireIdentifier(
                administratorId,
                "Administrator ID"
        );

        return adminUserRepository
                .findById(administratorId)
                .orElseThrow(() -> notFound(
                        "Administrator account was not found."
                ));
    }

    private void validateRequiredFields(
            WebsiteBusinessProfile profile
    ) {
        if (isBlank(profile.getBusinessName())) {
            throw badRequest(
                    "Business name is required."
            );
        }

        if (
                isBlank(profile.getCountryCode())
                        || profile.getCountryCode()
                        .trim()
                        .length() != 2
        ) {
            throw badRequest(
                    "Country code must contain exactly two characters."
            );
        }

        if (isBlank(profile.getDefaultLocale())) {
            throw badRequest(
                    "Default locale is required."
            );
        }

        if (isBlank(profile.getDefaultTimezone())) {
            throw badRequest(
                    "Default timezone is required."
            );
        }
    }

    private WebsiteBusinessProfile saveProfile(
            WebsiteBusinessProfile profile
    ) {
        try {
            return websiteBusinessProfileRepository
                    .saveAndFlush(profile);
        } catch (DataIntegrityViolationException exception) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Unable to save the website business profile. "
                            + "Only one active profile may exist.",
                    exception
            );
        }
    }

    private UUID getMediaAssetId(
            WebsiteMediaAsset mediaAsset
    ) {
        return mediaAsset == null
                ? null
                : mediaAsset.getMediaAssetId();
    }

    private void requireIdentifier(
            UUID identifier,
            String fieldName
    ) {
        if (identifier == null) {
            throw badRequest(
                    fieldName + " is required."
            );
        }
    }

    private boolean isBlank(
            String value
    ) {
        return value == null
                || value.trim().isEmpty();
    }

    private ResponseStatusException badRequest(
            String message
    ) {
        return new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                message
        );
    }

    private ResponseStatusException notFound(
            String message
    ) {
        return new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                message
        );
    }

    private ResponseStatusException conflict(
            String message
    ) {
        return new ResponseStatusException(
                HttpStatus.CONFLICT,
                message
        );
    }
}