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
import romelt_techcare.backend.dto.WebsiteSocialLinkCreateRequest;
import romelt_techcare.backend.dto.WebsiteSocialLinkResponse;
import romelt_techcare.backend.dto.WebsiteSocialLinkStatusRequest;
import romelt_techcare.backend.dto.WebsiteSocialLinkUpdateRequest;
import romelt_techcare.backend.entity.WebsiteSocialLink;
import romelt_techcare.backend.mapper.WebsiteSocialLinkMapper;
import romelt_techcare.backend.service.WebsiteSocialLinkService;

import java.util.UUID;

/**
 * ================================================================
 * ROMELT TECHCARE — ADMIN WEBSITE SOCIAL LINK CONTROLLER
 * ================================================================
 *
 * Purpose:
 * Exposes protected administrator endpoints for managing social and
 * external-profile links shown on the public website.
 *
 * Responsibilities:
 * - Creates and updates social links.
 * - Retrieves individual links and paginated search results.
 * - Activates and deactivates links.
 * - Permanently deletes obsolete links.
 * - Uses the authenticated administrator as the actor for writes.
 *
 * Base endpoint:
 * /api/v1/admin/website-social-links
 * ================================================================
 */
@RestController
@RequestMapping("/api/v1/admin/website-social-links")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
public class AdminWebsiteSocialLinkController {

    private final WebsiteSocialLinkService
            websiteSocialLinkService;

    private final WebsiteSocialLinkMapper
            websiteSocialLinkMapper;

    /**
     * Creates a social link.
     *
     * POST /api/v1/admin/website-social-links
     */
    @PostMapping
    public ResponseEntity<
            ApiResponse<WebsiteSocialLinkResponse>
            > createSocialLink(
            @AuthenticationPrincipal
            AdminJwtPrincipal principal,

            @Valid
            @RequestBody
            WebsiteSocialLinkCreateRequest request,

            HttpServletRequest httpRequest
    ) {
        WebsiteSocialLink socialLink =
                websiteSocialLinkService.createSocialLink(
                        websiteSocialLinkMapper.toEntity(request),
                        requireAdministratorId(principal)
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        ApiResponse.success(
                                "Website social link created successfully.",
                                websiteSocialLinkMapper.toResponse(
                                        socialLink
                                ),
                                httpRequest.getRequestURI()
                        )
                );
    }

    /**
     * Updates a social link.
     *
     * PUT /api/v1/admin/website-social-links/{socialLinkId}
     */
    @PutMapping("/{socialLinkId}")
    public ResponseEntity<
            ApiResponse<WebsiteSocialLinkResponse>
            > updateSocialLink(
            @AuthenticationPrincipal
            AdminJwtPrincipal principal,

            @PathVariable
            UUID socialLinkId,

            @Valid
            @RequestBody
            WebsiteSocialLinkUpdateRequest request,

            HttpServletRequest httpRequest
    ) {
        WebsiteSocialLink socialLink =
                websiteSocialLinkService.updateSocialLink(
                        socialLinkId,
                        websiteSocialLinkMapper.toUpdateEntity(
                                request
                        ),
                        requireAdministratorId(principal)
                );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Website social link updated successfully.",
                        websiteSocialLinkMapper.toResponse(
                                socialLink
                        ),
                        httpRequest.getRequestURI()
                )
        );
    }

    /**
     * Searches social links.
     *
     * GET /api/v1/admin/website-social-links
     */
    @GetMapping
    public ResponseEntity<
            ApiResponse<Page<WebsiteSocialLinkResponse>>
            > searchSocialLinks(
            @RequestParam(required = false)
            String keyword,

            @RequestParam(required = false)
            Boolean isActive,

            @PageableDefault(
                    size = 10,
                    sort = {
                            "displayOrder",
                            "platform"
                    },
                    direction = Sort.Direction.ASC
            )
            Pageable pageable,

            HttpServletRequest httpRequest
    ) {
        Page<WebsiteSocialLinkResponse> response =
                websiteSocialLinkService
                        .searchSocialLinks(
                                keyword,
                                isActive,
                                pageable
                        )
                        .map(
                                websiteSocialLinkMapper::toResponse
                        );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Website social links retrieved successfully.",
                        response,
                        httpRequest.getRequestURI()
                )
        );
    }

    /**
     * Retrieves one social link.
     *
     * GET /api/v1/admin/website-social-links/{socialLinkId}
     */
    @GetMapping("/{socialLinkId}")
    public ResponseEntity<
            ApiResponse<WebsiteSocialLinkResponse>
            > getSocialLink(
            @PathVariable
            UUID socialLinkId,

            HttpServletRequest httpRequest
    ) {
        WebsiteSocialLinkResponse response =
                websiteSocialLinkMapper.toResponse(
                        websiteSocialLinkService.getSocialLink(
                                socialLinkId
                        )
                );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Website social link retrieved successfully.",
                        response,
                        httpRequest.getRequestURI()
                )
        );
    }

    /**
     * Retrieves a social link by platform.
     *
     * GET /api/v1/admin/website-social-links/by-platform/{platform}
     */
    @GetMapping("/by-platform/{platform}")
    public ResponseEntity<
            ApiResponse<WebsiteSocialLinkResponse>
            > getSocialLinkByPlatform(
            @PathVariable
            String platform,

            HttpServletRequest httpRequest
    ) {
        WebsiteSocialLinkResponse response =
                websiteSocialLinkMapper.toResponse(
                        websiteSocialLinkService
                                .getSocialLinkByPlatform(platform)
                );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Website social link retrieved successfully.",
                        response,
                        httpRequest.getRequestURI()
                )
        );
    }

    /**
     * Updates active status.
     *
     * PATCH /api/v1/admin/website-social-links/{socialLinkId}/status
     */
    @PatchMapping("/{socialLinkId}/status")
    public ResponseEntity<
            ApiResponse<WebsiteSocialLinkResponse>
            > updateSocialLinkStatus(
            @AuthenticationPrincipal
            AdminJwtPrincipal principal,

            @PathVariable
            UUID socialLinkId,

            @Valid
            @RequestBody
            WebsiteSocialLinkStatusRequest request,

            HttpServletRequest httpRequest
    ) {
        WebsiteSocialLink socialLink =
                websiteSocialLinkService
                        .updateSocialLinkStatus(
                                socialLinkId,
                                request.isActive(),
                                requireAdministratorId(principal)
                        );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Website social-link status updated successfully.",
                        websiteSocialLinkMapper.toResponse(
                                socialLink
                        ),
                        httpRequest.getRequestURI()
                )
        );
    }

    /**
     * Activates a social link.
     *
     * PATCH /api/v1/admin/website-social-links/{socialLinkId}/activate
     */
    @PatchMapping("/{socialLinkId}/activate")
    public ResponseEntity<
            ApiResponse<WebsiteSocialLinkResponse>
            > activateSocialLink(
            @AuthenticationPrincipal
            AdminJwtPrincipal principal,

            @PathVariable
            UUID socialLinkId,

            HttpServletRequest httpRequest
    ) {
        WebsiteSocialLink socialLink =
                websiteSocialLinkService.activateSocialLink(
                        socialLinkId,
                        requireAdministratorId(principal)
                );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Website social link activated successfully.",
                        websiteSocialLinkMapper.toResponse(
                                socialLink
                        ),
                        httpRequest.getRequestURI()
                )
        );
    }

    /**
     * Deactivates a social link.
     *
     * PATCH /api/v1/admin/website-social-links/{socialLinkId}/deactivate
     */
    @PatchMapping("/{socialLinkId}/deactivate")
    public ResponseEntity<
            ApiResponse<WebsiteSocialLinkResponse>
            > deactivateSocialLink(
            @AuthenticationPrincipal
            AdminJwtPrincipal principal,

            @PathVariable
            UUID socialLinkId,

            HttpServletRequest httpRequest
    ) {
        WebsiteSocialLink socialLink =
                websiteSocialLinkService.deactivateSocialLink(
                        socialLinkId,
                        requireAdministratorId(principal)
                );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Website social link deactivated successfully.",
                        websiteSocialLinkMapper.toResponse(
                                socialLink
                        ),
                        httpRequest.getRequestURI()
                )
        );
    }

    /**
     * Permanently deletes a social link.
     *
     * DELETE /api/v1/admin/website-social-links/{socialLinkId}
     */
    @DeleteMapping("/{socialLinkId}")
    public ResponseEntity<Void> deleteSocialLink(
            @AuthenticationPrincipal
            AdminJwtPrincipal principal,

            @PathVariable
            UUID socialLinkId
    ) {
        websiteSocialLinkService.deleteSocialLink(
                socialLinkId,
                requireAdministratorId(principal)
        );

        return ResponseEntity.noContent().build();
    }

    /**
     * Counts active social links.
     *
     * GET /api/v1/admin/website-social-links/counts/active
     */
    @GetMapping("/counts/active")
    public ResponseEntity<ApiResponse<Long>>
    countActiveSocialLinks(
            HttpServletRequest httpRequest
    ) {
        long count =
                websiteSocialLinkService
                        .countActiveSocialLinks();

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Active website social-link count retrieved successfully.",
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
}