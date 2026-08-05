package romelt_techcare.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

/**
 * ================================================================
 * ROMELT TECHCARE — WEBSITE SERVICE FEATURE ENTITY
 * ================================================================
 *
 * Purpose:
 * Stores one customer-facing feature or benefit belonging to a
 * specific WebsiteServiceVersion.
 *
 * Responsibilities:
 * - Associates a feature with one service version.
 * - Stores the feature text displayed on the public website.
 * - Stores an optional frontend-recognized icon key.
 * - Controls feature ordering and active status.
 * - Tracks the administrators responsible for creation and updates.
 * - Supports optimistic locking.
 *
 * Lifecycle:
 * Features are version-owned content. They do not belong directly to
 * WebsiteService.
 *
 * Only features attached to a DRAFT service version may be created,
 * edited, reordered, activated, deactivated, or deleted.
 *
 * Features attached to PUBLISHED or ARCHIVED versions are preserved as
 * immutable historical content.
 * ================================================================
 */
@Entity
@Table(
        name = "website_service_features",
        indexes = {
                @Index(
                        name = "idx_website_service_features_order",
                        columnList =
                                "service_version_id, is_active, display_order"
                )
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebsiteServiceFeature {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(
            name = "service_feature_id",
            nullable = false,
            updatable = false
    )
    private UUID serviceFeatureId;

    /**
     * Service version that owns this feature.
     */
    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "service_version_id",
            nullable = false,
            updatable = false
    )
    private WebsiteServiceVersion serviceVersion;

    /**
     * Publicly displayed feature text.
     */
    @Column(
            name = "feature_text",
            nullable = false,
            length = 500
    )
    private String featureText;

    /**
     * Optional frontend-recognized icon key.
     *
     * Examples:
     * check-circle
     * shield-check
     * wifi
     * laptop
     */
    @Column(
            name = "icon_key",
            length = 100
    )
    private String iconKey;

    @Builder.Default
    @Column(
            name = "display_order",
            nullable = false
    )
    private Integer displayOrder = 0;

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
     * Updates the editable feature fields.
     */
    public void updateDetails(
            String newFeatureText,
            String newIconKey,
            Integer newDisplayOrder,
            Boolean active,
            AdminUser administrator
    ) {
        featureText = newFeatureText;
        iconKey = newIconKey;
        displayOrder = newDisplayOrder;
        isActive = active;
        updatedByAdminUser = administrator;
    }

    /**
     * Updates only the active state.
     */
    public void updateStatus(
            boolean active,
            AdminUser administrator
    ) {
        isActive = active;
        updatedByAdminUser = administrator;
    }

    /**
     * Updates only the display order.
     */
    public void updateDisplayOrder(
            Integer newDisplayOrder,
            AdminUser administrator
    ) {
        displayOrder = newDisplayOrder;
        updatedByAdminUser = administrator;
    }

    private void initializeDefaults() {
        if (displayOrder == null) {
            displayOrder = 0;
        }

        if (isActive == null) {
            isActive = true;
        }
    }

    private void normalizeFields() {
        featureText = normalizeRequired(
                featureText,
                "Feature text"
        );

        iconKey = normalizeIconKey(iconKey);
    }

    private void validateState() {
        if (serviceVersion == null) {
            throw new IllegalStateException(
                    "Service version is required."
            );
        }

        if (featureText.length() > 500) {
            throw new IllegalStateException(
                    "Feature text must not exceed 500 characters."
            );
        }

        if (iconKey != null && iconKey.length() > 100) {
            throw new IllegalStateException(
                    "Icon key must not exceed 100 characters."
            );
        }

        if (displayOrder == null || displayOrder < 0) {
            throw new IllegalStateException(
                    "Display order must not be negative."
            );
        }
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