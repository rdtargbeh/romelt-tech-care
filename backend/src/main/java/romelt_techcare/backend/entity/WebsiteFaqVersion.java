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
import romelt_techcare.backend.enums.WebsiteFaqVersionStatus;

import java.time.Instant;
import java.util.UUID;

/**
 * ================================================================
 * ROMELT TECHCARE — WEBSITE FAQ VERSION ENTITY
 * ================================================================
 *
 * Purpose:
 * Stores one draft, published, or archived content version for a
 * stable WebsiteFaq identity.
 *
 * Responsibilities:
 * - Stores the FAQ category.
 * - Stores customer-facing question and answer content.
 * - Controls display order, featured placement, and public visibility.
 * - Stores the change summary for each version.
 * - Tracks publication and archival lifecycle timestamps.
 * - Records administrator attribution.
 * - Supports optimistic locking.
 *
 * Lifecycle rules:
 * - Only DRAFT versions may be edited or published.
 * - Publishing a draft archives the previous published version.
 * - PUBLISHED and ARCHIVED versions are immutable.
 * - One FAQ may have only one DRAFT and one PUBLISHED version.
 *
 * Public availability:
 * A version is publicly available only when:
 * - the stable FAQ is active and not deleted;
 * - the version is PUBLISHED;
 * - it is the stable FAQ's current published version;
 * - isPublic is true.
 * ================================================================
 */
@Entity
@Table(
        name = "website_faq_versions",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_website_faq_version_number",
                        columnNames = {
                                "faq_id",
                                "version_number"
                        }
                )
        },
        indexes = {
                @Index(
                        name = "idx_website_faq_versions_public",
                        columnList =
                                "version_status, is_public, faq_category, display_order"
                )
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebsiteFaqVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(
            name = "faq_version_id",
            nullable = false,
            updatable = false
    )
    private UUID faqVersionId;

    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "faq_id",
            nullable = false,
            updatable = false
    )
    private WebsiteFaq faq;

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
    private WebsiteFaqVersionStatus versionStatus =
            WebsiteFaqVersionStatus.DRAFT;

    @Column(
            name = "faq_category",
            length = 120
    )
    private String faqCategory;

    @Column(
            name = "question",
            nullable = false,
            length = 500
    )
    private String question;

    @Column(
            name = "answer",
            nullable = false,
            columnDefinition = "text"
    )
    private String answer;

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
            name = "is_public",
            nullable = false
    )
    private Boolean isPublic = true;

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
     * Updates editable content belonging to a draft FAQ version.
     */
    public void updateDraft(
            String newFaqCategory,
            String newQuestion,
            String newAnswer,
            Integer newDisplayOrder,
            Boolean featured,
            Boolean publicFaq,
            String newChangeSummary,
            AdminUser administrator
    ) {
        requireDraft();

        faqCategory = newFaqCategory;
        question = newQuestion;
        answer = newAnswer;

        displayOrder = newDisplayOrder;
        isFeatured = featured;
        isPublic = publicFaq;

        changeSummary = newChangeSummary;
        updatedByAdminUser = administrator;
    }

    /**
     * Publishes this draft FAQ version.
     */
    public void publish(
            AdminUser administrator
    ) {
        requireDraft();

        versionStatus = WebsiteFaqVersionStatus.PUBLISHED;

        publishedAt = Instant.now();
        publishedByAdminUser = administrator;

        archivedAt = null;
        archivedByAdminUser = null;

        updatedByAdminUser = administrator;
    }

    /**
     * Archives this FAQ version.
     */
    public void archive(
            AdminUser administrator
    ) {
        if (isArchived()) {
            return;
        }

        versionStatus = WebsiteFaqVersionStatus.ARCHIVED;

        archivedAt = Instant.now();
        archivedByAdminUser = administrator;
        updatedByAdminUser = administrator;
    }

    public boolean isDraft() {
        return versionStatus == WebsiteFaqVersionStatus.DRAFT;
    }

    public boolean isPublished() {
        return versionStatus == WebsiteFaqVersionStatus.PUBLISHED;
    }

    public boolean isArchived() {
        return versionStatus == WebsiteFaqVersionStatus.ARCHIVED;
    }

    public boolean isPubliclyAvailable() {
        return isPublished()
                && publishedAt != null
                && Boolean.TRUE.equals(isPublic)
                && faq != null
                && faq.isPubliclyAvailable()
                && faqVersionId != null
                && faqVersionId.equals(
                faq.getPublishedVersionId()
        );
    }

    private void requireDraft() {
        if (!isDraft()) {
            throw new IllegalStateException(
                    "Only a draft FAQ version may be edited or published."
            );
        }
    }

    private void initializeDefaults() {
        if (versionStatus == null) {
            versionStatus = WebsiteFaqVersionStatus.DRAFT;
        }

        if (displayOrder == null) {
            displayOrder = 0;
        }

        if (isFeatured == null) {
            isFeatured = false;
        }

        if (isPublic == null) {
            isPublic = true;
        }
    }

    private void normalizeFields() {
        faqCategory = normalizeOptional(faqCategory);

        question = normalizeRequired(
                question,
                "FAQ question"
        );

        answer = normalizeRequired(
                answer,
                "FAQ answer"
        );

        changeSummary = normalizeOptional(changeSummary);
    }

    private void validateState() {
        if (faq == null) {
            throw new IllegalStateException(
                    "FAQ identity is required."
            );
        }

        if (versionNumber == null || versionNumber <= 0) {
            throw new IllegalStateException(
                    "Version number must be greater than zero."
            );
        }

        if (faqCategory != null && faqCategory.length() > 120) {
            throw new IllegalStateException(
                    "FAQ category must not exceed 120 characters."
            );
        }

        if (question.length() > 500) {
            throw new IllegalStateException(
                    "FAQ question must not exceed 500 characters."
            );
        }

        if (displayOrder == null || displayOrder < 0) {
            throw new IllegalStateException(
                    "Display order must not be negative."
            );
        }

        if (changeSummary != null && changeSummary.length() > 1000) {
            throw new IllegalStateException(
                    "Change summary must not exceed 1000 characters."
            );
        }

        if (isPublished() && publishedAt == null) {
            throw new IllegalStateException(
                    "A published FAQ version requires a publication timestamp."
            );
        }

        if (isArchived() && archivedAt == null) {
            throw new IllegalStateException(
                    "An archived FAQ version requires an archive timestamp."
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