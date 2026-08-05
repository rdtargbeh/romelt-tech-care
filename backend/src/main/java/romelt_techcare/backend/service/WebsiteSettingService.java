package romelt_techcare.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import romelt_techcare.backend.entity.WebsiteSetting;

import java.util.List;
import java.util.UUID;

/**
 * ================================================================
 * ROMELT TECHCARE — WEBSITE SETTING SERVICE
 * ================================================================
 *
 * Purpose:
 * Defines business operations for managing general website and CMS
 * settings.
 *
 * Responsibilities:
 * - Creates and updates settings.
 * - Supports setting upsert by group and key.
 * - Updates JSON values independently.
 * - Updates public and sensitive states.
 * - Retrieves administrator and public settings.
 * - Enforces group-and-key uniqueness.
 * - Prevents public exposure of sensitive settings.
 * - Attributes write operations to an administrator.
 * ================================================================
 */
public interface WebsiteSettingService {

    WebsiteSetting createWebsiteSetting(
            WebsiteSetting websiteSetting,
            UUID administratorId
    );

    WebsiteSetting updateWebsiteSetting(
            UUID websiteSettingId,
            WebsiteSetting requestedUpdate,
            UUID administratorId
    );

    /**
     * Creates or updates a setting using its stable group and key.
     */
    WebsiteSetting upsertWebsiteSetting(
            WebsiteSetting requestedSetting,
            UUID administratorId
    );

    WebsiteSetting updateWebsiteSettingValue(
            UUID websiteSettingId,
            JsonNode settingValue,
            UUID administratorId
    );

    WebsiteSetting updateWebsiteSettingVisibility(
            UUID websiteSettingId,
            boolean isPublic,
            boolean isSensitive,
            UUID administratorId
    );

    WebsiteSetting getWebsiteSetting(
            UUID websiteSettingId
    );

    WebsiteSetting getWebsiteSettingByGroupAndKey(
            String settingGroup,
            String settingKey
    );

    Page<WebsiteSetting> searchWebsiteSettings(
            String keyword,
            String settingGroup,
            Boolean isPublic,
            Boolean isSensitive,
            Pageable pageable
    );

    List<WebsiteSetting> getWebsiteSettingsByGroup(
            String settingGroup
    );

    List<WebsiteSetting> getPublicWebsiteSettings();

    List<WebsiteSetting> getPublicWebsiteSettingsByGroup(
            String settingGroup
    );

    WebsiteSetting getPublicWebsiteSetting(
            String settingGroup,
            String settingKey
    );

    void deleteWebsiteSetting(
            UUID websiteSettingId,
            UUID administratorId
    );

    long countPublicWebsiteSettings();

    long countSensitiveWebsiteSettings();
}