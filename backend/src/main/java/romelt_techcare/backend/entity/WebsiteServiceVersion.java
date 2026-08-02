package romelt_techcare.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import romelt_techcare.backend.enums.WebsiteServiceVersionStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

/**
 * ================================================================
 * ROMELT TECHCARE — WEBSITE SERVICE VERSION ENTITY
 * ================================================================
 *
 * Purpose:
 * Stores one editable, published, or archived content version for a
 * stable WebsiteService record.
 *
 * Responsibilities:
 * - Stores customer-facing service names and descriptions.
 * - Stores card and hero media assets.
 * - Stores preliminary pricing and currency information.
 * - Controls ordering, featured placement, booking, and public access.
 * - Stores SEO metadata.
 * - Supports optional effective publication windows.
 * - Supports draft, published, and archived lifecycle states.
 * - Tracks administrator attribution and lifecycle timestamps.
 * - Supports optimistic locking.
 *
 * Lifecycle rules:
 * - Only DRAFT versions may be edited.
 * - Publishing a draft archives the previously published version.
 * - PUBLISHED and ARCHIVED versions are immutable content history.
 *
 * Public availability:
 * A version is publicly available only when:
 * - it has PUBLISHED status;
 * - its stable service is active and not deleted;
 * - isPublic is true;
 * - the current time is within the optional effective window.
 * ================================================================
 */
@Entity
@Table(
        name = "website_service_versions",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_website_service_version_number",
                        columnNames = {
                                "service_id",
                                "version_number"
                        }
                )
        },
        indexes = {
                @Index(
                        name = "idx_website_service_versions_public",
                        columnList =
                                "version_status, is_public, display_order"
                ),
                @Index(
                        name = "idx_website_service_versions_bookable",
                        columnList =
                                "version_status, is_bookable, display_order"
                )
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebsiteServiceVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(
            name = "service_version_id",
            nullable = false,
            updatable = false
    )
    private UUID serviceVersionId;

    /**
     * Stable service identity that owns this version.
     */
    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "service_id",
            nullable = false
    )
    private WebsiteService websiteService;

    /**
     * Sequential version number within one stable service.
     */
    @Column(
            name = "version_number",
            nullable = false,
            updatable = false
    )
    private Integer versionNumber;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(
            name = "version_status",
            nullable = false,
            length = 30
    )
    private WebsiteServiceVersionStatus versionStatus =
            WebsiteServiceVersionStatus.DRAFT;

    @Column(
            name = "service_name",
            nullable = false,
            length = 180
    )
    private String serviceName;

    @Column(
            name = "short_description",
            length = 500
    )
    private String shortDescription;

    @Column(
            name = "full_description",
            columnDefinition = "text"
    )
    private String fullDescription;

    /**
     * Frontend-recognized icon key.
     *
     * Examples:
     * laptop
     * wifi
     * printer
     * shield-check
     */
    @Column(
            name = "icon_key",
            length = 100
    )
    private String iconKey;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "card_image_media_id")
    private WebsiteMediaAsset cardImageMedia;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hero_image_media_id")
    private WebsiteMediaAsset heroImageMedia;

    @Column(
            name = "starting_price",
            precision = 12,
            scale = 2
    )
    private BigDecimal startingPrice;

    @Builder.Default
    @Column(
            name = "currency_code",
            nullable = false,
            length = 3
    )
    private String currencyCode = "USD";

    @Column(
            name = "price_unit_label",
            length = 100
    )
    private String priceUnitLabel;

    @Builder.Default
    @Column(
            name = "display_order",
            nullable = false
    )
    private Integer displayOrder = 0;

    @Builder.Default
    @Column(
            name = "is_featured",
            nullable = false
    )
    private Boolean isFeatured = false;

    @Builder.Default
    @Column(
            name = "is_bookable",
            nullable = false
    )
    private Boolean isBookable = true;

    @Builder.Default
    @Column(
            name = "is_public",
            nullable = false
    )
    private Boolean isPublic = true;

    @Column(
            name = "seo_title",
            length = 255
    )
    private String seoTitle;

    @Column(
            name = "seo_description",
            length = 500
    )
    private String seoDescription;

    @Column(name = "effective_from")
    private Instant effectiveFrom;

    @Column(name = "effective_until")
    private Instant effectiveUntil;

    @Column(
            name = "change_summary",
            length = 1000
    )
    private String changeSummary;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "archived_at")
    private Instant archivedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_admin_user_id")
    private AdminUser createdByAdminUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by_admin_user_id")
    private AdminUser updatedByAdminUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "published_by_admin_user_id")
    private AdminUser publishedByAdminUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "archived_by_admin_user_id")
    private AdminUser archivedByAdminUser;

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

    /**
     * Updates the complete editable state of a draft version.
     */
    public void updateDraft(
            String newServiceName,
            String newShortDescription,
            String newFullDescription,
            String newIconKey,
            WebsiteMediaAsset newCardImageMedia,
            WebsiteMediaAsset newHeroImageMedia,
            BigDecimal newStartingPrice,
            String newCurrencyCode,
            String newPriceUnitLabel,
            Integer newDisplayOrder,
            Boolean featured,
            Boolean bookable,
            Boolean publicService,
            String newSeoTitle,
            String newSeoDescription,
            Instant newEffectiveFrom,
            Instant newEffectiveUntil,
            String newChangeSummary,
            AdminUser administrator
    ) {
        requireDraft();

        serviceName = newServiceName;
        shortDescription = newShortDescription;
        fullDescription = newFullDescription;
        iconKey = newIconKey;

        cardImageMedia = newCardImageMedia;
        heroImageMedia = newHeroImageMedia;

        startingPrice = newStartingPrice;
        currencyCode = newCurrencyCode;
        priceUnitLabel = newPriceUnitLabel;

        displayOrder = newDisplayOrder;
        isFeatured = featured;
        isBookable = bookable;
        isPublic = publicService;

        seoTitle = newSeoTitle;
        seoDescription = newSeoDescription;

        effectiveFrom = newEffectiveFrom;
        effectiveUntil = newEffectiveUntil;

        changeSummary = newChangeSummary;
        updatedByAdminUser = administrator;
    }

    /**
     * Publishes this draft version.
     */
    public void publish(
            AdminUser administrator
    ) {
        requireDraft();

        versionStatus = WebsiteServiceVersionStatus.PUBLISHED;
        publishedAt = Instant.now();
        publishedByAdminUser = administrator;

        archivedAt = null;
        archivedByAdminUser = null;

        updatedByAdminUser = administrator;
    }

    /**
     * Archives this draft or published version.
     */
    public void archive(
            AdminUser administrator
    ) {
        if (isArchived()) {
            return;
        }

        versionStatus = WebsiteServiceVersionStatus.ARCHIVED;
        archivedAt = Instant.now();
        archivedByAdminUser = administrator;
        updatedByAdminUser = administrator;
    }

    public boolean isDraft() {
        return versionStatus
                == WebsiteServiceVersionStatus.DRAFT;
    }

    public boolean isPublished() {
        return versionStatus
                == WebsiteServiceVersionStatus.PUBLISHED;
    }

    public boolean isArchived() {
        return versionStatus
                == WebsiteServiceVersionStatus.ARCHIVED;
    }

    public boolean isCurrentlyEffective() {
        return isCurrentlyEffective(Instant.now());
    }

    public boolean isCurrentlyEffective(
            Instant currentTime
    ) {
        Instant effectiveCheckTime =
                currentTime == null
                        ? Instant.now()
                        : currentTime;

        boolean hasStarted =
                effectiveFrom == null
                        || !effectiveCheckTime.isBefore(effectiveFrom);

        boolean hasNotEnded =
                effectiveUntil == null
                        || effectiveCheckTime.isBefore(effectiveUntil);

        return hasStarted && hasNotEnded;
    }

    public boolean isPubliclyAvailable() {
        return isPublished()
                && publishedAt != null
                && Boolean.TRUE.equals(isPublic)
                && isCurrentlyEffective()
                && websiteService != null
                && websiteService.isPubliclyAvailable();
    }

    public boolean isPubliclyBookable() {
        return isPubliclyAvailable()
                && Boolean.TRUE.equals(isBookable);
    }

    private void requireDraft() {
        if (!isDraft()) {
            throw new IllegalStateException(
                    "Only a draft service version may be edited or published."
            );
        }
    }

    private void initializeDefaults() {
        if (versionStatus == null) {
            versionStatus = WebsiteServiceVersionStatus.DRAFT;
        }

        if (currencyCode == null || currencyCode.isBlank()) {
            currencyCode = "USD";
        }

        if (displayOrder == null) {
            displayOrder = 0;
        }

        if (isFeatured == null) {
            isFeatured = false;
        }

        if (isBookable == null) {
            isBookable = true;
        }

        if (isPublic == null) {
            isPublic = true;
        }
    }

    private void normalizeFields() {
        serviceName = normalizeRequired(
                serviceName,
                "Service name"
        );

        shortDescription =
                normalizeOptional(shortDescription);

        fullDescription =
                normalizeOptional(fullDescription);

        iconKey = normalizeIconKey(iconKey);

        currencyCode =
                normalizeCurrencyCode(currencyCode);

        priceUnitLabel =
                normalizeOptional(priceUnitLabel);

        seoTitle = normalizeOptional(seoTitle);
        seoDescription = normalizeOptional(seoDescription);
        changeSummary = normalizeOptional(changeSummary);
    }

    private void validateState() {
        if (websiteService == null) {
            throw new IllegalStateException(
                    "Website service is required."
            );
        }

        if (versionNumber == null || versionNumber <= 0) {
            throw new IllegalStateException(
                    "Version number must be greater than zero."
            );
        }

        if (
                startingPrice != null
                        && startingPrice.compareTo(BigDecimal.ZERO) < 0
        ) {
            throw new IllegalStateException(
                    "Starting price must not be negative."
            );
        }

        if (displayOrder < 0) {
            throw new IllegalStateException(
                    "Display order must not be negative."
            );
        }

        if (
                effectiveUntil != null
                        && effectiveFrom != null
                        && !effectiveUntil.isAfter(effectiveFrom)
        ) {
            throw new IllegalStateException(
                    "Effective-until time must be after effective-from time."
            );
        }

        if (
                isPublished()
                        && publishedAt == null
        ) {
            throw new IllegalStateException(
                    "A published service version requires a publication timestamp."
            );
        }

        if (
                isArchived()
                        && archivedAt == null
        ) {
            throw new IllegalStateException(
                    "An archived service version requires an archive timestamp."
            );
        }
    }

    private String normalizeCurrencyCode(
            String value
    ) {
        String normalized = normalizeRequired(
                value,
                "Currency code"
        ).toUpperCase(Locale.ROOT);

        if (!normalized.matches("[A-Z]{3}")) {
            throw new IllegalStateException(
                    "Currency code must contain exactly three letters."
            );
        }

        return normalized;
    }

    private String normalizeIconKey(
            String value
    ) {
        String normalized = normalizeOptional(value);

        if (normalized == null) {
            return null;
        }

        normalized = normalized
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "");

        return normalized.isBlank()
                ? null
                : normalized;
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

    private String normalizeOptional(
            String value
    ) {
        if (value == null) {
            return null;
        }

        String normalized = value.trim();

        return normalized.isEmpty()
                ? null
                : normalized;
    }
}