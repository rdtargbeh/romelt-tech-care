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
import romelt_techcare.backend.enums.WebsiteMediaAssetStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

/**
 * ================================================================
 * ROMELT TECHCARE — WEBSITE MEDIA ASSET ENTITY
 * ================================================================
 *
 * Purpose:
 * Stores website-facing metadata for files used by the Romelt
 * TechCare content-management system and public website.
 *
 * Responsibilities:
 * - Identifies the corresponding file-storage attachment.
 * - Stores original file and public-delivery metadata.
 * - Stores image dimensions and focal-point positioning.
 * - Stores accessible alternative text and decorative-image status.
 * - Tracks upload, processing, active, archived, and deleted states.
 * - Tracks the administrators responsible for lifecycle changes.
 * - Supports optimistic locking for concurrent administrator updates.
 *
 * Storage architecture:
 * This entity stores metadata only. Binary file content must remain
 * in the shared file-storage module or approved object storage.
 *
 * FileAttachment integration:
 * fileAttachmentId is currently stored as a UUID because the exact
 * existing FileAttachment entity and physical table mapping have not
 * yet been connected to this CMS module. It can later be replaced
 * with or supplemented by a @ManyToOne relationship after that
 * entity is reviewed.
 *
 * Accessibility:
 * A public asset must either:
 * - contain meaningful alternative text, or
 * - be explicitly marked as decorative.
 *
 * Soft deletion:
 * Deleted assets remain stored as records for audit and reference
 * protection. Permanent deletion must be handled by a controlled
 * cleanup service after confirming that the asset is not in use.
 *
 * Real-data integration:
 * This entity will be used by:
 * - Website media administration
 * - Public image delivery
 * - Page content management
 * - Business branding
 * - Services and pricing content
 * - Customer-review photographs
 * - Future media-usage tracking
 * ================================================================
 */
@Entity
@Table(
        name = "website_media_assets",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_website_media_file_attachment",
                        columnNames = "file_attachment_id"
                ),
                @UniqueConstraint(
                        name = "uk_website_media_asset_key",
                        columnNames = "asset_key"
                )
        },
        indexes = {
                @Index(
                        name = "idx_website_media_status",
                        columnList = "asset_status, created_at"
                ),
                @Index(
                        name = "idx_website_media_mime_type",
                        columnList = "mime_type"
                ),
                @Index(
                        name = "idx_website_media_public",
                        columnList = "is_public, asset_status, created_at"
                ),
                @Index(
                        name = "idx_website_media_deleted",
                        columnList = "deleted_at"
                )
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebsiteMediaAsset {

    /**
     * Primary identifier for the CMS media record.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(
            name = "media_asset_id",
            nullable = false,
            updatable = false
    )
    private UUID mediaAssetId;

    /**
     * Optional identifier of the corresponding shared file attachment.
     *
     * This remains a UUID until the existing FileAttachment entity is
     * reviewed and connected through a direct JPA relationship.
     */
    @Column(
            name = "file_attachment_id",
            unique = true
    )
    private UUID fileAttachmentId;

    /**
     * Optional stable application-facing key.
     *
     * Examples:
     * - PRIMARY_LOGO
     * - HOME_HERO_IMAGE
     * - SERVICES_HERO_IMAGE
     * - CONTACT_SUPPORT_IMAGE
     */
    @Column(
            name = "asset_key",
            unique = true,
            length = 180
    )
    private String assetKey;

    /**
     * Original file name supplied during upload.
     */
    @Column(
            name = "original_file_name",
            nullable = false,
            length = 255
    )
    private String originalFileName;

    /**
     * Publicly accessible or API-proxied URL used to retrieve the asset.
     */
    @Column(
            name = "public_url",
            length = 1500
    )
    private String publicUrl;

    /**
     * Internal object-storage or file-system key.
     *
     * This value must not automatically be exposed through public APIs.
     */
    @Column(
            name = "storage_key",
            length = 1000
    )
    private String storageKey;

    /**
     * File MIME type.
     *
     * Examples:
     * - image/png
     * - image/jpeg
     * - image/webp
     * - application/pdf
     */
    @Column(
            name = "mime_type",
            nullable = false,
            length = 150
    )
    private String mimeType;

    /**
     * Normalized file extension without a leading period.
     *
     * Examples:
     * - png
     * - jpg
     * - webp
     * - pdf
     */
    @Column(
            name = "file_extension",
            length = 30
    )
    private String fileExtension;

    /**
     * File size in bytes.
     */
    @Column(
            name = "file_size_bytes"
    )
    private Long fileSizeBytes;

    /**
     * Image width in pixels when the asset is an image.
     */
    @Column(
            name = "width_pixels"
    )
    private Integer widthPixels;

    /**
     * Image height in pixels when the asset is an image.
     */
    @Column(
            name = "height_pixels"
    )
    private Integer heightPixels;

    /**
     * Administrative media title.
     */
    @Column(
            name = "title",
            length = 255
    )
    private String title;

    /**
     * Accessible alternative text for informative public images.
     */
    @Column(
            name = "alt_text",
            length = 500
    )
    private String altText;

    /**
     * Optional public-facing image caption.
     */
    @Column(
            name = "caption",
            columnDefinition = "TEXT"
    )
    private String caption;

    /**
     * Optional private or administrative description of the asset.
     */
    @Column(
            name = "description",
            columnDefinition = "TEXT"
    )
    private String description;

    /**
     * Indicates that the image conveys no meaningful information and
     * should use an empty alternative-text value when rendered.
     */
    @Builder.Default
    @Column(
            name = "is_decorative",
            nullable = false
    )
    private Boolean isDecorative = false;

    /**
     * Horizontal focal point represented as a percentage from 0 to 100.
     */
    @Column(
            name = "focal_point_x",
            precision = 5,
            scale = 2
    )
    private BigDecimal focalPointX;

    /**
     * Vertical focal point represented as a percentage from 0 to 100.
     */
    @Column(
            name = "focal_point_y",
            precision = 5,
            scale = 2
    )
    private BigDecimal focalPointY;

    /**
     * Current processing and lifecycle status.
     */
    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(
            name = "asset_status",
            nullable = false,
            length = 30
    )
    private WebsiteMediaAssetStatus assetStatus =
            WebsiteMediaAssetStatus.ACTIVE;

    /**
     * Indicates whether the asset is approved for public website use.
     */
    @Builder.Default
    @Column(
            name = "is_public",
            nullable = false
    )
    private Boolean isPublic = true;

    /**
     * Administrator who originally created the media record.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "created_by_admin_user_id"
    )
    private AdminUser createdByAdminUser;

    /**
     * Administrator who most recently updated the media record.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "updated_by_admin_user_id"
    )
    private AdminUser updatedByAdminUser;

    /**
     * Administrator who archived the asset.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "archived_by_admin_user_id"
    )
    private AdminUser archivedByAdminUser;

    /**
     * Administrator who soft-deleted the asset.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "deleted_by_admin_user_id"
    )
    private AdminUser deletedByAdminUser;

    /**
     * Timestamp when the asset was archived.
     */
    @Column(
            name = "archived_at"
    )
    private Instant archivedAt;

    /**
     * Timestamp when the asset was soft-deleted.
     */
    @Column(
            name = "deleted_at"
    )
    private Instant deletedAt;

    /**
     * Record creation timestamp.
     */
    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private Instant createdAt;

    /**
     * Most recent record update timestamp.
     */
    @Column(
            name = "updated_at",
            nullable = false
    )
    private Instant updatedAt;

    /**
     * Hibernate-managed optimistic-lock value.
     *
     * Services must not increment this field manually.
     */
    @Version
    @Column(
            name = "row_version",
            nullable = false
    )
    private Long rowVersion;

    /**
     * Initializes defaults, normalizes metadata, and validates the
     * entity before its first database insert.
     */
    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();

        normalizeFields();
        initializeDefaults();
        validateState();

        if (createdAt == null) {
            createdAt = now;
        }

        updatedAt = now;
    }

    /**
     * Normalizes and validates the entity before database updates.
     */
    @PreUpdate
    protected void onUpdate() {
        normalizeFields();
        initializeDefaults();
        validateState();

        updatedAt = Instant.now();
    }

    /**
     * Marks this asset as active and available for normal use.
     */
    public void activate(
            AdminUser administrator
    ) {
        assetStatus = WebsiteMediaAssetStatus.ACTIVE;
        deletedAt = null;
        deletedByAdminUser = null;
        archivedAt = null;
        archivedByAdminUser = null;
        updatedByAdminUser = administrator;
    }

    /**
     * Marks this asset as archived.
     */
    public void archive(
            AdminUser administrator
    ) {
        assetStatus = WebsiteMediaAssetStatus.ARCHIVED;
        archivedAt = Instant.now();
        archivedByAdminUser = administrator;
        updatedByAdminUser = administrator;

        /*
         * Archived assets should not remain publicly selectable.
         */
        isPublic = false;
    }

    /**
     * Restores an archived asset to active status.
     */
    public void restore(
            AdminUser administrator
    ) {
        assetStatus = WebsiteMediaAssetStatus.ACTIVE;
        archivedAt = null;
        archivedByAdminUser = null;
        deletedAt = null;
        deletedByAdminUser = null;
        updatedByAdminUser = administrator;
    }

    /**
     * Soft-deletes this asset.
     *
     * The service layer must verify that no active media-usage records
     * reference this asset before calling this method.
     */
    public void softDelete(
            AdminUser administrator
    ) {
        assetStatus = WebsiteMediaAssetStatus.DELETED;
        deletedAt = Instant.now();
        deletedByAdminUser = administrator;
        updatedByAdminUser = administrator;
        isPublic = false;
    }

    /**
     * Marks an upload or media-processing operation as failed.
     */
    public void markFailed(
            AdminUser administrator
    ) {
        assetStatus = WebsiteMediaAssetStatus.FAILED;
        updatedByAdminUser = administrator;
        isPublic = false;
    }

    /**
     * Marks the asset as being processed.
     */
    public void markProcessing(
            AdminUser administrator
    ) {
        assetStatus = WebsiteMediaAssetStatus.PROCESSING;
        updatedByAdminUser = administrator;
        isPublic = false;
    }

    /**
     * Updates accessibility information for the media asset.
     */
    public void updateAccessibility(
            String newAltText,
            boolean decorative,
            AdminUser administrator
    ) {
        altText = normalizeOptional(newAltText);
        isDecorative = decorative;
        updatedByAdminUser = administrator;

        validatePublicAccessibility();
    }

    /**
     * Updates image focal-point values.
     */
    public void updateFocalPoint(
            BigDecimal newFocalPointX,
            BigDecimal newFocalPointY,
            AdminUser administrator
    ) {
        validateFocalPoint(
                newFocalPointX,
                "Horizontal focal point"
        );

        validateFocalPoint(
                newFocalPointY,
                "Vertical focal point"
        );

        focalPointX = newFocalPointX;
        focalPointY = newFocalPointY;
        updatedByAdminUser = administrator;
    }

    /**
     * Returns true when the asset is active, public, and not deleted.
     */
    public boolean isPubliclyAvailable() {
        return Boolean.TRUE.equals(isPublic)
                && assetStatus == WebsiteMediaAssetStatus.ACTIVE
                && deletedAt == null;
    }

    /**
     * Returns true when the record is soft-deleted.
     */
    public boolean isDeleted() {
        return assetStatus == WebsiteMediaAssetStatus.DELETED
                || deletedAt != null;
    }

    /**
     * Returns true when the record is archived.
     */
    public boolean isArchived() {
        return assetStatus == WebsiteMediaAssetStatus.ARCHIVED;
    }

    /**
     * Initializes required default values that may be absent when the
     * entity is created without Lombok's builder.
     */
    private void initializeDefaults() {
        if (assetStatus == null) {
            assetStatus = WebsiteMediaAssetStatus.ACTIVE;
        }

        if (isPublic == null) {
            isPublic = true;
        }

        if (isDecorative == null) {
            isDecorative = false;
        }
    }

    /**
     * Normalizes textual metadata before persistence.
     */
    private void normalizeFields() {
        assetKey = normalizeAssetKey(assetKey);
        originalFileName = normalizeRequired(
                originalFileName,
                "Original file name"
        );
        publicUrl = normalizeOptional(publicUrl);
        storageKey = normalizeOptional(storageKey);
        mimeType = normalizeMimeType(mimeType);
        fileExtension = normalizeExtension(fileExtension);
        title = normalizeOptional(title);
        altText = normalizeOptional(altText);
        caption = normalizeOptional(caption);
        description = normalizeOptional(description);
    }

    /**
     * Validates all persistence-level business rules represented by
     * this entity.
     */
    private void validateState() {
        validateNonNegative(
                fileSizeBytes,
                "File size"
        );

        validatePositive(
                widthPixels,
                "Image width"
        );

        validatePositive(
                heightPixels,
                "Image height"
        );

        validateFocalPoint(
                focalPointX,
                "Horizontal focal point"
        );

        validateFocalPoint(
                focalPointY,
                "Vertical focal point"
        );

        validatePublicAccessibility();
        validateLifecycleState();
    }

    /**
     * Ensures that a public asset has accessible alternative text
     * unless it is explicitly decorative.
     */
    private void validatePublicAccessibility() {
        if (
                Boolean.TRUE.equals(isPublic)
                        && !Boolean.TRUE.equals(isDecorative)
                        && isBlank(altText)
        ) {
            throw new IllegalStateException(
                    "A public media asset must include alternative text "
                            + "unless it is marked as decorative."
            );
        }
    }

    /**
     * Ensures archive and deletion timestamps agree with the status.
     */
    private void validateLifecycleState() {
        if (
                assetStatus == WebsiteMediaAssetStatus.ARCHIVED
                        && archivedAt == null
        ) {
            throw new IllegalStateException(
                    "An archived media asset must include an archived timestamp."
            );
        }

        if (
                assetStatus == WebsiteMediaAssetStatus.DELETED
                        && deletedAt == null
        ) {
            throw new IllegalStateException(
                    "A deleted media asset must include a deleted timestamp."
            );
        }

        if (
                assetStatus == WebsiteMediaAssetStatus.DELETED
                        && Boolean.TRUE.equals(isPublic)
        ) {
            throw new IllegalStateException(
                    "A deleted media asset cannot remain public."
            );
        }
    }

    /**
     * Validates that a focal-point percentage is between 0 and 100.
     */
    private void validateFocalPoint(
            BigDecimal value,
            String fieldName
    ) {
        if (value == null) {
            return;
        }

        if (
                value.compareTo(BigDecimal.ZERO) < 0
                        || value.compareTo(
                        BigDecimal.valueOf(100)
                ) > 0
        ) {
            throw new IllegalArgumentException(
                    fieldName + " must be between 0 and 100."
            );
        }
    }

    /**
     * Validates an optional nonnegative long value.
     */
    private void validateNonNegative(
            Long value,
            String fieldName
    ) {
        if (value != null && value < 0) {
            throw new IllegalArgumentException(
                    fieldName + " must not be negative."
            );
        }
    }

    /**
     * Validates an optional positive integer value.
     */
    private void validatePositive(
            Integer value,
            String fieldName
    ) {
        if (value != null && value <= 0) {
            throw new IllegalArgumentException(
                    fieldName + " must be greater than zero."
            );
        }
    }

    /**
     * Normalizes a required string.
     */
    private String normalizeRequired(
            String value,
            String fieldName
    ) {
        String normalizedValue = normalizeOptional(value);

        if (normalizedValue == null) {
            throw new IllegalStateException(
                    fieldName + " is required."
            );
        }

        return normalizedValue;
    }

    /**
     * Trims an optional value and converts blanks to null.
     */
    private String normalizeOptional(
            String value
    ) {
        if (value == null) {
            return null;
        }

        String normalizedValue = value.trim();

        return normalizedValue.isEmpty()
                ? null
                : normalizedValue;
    }

    /**
     * Normalizes a stable asset key to uppercase underscore format.
     */
    private String normalizeAssetKey(
            String value
    ) {
        String normalizedValue = normalizeOptional(value);

        if (normalizedValue == null) {
            return null;
        }

        return normalizedValue
                .toUpperCase(Locale.ROOT)
                .replaceAll("[^A-Z0-9]+", "_")
                .replaceAll("^_+|_+$", "");
    }

    /**
     * Normalizes a MIME type to lowercase.
     */
    private String normalizeMimeType(
            String value
    ) {
        return normalizeRequired(
                value,
                "MIME type"
        ).toLowerCase(Locale.ROOT);
    }

    /**
     * Normalizes a file extension to lowercase without a leading dot.
     */
    private String normalizeExtension(
            String value
    ) {
        String normalizedValue = normalizeOptional(value);

        if (normalizedValue == null) {
            return null;
        }

        while (normalizedValue.startsWith(".")) {
            normalizedValue =
                    normalizedValue.substring(1);
        }

        normalizedValue =
                normalizedValue.trim()
                        .toLowerCase(Locale.ROOT);

        return normalizedValue.isEmpty()
                ? null
                : normalizedValue;
    }

    /**
     * Returns true when a value is null or blank.
     */
    private boolean isBlank(
            String value
    ) {
        return value == null
                || value.trim().isEmpty();
    }
}