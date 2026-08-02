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
import romelt_techcare.backend.enums.WebsitePageType;

import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

/**
 * ================================================================
 * ROMELT TECHCARE — WEBSITE PAGE ENTITY
 * ================================================================
 *
 * Purpose:
 * Stores the stable identity and routing information for each fixed
 * React page managed by the Romelt TechCare CMS.
 *
 * Responsibilities:
 * - Stores a stable application-facing page key.
 * - Stores the administrator-facing page name.
 * - Stores the React route path.
 * - Categorizes the page by functional type.
 * - Points to the current editable draft version.
 * - Points to the current publicly published version.
 * - Tracks the expected content-schema version.
 * - Distinguishes protected system pages from custom pages.
 * - Supports activation, deactivation, and soft deletion.
 * - Tracks administrator attribution and optimistic locking.
 *
 * Version references:
 * draftVersionId and publishedVersionId remain UUID fields because the
 * supplied website_pages schema does not yet define foreign keys to a
 * website page-version table. These fields can be converted to direct
 * JPA relationships after that schema is implemented.
 *
 * Deletion:
 * Records are soft-deleted using deletedAt and deletedByAdminUser.
 * Public and normal administrator queries exclude deleted records.
 * ================================================================
 */
@Entity
@Table(
        name = "website_pages",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_website_page_key",
                        columnNames = "page_key"
                ),
                @UniqueConstraint(
                        name = "uk_website_page_route",
                        columnNames = "route_path"
                )
        },
        indexes = {
                @Index(
                        name = "idx_website_pages_public_route",
                        columnList = "route_path, is_active"
                ),
                @Index(
                        name = "idx_website_pages_type",
                        columnList = "page_type, is_active"
                )
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebsitePage {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(
            name = "website_page_id",
            nullable = false,
            updatable = false
    )
    private UUID websitePageId;

    /**
     * Stable application-facing identifier.
     *
     * Examples:
     * HOME
     * SERVICES
     * PRICING
     * CONTACT
     */
    @Column(
            name = "page_key",
            nullable = false,
            unique = true,
            length = 120
    )
    private String pageKey;

    /**
     * Human-readable administrator-facing page name.
     */
    @Column(
            name = "page_name",
            nullable = false,
            length = 180
    )
    private String pageName;

    /**
     * Public React route.
     *
     * Examples:
     * /
     * /services
     * /pricing
     * /contact
     */
    @Column(
            name = "route_path",
            nullable = false,
            unique = true,
            length = 500
    )
    private String routePath;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(
            name = "page_type",
            nullable = false,
            length = 50
    )
    private WebsitePageType pageType = WebsitePageType.STANDARD;

    /**
     * Identifier of the current administrator-editable version.
     */
    @Column(name = "draft_version_id")
    private UUID draftVersionId;

    /**
     * Identifier of the current public version.
     */
    @Column(name = "published_version_id")
    private UUID publishedVersionId;

    /**
     * Expected version of the JSON content contract used by React.
     */
    @Builder.Default
    @Column(
            name = "content_schema_version",
            nullable = false
    )
    private Integer contentSchemaVersion = 1;

    /**
     * Indicates that the page is part of the application-defined site
     * structure and should not normally be permanently removed.
     */
    @Builder.Default
    @Column(
            name = "is_system_page",
            nullable = false
    )
    private Boolean isSystemPage = true;

    /**
     * Controls whether the page may be resolved publicly.
     */
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
     * Applies the complete editable page identity state.
     */
    public void updateDetails(
            String newPageKey,
            String newPageName,
            String newRoutePath,
            WebsitePageType newPageType,
            Integer newContentSchemaVersion,
            Boolean systemPage,
            Boolean active,
            AdminUser administrator
    ) {
        pageKey = newPageKey;
        pageName = newPageName;
        routePath = newRoutePath;
        pageType = newPageType;
        contentSchemaVersion = newContentSchemaVersion;
        isSystemPage = systemPage;
        isActive = active;
        updatedByAdminUser = administrator;
    }

    public void assignDraftVersion(
            UUID versionId,
            AdminUser administrator
    ) {
        draftVersionId = versionId;
        updatedByAdminUser = administrator;
    }

    public void assignPublishedVersion(
            UUID versionId,
            AdminUser administrator
    ) {
        publishedVersionId = versionId;
        updatedByAdminUser = administrator;
    }

    public void clearDraftVersion(
            AdminUser administrator
    ) {
        draftVersionId = null;
        updatedByAdminUser = administrator;
    }

    public void clearPublishedVersion(
            AdminUser administrator
    ) {
        publishedVersionId = null;
        updatedByAdminUser = administrator;
    }

    public void activate(
            AdminUser administrator
    ) {
        ensureNotDeleted();

        isActive = true;
        updatedByAdminUser = administrator;
    }

    public void deactivate(
            AdminUser administrator
    ) {
        ensureNotDeleted();

        isActive = false;
        updatedByAdminUser = administrator;
    }

    public void softDelete(
            AdminUser administrator
    ) {
        isActive = false;
        deletedAt = Instant.now();
        deletedByAdminUser = administrator;
        updatedByAdminUser = administrator;
    }

    public void restore(
            AdminUser administrator
    ) {
        deletedAt = null;
        deletedByAdminUser = null;
        updatedByAdminUser = administrator;
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }

    public boolean isPubliclyAvailable() {
        return deletedAt == null
                && Boolean.TRUE.equals(isActive)
                && publishedVersionId != null;
    }

    private void initializeDefaults() {
        if (pageType == null) {
            pageType = WebsitePageType.STANDARD;
        }

        if (contentSchemaVersion == null) {
            contentSchemaVersion = 1;
        }

        if (isSystemPage == null) {
            isSystemPage = true;
        }

        if (isActive == null) {
            isActive = true;
        }
    }

    private void normalizeFields() {
        pageKey = normalizeStableKey(
                pageKey,
                "Page key"
        );

        pageName = normalizeRequired(
                pageName,
                "Page name"
        );

        routePath = normalizeRoutePath(routePath);
    }

    private void validateState() {
        if (contentSchemaVersion <= 0) {
            throw new IllegalStateException(
                    "Content schema version must be greater than zero."
            );
        }

        if (deletedAt != null && Boolean.TRUE.equals(isActive)) {
            throw new IllegalStateException(
                    "A deleted website page cannot remain active."
            );
        }
    }

    private void ensureNotDeleted() {
        if (deletedAt != null) {
            throw new IllegalStateException(
                    "A deleted website page cannot be modified."
            );
        }
    }

    private String normalizeStableKey(
            String value,
            String fieldName
    ) {
        String normalized = normalizeRequired(value, fieldName)
                .toUpperCase(Locale.ROOT)
                .replaceAll("[^A-Z0-9]+", "_")
                .replaceAll("^_+|_+$", "");

        if (normalized.isBlank()) {
            throw new IllegalStateException(
                    fieldName + " is required."
            );
        }

        return normalized;
    }

    private String normalizeRoutePath(
            String value
    ) {
        String normalized = normalizeRequired(
                value,
                "Route path"
        );

        if (!normalized.startsWith("/")) {
            normalized = "/" + normalized;
        }

        normalized = normalized.replaceAll("/{2,}", "/");

        if (normalized.length() > 1 && normalized.endsWith("/")) {
            normalized = normalized.substring(
                    0,
                    normalized.length() - 1
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