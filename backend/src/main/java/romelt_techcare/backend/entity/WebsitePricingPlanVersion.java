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
import romelt_techcare.backend.enums.WebsiteBillingInterval;
import romelt_techcare.backend.enums.WebsitePricingModel;
import romelt_techcare.backend.enums.WebsitePricingPlanVersionStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

/**
 * ================================================================
 * ROMELT TECHCARE — WEBSITE PRICING PLAN VERSION ENTITY
 * ================================================================
 *
 * Purpose:
 * Stores one editable, published, or archived content version for a
 * stable WebsitePricingPlan identity.
 *
 * Responsibilities:
 * - Stores public plan names and descriptions.
 * - Stores the pricing model, amount, currency, and billing interval.
 * - Stores optional price prefix and suffix text.
 * - Stores call-to-action content.
 * - Controls display order, recommendation, featured placement, and
 *   public visibility.
 * - Supports effective publication windows.
 * - Supports draft, published, and archived lifecycle states.
 * - Records administrator attribution and lifecycle timestamps.
 * - Supports optimistic locking.
 *
 * Lifecycle:
 * - Only DRAFT versions may be edited or published.
 * - PUBLISHED versions represent current public content.
 * - ARCHIVED versions preserve historical content.
 *
 * Public availability:
 * A version is publicly available only when:
 * - it has PUBLISHED status;
 * - its stable pricing plan is active and not deleted;
 * - it is the stable plan's published version;
 * - isPublic is true;
 * - the current time is within its optional effective window.
 * ================================================================
 */
@Entity
@Table(
        name = "website_pricing_plan_versions",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_website_pricing_version_number",
                        columnNames = {
                                "pricing_plan_id",
                                "version_number"
                        }
                )
        },
        indexes = {
                @Index(
                        name = "idx_website_pricing_versions_public",
                        columnList =
                                "version_status, is_public, display_order"
                )
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebsitePricingPlanVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(
            name = "pricing_plan_version_id",
            nullable = false,
            updatable = false
    )
    private UUID pricingPlanVersionId;

    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "pricing_plan_id",
            nullable = false
    )
    private WebsitePricingPlan pricingPlan;

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
    private WebsitePricingPlanVersionStatus versionStatus =
            WebsitePricingPlanVersionStatus.DRAFT;

    @Column(
            name = "plan_name",
            nullable = false,
            length = 180
    )
    private String planName;

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

    @Enumerated(EnumType.STRING)
    @Column(
            name = "pricing_model",
            nullable = false,
            length = 30
    )
    private WebsitePricingModel pricingModel;

    @Column(
            name = "amount",
            precision = 12,
            scale = 2
    )
    private BigDecimal amount;

    @Builder.Default
    @Column(
            name = "currency_code",
            nullable = false,
            length = 3
    )
    private String currencyCode = "USD";

    @Column(
            name = "price_prefix",
            length = 100
    )
    private String pricePrefix;

    @Column(
            name = "price_suffix",
            length = 100
    )
    private String priceSuffix;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "billing_interval",
            length = 30
    )
    private WebsiteBillingInterval billingInterval;

    @Column(
            name = "call_to_action_label",
            length = 180
    )
    private String callToActionLabel;

    @Column(
            name = "call_to_action_url",
            length = 1000
    )
    private String callToActionUrl;

    @Builder.Default
    @Column(
            name = "display_order",
            nullable = false
    )
    private Integer displayOrder = 0;

    @Builder.Default
    @Column(
            name = "is_recommended",
            nullable = false
    )
    private Boolean isRecommended = false;

    @Builder.Default
    @Column(
            name = "is_featured",
            nullable = false
    )
    private Boolean isFeatured = false;

    @Builder.Default
    @Column(
            name = "is_public",
            nullable = false
    )
    private Boolean isPublic = true;

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

    public void updateDraft(
            String newPlanName,
            String newShortDescription,
            String newFullDescription,
            WebsitePricingModel newPricingModel,
            BigDecimal newAmount,
            String newCurrencyCode,
            String newPricePrefix,
            String newPriceSuffix,
            WebsiteBillingInterval newBillingInterval,
            String newCallToActionLabel,
            String newCallToActionUrl,
            Integer newDisplayOrder,
            Boolean recommended,
            Boolean featured,
            Boolean publicPlan,
            Instant newEffectiveFrom,
            Instant newEffectiveUntil,
            String newChangeSummary,
            AdminUser administrator
    ) {
        requireDraft();

        planName = newPlanName;
        shortDescription = newShortDescription;
        fullDescription = newFullDescription;

        pricingModel = newPricingModel;
        amount = newAmount;
        currencyCode = newCurrencyCode;

        pricePrefix = newPricePrefix;
        priceSuffix = newPriceSuffix;
        billingInterval = newBillingInterval;

        callToActionLabel = newCallToActionLabel;
        callToActionUrl = newCallToActionUrl;

        displayOrder = newDisplayOrder;
        isRecommended = recommended;
        isFeatured = featured;
        isPublic = publicPlan;

        effectiveFrom = newEffectiveFrom;
        effectiveUntil = newEffectiveUntil;

        changeSummary = newChangeSummary;
        updatedByAdminUser = administrator;
    }

    public void publish(
            AdminUser administrator
    ) {
        requireDraft();

        versionStatus =
                WebsitePricingPlanVersionStatus.PUBLISHED;

        publishedAt = Instant.now();
        publishedByAdminUser = administrator;

        archivedAt = null;
        archivedByAdminUser = null;

        updatedByAdminUser = administrator;
    }

    public void archive(
            AdminUser administrator
    ) {
        if (isArchived()) {
            return;
        }

        versionStatus =
                WebsitePricingPlanVersionStatus.ARCHIVED;

        archivedAt = Instant.now();
        archivedByAdminUser = administrator;
        updatedByAdminUser = administrator;
    }

    public boolean isDraft() {
        return versionStatus
                == WebsitePricingPlanVersionStatus.DRAFT;
    }

    public boolean isPublished() {
        return versionStatus
                == WebsitePricingPlanVersionStatus.PUBLISHED;
    }

    public boolean isArchived() {
        return versionStatus
                == WebsitePricingPlanVersionStatus.ARCHIVED;
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

        boolean started =
                effectiveFrom == null
                        || !effectiveCheckTime.isBefore(
                        effectiveFrom
                );

        boolean notEnded =
                effectiveUntil == null
                        || effectiveCheckTime.isBefore(
                        effectiveUntil
                );

        return started && notEnded;
    }

    public boolean isPubliclyAvailable() {
        return isPublished()
                && publishedAt != null
                && Boolean.TRUE.equals(isPublic)
                && isCurrentlyEffective()
                && pricingPlan != null
                && pricingPlan.isPubliclyAvailable()
                && pricingPlanVersionId != null
                && pricingPlanVersionId.equals(
                pricingPlan.getPublishedVersionId()
        );
    }

    public String resolveDisplayPrice() {
        if (
                pricingModel
                        == WebsitePricingModel.CONTACT_FOR_PRICE
        ) {
            return "Contact for price";
        }

        if (
                pricingModel == WebsitePricingModel.CUSTOM
                        && amount == null
        ) {
            return "Custom pricing";
        }

        if (amount == null) {
            return null;
        }

        StringBuilder display = new StringBuilder();

        if (pricePrefix != null) {
            display.append(pricePrefix).append(" ");
        }

        display.append(amount);

        if (priceSuffix != null) {
            display.append(" ").append(priceSuffix);
        }

        return display.toString().trim();
    }

    private void requireDraft() {
        if (!isDraft()) {
            throw new IllegalStateException(
                    "Only a draft pricing-plan version may be edited or published."
            );
        }
    }

    private void initializeDefaults() {
        if (versionStatus == null) {
            versionStatus =
                    WebsitePricingPlanVersionStatus.DRAFT;
        }

        if (currencyCode == null || currencyCode.isBlank()) {
            currencyCode = "USD";
        }

        if (displayOrder == null) {
            displayOrder = 0;
        }

        if (isRecommended == null) {
            isRecommended = false;
        }

        if (isFeatured == null) {
            isFeatured = false;
        }

        if (isPublic == null) {
            isPublic = true;
        }
    }

    private void normalizeFields() {
        planName = normalizeRequired(
                planName,
                "Plan name"
        );

        shortDescription =
                normalizeOptional(shortDescription);

        fullDescription =
                normalizeOptional(fullDescription);

        currencyCode =
                normalizeCurrencyCode(currencyCode);

        pricePrefix = normalizeOptional(pricePrefix);
        priceSuffix = normalizeOptional(priceSuffix);

        callToActionLabel =
                normalizeOptional(callToActionLabel);

        callToActionUrl =
                normalizeOptional(callToActionUrl);

        changeSummary =
                normalizeOptional(changeSummary);
    }

    private void validateState() {
        if (pricingPlan == null) {
            throw new IllegalStateException(
                    "Pricing plan is required."
            );
        }

        if (versionNumber == null || versionNumber <= 0) {
            throw new IllegalStateException(
                    "Version number must be greater than zero."
            );
        }

        if (pricingModel == null) {
            throw new IllegalStateException(
                    "Pricing model is required."
            );
        }

        if (
                amount != null
                        && amount.compareTo(BigDecimal.ZERO) < 0
        ) {
            throw new IllegalStateException(
                    "Pricing amount must not be negative."
            );
        }

        if (displayOrder == null || displayOrder < 0) {
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
                    "A published pricing-plan version requires a publication timestamp."
            );
        }

        if (
                isArchived()
                        && archivedAt == null
        ) {
            throw new IllegalStateException(
                    "An archived pricing-plan version requires an archive timestamp."
            );
        }

        validatePricingModelRules();
    }

    private void validatePricingModelRules() {
        if (
                pricingModel
                        == WebsitePricingModel.CONTACT_FOR_PRICE
                        && amount != null
        ) {
            throw new IllegalStateException(
                    "CONTACT_FOR_PRICE pricing must not contain a numeric amount."
            );
        }

        if (
                requiresAmount(pricingModel)
                        && amount == null
        ) {
            throw new IllegalStateException(
                    "The selected pricing model requires an amount."
            );
        }

        if (
                pricingModel
                        == WebsitePricingModel.ONE_TIME
                        && billingInterval != null
                        && billingInterval
                        != WebsiteBillingInterval.ONE_TIME
        ) {
            throw new IllegalStateException(
                    "ONE_TIME pricing must use the ONE_TIME billing interval."
            );
        }
    }

    private boolean requiresAmount(
            WebsitePricingModel model
    ) {
        return switch (model) {
            case ONE_TIME,
                 HOURLY,
                 DAILY,
                 WEEKLY,
                 MONTHLY,
                 QUARTERLY,
                 ANNUAL -> true;

            case CUSTOM,
                 CONTACT_FOR_PRICE -> false;
        };
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