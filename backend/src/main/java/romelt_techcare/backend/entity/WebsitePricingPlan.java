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
import romelt_techcare.backend.enums.WebsitePricingPlanStatus;

import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

/**
 * ================================================================
 * ROMELT TECHCARE — WEBSITE PRICING PLAN ENTITY
 * ================================================================
 *
 * Purpose:
 * Stores the stable identity of a pricing plan displayed on the
 * Romelt TechCare public website.
 *
 * Responsibilities:
 * - Stores a stable internal plan code.
 * - Stores a unique public URL slug.
 * - Points to the current administrator-editable plan version.
 * - Points to the current publicly published plan version.
 * - Controls the stable plan lifecycle status.
 * - Supports soft deletion and restoration.
 * - Records administrator attribution.
 * - Supports optimistic locking.
 *
 * Versioned content:
 * Customer-facing names, prices, billing intervals, descriptions,
 * features, recommendation status, call-to-action text, SEO data, and
 * display settings belong to WebsitePricingPlanVersion.
 *
 * Version-pointer access:
 * The draft and published version pointers remain internal lifecycle
 * fields. They must be changed only by the pricing-plan version service
 * after the version table is implemented.
 * ================================================================
 */
@Entity
@Table(
        name = "website_pricing_plans",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_website_pricing_code",
                        columnNames = "plan_code"
                ),
                @UniqueConstraint(
                        name = "uk_website_pricing_slug",
                        columnNames = "plan_slug"
                )
        },
        indexes = {
                @Index(
                        name = "idx_website_pricing_status",
                        columnList = "plan_status"
                )
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebsitePricingPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(
            name = "pricing_plan_id",
            nullable = false,
            updatable = false
    )
    private UUID pricingPlanId;

    /**
     * Stable internal plan identifier.
     *
     * Examples:
     * ESSENTIAL_CARE
     * BUSINESS_CARE
     * COMPLETE_CARE
     */
    @Column(
            name = "plan_code",
            nullable = false,
            unique = true,
            length = 100
    )
    private String planCode;

    /**
     * Stable public plan slug.
     *
     * Examples:
     * essential-care
     * business-care
     * complete-care
     */
    @Column(
            name = "plan_slug",
            nullable = false,
            unique = true,
            length = 180
    )
    private String planSlug;

    /**
     * Current administrator-editable pricing-plan version.
     *
     * This pointer is managed internally by the future
     * WebsitePricingPlanVersionService.
     */
    @Column(name = "draft_version_id")
    private UUID draftVersionId;

    /**
     * Current publicly published pricing-plan version.
     *
     * This pointer is managed internally by the future
     * WebsitePricingPlanVersionService.
     */
    @Column(name = "published_version_id")
    private UUID publishedVersionId;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(
            name = "plan_status",
            nullable = false,
            length = 30
    )
    private WebsitePricingPlanStatus planStatus =
            WebsitePricingPlanStatus.ACTIVE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_admin_user_id")
    private AdminUser createdByAdminUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by_admin_user_id")
    private AdminUser updatedByAdminUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "deleted_by_admin_user_id")
    private AdminUser deletedByAdminUser;

    @Column(name = "deleted_at")
    private Instant deletedAt;

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
     * Updates the stable pricing-plan identity.
     */
    public void updateIdentity(
            String newPlanCode,
            String newPlanSlug,
            WebsitePricingPlanStatus newPlanStatus,
            AdminUser administrator
    ) {
        ensureNotDeleted();

        planCode = newPlanCode;
        planSlug = newPlanSlug;
        planStatus = newPlanStatus;
        updatedByAdminUser = administrator;
    }

    /**
     * Internal lifecycle operation used only by the pricing-plan
     * version service.
     */
    public void assignDraftVersion(
            UUID versionId,
            AdminUser administrator
    ) {
        ensureNotDeleted();

        if (versionId == null) {
            throw new IllegalArgumentException(
                    "Draft pricing-plan version ID is required."
            );
        }

        draftVersionId = versionId;
        updatedByAdminUser = administrator;
    }

    /**
     * Internal lifecycle operation used only by the pricing-plan
     * version service.
     */
    public void clearDraftVersion(
            AdminUser administrator
    ) {
        ensureNotDeleted();

        draftVersionId = null;
        updatedByAdminUser = administrator;
    }

    /**
     * Internal lifecycle operation used only by the pricing-plan
     * version service.
     */
    public void assignPublishedVersion(
            UUID versionId,
            AdminUser administrator
    ) {
        ensureNotDeleted();

        if (versionId == null) {
            throw new IllegalArgumentException(
                    "Published pricing-plan version ID is required."
            );
        }

        publishedVersionId = versionId;
        updatedByAdminUser = administrator;
    }

    /**
     * Internal lifecycle operation used only by the pricing-plan
     * version service.
     */
    public void clearPublishedVersion(
            AdminUser administrator
    ) {
        ensureNotDeleted();

        publishedVersionId = null;
        updatedByAdminUser = administrator;
    }

    public void activate(
            AdminUser administrator
    ) {
        ensureNotDeleted();

        planStatus = WebsitePricingPlanStatus.ACTIVE;
        updatedByAdminUser = administrator;
    }

    public void deactivate(
            AdminUser administrator
    ) {
        ensureNotDeleted();

        planStatus = WebsitePricingPlanStatus.INACTIVE;
        updatedByAdminUser = administrator;
    }

    public void archive(
            AdminUser administrator
    ) {
        ensureNotDeleted();

        planStatus = WebsitePricingPlanStatus.ARCHIVED;
        updatedByAdminUser = administrator;
    }

    public void softDelete(
            AdminUser administrator
    ) {
        if (deletedAt != null) {
            return;
        }

        planStatus = WebsitePricingPlanStatus.INACTIVE;
        deletedAt = Instant.now();
        deletedByAdminUser = administrator;
        updatedByAdminUser = administrator;
    }

    public void restore(
            AdminUser administrator
    ) {
        if (deletedAt == null) {
            return;
        }

        deletedAt = null;
        deletedByAdminUser = null;
        planStatus = WebsitePricingPlanStatus.INACTIVE;
        updatedByAdminUser = administrator;
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }

    public boolean isPubliclyAvailable() {
        return deletedAt == null
                && planStatus == WebsitePricingPlanStatus.ACTIVE
                && publishedVersionId != null;
    }

    private void initializeDefaults() {
        if (planStatus == null) {
            planStatus = WebsitePricingPlanStatus.ACTIVE;
        }
    }

    private void normalizeFields() {
        planCode = normalizePlanCode(planCode);
        planSlug = normalizePlanSlug(planSlug);
    }

    private void validateState() {
        if (
                deletedAt != null
                        && planStatus == WebsitePricingPlanStatus.ACTIVE
        ) {
            throw new IllegalStateException(
                    "A deleted pricing plan cannot remain active."
            );
        }

        if (planCode.length() > 100) {
            throw new IllegalStateException(
                    "Plan code must not exceed 100 characters."
            );
        }

        if (planSlug.length() > 180) {
            throw new IllegalStateException(
                    "Plan slug must not exceed 180 characters."
            );
        }
    }

    private void ensureNotDeleted() {
        if (deletedAt != null) {
            throw new IllegalStateException(
                    "A deleted pricing plan cannot be modified."
            );
        }
    }

    private String normalizePlanCode(
            String value
    ) {
        String normalized = normalizeRequired(
                value,
                "Plan code"
        )
                .toUpperCase(Locale.ROOT)
                .replaceAll("[^A-Z0-9]+", "_")
                .replaceAll("^_+|_+$", "");

        if (normalized.isBlank()) {
            throw new IllegalStateException(
                    "Plan code is required."
            );
        }

        return normalized;
    }

    private String normalizePlanSlug(
            String value
    ) {
        String normalized = normalizeRequired(
                value,
                "Plan slug"
        )
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "");

        if (normalized.isBlank()) {
            throw new IllegalStateException(
                    "Plan slug is required."
            );
        }

        return normalized;
    }

    private String normalizeRequired(
            String value,
            String fieldName
    ) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalStateException(
                    fieldName + " is required."
            );
        }

        return value.trim();
    }
}