package romelt_techcare.backend.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import romelt_techcare.backend.api.ApiResponse;
import romelt_techcare.backend.dto.AdminJwtPrincipal;
import romelt_techcare.backend.dto.WebsiteBusinessProfileCreateRequest;
import romelt_techcare.backend.dto.WebsiteBusinessProfileResponse;
import romelt_techcare.backend.dto.WebsiteBusinessProfileUpdateRequest;
import romelt_techcare.backend.entity.WebsiteBusinessProfile;
import romelt_techcare.backend.mapper.WebsiteBusinessProfileMapper;
import romelt_techcare.backend.service.WebsiteBusinessProfileService;

import java.util.UUID;

/**
 * ================================================================
 * ROMELT TECHCARE — ADMIN WEBSITE BUSINESS PROFILE CONTROLLER
 * ================================================================
 *
 * Purpose:
 * Exposes protected administrator endpoints for managing the public
 * Romelt TechCare business identity and branding profile.
 *
 * Base endpoint:
 * /api/v1/admin/website-business-profile
 * ================================================================
 */
@RestController
@RequestMapping("/api/v1/admin/website-business-profile")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
public class AdminWebsiteBusinessProfileController {

    private final WebsiteBusinessProfileService
            websiteBusinessProfileService;

    private final WebsiteBusinessProfileMapper
            websiteBusinessProfileMapper;

    @PostMapping
    public ResponseEntity<
            ApiResponse<WebsiteBusinessProfileResponse>
            > createBusinessProfile(
            @AuthenticationPrincipal
            AdminJwtPrincipal principal,

            @Valid
            @RequestBody
            WebsiteBusinessProfileCreateRequest request,

            HttpServletRequest httpRequest
    ) {
        WebsiteBusinessProfile profile =
                websiteBusinessProfileService
                        .createBusinessProfile(
                                websiteBusinessProfileMapper
                                        .toEntity(request),
                                requireAdministratorId(principal)
                        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        ApiResponse.success(
                                "Website business profile created successfully.",
                                websiteBusinessProfileMapper
                                        .toResponse(profile),
                                httpRequest.getRequestURI()
                        )
                );
    }

    @PutMapping("/{businessProfileId}")
    public ResponseEntity<
            ApiResponse<WebsiteBusinessProfileResponse>
            > updateBusinessProfile(
            @AuthenticationPrincipal
            AdminJwtPrincipal principal,

            @PathVariable
            UUID businessProfileId,

            @Valid
            @RequestBody
            WebsiteBusinessProfileUpdateRequest request,

            HttpServletRequest httpRequest
    ) {
        WebsiteBusinessProfile profile =
                websiteBusinessProfileService
                        .updateBusinessProfile(
                                businessProfileId,
                                websiteBusinessProfileMapper
                                        .toUpdateEntity(request),
                                requireAdministratorId(principal)
                        );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Website business profile updated successfully.",
                        websiteBusinessProfileMapper.toResponse(profile),
                        httpRequest.getRequestURI()
                )
        );
    }

    @GetMapping("/{businessProfileId}")
    public ResponseEntity<
            ApiResponse<WebsiteBusinessProfileResponse>
            > getBusinessProfile(
            @PathVariable
            UUID businessProfileId,

            HttpServletRequest httpRequest
    ) {
        WebsiteBusinessProfile profile =
                websiteBusinessProfileService
                        .getBusinessProfile(businessProfileId);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Website business profile retrieved successfully.",
                        websiteBusinessProfileMapper.toResponse(profile),
                        httpRequest.getRequestURI()
                )
        );
    }

    @GetMapping("/active")
    public ResponseEntity<
            ApiResponse<WebsiteBusinessProfileResponse>
            > getActiveBusinessProfile(
            HttpServletRequest httpRequest
    ) {
        WebsiteBusinessProfile profile =
                websiteBusinessProfileService
                        .getActiveBusinessProfile();

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Active website business profile retrieved successfully.",
                        websiteBusinessProfileMapper.toResponse(profile),
                        httpRequest.getRequestURI()
                )
        );
    }

    @PatchMapping("/{businessProfileId}/activate")
    public ResponseEntity<
            ApiResponse<WebsiteBusinessProfileResponse>
            > activateBusinessProfile(
            @AuthenticationPrincipal
            AdminJwtPrincipal principal,

            @PathVariable
            UUID businessProfileId,

            HttpServletRequest httpRequest
    ) {
        WebsiteBusinessProfile profile =
                websiteBusinessProfileService
                        .activateBusinessProfile(
                                businessProfileId,
                                requireAdministratorId(principal)
                        );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Website business profile activated successfully.",
                        websiteBusinessProfileMapper.toResponse(profile),
                        httpRequest.getRequestURI()
                )
        );
    }

    @PatchMapping("/{businessProfileId}/deactivate")
    public ResponseEntity<
            ApiResponse<WebsiteBusinessProfileResponse>
            > deactivateBusinessProfile(
            @AuthenticationPrincipal
            AdminJwtPrincipal principal,

            @PathVariable
            UUID businessProfileId,

            HttpServletRequest httpRequest
    ) {
        WebsiteBusinessProfile profile =
                websiteBusinessProfileService
                        .deactivateBusinessProfile(
                                businessProfileId,
                                requireAdministratorId(principal)
                        );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Website business profile deactivated successfully.",
                        websiteBusinessProfileMapper.toResponse(profile),
                        httpRequest.getRequestURI()
                )
        );
    }

    private UUID requireAdministratorId(
            AdminJwtPrincipal principal
    ) {
        if (principal == null
                || principal.adminUserId() == null) {
            throw new IllegalArgumentException(
                    "Authenticated administrator information is required."
            );
        }

        return principal.adminUserId();
    }
}