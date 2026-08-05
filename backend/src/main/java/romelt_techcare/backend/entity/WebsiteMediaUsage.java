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
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import romelt_techcare.backend.enums.WebsiteMediaUsageResourceType;

import java.time.Instant;
import java.util.UUID;

/**
 * ================================================================
 * ROMELT TECHCARE — WEBSITE MEDIA USAGE ENTITY
 * ================================================================
 *
 * Purpose:
 * Tracks where a WebsiteMediaAsset is referenced throughout the
 * Romelt TechCare website and content-management system.
 *
 * Responsibilities:
 * - Connects one media asset to a CMS or website resource.
 * - Identifies the exact resource field using the asset.
 * - Prevents deletion of media that remains in active use.
 * - Supports administrator usage reports.
 * - Supports future media replacement and cleanup workflows.
 *
 * Examples:
 * - PRIMARY_LOGO on a BusinessProfile
 * - HERO_IMAGE on a WebsitePageVersion
 * - CARD_IMAGE on a WebsiteServiceVersion
 * - SOCIAL_IMAGE on a WebsitePageVersion
 * - CUSTOMER_PHOTO on a CustomerReview
 *
 * Generic resource relationship:
 * resourceId identifies the owning record, while resourceType
 * identifies the table or domain to which that record belongs.
 *
 * Important:
 * This is intentionally a generic reference table. JPA cannot define
 * a normal foreign key from resourceId to several different entity
 * tables. Resource existence must therefore be validated by the
 * corresponding application service before creating a usage record.
 * ================================================================
 */
@Entity
@Table(
        name = "website_media_usages",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_website_media_usage",
                        columnNames = {
                                "media_asset_id",
                                "resource_type",
                                "resource_id",
                                "usage_field"
                        }
                )
        },
        indexes = {
                @Index(
                        name = "idx_website_media_usage_asset",
                        columnList = "media_asset_id"
                ),
                @Index(
                        name = "idx_website_media_usage_resource",
                        columnList = "resource_type, resource_id"
                )
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebsiteMediaUsage {

    /**
     * Primary identifier for the usage record.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(
            name = "media_usage_id",
            nullable = false,
            updatable = false
    )
    private UUID mediaUsageId;

    /**
     * Media asset being referenced.
     */
    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "media_asset_id",
            nullable = false
    )
    private WebsiteMediaAsset mediaAsset;

    /**
     * Type of resource using the asset.
     */
    @Enumerated(EnumType.STRING)
    @Column(
            name = "resource_type",
            nullable = false,
            length = 60
    )
    private WebsiteMediaUsageResourceType resourceType;

    /**
     * Identifier of the specific resource using the asset.
     *
     * Examples:
     * - businessProfileId
     * - pageVersionId
     * - serviceVersionId
     * - customerReviewId
     */
    @Column(
            name = "resource_id",
            nullable = false
    )
    private UUID resourceId;

    /**
     * Name of the exact field or logical placement using the asset.
     *
     * Examples:
     * - PRIMARY_LOGO
     * - HERO_IMAGE
     * - CARD_IMAGE
     * - SOCIAL_IMAGE
     * - CUSTOMER_PHOTO
     */
    @Column(
            name = "usage_field",
            nullable = false,
            length = 120
    )
    private String usageField;

    /**
     * Optional administrator-facing explanation of the usage.
     */
    @Column(
            name = "usage_description",
            length = 500
    )
    private String usageDescription;

    /**
     * Timestamp when the media usage was registered.
     */
    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private Instant createdAt;

    /**
     * Initializes and validates the usage record before insertion.
     */
    @PrePersist
    protected void onCreate() {
        normalizeFields();
        validateState();

        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    /**
     * Normalizes human-entered values.
     */
    private void normalizeFields() {
        usageField = normalizeRequired(
                usageField,
                "Media usage field"
        );

        usageDescription = normalizeOptional(
                usageDescription
        );
    }

    /**
     * Validates required relationships and identifiers.
     */
    private void validateState() {
        if (mediaAsset == null) {
            throw new IllegalStateException(
                    "Website media asset is required."
            );
        }

        if (resourceType == null) {
            throw new IllegalStateException(
                    "Media usage resource type is required."
            );
        }

        if (resourceId == null) {
            throw new IllegalStateException(
                    "Media usage resource ID is required."
            );
        }
    }

    /**
     * Normalizes a required value.
     */
    private String normalizeRequired(
            String value,
            String fieldName
    ) {
        String normalizedValue =
                normalizeOptional(value);

        if (normalizedValue == null) {
            throw new IllegalStateException(
                    fieldName + " is required."
            );
        }

        return normalizedValue;
    }

    /**
     * Trims an optional value and converts blank values to null.
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
}