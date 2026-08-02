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
import romelt_techcare.backend.dto.WebsiteNavigationItemCreateRequest;
import romelt_techcare.backend.dto.WebsiteNavigationItemResponse;
import romelt_techcare.backend.dto.WebsiteNavigationItemUpdateRequest;
import romelt_techcare.backend.dto.WebsiteNavigationItemVisibilityRequest;
import romelt_techcare.backend.entity.WebsiteNavigationItem;
import romelt_techcare.backend.enums.WebsiteNavigationDestinationType;
import romelt_techcare.backend.enums.WebsiteNavigationLocation;
import romelt_techcare.backend.mapper.WebsiteNavigationItemMapper;
import romelt_techcare.backend.service.WebsiteNavigationItemService;

import java.util.UUID;

/**
 * ================================================================
 * ROMELT TECHCARE — ADMIN NAVIGATION ITEM CONTROLLER
 * ================================================================
 *
 * Purpose:
 * Exposes protected administrator endpoints for managing website
 * header, mobile, and footer navigation items.
 *
 * Base endpoint:
 * /api/v1/admin/website-navigation-items
 * ================================================================
 */
@RestController
@RequestMapping("/api/v1/admin/website-navigation-items")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
public class AdminWebsiteNavigationItemController {

    private final WebsiteNavigationItemService
            websiteNavigationItemService;

    private final WebsiteNavigationItemMapper
            websiteNavigationItemMapper;

    @PostMapping
    public ResponseEntity<
            ApiResponse<WebsiteNavigationItemResponse>
            > createNavigationItem(
            @AuthenticationPrincipal
            AdminJwtPrincipal principal,

            @Valid
            @RequestBody
            WebsiteNavigationItemCreateRequest request,

            HttpServletRequest httpRequest
    ) {
        WebsiteNavigationItem item =
                websiteNavigationItemService
                        .createNavigationItem(
                                websiteNavigationItemMapper
                                        .toEntity(request),
                                requireAdministratorId(principal)
                        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        ApiResponse.success(
                                "Website navigation item created successfully.",
                                websiteNavigationItemMapper
                                        .toResponse(item),
                                httpRequest.getRequestURI()
                        )
                );
    }

    @PutMapping("/{navigationItemId}")
    public ResponseEntity<
            ApiResponse<WebsiteNavigationItemResponse>
            > updateNavigationItem(
            @AuthenticationPrincipal
            AdminJwtPrincipal principal,

            @PathVariable
            UUID navigationItemId,

            @Valid
            @RequestBody
            WebsiteNavigationItemUpdateRequest request,

            HttpServletRequest httpRequest
    ) {
        WebsiteNavigationItem item =
                websiteNavigationItemService
                        .updateNavigationItem(
                                navigationItemId,
                                websiteNavigationItemMapper
                                        .toUpdateEntity(request),
                                requireAdministratorId(principal)
                        );

        return successfulResponse(
                "Website navigation item updated successfully.",
                item,
                httpRequest
        );
    }

    @GetMapping
    public ResponseEntity<
            ApiResponse<Page<WebsiteNavigationItemResponse>>
            > searchNavigationItems(
            @RequestParam(required = false)
            String keyword,

            @RequestParam(required = false)
            WebsiteNavigationLocation navigationLocation,

            @RequestParam(required = false)
            WebsiteNavigationDestinationType destinationType,

            @RequestParam(required = false)
            Boolean isVisible,

            @PageableDefault(
                    size = 10,
                    sort = {
                            "navigationLocation",
                            "displayOrder",
                            "label"
                    },
                    direction = Sort.Direction.ASC
            )
            Pageable pageable,

            HttpServletRequest httpRequest
    ) {
        Page<WebsiteNavigationItemResponse> response =
                websiteNavigationItemService
                        .searchNavigationItems(
                                keyword,
                                navigationLocation,
                                destinationType,
                                isVisible,
                                pageable
                        )
                        .map(
                                websiteNavigationItemMapper
                                        ::toResponse
                        );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Website navigation items retrieved successfully.",
                        response,
                        httpRequest.getRequestURI()
                )
        );
    }

    @GetMapping("/deleted")
    public ResponseEntity<
            ApiResponse<Page<WebsiteNavigationItemResponse>>
            > getDeletedNavigationItems(
            @PageableDefault(
                    size = 10,
                    sort = "deletedAt",
                    direction = Sort.Direction.DESC
            )
            Pageable pageable,

            HttpServletRequest httpRequest
    ) {
        Page<WebsiteNavigationItemResponse> response =
                websiteNavigationItemService
                        .getDeletedNavigationItems(pageable)
                        .map(
                                websiteNavigationItemMapper
                                        ::toResponse
                        );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Deleted website navigation items retrieved successfully.",
                        response,
                        httpRequest.getRequestURI()
                )
        );
    }

    @GetMapping("/{navigationItemId}")
    public ResponseEntity<
            ApiResponse<WebsiteNavigationItemResponse>
            > getNavigationItem(
            @PathVariable
            UUID navigationItemId,

            HttpServletRequest httpRequest
    ) {
        return successfulResponse(
                "Website navigation item retrieved successfully.",
                websiteNavigationItemService
                        .getNavigationItem(
                                navigationItemId
                        ),
                httpRequest
        );
    }

    @GetMapping("/by-key/{navigationLocation}/{itemKey}")
    public ResponseEntity<
            ApiResponse<WebsiteNavigationItemResponse>
            > getNavigationItemByLocationAndKey(
            @PathVariable
            WebsiteNavigationLocation navigationLocation,

            @PathVariable
            String itemKey,

            HttpServletRequest httpRequest
    ) {
        return successfulResponse(
                "Website navigation item retrieved successfully.",
                websiteNavigationItemService
                        .getNavigationItemByLocationAndKey(
                                navigationLocation,
                                itemKey
                        ),
                httpRequest
        );
    }

    @PatchMapping("/{navigationItemId}/visibility")
    public ResponseEntity<
            ApiResponse<WebsiteNavigationItemResponse>
            > updateNavigationItemVisibility(
            @AuthenticationPrincipal
            AdminJwtPrincipal principal,

            @PathVariable
            UUID navigationItemId,

            @Valid
            @RequestBody
            WebsiteNavigationItemVisibilityRequest request,

            HttpServletRequest httpRequest
    ) {
        WebsiteNavigationItem item =
                websiteNavigationItemService
                        .updateNavigationItemVisibility(
                                navigationItemId,
                                request.isVisible(),
                                requireAdministratorId(principal)
                        );

        return successfulResponse(
                "Website navigation-item visibility updated successfully.",
                item,
                httpRequest
        );
    }

    @DeleteMapping("/{navigationItemId}")
    public ResponseEntity<Void> deleteNavigationItem(
            @AuthenticationPrincipal
            AdminJwtPrincipal principal,

            @PathVariable
            UUID navigationItemId
    ) {
        websiteNavigationItemService
                .deleteNavigationItem(
                        navigationItemId,
                        requireAdministratorId(principal)
                );

        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{navigationItemId}/restore")
    public ResponseEntity<
            ApiResponse<WebsiteNavigationItemResponse>
            > restoreNavigationItem(
            @AuthenticationPrincipal
            AdminJwtPrincipal principal,

            @PathVariable
            UUID navigationItemId,

            HttpServletRequest httpRequest
    ) {
        WebsiteNavigationItem item =
                websiteNavigationItemService
                        .restoreNavigationItem(
                                navigationItemId,
                                requireAdministratorId(principal)
                        );

        return successfulResponse(
                "Website navigation item restored successfully.",
                item,
                httpRequest
        );
    }

    @GetMapping("/counts/visible/{navigationLocation}")
    public ResponseEntity<ApiResponse<Long>>
    countVisibleNavigationItems(
            @PathVariable
            WebsiteNavigationLocation navigationLocation,

            HttpServletRequest httpRequest
    ) {
        long count =
                websiteNavigationItemService
                        .countVisibleNavigationItems(
                                navigationLocation
                        );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Visible navigation-item count retrieved successfully.",
                        count,
                        httpRequest.getRequestURI()
                )
        );
    }

    @GetMapping("/counts/deleted")
    public ResponseEntity<ApiResponse<Long>>
    countDeletedNavigationItems(
            HttpServletRequest httpRequest
    ) {
        long count =
                websiteNavigationItemService
                        .countDeletedNavigationItems();

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Deleted navigation-item count retrieved successfully.",
                        count,
                        httpRequest.getRequestURI()
                )
        );
    }

    private ResponseEntity<
            ApiResponse<WebsiteNavigationItemResponse>
            > successfulResponse(
            String message,
            WebsiteNavigationItem item,
            HttpServletRequest request
    ) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        message,
                        websiteNavigationItemMapper
                                .toResponse(item),
                        request.getRequestURI()
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