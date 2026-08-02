package romelt_techcare.backend.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import romelt_techcare.backend.api.ApiResponse;
import romelt_techcare.backend.dto.AdminJwtPrincipal;
import romelt_techcare.backend.dto.WebsiteSettingCreateRequest;
import romelt_techcare.backend.dto.WebsiteSettingResponse;
import romelt_techcare.backend.dto.WebsiteSettingUpdateRequest;
import romelt_techcare.backend.dto.WebsiteSettingValueUpdateRequest;
import romelt_techcare.backend.dto.WebsiteSettingVisibilityRequest;
import romelt_techcare.backend.entity.WebsiteSetting;
import romelt_techcare.backend.mapper.WebsiteSettingMapper;
import romelt_techcare.backend.service.WebsiteSettingService;

import java.util.List;
import java.util.UUID;

/**
 * ================================================================
 * ROMELT TECHCARE — ADMIN WEBSITE SETTING CONTROLLER
 * ================================================================
 *
 * Purpose:
 * Exposes protected administrator endpoints for managing general
 * website and CMS settings.
 *
 * Responsibilities:
 * - Creates, updates, and upserts settings.
 * - Updates JSON values independently.
 * - Updates public and sensitive states.
 * - Retrieves settings by ID, group, and key.
 * - Searches settings using pagination and filters.
 * - Permanently deletes settings.
 *
 * Base endpoint:
 * /api/v1/admin/website-settings
 *
 * Security:
 * Administrator responses may contain sensitive configuration values.
 * These endpoints must remain protected.
 * ================================================================
 */
@RestController
@RequestMapping("/api/v1/admin/website-settings")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
public class AdminWebsiteSettingController {

    private final WebsiteSettingService websiteSettingService;
    private final WebsiteSettingMapper websiteSettingMapper;

    /**
     * Creates a website setting.
     *
     * POST /api/v1/admin/website-settings
     */
    @PostMapping
    public ResponseEntity<
            ApiResponse<WebsiteSettingResponse>
            > createWebsiteSetting(
            @AuthenticationPrincipal
            AdminJwtPrincipal principal,

            @Valid
            @RequestBody
            WebsiteSettingCreateRequest request,

            HttpServletRequest httpRequest
    ) {
        WebsiteSetting websiteSetting =
                websiteSettingService.createWebsiteSetting(
                        websiteSettingMapper.toEntity(request),
                        requireAdministratorId(principal)
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        ApiResponse.success(
                                "Website setting created successfully.",
                                websiteSettingMapper.toResponse(
                                        websiteSetting
                                ),
                                httpRequest.getRequestURI()
                        )
                );
    }

    /**
     * Updates a website setting.
     *
     * PUT /api/v1/admin/website-settings/{websiteSettingId}
     */
    @PutMapping("/{websiteSettingId}")
    public ResponseEntity<
            ApiResponse<WebsiteSettingResponse>
            > updateWebsiteSetting(
            @AuthenticationPrincipal
            AdminJwtPrincipal principal,

            @PathVariable
            UUID websiteSettingId,

            @Valid
            @RequestBody
            WebsiteSettingUpdateRequest request,

            HttpServletRequest httpRequest
    ) {
        WebsiteSetting websiteSetting =
                websiteSettingService.updateWebsiteSetting(
                        websiteSettingId,
                        websiteSettingMapper.toUpdateEntity(
                                request
                        ),
                        requireAdministratorId(principal)
                );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Website setting updated successfully.",
                        websiteSettingMapper.toResponse(
                                websiteSetting
                        ),
                        httpRequest.getRequestURI()
                )
        );
    }

    /**
     * Creates or updates a setting by group and key.
     *
     * PUT /api/v1/admin/website-settings/by-key/{group}/{key}
     */
    @PutMapping(
            "/by-key/{settingGroup}/{settingKey}"
    )
    public ResponseEntity<
            ApiResponse<WebsiteSettingResponse>
            > upsertWebsiteSetting(
            @AuthenticationPrincipal
            AdminJwtPrincipal principal,

            @PathVariable
            String settingGroup,

            @PathVariable
            String settingKey,

            @Valid
            @RequestBody
            WebsiteSettingUpdateRequest request,

            HttpServletRequest httpRequest
    ) {
        if (
                !normalizeRouteKey(settingGroup).equals(
                        normalizeRouteKey(
                                request.settingGroup()
                        )
                )
        ) {
            throw new IllegalArgumentException(
                    "The route setting group and request setting group "
                            + "must match."
            );
        }

        if (
                !normalizeRouteKey(settingKey).equals(
                        normalizeRouteKey(
                                request.settingKey()
                        )
                )
        ) {
            throw new IllegalArgumentException(
                    "The route setting key and request setting key "
                            + "must match."
            );
        }

        WebsiteSetting websiteSetting =
                websiteSettingService.upsertWebsiteSetting(
                        websiteSettingMapper.toUpdateEntity(
                                request
                        ),
                        requireAdministratorId(principal)
                );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Website setting saved successfully.",
                        websiteSettingMapper.toResponse(
                                websiteSetting
                        ),
                        httpRequest.getRequestURI()
                )
        );
    }

    /**
     * Updates only a setting's JSON value.
     *
     * PATCH /api/v1/admin/website-settings/{id}/value
     */
    @PatchMapping("/{websiteSettingId}/value")
    public ResponseEntity<
            ApiResponse<WebsiteSettingResponse>
            > updateWebsiteSettingValue(
            @AuthenticationPrincipal
            AdminJwtPrincipal principal,

            @PathVariable
            UUID websiteSettingId,

            @Valid
            @RequestBody
            WebsiteSettingValueUpdateRequest request,

            HttpServletRequest httpRequest
    ) {
        WebsiteSetting websiteSetting =
                websiteSettingService
                        .updateWebsiteSettingValue(
                                websiteSettingId,
                                request.settingValue(),
                                requireAdministratorId(principal)
                        );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Website setting value updated successfully.",
                        websiteSettingMapper.toResponse(
                                websiteSetting
                        ),
                        httpRequest.getRequestURI()
                )
        );
    }

    /**
     * Updates public and sensitive states.
     *
     * PATCH /api/v1/admin/website-settings/{id}/visibility
     */
    @PatchMapping("/{websiteSettingId}/visibility")
    public ResponseEntity<
            ApiResponse<WebsiteSettingResponse>
            > updateWebsiteSettingVisibility(
            @AuthenticationPrincipal
            AdminJwtPrincipal principal,

            @PathVariable
            UUID websiteSettingId,

            @Valid
            @RequestBody
            WebsiteSettingVisibilityRequest request,

            HttpServletRequest httpRequest
    ) {
        WebsiteSetting websiteSetting =
                websiteSettingService
                        .updateWebsiteSettingVisibility(
                                websiteSettingId,
                                request.isPublic(),
                                request.isSensitive(),
                                requireAdministratorId(principal)
                        );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Website setting visibility updated successfully.",
                        websiteSettingMapper.toResponse(
                                websiteSetting
                        ),
                        httpRequest.getRequestURI()
                )
        );
    }

    /**
     * Searches administrator-facing settings.
     *
     * GET /api/v1/admin/website-settings
     */
    @GetMapping
    public ResponseEntity<
            ApiResponse<Page<WebsiteSettingResponse>>
            > searchWebsiteSettings(
            @RequestParam(required = false)
            String keyword,

            @RequestParam(required = false)
            String settingGroup,

            @RequestParam(required = false)
            Boolean isPublic,

            @RequestParam(required = false)
            Boolean isSensitive,

            @PageableDefault(
                    size = 10,
                    sort = {
                            "settingGroup",
                            "settingKey"
                    },
                    direction = Sort.Direction.ASC
            )
            Pageable pageable,

            HttpServletRequest httpRequest
    ) {
        Page<WebsiteSettingResponse> response =
                websiteSettingService
                        .searchWebsiteSettings(
                                keyword,
                                settingGroup,
                                isPublic,
                                isSensitive,
                                pageable
                        )
                        .map(
                                websiteSettingMapper::toResponse
                        );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Website settings retrieved successfully.",
                        response,
                        httpRequest.getRequestURI()
                )
        );
    }

    /**
     * Retrieves one setting by identifier.
     *
     * GET /api/v1/admin/website-settings/{websiteSettingId}
     */
    @GetMapping("/{websiteSettingId}")
    public ResponseEntity<
            ApiResponse<WebsiteSettingResponse>
            > getWebsiteSetting(
            @PathVariable
            UUID websiteSettingId,

            HttpServletRequest httpRequest
    ) {
        WebsiteSettingResponse response =
                websiteSettingMapper.toResponse(
                        websiteSettingService
                                .getWebsiteSetting(
                                        websiteSettingId
                                )
                );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Website setting retrieved successfully.",
                        response,
                        httpRequest.getRequestURI()
                )
        );
    }

    /**
     * Retrieves one setting by group and key.
     *
     * GET /api/v1/admin/website-settings/by-key/{group}/{key}
     */
    @GetMapping(
            "/by-key/{settingGroup}/{settingKey}"
    )
    public ResponseEntity<
            ApiResponse<WebsiteSettingResponse>
            > getWebsiteSettingByGroupAndKey(
            @PathVariable
            String settingGroup,

            @PathVariable
            String settingKey,

            HttpServletRequest httpRequest
    ) {
        WebsiteSettingResponse response =
                websiteSettingMapper.toResponse(
                        websiteSettingService
                                .getWebsiteSettingByGroupAndKey(
                                        settingGroup,
                                        settingKey
                                )
                );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Website setting retrieved successfully.",
                        response,
                        httpRequest.getRequestURI()
                )
        );
    }

    /**
     * Retrieves all settings in one group.
     *
     * GET /api/v1/admin/website-settings/group/{settingGroup}
     */
    @GetMapping("/group/{settingGroup}")
    public ResponseEntity<
            ApiResponse<List<WebsiteSettingResponse>>
            > getWebsiteSettingsByGroup(
            @PathVariable
            String settingGroup,

            HttpServletRequest httpRequest
    ) {
        List<WebsiteSettingResponse> response =
                websiteSettingService
                        .getWebsiteSettingsByGroup(
                                settingGroup
                        )
                        .stream()
                        .map(
                                websiteSettingMapper::toResponse
                        )
                        .toList();

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Website settings retrieved successfully.",
                        response,
                        httpRequest.getRequestURI()
                )
        );
    }

    /**
     * Permanently deletes a setting.
     *
     * DELETE /api/v1/admin/website-settings/{websiteSettingId}
     */
    @DeleteMapping("/{websiteSettingId}")
    public ResponseEntity<Void> deleteWebsiteSetting(
            @AuthenticationPrincipal
            AdminJwtPrincipal principal,

            @PathVariable
            UUID websiteSettingId
    ) {
        websiteSettingService.deleteWebsiteSetting(
                websiteSettingId,
                requireAdministratorId(principal)
        );

        return ResponseEntity.noContent().build();
    }

    /**
     * Counts public non-sensitive settings.
     *
     * GET /api/v1/admin/website-settings/counts/public
     */
    @GetMapping("/counts/public")
    public ResponseEntity<ApiResponse<Long>>
    countPublicWebsiteSettings(
            HttpServletRequest httpRequest
    ) {
        long count =
                websiteSettingService
                        .countPublicWebsiteSettings();

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Public website-setting count retrieved successfully.",
                        count,
                        httpRequest.getRequestURI()
                )
        );
    }

    /**
     * Counts sensitive settings.
     *
     * GET /api/v1/admin/website-settings/counts/sensitive
     */
    @GetMapping("/counts/sensitive")
    public ResponseEntity<ApiResponse<Long>>
    countSensitiveWebsiteSettings(
            HttpServletRequest httpRequest
    ) {
        long count =
                websiteSettingService
                        .countSensitiveWebsiteSettings();

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Sensitive website-setting count retrieved successfully.",
                        count,
                        httpRequest.getRequestURI()
                )
        );
    }

    private UUID requireAdministratorId(
            AdminJwtPrincipal principal
    ) {
        if (
                principal == null
                        || principal.adminUserId() == null
        ) {
            throw new IllegalArgumentException(
                    "Authenticated administrator information is required."
            );
        }

        return principal.adminUserId();
    }

    private String normalizeRouteKey(
            String value
    ) {
        if (value == null) {
            return "";
        }

        return value.trim()
                .toUpperCase()
                .replaceAll("[^A-Z0-9]+", "_")
                .replaceAll("^_+|_+$", "");
    }
}