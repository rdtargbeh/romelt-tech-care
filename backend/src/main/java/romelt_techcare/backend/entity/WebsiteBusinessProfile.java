package romelt_techcare.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

/**
 * ================================================================
 * ROMELT TECHCARE — WEBSITE BUSINESS PROFILE ENTITY
 * ================================================================
 *
 * Purpose:
 * Stores the shared public identity, contact information, branding,
 * location, service-area, locale, and timezone settings used across
 * the Romelt TechCare public website.
 *
 * Responsibilities:
 * - Stores the public and legal business names.
 * - Stores the primary and secondary taglines.
 * - Stores public contact and address information.
 * - Stores the service-area description.
 * - Stores appointment, locale, timezone, and domain settings.
 * - References approved website media assets for logos, favicon, and
 *   social-sharing imagery.
 * - Records the administrators who created and last updated the
 *   business profile.
 * - Supports optimistic locking for concurrent administrator updates.
 *
 * Business rule:
 * Only one active business profile may exist. PostgreSQL enforces this
 * using the partial unique index uk_website_single_active_profile.
 * The service layer also validates the rule before persistence.
 *
 * Publishing model:
 * Business profile changes become public immediately after a successful
 * administrator update. Changes must therefore be validated, attributed,
 * and later recorded by the website content audit-log module.
 * ================================================================
 */
@Entity
@Table(name = "website_business_profiles")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebsiteBusinessProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(
            name = "business_profile_id",
            nullable = false,
            updatable = false
    )
    private UUID businessProfileId;

    @Column(
            name = "business_name",
            nullable = false,
            length = 180
    )
    private String businessName;

    @Column(
            name = "legal_business_name",
            length = 220
    )
    private String legalBusinessName;

    @Column(
            name = "tagline",
            length = 255
    )
    private String tagline;

    @Column(
            name = "secondary_tagline",
            length = 255
    )
    private String secondaryTagline;

    @Column(
            name = "short_description",
            length = 500
    )
    private String shortDescription;

    @Column(
            name = "full_description",
            columnDefinition = "TEXT"
    )
    private String fullDescription;

    @Column(
            name = "public_email",
            length = 254
    )
    private String publicEmail;

    @Column(
            name = "public_phone",
            length = 40
    )
    private String publicPhone;

    @Column(
            name = "street_address",
            length = 180
    )
    private String streetAddress;

    @Column(
            name = "address_line_2",
            length = 180
    )
    private String addressLine2;

    @Column(
            name = "city",
            length = 100
    )
    private String city;

    @Column(
            name = "state_region",
            length = 100
    )
    private String stateRegion;

    @Column(
            name = "postal_code",
            length = 30
    )
    private String postalCode;

    @Builder.Default
    @Column(
            name = "country_code",
            nullable = false,
            length = 2
    )
    private String countryCode = "US";

    @Column(
            name = "service_area",
            length = 500
    )
    private String serviceArea;

    @Builder.Default
    @Column(
            name = "appointment_only",
            nullable = false
    )
    private Boolean appointmentOnly = true;

    @Builder.Default
    @Column(
            name = "default_locale",
            nullable = false,
            length = 20
    )
    private String defaultLocale = "en-US";

    @Builder.Default
    @Column(
            name = "default_timezone",
            nullable = false,
            length = 80
    )
    private String defaultTimezone = "America/Chicago";

    @Column(
            name = "primary_domain",
            length = 255
    )
    private String primaryDomain;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "primary_logo_media_id")
    private WebsiteMediaAsset primaryLogoMedia;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "light_logo_media_id")
    private WebsiteMediaAsset lightLogoMedia;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dark_logo_media_id")
    private WebsiteMediaAsset darkLogoMedia;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "favicon_media_id")
    private WebsiteMediaAsset faviconMedia;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "default_social_image_media_id")
    private WebsiteMediaAsset defaultSocialImageMedia;

    @Builder.Default
    @Column(
            name = "is_active",
            nullable = false
    )
    private Boolean isActive = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_admin_user_id")
    private AdminUser createdByAdminUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by_admin_user_id")
    private AdminUser updatedByAdminUser;

    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private Instant createdAt;

    @Column(
            name = "updated_at",
            nullable = false
    )
    private Instant updatedAt;

    @Version
    @Column(
            name = "row_version",
            nullable = false
    )
    private Long rowVersion;

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();

        initializeDefaults();
        normalizeFields();
        validateState();

        if (createdAt == null) {
            createdAt = now;
        }

        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        initializeDefaults();
        normalizeFields();
        validateState();

        updatedAt = Instant.now();
    }

    public void activate(AdminUser administrator) {
        isActive = true;
        updatedByAdminUser = administrator;
    }

    public void deactivate(AdminUser administrator) {
        isActive = false;
        updatedByAdminUser = administrator;
    }

    private void initializeDefaults() {
        if (appointmentOnly == null) {
            appointmentOnly = true;
        }

        if (isActive == null) {
            isActive = true;
        }

        if (countryCode == null || countryCode.isBlank()) {
            countryCode = "US";
        }

        if (defaultLocale == null || defaultLocale.isBlank()) {
            defaultLocale = "en-US";
        }

        if (defaultTimezone == null || defaultTimezone.isBlank()) {
            defaultTimezone = "America/Chicago";
        }
    }

    private void normalizeFields() {
        businessName = normalizeRequired(
                businessName,
                "Business name"
        );

        legalBusinessName = normalizeOptional(legalBusinessName);
        tagline = normalizeOptional(tagline);
        secondaryTagline = normalizeOptional(secondaryTagline);
        shortDescription = normalizeOptional(shortDescription);
        fullDescription = normalizeOptional(fullDescription);

        publicEmail = normalizeEmail(publicEmail);
        publicPhone = normalizeOptional(publicPhone);

        streetAddress = normalizeOptional(streetAddress);
        addressLine2 = normalizeOptional(addressLine2);
        city = normalizeOptional(city);
        stateRegion = normalizeOptional(stateRegion);
        postalCode = normalizeOptional(postalCode);

        countryCode = normalizeCountryCode(countryCode);

        serviceArea = normalizeOptional(serviceArea);

        defaultLocale = normalizeRequired(
                defaultLocale,
                "Default locale"
        );

        defaultTimezone = normalizeRequired(
                defaultTimezone,
                "Default timezone"
        );

        primaryDomain = normalizeDomain(primaryDomain);
    }

    private void validateState() {
        if (countryCode.length() != 2) {
            throw new IllegalStateException(
                    "Country code must contain exactly two characters."
            );
        }
    }

    private String normalizeRequired(
            String value,
            String fieldName
    ) {
        String normalized = normalizeOptional(value);

        if (normalized == null) {
            throw new IllegalStateException(
                    fieldName + " is required."
            );
        }

        return normalized;
    }

    private String normalizeOptional(String value) {
        if (value == null) {
            return null;
        }

        String normalized = value.trim();

        return normalized.isEmpty()
                ? null
                : normalized;
    }

    private String normalizeEmail(String value) {
        String normalized = normalizeOptional(value);

        return normalized == null
                ? null
                : normalized.toLowerCase(Locale.ROOT);
    }

    private String normalizeCountryCode(String value) {
        return normalizeRequired(
                value,
                "Country code"
        ).toUpperCase(Locale.ROOT);
    }

    private String normalizeDomain(String value) {
        String normalized = normalizeOptional(value);

        if (normalized == null) {
            return null;
        }

        normalized = normalized.toLowerCase(Locale.ROOT);

        if (normalized.startsWith("https://")) {
            normalized = normalized.substring(8);
        } else if (normalized.startsWith("http://")) {
            normalized = normalized.substring(7);
        }

        while (normalized.endsWith("/")) {
            normalized = normalized.substring(
                    0,
                    normalized.length() - 1
            );
        }

        return normalized.isBlank()
                ? null
                : normalized;
    }
}