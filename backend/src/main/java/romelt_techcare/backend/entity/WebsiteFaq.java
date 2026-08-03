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
import romelt_techcare.backend.enums.WebsiteFaqStatus;

import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

/**
 * ================================================================
 * ROMELT TECHCARE — WEBSITE FAQ ENTITY
 * ================================================================
 *
 * Purpose:
 * Stores the stable identity of one website frequently asked
 * question.
 *
 * Responsibilities:
 * - Stores a unique internal FAQ key.
 * - Points to the current administrator-editable FAQ version.
 * - Points to the current publicly published FAQ version.
 * - Controls the stable FAQ lifecycle status.
 * - Supports soft deletion and restoration.
 * - Records administrator attribution.
 * - Supports optimistic locking.
 *
 * Versioned content:
 * FAQ question text, answer text, category, display order, visibility,
 * page placement, SEO data, and publication dates belong to
 * WebsiteFaqVersion.
 *
 * Version-pointer access:
 * draftVersionId and publishedVersionId are internal lifecycle fields.
 * They must be changed only by WebsiteFaqVersionService after the FAQ
 * version module is implemented.
 * ================================================================
 */
@Entity
@Table(
        name = "website_faqs",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_website_faq_key",
                        columnNames = "faq_key"
                )
        },
        indexes = {
                @Index(
                        name = "idx_website_faq_status",
                        columnList = "faq_status"
                )
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebsiteFaq {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(
            name = "faq_id",
            nullable = false,
            updatable = false
    )
    private UUID faqId;

    /**
     * Stable internal FAQ identifier.
     *
     * Examples:
     * SERVICE_AREA
     * REMOTE_SUPPORT
     * APPOINTMENT_REQUIRED
     * PAYMENT_METHODS
     */
    @Column(
            name = "faq_key",
            nullable = false,
            unique = true,
            length = 120
    )
    private String faqKey;

    /**
     * Current administrator-editable FAQ version.
     *
     * Managed only by WebsiteFaqVersionService.
     */
    @Column(name = "draft_version_id")
    private UUID draftVersionId;

    /**
     * Current publicly published FAQ version.
     *
     * Managed only by WebsiteFaqVersionService.
     */
    @Column(name = "published_version_id")
    private UUID publishedVersionId;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(
            name = "faq_status",
            nullable = false,
            length = 30
    )
    private WebsiteFaqStatus faqStatus =
            WebsiteFaqStatus.ACTIVE;

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
     * Updates the stable FAQ identity.
     */
    public void updateIdentity(
            String newFaqKey,
            WebsiteFaqStatus newFaqStatus,
            AdminUser administrator
    ) {
        ensureNotDeleted();

        faqKey = newFaqKey;
        faqStatus = newFaqStatus;
        updatedByAdminUser = administrator;
    }

    /**
     * Assigns the current draft version.
     *
     * This method must be used only by WebsiteFaqVersionService.
     */
    public void assignDraftVersion(
            UUID versionId,
            AdminUser administrator
    ) {
        ensureNotDeleted();

        if (versionId == null) {
            throw new IllegalArgumentException(
                    "Draft FAQ version ID is required."
            );
        }

        draftVersionId = versionId;
        updatedByAdminUser = administrator;
    }

    /**
     * Clears the current draft version pointer.
     *
     * This method must be used only by WebsiteFaqVersionService.
     */
    public void clearDraftVersion(
            AdminUser administrator
    ) {
        ensureNotDeleted();

        draftVersionId = null;
        updatedByAdminUser = administrator;
    }

    /**
     * Assigns the current published version.
     *
     * This method must be used only by WebsiteFaqVersionService.
     */
    public void assignPublishedVersion(
            UUID versionId,
            AdminUser administrator
    ) {
        ensureNotDeleted();

        if (versionId == null) {
            throw new IllegalArgumentException(
                    "Published FAQ version ID is required."
            );
        }

        publishedVersionId = versionId;
        updatedByAdminUser = administrator;
    }

    /**
     * Clears the current published version pointer.
     *
     * This method must be used only by WebsiteFaqVersionService.
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

        faqStatus = WebsiteFaqStatus.ACTIVE;
        updatedByAdminUser = administrator;
    }

    public void deactivate(
            AdminUser administrator
    ) {
        ensureNotDeleted();

        faqStatus = WebsiteFaqStatus.INACTIVE;
        updatedByAdminUser = administrator;
    }

    public void archive(
            AdminUser administrator
    ) {
        ensureNotDeleted();

        faqStatus = WebsiteFaqStatus.ARCHIVED;
        updatedByAdminUser = administrator;
    }

    public void softDelete(
            AdminUser administrator
    ) {
        if (deletedAt != null) {
            return;
        }

        faqStatus = WebsiteFaqStatus.INACTIVE;
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
        faqStatus = WebsiteFaqStatus.INACTIVE;
        updatedByAdminUser = administrator;
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }

    public boolean isPubliclyAvailable() {
        return deletedAt == null
                && faqStatus == WebsiteFaqStatus.ACTIVE
                && publishedVersionId != null;
    }

    private void initializeDefaults() {
        if (faqStatus == null) {
            faqStatus = WebsiteFaqStatus.ACTIVE;
        }
    }

    private void normalizeFields() {
        faqKey = normalizeFaqKey(faqKey);
    }

    private void validateState() {
        if (
                deletedAt != null
                        && faqStatus == WebsiteFaqStatus.ACTIVE
        ) {
            throw new IllegalStateException(
                    "A deleted FAQ cannot remain active."
            );
        }

        if (faqKey.length() > 120) {
            throw new IllegalStateException(
                    "FAQ key must not exceed 120 characters."
            );
        }
    }

    private void ensureNotDeleted() {
        if (deletedAt != null) {
            throw new IllegalStateException(
                    "A deleted FAQ cannot be modified."
            );
        }
    }

    private String normalizeFaqKey(
            String value
    ) {
        String normalized = normalizeRequired(
                value,
                "FAQ key"
        )
                .toUpperCase(Locale.ROOT)
                .replaceAll("[^A-Z0-9]+", "_")
                .replaceAll("^_+|_+$", "");

        if (normalized.isBlank()) {
            throw new IllegalStateException(
                    "FAQ key is required."
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