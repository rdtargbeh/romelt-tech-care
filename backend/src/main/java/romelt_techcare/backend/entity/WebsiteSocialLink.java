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
import jakarta.persistence.UniqueConstraint;
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
 * ROMELT TECHCARE — WEBSITE SOCIAL LINK ENTITY
 * ================================================================
 *
 * Purpose:
 * Stores public social-media and external-profile links displayed on
 * the Romelt TechCare website.
 *
 * Responsibilities:
 * - Stores one unique link per social platform.
 * - Stores an optional administrator-defined display label.
 * - Stores the public profile URL.
 * - Stores an optional frontend icon key.
 * - Controls public visibility and display ordering.
 * - Records the administrators who created and updated the record.
 * - Supports optimistic locking for concurrent updates.
 *
 * Examples:
 * - FACEBOOK
 * - INSTAGRAM
 * - LINKEDIN
 * - YOUTUBE
 * - X
 * - TIKTOK
 * - GOOGLE_BUSINESS
 *
 * Publishing model:
 * Active social links are immediately available through the public
 * website endpoint after a successful administrator update.
 * ================================================================
 */
@Entity
@Table(
        name = "website_social_links",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_website_social_platform",
                        columnNames = "platform"
                )
        },
        indexes = {
                @Index(
                        name = "idx_website_social_public",
                        columnList = "is_active, display_order"
                )
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebsiteSocialLink {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(
            name = "social_link_id",
            nullable = false,
            updatable = false
    )
    private UUID socialLinkId;

    /**
     * Stable normalized platform identifier.
     *
     * Examples:
     * FACEBOOK
     * INSTAGRAM
     * GOOGLE_BUSINESS
     */
    @Column(
            name = "platform",
            nullable = false,
            unique = true,
            length = 50
    )
    private String platform;

    /**
     * Optional public-facing label.
     *
     * When absent, the frontend may derive a label from platform.
     */
    @Column(
            name = "label",
            length = 100
    )
    private String label;

    /**
     * Public URL of the social or external business profile.
     */
    @Column(
            name = "profile_url",
            nullable = false,
            length = 1000
    )
    private String profileUrl;

    /**
     * Optional frontend-recognized icon key.
     *
     * Examples:
     * facebook
     * instagram
     * linkedin
     * youtube
     */
    @Column(
            name = "icon_key",
            length = 100
    )
    private String iconKey;

    /**
     * Controls display ordering.
     */
    @Builder.Default
    @Column(
            name = "display_order",
            nullable = false
    )
    private Integer displayOrder = 0;

    /**
     * Controls whether the link is returned publicly.
     */
    @Builder.Default
    @Column(
            name = "is_active",
            nullable = false
    )
    private Boolean isActive = true;

    /**
     * Administrator who created the record.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_admin_user_id")
    private AdminUser createdByAdminUser;

    /**
     * Administrator who most recently updated the record.
     */
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

    /**
     * Hibernate-managed optimistic-lock version.
     */
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
     * Applies the complete editable state of the social link.
     */
    public void updateDetails(
            String newPlatform,
            String newLabel,
            String newProfileUrl,
            String newIconKey,
            Integer newDisplayOrder,
            Boolean active,
            AdminUser administrator
    ) {
        platform = newPlatform;
        label = newLabel;
        profileUrl = newProfileUrl;
        iconKey = newIconKey;
        displayOrder = newDisplayOrder;
        isActive = active;
        updatedByAdminUser = administrator;
    }

    /**
     * Activates the social link.
     */
    public void activate(
            AdminUser administrator
    ) {
        isActive = true;
        updatedByAdminUser = administrator;
    }

    /**
     * Deactivates the social link.
     */
    public void deactivate(
            AdminUser administrator
    ) {
        isActive = false;
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
        platform = normalizePlatform(platform);
        label = normalizeOptional(label);
        profileUrl = normalizeRequired(
                profileUrl,
                "Profile URL"
        );
        iconKey = normalizeIconKey(iconKey);
    }

    private void validateState() {
        if (displayOrder < 0) {
            throw new IllegalStateException(
                    "Display order must not be negative."
            );
        }

        if (
                !profileUrl.startsWith("https://")
                        && !profileUrl.startsWith("http://")
        ) {
            throw new IllegalStateException(
                    "Profile URL must begin with http:// or https://."
            );
        }
    }

    private String normalizePlatform(
            String value
    ) {
        String normalized = normalizeRequired(
                value,
                "Platform"
        );

        normalized = normalized
                .toUpperCase(Locale.ROOT)
                .replaceAll("[^A-Z0-9]+", "_")
                .replaceAll("^_+|_+$", "");

        if (normalized.isBlank()) {
            throw new IllegalStateException(
                    "Platform is required."
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