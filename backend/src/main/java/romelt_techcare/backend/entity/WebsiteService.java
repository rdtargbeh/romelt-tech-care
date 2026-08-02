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
import romelt_techcare.backend.enums.WebsiteServiceStatus;

import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

/**
 * ================================================================
 * ROMELT TECHCARE — WEBSITE SERVICE ENTITY
 * ================================================================
 *
 * Purpose:
 * Stores the stable identity of a service offered on the Romelt
 * TechCare public website.
 *
 * Responsibilities:
 * - Stores a stable internal service code.
 * - Stores a unique public URL slug.
 * - Points to the current administrator-editable service version.
 * - Points to the current publicly published service version.
 * - Controls the service lifecycle status.
 * - Supports soft deletion and restoration.
 * - Records administrator attribution.
 * - Supports optimistic locking.
 *
 * Version fields:
 * Editable service names, descriptions, features, pricing text,
 * images, SEO content, and display settings belong to the future
 * WebsiteServiceVersion entity.
 *
 * draftVersionId and publishedVersionId remain UUID fields until the
 * service-version schema is implemented and its foreign keys are
 * available.
 * ================================================================
 */
@Entity
@Table(
        name = "website_services",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_website_service_code",
                        columnNames = "service_code"
                ),
                @UniqueConstraint(
                        name = "uk_website_service_slug",
                        columnNames = "service_slug"
                )
        },
        indexes = {
                @Index(
                        name = "idx_website_services_status",
                        columnList = "service_status"
                )
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebsiteService {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(
            name = "service_id",
            nullable = false,
            updatable = false
    )
    private UUID serviceId;

    /**
     * Stable normalized internal service identifier.
     *
     * Examples:
     * COMPUTER_SUPPORT
     * WIFI_NETWORKING
     * REMOTE_SUPPORT
     */
    @Column(
            name = "service_code",
            nullable = false,
            unique = true,
            length = 100
    )
    private String serviceCode;

    /**
     * Stable public URL slug.
     *
     * Examples:
     * computer-support
     * wifi-networking
     * remote-support
     */
    @Column(
            name = "service_slug",
            nullable = false,
            unique = true,
            length = 180
    )
    private String serviceSlug;

    /**
     * Current administrator-editable service-version identifier.
     */
    @Column(name = "draft_version_id")
    private UUID draftVersionId;

    /**
     * Current public service-version identifier.
     */
    @Column(name = "published_version_id")
    private UUID publishedVersionId;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(
            name = "service_status",
            nullable = false,
            length = 30
    )
    private WebsiteServiceStatus serviceStatus =
            WebsiteServiceStatus.ACTIVE;

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
     * Updates the stable service identity.
     */
    public void updateIdentity(
            String newServiceCode,
            String newServiceSlug,
            WebsiteServiceStatus newServiceStatus,
            AdminUser administrator
    ) {
        ensureNotDeleted();

        serviceCode = newServiceCode;
        serviceSlug = newServiceSlug;
        serviceStatus = newServiceStatus;
        updatedByAdminUser = administrator;
    }

    public void assignDraftVersion(
            UUID versionId,
            AdminUser administrator
    ) {
        ensureNotDeleted();

        draftVersionId = versionId;
        updatedByAdminUser = administrator;
    }

    public void clearDraftVersion(
            AdminUser administrator
    ) {
        ensureNotDeleted();

        draftVersionId = null;
        updatedByAdminUser = administrator;
    }

    public void assignPublishedVersion(
            UUID versionId,
            AdminUser administrator
    ) {
        ensureNotDeleted();

        publishedVersionId = versionId;
        updatedByAdminUser = administrator;
    }

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

        serviceStatus = WebsiteServiceStatus.ACTIVE;
        updatedByAdminUser = administrator;
    }

    public void deactivate(
            AdminUser administrator
    ) {
        ensureNotDeleted();

        serviceStatus = WebsiteServiceStatus.INACTIVE;
        updatedByAdminUser = administrator;
    }

    public void archive(
            AdminUser administrator
    ) {
        ensureNotDeleted();

        serviceStatus = WebsiteServiceStatus.ARCHIVED;
        updatedByAdminUser = administrator;
    }

    public void softDelete(
            AdminUser administrator
    ) {
        if (deletedAt != null) {
            return;
        }

        serviceStatus = WebsiteServiceStatus.INACTIVE;
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
        serviceStatus = WebsiteServiceStatus.INACTIVE;
        updatedByAdminUser = administrator;
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }

    public boolean isPubliclyAvailable() {
        return deletedAt == null
                && serviceStatus == WebsiteServiceStatus.ACTIVE
                && publishedVersionId != null;
    }

    private void initializeDefaults() {
        if (serviceStatus == null) {
            serviceStatus = WebsiteServiceStatus.ACTIVE;
        }
    }

    private void normalizeFields() {
        serviceCode = normalizeServiceCode(serviceCode);
        serviceSlug = normalizeServiceSlug(serviceSlug);
    }

    private void validateState() {
        if (
                deletedAt != null
                        && serviceStatus
                        == WebsiteServiceStatus.ACTIVE
        ) {
            throw new IllegalStateException(
                    "A deleted website service cannot remain active."
            );
        }
    }

    private void ensureNotDeleted() {
        if (deletedAt != null) {
            throw new IllegalStateException(
                    "A deleted website service cannot be modified."
            );
        }
    }

    private String normalizeServiceCode(
            String value
    ) {
        String normalized = normalizeRequired(
                value,
                "Service code"
        )
                .toUpperCase(Locale.ROOT)
                .replaceAll("[^A-Z0-9]+", "_")
                .replaceAll("^_+|_+$", "");

        if (normalized.isBlank()) {
            throw new IllegalStateException(
                    "Service code is required."
            );
        }

        return normalized;
    }

    private String normalizeServiceSlug(
            String value
    ) {
        String normalized = normalizeRequired(
                value,
                "Service slug"
        )
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "");

        if (normalized.isBlank()) {
            throw new IllegalStateException(
                    "Service slug is required."
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