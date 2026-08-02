package romelt_techcare.backend.service.implement;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import romelt_techcare.backend.entity.AdminUser;
import romelt_techcare.backend.entity.WebsiteSetting;
import romelt_techcare.backend.repository.AdminUserRepository;
import romelt_techcare.backend.repository.WebsiteSettingRepository;
import romelt_techcare.backend.service.WebsiteSettingService;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * ================================================================
 * ROMELT TECHCARE — WEBSITE SETTING SERVICE IMPLEMENTATION
 * ================================================================
 *
 * Purpose:
 * Implements production business rules for general website and CMS
 * settings.
 *
 * Responsibilities:
 * - Creates, updates, and upserts settings.
 * - Normalizes stable group and key values.
 * - Preserves group-and-key uniqueness.
 * - Stores primitive and structured JSON values.
 * - Prevents sensitive settings from becoming public.
 * - Supports administrator search and public retrieval.
 * - Uses pessimistic locking for write operations.
 * - Records the administrator responsible for each write.
 *
 * Publishing behavior:
 * Public non-sensitive settings become available immediately after a
 * successful database update.
 *
 * Sensitive-data behavior:
 * A sensitive setting is always forced private. Public queries also
 * require isSensitive to be false, providing defense in depth.
 *
 * Deletion behavior:
 * The supplied schema does not include soft-deletion fields.
 * deleteWebsiteSetting therefore permanently removes the record.
 * ================================================================
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WebsiteSettingServiceImplementation
        implements WebsiteSettingService {

    private static final String DEFAULT_SETTING_GROUP =
            "GENERAL";

    private final WebsiteSettingRepository
            websiteSettingRepository;

    private final AdminUserRepository adminUserRepository;

    @Override
    @Transactional
    public WebsiteSetting createWebsiteSetting(
            WebsiteSetting websiteSetting,
            UUID administratorId
    ) {
        if (websiteSetting == null) {
            throw badRequest(
                    "Website setting information is required."
            );
        }

        AdminUser administrator =
                getRequiredAdministrator(administratorId);

        String normalizedGroup =
                normalizeSettingGroup(
                        websiteSetting.getSettingGroup()
                );

        String normalizedKey =
                normalizeSettingKey(
                        websiteSetting.getSettingKey()
                );

        validateSettingValue(
                websiteSetting.getSettingValue()
        );

        validateTextLengths(websiteSetting);

        if (
                websiteSettingRepository
                        .existsBySettingGroupAndSettingKey(
                                normalizedGroup,
                                normalizedKey
                        )
        ) {
            throw conflict(
                    "A website setting already exists for this group "
                            + "and key."
            );
        }

        boolean sensitive =
                Boolean.TRUE.equals(
                        websiteSetting.getIsSensitive()
                );

        WebsiteSetting settingToCreate =
                WebsiteSetting.builder()
                        .settingGroup(normalizedGroup)
                        .settingKey(normalizedKey)
                        .settingValue(
                                websiteSetting.getSettingValue()
                        )
                        .description(
                                normalizeOptional(
                                        websiteSetting.getDescription()
                                )
                        )
                        .isSensitive(sensitive)
                        .isPublic(
                                !sensitive
                                        && Boolean.TRUE.equals(
                                        websiteSetting.getIsPublic()
                                )
                        )
                        .createdByAdminUser(administrator)
                        .updatedByAdminUser(administrator)
                        .build();

        return saveWebsiteSetting(
                settingToCreate,
                "A website setting already exists for this group "
                        + "and key."
        );
    }

    @Override
    @Transactional
    public WebsiteSetting updateWebsiteSetting(
            UUID websiteSettingId,
            WebsiteSetting requestedUpdate,
            UUID administratorId
    ) {
        requireIdentifier(
                websiteSettingId,
                "Website setting ID"
        );

        if (requestedUpdate == null) {
            throw badRequest(
                    "Updated website setting information is required."
            );
        }

        AdminUser administrator =
                getRequiredAdministrator(administratorId);

        WebsiteSetting existingSetting =
                getWebsiteSettingForUpdate(
                        websiteSettingId
                );

        String normalizedGroup =
                normalizeSettingGroup(
                        requestedUpdate.getSettingGroup()
                );

        String normalizedKey =
                normalizeSettingKey(
                        requestedUpdate.getSettingKey()
                );

        validateSettingValue(
                requestedUpdate.getSettingValue()
        );

        validateTextLengths(requestedUpdate);

        if (
                websiteSettingRepository
                        .existsBySettingGroupAndSettingKeyAndWebsiteSettingIdNot(
                                normalizedGroup,
                                normalizedKey,
                                websiteSettingId
                        )
        ) {
            throw conflict(
                    "Another website setting already uses this group "
                            + "and key."
            );
        }

        boolean sensitive =
                Boolean.TRUE.equals(
                        requestedUpdate.getIsSensitive()
                );

        existingSetting.updateDetails(
                normalizedGroup,
                normalizedKey,
                requestedUpdate.getSettingValue(),
                normalizeOptional(
                        requestedUpdate.getDescription()
                ),
                Boolean.TRUE.equals(
                        requestedUpdate.getIsPublic()
                ),
                sensitive,
                administrator
        );

        return saveWebsiteSetting(
                existingSetting,
                "Another website setting already uses this group "
                        + "and key."
        );
    }

    @Override
    @Transactional
    public WebsiteSetting upsertWebsiteSetting(
            WebsiteSetting requestedSetting,
            UUID administratorId
    ) {
        if (requestedSetting == null) {
            throw badRequest(
                    "Website setting information is required."
            );
        }

        AdminUser administrator =
                getRequiredAdministrator(administratorId);

        String normalizedGroup =
                normalizeSettingGroup(
                        requestedSetting.getSettingGroup()
                );

        String normalizedKey =
                normalizeSettingKey(
                        requestedSetting.getSettingKey()
                );

        validateSettingValue(
                requestedSetting.getSettingValue()
        );

        validateTextLengths(requestedSetting);

        WebsiteSetting existingSetting =
                websiteSettingRepository
                        .findByGroupAndKeyForUpdate(
                                normalizedGroup,
                                normalizedKey
                        )
                        .orElse(null);

        boolean sensitive =
                Boolean.TRUE.equals(
                        requestedSetting.getIsSensitive()
                );

        if (existingSetting == null) {
            WebsiteSetting newSetting =
                    WebsiteSetting.builder()
                            .settingGroup(normalizedGroup)
                            .settingKey(normalizedKey)
                            .settingValue(
                                    requestedSetting.getSettingValue()
                            )
                            .description(
                                    normalizeOptional(
                                            requestedSetting
                                                    .getDescription()
                                    )
                            )
                            .isSensitive(sensitive)
                            .isPublic(
                                    !sensitive
                                            && Boolean.TRUE.equals(
                                            requestedSetting
                                                    .getIsPublic()
                                    )
                            )
                            .createdByAdminUser(administrator)
                            .updatedByAdminUser(administrator)
                            .build();

            return saveWebsiteSetting(
                    newSetting,
                    "The website setting could not be created because "
                            + "the group and key are already in use."
            );
        }

        existingSetting.updateDetails(
                normalizedGroup,
                normalizedKey,
                requestedSetting.getSettingValue(),
                normalizeOptional(
                        requestedSetting.getDescription()
                ),
                Boolean.TRUE.equals(
                        requestedSetting.getIsPublic()
                ),
                sensitive,
                administrator
        );

        return saveWebsiteSetting(
                existingSetting,
                "The website setting could not be updated."
        );
    }

    @Override
    @Transactional
    public WebsiteSetting updateWebsiteSettingValue(
            UUID websiteSettingId,
            JsonNode settingValue,
            UUID administratorId
    ) {
        validateSettingValue(settingValue);

        AdminUser administrator =
                getRequiredAdministrator(administratorId);

        WebsiteSetting websiteSetting =
                getWebsiteSettingForUpdate(
                        websiteSettingId
                );

        websiteSetting.updateValue(
                settingValue,
                administrator
        );

        return websiteSettingRepository.save(
                websiteSetting
        );
    }

    @Override
    @Transactional
    public WebsiteSetting updateWebsiteSettingVisibility(
            UUID websiteSettingId,
            boolean isPublic,
            boolean isSensitive,
            UUID administratorId
    ) {
        AdminUser administrator =
                getRequiredAdministrator(administratorId);

        WebsiteSetting websiteSetting =
                getWebsiteSettingForUpdate(
                        websiteSettingId
                );

        websiteSetting.updateVisibility(
                isPublic,
                isSensitive,
                administrator
        );

        return websiteSettingRepository.save(
                websiteSetting
        );
    }

    @Override
    public WebsiteSetting getWebsiteSetting(
            UUID websiteSettingId
    ) {
        requireIdentifier(
                websiteSettingId,
                "Website setting ID"
        );

        return websiteSettingRepository
                .findByWebsiteSettingId(
                        websiteSettingId
                )
                .orElseThrow(() -> notFound(
                        "Website setting was not found."
                ));
    }

    @Override
    public WebsiteSetting getWebsiteSettingByGroupAndKey(
            String settingGroup,
            String settingKey
    ) {
        String normalizedGroup =
                normalizeSettingGroup(settingGroup);

        String normalizedKey =
                normalizeSettingKey(settingKey);

        return websiteSettingRepository
                .findBySettingGroupAndSettingKey(
                        normalizedGroup,
                        normalizedKey
                )
                .orElseThrow(() -> notFound(
                        "Website setting was not found."
                ));
    }

    @Override
    public Page<WebsiteSetting> searchWebsiteSettings(
            String keyword,
            String settingGroup,
            Boolean isPublic,
            Boolean isSensitive,
            Pageable pageable
    ) {
        requirePageable(pageable);

        return websiteSettingRepository.searchWebsiteSettings(
                normalizeOptional(keyword),
                normalizeOptionalGroup(settingGroup),
                isPublic,
                isSensitive,
                pageable
        );
    }

    @Override
    public List<WebsiteSetting> getWebsiteSettingsByGroup(
            String settingGroup
    ) {
        String normalizedGroup =
                normalizeSettingGroup(settingGroup);

        return websiteSettingRepository
                .findAllBySettingGroupOrderBySettingKeyAsc(
                        normalizedGroup
                );
    }

    @Override
    public List<WebsiteSetting> getPublicWebsiteSettings() {
        return websiteSettingRepository
                .findAllByIsPublicTrueAndIsSensitiveFalseOrderBySettingGroupAscSettingKeyAsc();
    }

    @Override
    public List<WebsiteSetting>
    getPublicWebsiteSettingsByGroup(
            String settingGroup
    ) {
        String normalizedGroup =
                normalizeSettingGroup(settingGroup);

        return websiteSettingRepository
                .findAllBySettingGroupAndIsPublicTrueAndIsSensitiveFalseOrderBySettingKeyAsc(
                        normalizedGroup
                );
    }

    @Override
    public WebsiteSetting getPublicWebsiteSetting(
            String settingGroup,
            String settingKey
    ) {
        String normalizedGroup =
                normalizeSettingGroup(settingGroup);

        String normalizedKey =
                normalizeSettingKey(settingKey);

        return websiteSettingRepository
                .findBySettingGroupAndSettingKeyAndIsPublicTrueAndIsSensitiveFalse(
                        normalizedGroup,
                        normalizedKey
                )
                .orElseThrow(() -> notFound(
                        "Public website setting was not found."
                ));
    }

    @Override
    @Transactional
    public void deleteWebsiteSetting(
            UUID websiteSettingId,
            UUID administratorId
    ) {
        getRequiredAdministrator(administratorId);

        WebsiteSetting websiteSetting =
                getWebsiteSettingForUpdate(
                        websiteSettingId
                );

        websiteSettingRepository.delete(
                websiteSetting
        );
    }

    @Override
    public long countPublicWebsiteSettings() {
        return websiteSettingRepository
                .countByIsPublicTrueAndIsSensitiveFalse();
    }

    @Override
    public long countSensitiveWebsiteSettings() {
        return websiteSettingRepository
                .countByIsSensitiveTrue();
    }

    private WebsiteSetting getWebsiteSettingForUpdate(
            UUID websiteSettingId
    ) {
        requireIdentifier(
                websiteSettingId,
                "Website setting ID"
        );

        return websiteSettingRepository
                .findByIdForUpdate(websiteSettingId)
                .orElseThrow(() -> notFound(
                        "Website setting was not found."
                ));
    }

    private void validateSettingValue(
            JsonNode settingValue
    ) {
        if (settingValue == null) {
            throw badRequest(
                    "Setting value is required."
            );
        }
    }

    private void validateTextLengths(
            WebsiteSetting setting
    ) {
        if (
                setting.getSettingGroup() != null
                        && setting
                        .getSettingGroup()
                        .trim()
                        .length() > 100
        ) {
            throw badRequest(
                    "Setting group must not exceed 100 characters."
            );
        }

        if (
                setting.getSettingKey() != null
                        && setting
                        .getSettingKey()
                        .trim()
                        .length() > 160
        ) {
            throw badRequest(
                    "Setting key must not exceed 160 characters."
            );
        }

        if (
                setting.getDescription() != null
                        && setting
                        .getDescription()
                        .trim()
                        .length() > 500
        ) {
            throw badRequest(
                    "Description must not exceed 500 characters."
            );
        }
    }

    private String normalizeSettingGroup(
            String settingGroup
    ) {
        String normalized =
                normalizeOptional(settingGroup);

        if (normalized == null) {
            normalized = DEFAULT_SETTING_GROUP;
        }

        normalized = normalizeStableKey(
                normalized,
                "Setting group"
        );

        if (normalized.length() > 100) {
            throw badRequest(
                    "Setting group must not exceed 100 characters."
            );
        }

        return normalized;
    }

    private String normalizeOptionalGroup(
            String settingGroup
    ) {
        String normalized =
                normalizeOptional(settingGroup);

        if (normalized == null) {
            return null;
        }

        return normalizeSettingGroup(normalized);
    }

    private String normalizeSettingKey(
            String settingKey
    ) {
        String normalized = normalizeStableKey(
                settingKey,
                "Setting key"
        );

        if (normalized.length() > 160) {
            throw badRequest(
                    "Setting key must not exceed 160 characters."
            );
        }

        return normalized;
    }

    private String normalizeStableKey(
            String value,
            String fieldName
    ) {
        String normalized =
                normalizeOptional(value);

        if (normalized == null) {
            throw badRequest(
                    fieldName + " is required."
            );
        }

        normalized = normalized
                .toUpperCase(Locale.ROOT)
                .replaceAll("[^A-Z0-9]+", "_")
                .replaceAll("^_+|_+$", "");

        if (normalized.isBlank()) {
            throw badRequest(
                    fieldName + " is required."
            );
        }

        return normalized;
    }

    private AdminUser getRequiredAdministrator(
            UUID administratorId
    ) {
        requireIdentifier(
                administratorId,
                "Administrator ID"
        );

        return adminUserRepository
                .findById(administratorId)
                .orElseThrow(() -> notFound(
                        "Administrator account was not found."
                ));
    }

    private WebsiteSetting saveWebsiteSetting(
            WebsiteSetting websiteSetting,
            String conflictMessage
    ) {
        try {
            return websiteSettingRepository
                    .saveAndFlush(websiteSetting);
        } catch (DataIntegrityViolationException exception) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    conflictMessage,
                    exception
            );
        }
    }

    private void requirePageable(
            Pageable pageable
    ) {
        if (pageable == null) {
            throw badRequest(
                    "Pagination information is required."
            );
        }
    }

    private void requireIdentifier(
            UUID identifier,
            String fieldName
    ) {
        if (identifier == null) {
            throw badRequest(
                    fieldName + " is required."
            );
        }
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

    private ResponseStatusException badRequest(
            String message
    ) {
        return new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                message
        );
    }

    private ResponseStatusException notFound(
            String message
    ) {
        return new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                message
        );
    }

    private ResponseStatusException conflict(
            String message
    ) {
        return new ResponseStatusException(
                HttpStatus.CONFLICT,
                message
        );
    }
}