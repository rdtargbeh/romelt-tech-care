package romelt_techcare.backend.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import romelt_techcare.backend.api.ApiResponse;
import romelt_techcare.backend.dto.PublicWebsiteSettingResponse;
import romelt_techcare.backend.dto.PublicWebsiteSettingsGroupResponse;
import romelt_techcare.backend.mapper.WebsiteSettingMapper;
import romelt_techcare.backend.service.WebsiteSettingService;

import java.time.Duration;
import java.util.List;

/**
 * ================================================================
 * ROMELT TECHCARE — PUBLIC WEBSITE SETTING CONTROLLER
 * ================================================================
 *
 * Purpose:
 * Exposes explicitly public, non-sensitive settings to the Romelt
 * TechCare React website.
 *
 * Responsibilities:
 * - Returns all public settings.
 * - Returns public settings by group.
 * - Returns one public setting by group and key.
 * - Returns compact group-based key-value responses.
 * - Prevents sensitive settings from being exposed.
 *
 * Base endpoint:
 * /api/v1/public/website-settings
 *
 * Security:
 * Repository queries require:
 * - isPublic = true
 * - isSensitive = false
 * ================================================================
 */
@RestController
@RequestMapping("/api/v1/public/website-settings")
@RequiredArgsConstructor
public class PublicWebsiteSettingController {

    private static final Duration PUBLIC_CACHE_DURATION =
            Duration.ofMinutes(5);

    private final WebsiteSettingService websiteSettingService;
    private final WebsiteSettingMapper websiteSettingMapper;

    /**
     * Retrieves all public non-sensitive settings.
     *
     * GET /api/v1/public/website-settings
     */
    @GetMapping
    public ResponseEntity<
            ApiResponse<List<PublicWebsiteSettingResponse>>
            > getPublicWebsiteSettings(
            HttpServletRequest httpRequest
    ) {
        List<PublicWebsiteSettingResponse> response =
                websiteSettingService
                        .getPublicWebsiteSettings()
                        .stream()
                        .map(
                                websiteSettingMapper
                                        ::toPublicResponse
                        )
                        .toList();

        return ResponseEntity.ok()
                .cacheControl(
                        CacheControl
                                .maxAge(PUBLIC_CACHE_DURATION)
                                .cachePublic()
                )
                .body(
                        ApiResponse.success(
                                "Public website settings retrieved successfully.",
                                response,
                                httpRequest.getRequestURI()
                        )
                );
    }

    /**
     * Retrieves public settings in one group as a compact map.
     *
     * GET /api/v1/public/website-settings/group/{settingGroup}
     */
    @GetMapping("/group/{settingGroup}")
    public ResponseEntity<
            ApiResponse<PublicWebsiteSettingsGroupResponse>
            > getPublicWebsiteSettingsByGroup(
            @PathVariable
            String settingGroup,

            HttpServletRequest httpRequest
    ) {
        PublicWebsiteSettingsGroupResponse response =
                websiteSettingMapper.toPublicGroupResponse(
                        settingGroup,
                        websiteSettingService
                                .getPublicWebsiteSettingsByGroup(
                                        settingGroup
                                )
                );

        return ResponseEntity.ok()
                .cacheControl(
                        CacheControl
                                .maxAge(PUBLIC_CACHE_DURATION)
                                .cachePublic()
                )
                .body(
                        ApiResponse.success(
                                "Public website setting group retrieved successfully.",
                                response,
                                httpRequest.getRequestURI()
                        )
                );
    }

    /**
     * Retrieves one public non-sensitive setting.
     *
     * GET /api/v1/public/website-settings/{group}/{key}
     */
    @GetMapping("/{settingGroup}/{settingKey}")
    public ResponseEntity<
            ApiResponse<PublicWebsiteSettingResponse>
            > getPublicWebsiteSetting(
            @PathVariable
            String settingGroup,

            @PathVariable
            String settingKey,

            HttpServletRequest httpRequest
    ) {
        PublicWebsiteSettingResponse response =
                websiteSettingMapper.toPublicResponse(
                        websiteSettingService
                                .getPublicWebsiteSetting(
                                        settingGroup,
                                        settingKey
                                )
                );

        return ResponseEntity.ok()
                .cacheControl(
                        CacheControl
                                .maxAge(PUBLIC_CACHE_DURATION)
                                .cachePublic()
                )
                .body(
                        ApiResponse.success(
                                "Public website setting retrieved successfully.",
                                response,
                                httpRequest.getRequestURI()
                        )
                );
    }
}