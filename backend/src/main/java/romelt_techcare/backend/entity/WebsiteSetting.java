package romelt_techcare.backend.entity;

import com.fasterxml.jackson.databind.JsonNode;
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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

/**
 * ================================================================
 * ROMELT TECHCARE — WEBSITE SETTING ENTITY
 * ================================================================
 *
 * Purpose:
 * Stores configurable website and CMS settings that do not require
 * dedicated relational columns or tables.
 *
 * Responsibilities:
 * - Groups related settings using settingGroup.
 * - Identifies each setting using a stable settingKey.
 * - Stores structured or primitive JSON values.
 * - Controls whether a setting may be returned publicly.
 * - Prevents sensitive settings from being marked public.
 * - Records the administrators who created and updated each setting.
 * - Supports optimistic locking for concurrent administrator updates.
 *
 * Examples:
 * - BRANDING / PRIMARY_COLOR / "#1976D2"
 * - BOOKING / PRIMARY_BUTTON_LABEL / "Book a Service"
 * - REVIEWS / REQUIRE_MODERATION / true
 * - CMS / PUBLIC_CACHE_SECONDS / 300
 *
 * JSON support:
 * settingValue may contain any valid JSON value:
 * - object
 * - array
 * - string
 * - number
 * - boolean
 * - null
 *
 * Security:
 * isSensitive settings must never be public. This rule is enforced by
 * PostgreSQL, entity validation, service validation, and public
 * repository queries.
 * ================================================================
 */
@Entity
@Table(
        name = "website_settings",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_website_setting_key",
                        columnNames = {
                                "setting_group",
                                "setting_key"
                        }
                )
        },
        indexes = {
                @Index(
                        name = "idx_website_settings_group",
                        columnList = "setting_group"
                ),
                @Index(
                        name = "idx_website_settings_public",
                        columnList =
                                "is_public, setting_group, setting_key"
                )
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebsiteSetting {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(
            name = "website_setting_id",
            nullable = false,
            updatable = false
    )
    private UUID websiteSettingId;

    /**
     * Stable normalized setting group.
     *
     * Examples:
     * GENERAL
     * BRANDING
     * BOOKING
     * REVIEWS
     * CMS
     */
    @Builder.Default
    @Column(
            name = "setting_group",
            nullable = false,
            length = 100
    )
    private String settingGroup = "GENERAL";

    /**
     * Stable normalized setting key.
     *
     * Examples:
     * PRIMARY_COLOR
     * REQUIRE_MODERATION
     * PUBLIC_CACHE_SECONDS
     */
    @Column(
            name = "setting_key",
            nullable = false,
            length = 160
    )
    private String settingKey;

    /**
     * JSON value stored in PostgreSQL JSONB.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(
            name = "setting_value",
            nullable = false,
            columnDefinition = "jsonb"
    )
    private JsonNode settingValue;

    /**
     * Administrator-facing description of the setting.
     */
    @Column(
            name = "description",
            length = 500
    )
    private String description;

    /**
     * Determines whether the setting may be returned through the
     * public website API.
     */
    @Builder.Default
    @Column(
            name = "is_public",
            nullable = false
    )
    private Boolean isPublic = false;

    /**
     * Identifies internal or confidential configuration.
     *
     * Sensitive settings must never be publicly exposed.
     */
    @Builder.Default
    @Column(
            name = "is_sensitive",
            nullable = false
    )
    private Boolean isSensitive = false;

    /**
     * Administrator who created the setting.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_admin_user_id")
    private AdminUser createdByAdminUser;

    /**
     * Administrator who most recently updated the setting.
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
     * Hibernate-managed optimistic-lock value.
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
     * Applies the complete editable setting state.
     */
    public void updateDetails(
            String newSettingGroup,
            String newSettingKey,
            JsonNode newSettingValue,
            String newDescription,
            boolean publicSetting,
            boolean sensitiveSetting,
            AdminUser administrator
    ) {
        settingGroup = newSettingGroup;
        settingKey = newSettingKey;
        settingValue = newSettingValue;
        description = newDescription;
        isSensitive = sensitiveSetting;

        /*
         * Sensitive settings are always forced private.
         */
        isPublic = sensitiveSetting
                ? false
                : publicSetting;

        updatedByAdminUser = administrator;
    }

    /**
     * Updates only the JSON value.
     */
    public void updateValue(
            JsonNode newSettingValue,
            AdminUser administrator
    ) {
        settingValue = newSettingValue;
        updatedByAdminUser = administrator;
    }

    /**
     * Updates setting visibility and sensitivity.
     */
    public void updateVisibility(
            boolean publicSetting,
            boolean sensitiveSetting,
            AdminUser administrator
    ) {
        isSensitive = sensitiveSetting;
        isPublic = sensitiveSetting
                ? false
                : publicSetting;
        updatedByAdminUser = administrator;
    }

    /**
     * Returns true when this setting may be returned publicly.
     */
    public boolean isPubliclyAvailable() {
        return Boolean.TRUE.equals(isPublic)
                && !Boolean.TRUE.equals(isSensitive);
    }

    private void initializeDefaults() {
        if (settingGroup == null || settingGroup.isBlank()) {
            settingGroup = "GENERAL";
        }

        if (isPublic == null) {
            isPublic = false;
        }

        if (isSensitive == null) {
            isSensitive = false;
        }
    }

    private void normalizeFields() {
        settingGroup = normalizeStableKey(
                settingGroup,
                "Setting group"
        );

        settingKey = normalizeStableKey(
                settingKey,
                "Setting key"
        );

        description = normalizeOptional(description);
    }

    private void validateState() {
        if (settingValue == null) {
            throw new IllegalStateException(
                    "Setting value is required."
            );
        }

        if (
                Boolean.TRUE.equals(isSensitive)
                        && Boolean.TRUE.equals(isPublic)
        ) {
            throw new IllegalStateException(
                    "A sensitive website setting cannot be public."
            );
        }
    }

    private String normalizeStableKey(
            String value,
            String fieldName
    ) {
        String normalized = normalizeOptional(value);

        if (normalized == null) {
            throw new IllegalStateException(
                    fieldName + " is required."
            );
        }

        normalized = normalized
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