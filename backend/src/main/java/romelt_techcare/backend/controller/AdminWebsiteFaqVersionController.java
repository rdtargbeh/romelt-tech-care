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
import romelt_techcare.backend.dto.WebsiteFaqVersionCloneRequest;
import romelt_techcare.backend.dto.WebsiteFaqVersionCreateRequest;
import romelt_techcare.backend.dto.WebsiteFaqVersionResponse;
import romelt_techcare.backend.dto.WebsiteFaqVersionUpdateRequest;
import romelt_techcare.backend.entity.WebsiteFaqVersion;
import romelt_techcare.backend.enums.WebsiteFaqVersionStatus;
import romelt_techcare.backend.mapper.WebsiteFaqVersionMapper;
import romelt_techcare.backend.service.WebsiteFaqVersionService;

import java.util.UUID;

/**
 * ================================================================
 * ROMELT TECHCARE — ADMIN FAQ VERSION CONTROLLER
 * ================================================================
 *
 * Purpose:
 * Exposes protected administrator endpoints for FAQ draft creation,
 * editing, publishing, archival, deletion, and version history.
 *
 * Base endpoint:
 * /api/v1/admin/website-faq-versions
 * ================================================================
 */
@RestController
@RequestMapping("/api/v1/admin/website-faq-versions")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
public class AdminWebsiteFaqVersionController {

    private final WebsiteFaqVersionService
            websiteFaqVersionService;

    private final WebsiteFaqVersionMapper
            websiteFaqVersionMapper;

    @PostMapping("/faq/{faqId}/draft")
    public ResponseEntity<
            ApiResponse<WebsiteFaqVersionResponse>
            > createDraft(
            @AuthenticationPrincipal
            AdminJwtPrincipal principal,

            @PathVariable
            UUID faqId,

            @Valid
            @RequestBody
            WebsiteFaqVersionCreateRequest request,

            HttpServletRequest httpRequest
    ) {
        WebsiteFaqVersion version =
                websiteFaqVersionService.createDraft(
                        faqId,
                        websiteFaqVersionMapper.toEntity(
                                request
                        ),
                        requireAdministratorId(principal)
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        ApiResponse.success(
                                "Website FAQ draft created successfully.",
                                websiteFaqVersionMapper
                                        .toResponse(version),
                                httpRequest.getRequestURI()
                        )
                );
    }

    @PostMapping("/faq/{faqId}/draft-from-published")
    public ResponseEntity<
            ApiResponse<WebsiteFaqVersionResponse>
            > createDraftFromPublishedVersion(
            @AuthenticationPrincipal
            AdminJwtPrincipal principal,

            @PathVariable
            UUID faqId,

            @Valid
            @RequestBody(required = false)
            WebsiteFaqVersionCloneRequest request,

            HttpServletRequest httpRequest
    ) {
        String changeSummary =
                request == null
                        ? null
                        : request.changeSummary();

        WebsiteFaqVersion version =
                websiteFaqVersionService
                        .createDraftFromPublishedVersion(
                                faqId,
                                changeSummary,
                                requireAdministratorId(principal)
                        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        ApiResponse.success(
                                "Draft created from the published FAQ version.",
                                websiteFaqVersionMapper
                                        .toResponse(version),
                                httpRequest.getRequestURI()
                        )
                );
    }

    @PutMapping("/{faqVersionId}")
    public ResponseEntity<
            ApiResponse<WebsiteFaqVersionResponse>
            > updateDraft(
            @AuthenticationPrincipal
            AdminJwtPrincipal principal,

            @PathVariable
            UUID faqVersionId,

            @Valid
            @RequestBody
            WebsiteFaqVersionUpdateRequest request,

            HttpServletRequest httpRequest
    ) {
        WebsiteFaqVersion version =
                websiteFaqVersionService.updateDraft(
                        faqVersionId,
                        websiteFaqVersionMapper
                                .toUpdateEntity(request),
                        requireAdministratorId(principal)
                );

        return successfulResponse(
                "Website FAQ draft updated successfully.",
                version,
                httpRequest
        );
    }

    @GetMapping("/{faqVersionId}")
    public ResponseEntity<
            ApiResponse<WebsiteFaqVersionResponse>
            > getFaqVersion(
            @PathVariable
            UUID faqVersionId,

            HttpServletRequest httpRequest
    ) {
        return successfulResponse(
                "Website FAQ version retrieved successfully.",
                websiteFaqVersionService
                        .getFaqVersion(faqVersionId),
                httpRequest
        );
    }

    @GetMapping("/faq/{faqId}/draft")
    public ResponseEntity<
            ApiResponse<WebsiteFaqVersionResponse>
            > getCurrentDraft(
            @PathVariable
            UUID faqId,

            HttpServletRequest httpRequest
    ) {
        return successfulResponse(
                "Current website FAQ draft retrieved successfully.",
                websiteFaqVersionService
                        .getCurrentDraft(faqId),
                httpRequest
        );
    }

    @GetMapping("/faq/{faqId}/published")
    public ResponseEntity<
            ApiResponse<WebsiteFaqVersionResponse>
            > getCurrentPublishedVersion(
            @PathVariable
            UUID faqId,

            HttpServletRequest httpRequest
    ) {
        return successfulResponse(
                "Current published FAQ version retrieved successfully.",
                websiteFaqVersionService
                        .getCurrentPublishedVersion(faqId),
                httpRequest
        );
    }

    @GetMapping("/faq/{faqId}/history")
    public ResponseEntity<
            ApiResponse<Page<WebsiteFaqVersionResponse>>
            > getVersionHistory(
            @PathVariable
            UUID faqId,

            @RequestParam(required = false)
            WebsiteFaqVersionStatus versionStatus,

            @PageableDefault(
                    size = 10,
                    sort = "versionNumber",
                    direction = Sort.Direction.DESC
            )
            Pageable pageable,

            HttpServletRequest httpRequest
    ) {
        Page<WebsiteFaqVersionResponse> response =
                websiteFaqVersionService
                        .getVersionHistory(
                                faqId,
                                versionStatus,
                                pageable
                        )
                        .map(
                                websiteFaqVersionMapper::toResponse
                        );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Website FAQ version history retrieved successfully.",
                        response,
                        httpRequest.getRequestURI()
                )
        );
    }

    @PatchMapping("/{faqVersionId}/publish")
    public ResponseEntity<
            ApiResponse<WebsiteFaqVersionResponse>
            > publishDraft(
            @AuthenticationPrincipal
            AdminJwtPrincipal principal,

            @PathVariable
            UUID faqVersionId,

            HttpServletRequest httpRequest
    ) {
        WebsiteFaqVersion version =
                websiteFaqVersionService.publishDraft(
                        faqVersionId,
                        requireAdministratorId(principal)
                );

        return successfulResponse(
                "Website FAQ published successfully.",
                version,
                httpRequest
        );
    }

    @PatchMapping("/{faqVersionId}/archive")
    public ResponseEntity<
            ApiResponse<WebsiteFaqVersionResponse>
            > archiveVersion(
            @AuthenticationPrincipal
            AdminJwtPrincipal principal,

            @PathVariable
            UUID faqVersionId,

            HttpServletRequest httpRequest
    ) {
        WebsiteFaqVersion version =
                websiteFaqVersionService.archiveVersion(
                        faqVersionId,
                        requireAdministratorId(principal)
                );

        return successfulResponse(
                "Website FAQ version archived successfully.",
                version,
                httpRequest
        );
    }

    @DeleteMapping("/{faqVersionId}/draft")
    public ResponseEntity<Void> deleteDraft(
            @AuthenticationPrincipal
            AdminJwtPrincipal principal,

            @PathVariable
            UUID faqVersionId
    ) {
        websiteFaqVersionService.deleteDraft(
                faqVersionId,
                requireAdministratorId(principal)
        );

        return ResponseEntity.noContent().build();
    }

    private ResponseEntity<
            ApiResponse<WebsiteFaqVersionResponse>
            > successfulResponse(
            String message,
            WebsiteFaqVersion version,
            HttpServletRequest request
    ) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        message,
                        websiteFaqVersionMapper.toResponse(version),
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