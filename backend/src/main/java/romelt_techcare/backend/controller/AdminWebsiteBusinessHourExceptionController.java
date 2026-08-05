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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import romelt_techcare.backend.api.ApiResponse;
import romelt_techcare.backend.dto.AdminJwtPrincipal;
import romelt_techcare.backend.dto.WebsiteBusinessHourExceptionResponse;
import romelt_techcare.backend.dto.WebsiteBusinessHourExceptionUpsertRequest;
import romelt_techcare.backend.entity.WebsiteBusinessHourException;
import romelt_techcare.backend.mapper.WebsiteBusinessHourExceptionMapper;
import romelt_techcare.backend.service.WebsiteBusinessHourExceptionService;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * ================================================================
 * ROMELT TECHCARE — ADMIN BUSINESS-HOUR EXCEPTION CONTROLLER
 * ================================================================
 *
 * Purpose:
 * Exposes protected administrator endpoints for managing holidays,
 * temporary closures, vacations, emergency closures, and special
 * business hours.
 *
 * Base endpoint:
 * /api/v1/admin/website-business-hour-exceptions
 * ================================================================
 */
@RestController
@RequestMapping(
        "/api/v1/admin/website-business-hour-exceptions"
)
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
public class AdminWebsiteBusinessHourExceptionController {

    private final WebsiteBusinessHourExceptionService
            websiteBusinessHourExceptionService;

    private final WebsiteBusinessHourExceptionMapper
            websiteBusinessHourExceptionMapper;

    /**
     * Creates a new exception.
     *
     * POST /api/v1/admin/website-business-hour-exceptions/profile/{id}
     */
    @PostMapping("/profile/{businessProfileId}")
    public ResponseEntity<
            ApiResponse<WebsiteBusinessHourExceptionResponse>
            > createBusinessHourException(
            @AuthenticationPrincipal
            AdminJwtPrincipal principal,

            @PathVariable
            UUID businessProfileId,

            @Valid
            @RequestBody
            WebsiteBusinessHourExceptionUpsertRequest request,

            HttpServletRequest httpRequest
    ) {
        WebsiteBusinessHourException exception =
                websiteBusinessHourExceptionService
                        .createBusinessHourException(
                                businessProfileId,
                                websiteBusinessHourExceptionMapper
                                        .toEntity(request),
                                requireAdministratorId(principal)
                        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        ApiResponse.success(
                                "Website business-hour exception created successfully.",
                                websiteBusinessHourExceptionMapper
                                        .toResponse(exception),
                                httpRequest.getRequestURI()
                        )
                );
    }

    /**
     * Creates or updates a profile/date exception.
     *
     * PUT /api/v1/admin/website-business-hour-exceptions/profile/{id}/date/{date}
     */
    @PutMapping(
            "/profile/{businessProfileId}/date/{exceptionDate}"
    )
    public ResponseEntity<
            ApiResponse<WebsiteBusinessHourExceptionResponse>
            > upsertBusinessHourException(
            @AuthenticationPrincipal
            AdminJwtPrincipal principal,

            @PathVariable
            UUID businessProfileId,

            @PathVariable
            LocalDate exceptionDate,

            @Valid
            @RequestBody
            WebsiteBusinessHourExceptionUpsertRequest request,

            HttpServletRequest httpRequest
    ) {
        if (!exceptionDate.equals(request.exceptionDate())) {
            throw new IllegalArgumentException(
                    "The route exception date and request exception "
                            + "date must match."
            );
        }

        WebsiteBusinessHourException exception =
                websiteBusinessHourExceptionService
                        .upsertBusinessHourException(
                                businessProfileId,
                                websiteBusinessHourExceptionMapper
                                        .toEntity(request),
                                requireAdministratorId(principal)
                        );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Website business-hour exception saved successfully.",
                        websiteBusinessHourExceptionMapper
                                .toResponse(exception),
                        httpRequest.getRequestURI()
                )
        );
    }

    /**
     * Updates an exception by identifier.
     *
     * PUT /api/v1/admin/website-business-hour-exceptions/{id}
     */
    @PutMapping("/{businessHourExceptionId}")
    public ResponseEntity<
            ApiResponse<WebsiteBusinessHourExceptionResponse>
            > updateBusinessHourException(
            @AuthenticationPrincipal
            AdminJwtPrincipal principal,

            @PathVariable
            UUID businessHourExceptionId,

            @Valid
            @RequestBody
            WebsiteBusinessHourExceptionUpsertRequest request,

            HttpServletRequest httpRequest
    ) {
        WebsiteBusinessHourException exception =
                websiteBusinessHourExceptionService
                        .updateBusinessHourException(
                                businessHourExceptionId,
                                websiteBusinessHourExceptionMapper
                                        .toEntity(request),
                                requireAdministratorId(principal)
                        );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Website business-hour exception updated successfully.",
                        websiteBusinessHourExceptionMapper
                                .toResponse(exception),
                        httpRequest.getRequestURI()
                )
        );
    }

    /**
     * Retrieves one exception.
     */
    @GetMapping("/{businessHourExceptionId}")
    public ResponseEntity<
            ApiResponse<WebsiteBusinessHourExceptionResponse>
            > getBusinessHourException(
            @PathVariable
            UUID businessHourExceptionId,

            HttpServletRequest httpRequest
    ) {
        WebsiteBusinessHourExceptionResponse response =
                websiteBusinessHourExceptionMapper.toResponse(
                        websiteBusinessHourExceptionService
                                .getBusinessHourException(
                                        businessHourExceptionId
                                )
                );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Website business-hour exception retrieved successfully.",
                        response,
                        httpRequest.getRequestURI()
                )
        );
    }

    /**
     * Retrieves one profile/date exception.
     */
    @GetMapping(
            "/profile/{businessProfileId}/date/{exceptionDate}"
    )
    public ResponseEntity<
            ApiResponse<WebsiteBusinessHourExceptionResponse>
            > getBusinessHourExceptionByDate(
            @PathVariable
            UUID businessProfileId,

            @PathVariable
            LocalDate exceptionDate,

            HttpServletRequest httpRequest
    ) {
        WebsiteBusinessHourExceptionResponse response =
                websiteBusinessHourExceptionMapper.toResponse(
                        websiteBusinessHourExceptionService
                                .getBusinessHourExceptionByDate(
                                        businessProfileId,
                                        exceptionDate
                                )
                );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Website business-hour exception retrieved successfully.",
                        response,
                        httpRequest.getRequestURI()
                )
        );
    }

    /**
     * Retrieves paginated exceptions for one profile.
     */
    @GetMapping("/profile/{businessProfileId}")
    public ResponseEntity<
            ApiResponse<Page<WebsiteBusinessHourExceptionResponse>>
            > getBusinessHourExceptionsByProfile(
            @PathVariable
            UUID businessProfileId,

            @PageableDefault(
                    size = 10,
                    sort = "exceptionDate",
                    direction = Sort.Direction.ASC
            )
            Pageable pageable,

            HttpServletRequest httpRequest
    ) {
        Page<WebsiteBusinessHourExceptionResponse> response =
                websiteBusinessHourExceptionService
                        .getBusinessHourExceptionsByProfile(
                                businessProfileId,
                                pageable
                        )
                        .map(
                                websiteBusinessHourExceptionMapper
                                        ::toResponse
                        );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Website business-hour exceptions retrieved successfully.",
                        response,
                        httpRequest.getRequestURI()
                )
        );
    }

    /**
     * Retrieves profile exceptions within an inclusive date range.
     */
    @GetMapping(
            "/profile/{businessProfileId}/range"
    )
    public ResponseEntity<
            ApiResponse<List<WebsiteBusinessHourExceptionResponse>>
            > getBusinessHourExceptionsByDateRange(
            @PathVariable
            UUID businessProfileId,

            @RequestParam
            LocalDate startDate,

            @RequestParam
            LocalDate endDate,

            HttpServletRequest httpRequest
    ) {
        List<WebsiteBusinessHourExceptionResponse> response =
                websiteBusinessHourExceptionService
                        .getBusinessHourExceptionsByDateRange(
                                businessProfileId,
                                startDate,
                                endDate
                        )
                        .stream()
                        .map(
                                websiteBusinessHourExceptionMapper
                                        ::toResponse
                        )
                        .toList();

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Website business-hour exceptions retrieved successfully.",
                        response,
                        httpRequest.getRequestURI()
                )
        );
    }

    /**
     * Retrieves upcoming exceptions.
     */
    @GetMapping(
            "/profile/{businessProfileId}/upcoming"
    )
    public ResponseEntity<
            ApiResponse<List<WebsiteBusinessHourExceptionResponse>>
            > getUpcomingBusinessHourExceptions(
            @PathVariable
            UUID businessProfileId,

            @RequestParam(required = false)
            LocalDate startDate,

            HttpServletRequest httpRequest
    ) {
        List<WebsiteBusinessHourExceptionResponse> response =
                websiteBusinessHourExceptionService
                        .getUpcomingBusinessHourExceptions(
                                businessProfileId,
                                startDate
                        )
                        .stream()
                        .map(
                                websiteBusinessHourExceptionMapper
                                        ::toResponse
                        )
                        .toList();

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Upcoming website business-hour exceptions retrieved successfully.",
                        response,
                        httpRequest.getRequestURI()
                )
        );
    }

    /**
     * Deletes one exception.
     */
    @DeleteMapping("/{businessHourExceptionId}")
    public ResponseEntity<Void> deleteBusinessHourException(
            @AuthenticationPrincipal
            AdminJwtPrincipal principal,

            @PathVariable
            UUID businessHourExceptionId
    ) {
        websiteBusinessHourExceptionService
                .deleteBusinessHourException(
                        businessHourExceptionId,
                        requireAdministratorId(principal)
                );

        return ResponseEntity.noContent().build();
    }

    /**
     * Deletes every exception belonging to one profile.
     */
    @DeleteMapping("/profile/{businessProfileId}")
    public ResponseEntity<Void>
    deleteBusinessHourExceptionsByProfile(
            @AuthenticationPrincipal
            AdminJwtPrincipal principal,

            @PathVariable
            UUID businessProfileId
    ) {
        websiteBusinessHourExceptionService
                .deleteBusinessHourExceptionsByProfile(
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