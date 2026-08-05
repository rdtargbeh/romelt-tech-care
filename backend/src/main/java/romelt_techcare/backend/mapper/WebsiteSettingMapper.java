package romelt_techcare.backend.mapper;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;
import romelt_techcare.backend.dto.PublicWebsiteSettingResponse;
import romelt_techcare.backend.dto.PublicWebsiteSettingsGroupResponse;
import romelt_techcare.backend.dto.WebsiteSettingCreateRequest;
import romelt_techcare.backend.dto.WebsiteSettingResponse;
import romelt_techcare.backend.dto.WebsiteSettingUpdateRequest;
import romelt_techcare.backend.entity.AdminUser;
import romelt_techcare.backend.entity.WebsiteSetting;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * ================================================================
 * ROMELT TECHCARE — WEBSITE SETTING MAPPER
 * ================================================================
 *
 * Purpose:
 * Converts website-setting request DTOs into service-compatible
 * entities and persisted entities into administrator and public
 * responses.
 *
 * Responsibilities:
 * - Excludes persistence-owned fields from request mappings.
 * - Prevents direct JPA entity serialization.
 * - Maps administrator attribution safely.
 * - Produces minimal public setting responses.
 * - Produces compact public setting-group maps.
 * ================================================================
 */
@Component
public class WebsiteSettingMapper {

    /**
     * Converts a create request into a new unsaved entity.
     */
    public WebsiteSetting toEntity(
            WebsiteSettingCreateRequest request
    ) {
        if (request == null) {
            return null;
        }

        boolean sensitive =
                Boolean.TRUE.equals(request.isSensitive());

        return WebsiteSetting.builder()
                .settingGroup(
                        request.settingGroup() == null
                                || request.settingGroup().isBlank()
                                ? "GENERAL"
                                : request.settingGroup()
                )
                .settingKey(request.settingKey())
                .settingValue(request.settingValue())
                .description(request.description())
                .isSensitive(sensitive)
                .isPublic(
                        !sensitive
                                && Boolean.TRUE.equals(
                                request.isPublic()
                        )
                )
                .build();
    }

    /**
     * Converts a full update request into a detached update entity.
     */
    public WebsiteSetting toUpdateEntity(
            WebsiteSettingUpdateRequest request
    ) {
        if (request == null) {
            return null;
        }

        boolean sensitive =
                Boolean.TRUE.equals(request.isSensitive());

        return WebsiteSetting.builder()
                .settingGroup(request.settingGroup())
                .settingKey(request.settingKey())
                .settingValue(request.settingValue())
                .description(request.description())
                .isSensitive(sensitive)
                .isPublic(
                        !sensitive
                                && Boolean.TRUE.equals(
                                request.isPublic()
                        )
                )
                .build();
    }

    /**
     * Converts a setting into an administrator response.
     */
    public WebsiteSettingResponse toResponse(
            WebsiteSetting setting
    ) {
        if (setting == null) {
            return null;
        }

        AdminUser createdBy =
                setting.getCreatedByAdminUser();

        AdminUser updatedBy =
                setting.getUpdatedByAdminUser();

        return new WebsiteSettingResponse(
                setting.getWebsiteSettingId(),
                setting.getSettingGroup(),
                setting.getSettingKey(),
                setting.getSettingValue(),
                setting.getDescription(),
                setting.getIsPublic(),
                setting.getIsSensitive(),
                setting.isPubliclyAvailable(),
                getAdminUserId(createdBy),
                getAdminUserDisplayName(createdBy),
                getAdminUserId(updatedBy),
                getAdminUserDisplayName(updatedBy),
                setting.getCreatedAt(),
                setting.getUpdatedAt(),
                setting.getRowVersion()
        );
    }

    /**
     * Converts a verified public setting into a public response.
     */
    public PublicWebsiteSettingResponse toPublicResponse(
            WebsiteSetting setting
    ) {
        if (setting == null) {
            return null;
        }

        if (!setting.isPubliclyAvailable()) {
            throw new IllegalArgumentException(
                    "Only public non-sensitive settings may be mapped "
                            + "to a public response."
            );
        }

        return new PublicWebsiteSettingResponse(
                setting.getSettingGroup(),
                setting.getSettingKey(),
                setting.getSettingValue(),
                setting.getDescription()
        );
    }

    /**
     * Converts a list of one group's settings into a compact map.
     */
    public PublicWebsiteSettingsGroupResponse
    toPublicGroupResponse(
            String settingGroup,
            List<WebsiteSetting> settings
    ) {
        Map<String, JsonNode> values =
                new LinkedHashMap<>();

        if (settings != null) {
            for (WebsiteSetting setting : settings) {
                if (
                        setting != null
                                && setting.isPubliclyAvailable()
                ) {
                    values.put(
                            setting.getSettingKey(),
                            setting.getSettingValue()
                    );
                }
            }
        }

        return new PublicWebsiteSettingsGroupResponse(
                settingGroup,
                Map.copyOf(values)
        );
    }

    private UUID getAdminUserId(
            AdminUser adminUser
    ) {
        return adminUser == null
                ? null
                : adminUser.getAdminUserId();
    }

    private String getAdminUserDisplayName(
            AdminUser adminUser
    ) {
        if (adminUser == null) {
            return null;
        }

        String firstName =
                normalizeOptional(adminUser.getFirstName());

        String lastName =
                normalizeOptional(adminUser.getLastName());

        if (firstName != null && lastName != null) {
            return firstName + " " + lastName;
        }

        if (firstName != null) {
            return firstName;
        }

        if (lastName != null) {
            return lastName;
        }

        return normalizeOptional(adminUser.getEmail());
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