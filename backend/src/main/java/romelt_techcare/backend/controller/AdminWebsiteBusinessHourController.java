package romelt_techcare.backend.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import romelt_techcare.backend.api.ApiResponse;
import romelt_techcare.backend.dto.AdminJwtPrincipal;
import romelt_techcare.backend.dto.WebsiteBusinessHourResponse;
import romelt_techcare.backend.dto.WebsiteBusinessHoursBulkUpdateRequest;
import romelt_techcare.backend.dto.WebsiteBusinessHourUpsertRequest;
import romelt_techcare.backend.entity.WebsiteBusinessHour;
import romelt_techcare.backend.mapper.WebsiteBusinessHourMapper;
import romelt_techcare.backend.service.WebsiteBusinessHourService;

import java.util.List;
import java.util.UUID;

/**
 * ================================================================
 * ROMELT TECHCARE — ADMIN WEBSITE BUSINESS HOUR CONTROLLER
 * ================================================================
 *
 * Purpose:
 * Exposes protected administrator endpoints for managing the normal
 * weekly website schedule.
 *
 * Responsibilities:
 * - Creates or updates one weekday.
 * - Updates several weekdays transactionally.
 * - Retrieves schedule rows by ID, day, or profile.
 * - Removes individual or complete profile schedules.
 *
 * Base endpoint:
 * /api/v1/admin/website-business-hours
 *
 * Important:
 * Date-specific exceptions are managed through a separate controller.
 * ================================================================
 */
@RestController
@RequestMapping("/api/v1/admin/website-business-hours")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
public class AdminWebsiteBusinessHourController {

    private final WebsiteBusinessHourService
            websiteBusinessHourService;

    private final WebsiteBusinessHourMapper
            websiteBusinessHourMapper;

    /**
     * Creates or updates one profile/day schedule.
     *
     * PUT /api/v1/admin/website-business-hours/profile/{profileId}/day/{day}
     */
    @PutMapping(
            "/profile/{businessProfileId}/day/{dayOfWeek}"
    )
    public ResponseEntity<
            ApiResponse<WebsiteBusinessHourResponse>
            > upsertBusinessHour(
            @AuthenticationPrincipal
            AdminJwtPrincipal principal,

            @PathVariable
            UUID businessProfileId,

            @PathVariable
            short dayOfWeek,

            @Valid
            @RequestBody
            WebsiteBusinessHourUpsertRequest request,

            HttpServletRequest httpRequest
    ) {
        if (request.dayOfWeek() != dayOfWeek) {
            throw new IllegalArgumentException(
                    "The route day and request day must match."
            );
        }

        WebsiteBusinessHour savedBusinessHour =
                websiteBusinessHourService
                        .upsertBusinessHour(
                                businessProfileId,
                                websiteBusinessHourMapper
                                        .toEntity(request),
                                requireAdministratorId(principal)
                        );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Website business hour saved successfully.",
                        websiteBusinessHourMapper.toResponse(
                                savedBusinessHour
                        ),
                        httpRequest.getRequestURI()
                )
        );
    }

    /**
     * Updates one or more weekdays in one transaction.
     *
     * PUT /api/v1/admin/website-business-hours/profile/{profileId}/weekly
     */
    @PutMapping(
            "/profile/{businessProfileId}/weekly"
    )
    public ResponseEntity<
            ApiResponse<List<WebsiteBusinessHourResponse>>
            > upsertWeeklyBusinessHours(
            @AuthenticationPrincipal
            AdminJwtPrincipal principal,

            @PathVariable
            UUID businessProfileId,

            @Valid
            @RequestBody
            WebsiteBusinessHoursBulkUpdateRequest request,

            HttpServletRequest httpRequest
    ) {
        List<WebsiteBusinessHour> requestedHours =
                request.businessHours()
                        .stream()
                        .map(
                                websiteBusinessHourMapper::toEntity
                        )
                        .toList();

        List<WebsiteBusinessHourResponse> response =
                websiteBusinessHourService
                        .upsertWeeklyBusinessHours(
                                businessProfileId,
                                requestedHours,
                                requireAdministratorId(principal)
                        )
                        .stream()
                        .map(
                                websiteBusinessHourMapper::toResponse
                        )
                        .toList();

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Website weekly business hours saved successfully.",
                        response,
                        httpRequest.getRequestURI()
                )
        );
    }

    /**
     * Retrieves one schedule row.
     *
     * GET /api/v1/admin/website-business-hours/{businessHourId}
     */
    @GetMapping("/{businessHourId}")
    public ResponseEntity<
            ApiResponse<WebsiteBusinessHourResponse>
            > getBusinessHour(
            @PathVariable
            UUID businessHourId,

            HttpServletRequest httpRequest
    ) {
        WebsiteBusinessHourResponse response =
                websiteBusinessHourMapper.toResponse(
                        websiteBusinessHourService
                                .getBusinessHour(
                                        businessHourId
                                )
                );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Website business hour retrieved successfully.",
                        response,
                        httpRequest.getRequestURI()
                )
        );
    }

    /**
     * Retrieves one profile/day schedule.
     *
     * GET /api/v1/admin/website-business-hours/profile/{profileId}/day/{day}
     */
    @GetMapping(
            "/profile/{businessProfileId}/day/{dayOfWeek}"
    )
    public ResponseEntity<
            ApiResponse<WebsiteBusinessHourResponse>
            > getBusinessHourByDay(
            @PathVariable
            UUID businessProfileId,

            @PathVariable
            short dayOfWeek,

            HttpServletRequest httpRequest
    ) {
        WebsiteBusinessHourResponse response =
                websiteBusinessHourMapper.toResponse(
                        websiteBusinessHourService
                                .getBusinessHourByDay(
                                        businessProfileId,
                                        dayOfWeek
                                )
                );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Website business hour retrieved successfully.",
                        response,
                        httpRequest.getRequestURI()
                )
        );
    }

    /**
     * Retrieves the complete weekly schedule for one profile.
     *
     * GET /api/v1/admin/website-business-hours/profile/{profileId}
     */
    @GetMapping("/profile/{businessProfileId}")
    public ResponseEntity<
            ApiResponse<List<WebsiteBusinessHourResponse>>
            > getBusinessHoursByProfile(
            @PathVariable
            UUID businessProfileId,

            HttpServletRequest httpRequest
    ) {
        List<WebsiteBusinessHourResponse> response =
                websiteBusinessHourService
                        .getBusinessHoursByProfile(
                                businessProfileId
                        )
                        .stream()
                        .map(
                                websiteBusinessHourMapper::toResponse
                        )
                        .toList();

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Website business hours retrieved successfully.",
                        response,
                        httpRequest.getRequestURI()
                )
        );
    }

    /**
     * Deletes one weekday schedule.
     *
     * DELETE /api/v1/admin/website-business-hours/{businessHourId}
     */
    @DeleteMapping("/{businessHourId}")
    public ResponseEntity<Void> deleteBusinessHour(
            @AuthenticationPrincipal
            AdminJwtPrincipal principal,

            @PathVariable
            UUID businessHourId
    ) {
        websiteBusinessHourService.deleteBusinessHour(
                businessHourId,
                requireAdministratorId(principal)
        );

        return ResponseEntity.noContent().build();
    }

    /**
     * Deletes the entire normal schedule for one business profile.
     *
     * DELETE /api/v1/admin/website-business-hours/profile/{profileId}
     */
    @DeleteMapping("/profile/{businessProfileId}")
    public ResponseEntity<Void> deleteBusinessHoursByProfile(
            @AuthenticationPrincipal
            AdminJwtPrincipal principal,

            @PathVariable
            UUID businessProfileId
    ) {
        websiteBusinessHourService
                .deleteBusinessHoursByProfile(
                        businessProfileId,
                        requireAdministratorId(principal)
                );

        return ResponseEntity.noContent().build();
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