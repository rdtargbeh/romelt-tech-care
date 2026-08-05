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
import romelt_techcare.backend.enums.WebsiteNavigationDestinationType;
import romelt_techcare.backend.enums.WebsiteNavigationLocation;
import romelt_techcare.backend.enums.WebsiteNavigationTargetBehavior;

import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

/**
 * ================================================================
 * ROMELT TECHCARE — WEBSITE NAVIGATION ITEM ENTITY
 * ================================================================
 *
 * Purpose:
 * Stores administrator-managed navigation links shown in the public
 * website header, mobile menu, and footer.
 *
 * Responsibilities:
 * - Assigns each item to a supported navigation location.
 * - Stores a stable item key and public label.
 * - Supports links to a managed WebsitePage.
 * - Supports internal routes, external URLs, anchors, and actions.
 * - Controls target behavior, icon key, display order, and visibility.
 * - Supports soft deletion and restoration.
 * - Records administrator attribution.
 * - Supports optimistic locking.
 *
 * Destination behavior:
 * A navigation item must reference either:
 * - a WebsitePage, or
 * - a nonblank destinationUrl.
 *
 * When a WebsitePage is assigned to an INTERNAL_ROUTE item, the page's
 * routePath becomes the authoritative public destination.
 * ================================================================
 */
@Entity
@Table(
        name = "website_navigation_items",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_website_navigation_item_key",
                        columnNames = {
                                "navigation_location",
                                "item_key"
                        }
                )
        },
        indexes = {
                @Index(
                        name = "idx_website_navigation_public",
                        columnList =
                                "navigation_location, is_visible, display_order"
                )
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebsiteNavigationItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(
            name = "navigation_item_id",
            nullable = false,
            updatable = false
    )
    private UUID navigationItemId;

    /**
     * Optional managed website page used as the navigation destination.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "website_page_id")
    private WebsitePage websitePage;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "navigation_location",
            nullable = false,
            length = 40
    )
    private WebsiteNavigationLocation navigationLocation;

    /**
     * Stable location-scoped item identifier.
     */
    @Column(
            name = "item_key",
            nullable = false,
            length = 120
    )
    private String itemKey;

    @Column(
            name = "label",
            nullable = false,
            length = 180
    )
    private String label;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(
            name = "destination_type",
            nullable = false,
            length = 30
    )
    private WebsiteNavigationDestinationType destinationType =
            WebsiteNavigationDestinationType.INTERNAL_ROUTE;

    /**
     * Explicit destination value.
     *
     * Examples:
     * - /services
     * - https://example.com
     * - #pricing
     * - OPEN_BOOKING
     */
    @Column(
            name = "destination_url",
            length = 1500
    )
    private String destinationUrl;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(
            name = "target_behavior",
            nullable = false,
            length = 30
    )
    private WebsiteNavigationTargetBehavior targetBehavior =
            WebsiteNavigationTargetBehavior.SAME_WINDOW;

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
            name = "is_visible",
            nullable = false
    )
    private Boolean isVisible = true;

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
     * Applies the complete editable state.
     */
    public void updateDetails(
            WebsitePage newWebsitePage,
            WebsiteNavigationLocation newLocation,
            String newItemKey,
            String newLabel,
            WebsiteNavigationDestinationType newDestinationType,
            String newDestinationUrl,
            WebsiteNavigationTargetBehavior newTargetBehavior,
            String newIconKey,
            Integer newDisplayOrder,
            Boolean visible,
            AdminUser administrator
    ) {
        ensureNotDeleted();

        websitePage = newWebsitePage;
        navigationLocation = newLocation;
        itemKey = newItemKey;
        label = newLabel;
        destinationType = newDestinationType;
        destinationUrl = newDestinationUrl;
        targetBehavior = newTargetBehavior;
        iconKey = newIconKey;
        displayOrder = newDisplayOrder;
        isVisible = visible;
        updatedByAdminUser = administrator;
    }

    public void updateVisibility(
            boolean visible,
            AdminUser administrator
    ) {
        ensureNotDeleted();

        isVisible = visible;
        updatedByAdminUser = administrator;
    }

    public void softDelete(
            AdminUser administrator
    ) {
        if (deletedAt != null) {
            return;
        }

        isVisible = false;
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
        isVisible = false;
        updatedByAdminUser = administrator;
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }

    /**
     * Returns the destination that should be sent to the public client.
     */
    public String resolveDestination() {
        if (
                destinationType
                        == WebsiteNavigationDestinationType.INTERNAL_ROUTE
                        && websitePage != null
        ) {
            return websitePage.getRoutePath();
        }

        return destinationUrl;
    }

    public boolean isPubliclyAvailable() {
        if (
                deletedAt != null
                        || !Boolean.TRUE.equals(isVisible)
        ) {
            return false;
        }

        if (
                destinationType
                        == WebsiteNavigationDestinationType.INTERNAL_ROUTE
                        && websitePage != null
        ) {
            return !websitePage.isDeleted()
                    && Boolean.TRUE.equals(
                    websitePage.getIsActive()
            );
        }

        return resolveDestination() != null;
    }

    private void initializeDefaults() {
        if (destinationType == null) {
            destinationType =
                    WebsiteNavigationDestinationType.INTERNAL_ROUTE;
        }

        if (targetBehavior == null) {
            targetBehavior =
                    WebsiteNavigationTargetBehavior.SAME_WINDOW;
        }

        if (displayOrder == null) {
            displayOrder = 0;
        }

        if (isVisible == null) {
            isVisible = true;
        }
    }

    private void normalizeFields() {
        itemKey = normalizeStableKey(
                itemKey,
                "Navigation item key"
        );

        label = normalizeRequired(
                label,
                "Navigation label"
        );

        destinationUrl = normalizeOptional(destinationUrl);
        iconKey = normalizeIconKey(iconKey);
    }

    private void validateState() {
        if (navigationLocation == null) {
            throw new IllegalStateException(
                    "Navigation location is required."
            );
        }

        if (destinationType == null) {
            throw new IllegalStateException(
                    "Navigation destination type is required."
            );
        }

        if (targetBehavior == null) {
            throw new IllegalStateException(
                    "Navigation target behavior is required."
            );
        }

        if (displayOrder < 0) {
            throw new IllegalStateException(
                    "Navigation display order must not be negative."
            );
        }

        if (
                websitePage == null
                        && destinationUrl == null
        ) {
            throw new IllegalStateException(
                    "A navigation item requires a website page or "
                            + "destination URL."
            );
        }

        if (
                deletedAt != null
                        && Boolean.TRUE.equals(isVisible)
        ) {
            throw new IllegalStateException(
                    "A deleted navigation item cannot remain visible."
            );
        }
    }

    private void ensureNotDeleted() {
        if (deletedAt != null) {
            throw new IllegalStateException(
                    "A deleted navigation item cannot be modified."
            );
        }
    }

    private String normalizeStableKey(
            String value,
            String fieldName
    ) {
        String normalized = normalizeRequired(
                value,
                fieldName
        )
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