package romelt_techcare.backend.entity;

import com.fasterxml.jackson.databind.JsonNode;
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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import romelt_techcare.backend.enums.WebsitePageVersionStatus;

import java.time.Instant;
import java.util.UUID;

/**
 * ================================================================
 * ROMELT TECHCARE — WEBSITE PAGE VERSION ENTITY
 * ================================================================
 *
 * Purpose:
 * Stores one immutable or administrator-editable content version for
 * a stable WebsitePage.
 *
 * Responsibilities:
 * - Stores structured React page content in PostgreSQL JSONB.
 * - Stores SEO, social-sharing, canonical, and robots metadata.
 * - Supports draft, published, and archived lifecycle states.
 * - Stores an optional social-sharing media asset.
 * - Preserves historical versions after publication.
 * - Tracks administrator attribution and lifecycle timestamps.
 * - Supports optimistic locking.
 *
 * Lifecycle rules:
 * - DRAFT versions may be edited.
 * - PUBLISHED versions are public and immutable.
 * - ARCHIVED versions are historical and immutable.
 * - A page may have only one DRAFT version.
 * - A page may have only one PUBLISHED version.
 *
 * Content contract:
 * contentJson must always be a JSON object. Its internal structure is
 * interpreted by the React page component and identified by
 * contentSchemaVersion.
 * ================================================================
 */
@Entity
@Table(
        name = "website_page_versions",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_website_page_version_number",
                        columnNames = {
                                "website_page_id",
                                "version_number"
                        }
                )
        },
        indexes = {
                @Index(
                        name = "idx_website_page_versions_history",
                        columnList =
                                "website_page_id, version_number DESC"
                )
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebsitePageVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(
            name = "page_version_id",
            nullable = false,
            updatable = false
    )
    private UUID pageVersionId;

    /**
     * Stable page that owns this version.
     */
    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "website_page_id",
            nullable = false
    )
    private WebsitePage websitePage;

    /**
     * Sequential version number within one page.
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
    private WebsitePageVersionStatus versionStatus =
            WebsitePageVersionStatus.DRAFT;

    /**
     * Version of the page-specific JSON content contract.
     */
    @Builder.Default
    @Column(
            name = "content_schema_version",
            nullable = false
    )
    private Integer contentSchemaVersion = 1;

    /**
     * Structured page content consumed by React.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(
            name = "content_json",
            nullable = false,
            columnDefinition = "jsonb"
    )
    private JsonNode contentJson;

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

    @Column(
            name = "social_title",
            length = 255
    )
    private String socialTitle;

    @Column(
            name = "social_description",
            length = 500
    )
    private String socialDescription;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "social_image_media_id")
    private WebsiteMediaAsset socialImageMedia;

    @Column(
            name = "canonical_url",
            length = 1000
    )
    private String canonicalUrl;

    @Builder.Default
    @Column(
            name = "robots_index",
            nullable = false
    )
    private Boolean robotsIndex = true;

    @Builder.Default
    @Column(
            name = "robots_follow",
            nullable = false
    )
    private Boolean robotsFollow = true;

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
     * Updates the complete editable state of a draft.
     */
    public void updateDraft(
            Integer newContentSchemaVersion,
            JsonNode newContentJson,
            String newSeoTitle,
            String newSeoDescription,
            String newSocialTitle,
            String newSocialDescription,
            WebsiteMediaAsset newSocialImageMedia,
            String newCanonicalUrl,
            Boolean newRobotsIndex,
            Boolean newRobotsFollow,
            String newChangeSummary,
            AdminUser administrator
    ) {
        requireDraft();

        contentSchemaVersion = newContentSchemaVersion;
        contentJson = newContentJson;

        seoTitle = newSeoTitle;
        seoDescription = newSeoDescription;

        socialTitle = newSocialTitle;
        socialDescription = newSocialDescription;
        socialImageMedia = newSocialImageMedia;

        canonicalUrl = newCanonicalUrl;

        robotsIndex = newRobotsIndex;
        robotsFollow = newRobotsFollow;

        changeSummary = newChangeSummary;
        updatedByAdminUser = administrator;
    }

    /**
     * Publishes this draft.
     */
    public void publish(
            AdminUser administrator
    ) {
        requireDraft();

        versionStatus = WebsitePageVersionStatus.PUBLISHED;
        publishedAt = Instant.now();
        publishedByAdminUser = administrator;

        archivedAt = null;
        archivedByAdminUser = null;

        updatedByAdminUser = administrator;
    }

    /**
     * Archives this version.
     */
    public void archive(
            AdminUser administrator
    ) {
        if (versionStatus == WebsitePageVersionStatus.ARCHIVED) {
            return;
        }

        versionStatus = WebsitePageVersionStatus.ARCHIVED;
        archivedAt = Instant.now();
        archivedByAdminUser = administrator;
        updatedByAdminUser = administrator;
    }

    public boolean isDraft() {
        return versionStatus == WebsitePageVersionStatus.DRAFT;
    }

    public boolean isPublished() {
        return versionStatus
                == WebsitePageVersionStatus.PUBLISHED;
    }

    public boolean isArchived() {
        return versionStatus
                == WebsitePageVersionStatus.ARCHIVED;
    }

    public boolean isPubliclyAvailable() {
        return isPublished()
                && publishedAt != null
                && websitePage != null
                && !websitePage.isDeleted()
                && Boolean.TRUE.equals(
                websitePage.getIsActive()
        );
    }

    private void requireDraft() {
        if (!isDraft()) {
            throw new IllegalStateException(
                    "Only a draft page version may be edited or published."
            );
        }
    }

    private void initializeDefaults() {
        if (versionStatus == null) {
            versionStatus = WebsitePageVersionStatus.DRAFT;
        }

        if (contentSchemaVersion == null) {
            contentSchemaVersion = 1;
        }

        if (robotsIndex == null) {
            robotsIndex = true;
        }

        if (robotsFollow == null) {
            robotsFollow = true;
        }
    }

    private void normalizeFields() {
        seoTitle = normalizeOptional(seoTitle);
        seoDescription = normalizeOptional(seoDescription);

        socialTitle = normalizeOptional(socialTitle);
        socialDescription = normalizeOptional(
                socialDescription
        );

        canonicalUrl = normalizeOptional(canonicalUrl);
        changeSummary = normalizeOptional(changeSummary);
    }

    private void validateState() {
        if (websitePage == null) {
            throw new IllegalStateException(
                    "Website page is required."
            );
        }

        if (versionNumber == null || versionNumber <= 0) {
            throw new IllegalStateException(
                    "Version number must be greater than zero."
            );
        }

        if (
                contentSchemaVersion == null
                        || contentSchemaVersion <= 0
        ) {
            throw new IllegalStateException(
                    "Content schema version must be greater than zero."
            );
        }

        if (contentJson == null || !contentJson.isObject()) {
            throw new IllegalStateException(
                    "Page content must be a JSON object."
            );
        }

        if (
                versionStatus
                        == WebsitePageVersionStatus.PUBLISHED
                        && publishedAt == null
        ) {
            throw new IllegalStateException(
                    "A published page version requires a publication timestamp."
            );
        }

        if (
                versionStatus
                        == WebsitePageVersionStatus.ARCHIVED
                        && archivedAt == null
        ) {
            throw new IllegalStateException(
                    "An archived page version requires an archive timestamp."
            );
        }
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