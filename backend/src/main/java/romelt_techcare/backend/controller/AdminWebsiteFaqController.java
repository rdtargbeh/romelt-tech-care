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
import romelt_techcare.backend.dto.WebsiteFaqCreateRequest;
import romelt_techcare.backend.dto.WebsiteFaqResponse;
import romelt_techcare.backend.dto.WebsiteFaqStatusRequest;
import romelt_techcare.backend.dto.WebsiteFaqUpdateRequest;
import romelt_techcare.backend.entity.WebsiteFaq;
import romelt_techcare.backend.enums.WebsiteFaqStatus;
import romelt_techcare.backend.mapper.WebsiteFaqMapper;
import romelt_techcare.backend.service.WebsiteFaqService;

import java.util.UUID;

/**
 * ================================================================
 * ROMELT TECHCARE — ADMIN WEBSITE FAQ CONTROLLER
 * ================================================================
 *
 * Purpose:
 * Exposes protected administrator endpoints for managing stable
 * website FAQ identities.
 *
 * Responsibilities:
 * - Creates stable FAQ identities.
 * - Updates FAQ keys and lifecycle status.
 * - Retrieves and searches FAQ identities.
 * - Activates, deactivates, and archives FAQs.
 * - Soft-deletes and restores FAQs.
 * - Returns FAQ counts.
 *
 * Version lifecycle:
 * Draft creation, question and answer editing, publishing, archival,
 * and version history will be handled by
 * AdminWebsiteFaqVersionController.
 *
 * This controller does not expose direct version-pointer assignment.
 *
 * Base endpoint:
 * /api/v1/admin/website-faqs
 * ================================================================
 */
@RestController
@RequestMapping("/api/v1/admin/website-faqs")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
public class AdminWebsiteFaqController {

    private final WebsiteFaqService websiteFaqService;

    private final WebsiteFaqMapper websiteFaqMapper;

    @PostMapping
    public ResponseEntity<ApiResponse<WebsiteFaqResponse>>
    createFaq(
            @AuthenticationPrincipal
            AdminJwtPrincipal principal,

            @Valid
            @RequestBody
            WebsiteFaqCreateRequest request,

            HttpServletRequest httpRequest
    ) {
        WebsiteFaq faq =
                websiteFaqService.createFaq(
                        websiteFaqMapper.toEntity(request),
                        requireAdministratorId(principal)
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        ApiResponse.success(
                                "Website FAQ created successfully.",
                                websiteFaqMapper.toResponse(faq),
                                httpRequest.getRequestURI()
                        )
                );
    }

    @PutMapping("/{faqId}")
    public ResponseEntity<ApiResponse<WebsiteFaqResponse>>
    updateFaq(
            @AuthenticationPrincipal
            AdminJwtPrincipal principal,

            @PathVariable
            UUID faqId,

            @Valid
            @RequestBody
            WebsiteFaqUpdateRequest request,

            HttpServletRequest httpRequest
    ) {
        WebsiteFaq faq =
                websiteFaqService.updateFaq(
                        faqId,
                        websiteFaqMapper.toUpdateEntity(request),
                        requireAdministratorId(principal)
                );

        return successfulResponse(
                "Website FAQ updated successfully.",
                faq,
                httpRequest
        );
    }

    @GetMapping
    public ResponseEntity<
            ApiResponse<Page<WebsiteFaqResponse>>
            > searchFaqs(
            @RequestParam(required = false)
            String keyword,

            @RequestParam(required = false)
            WebsiteFaqStatus faqStatus,

            @PageableDefault(
                    size = 10,
                    sort = "faqKey",
                    direction = Sort.Direction.ASC
            )
            Pageable pageable,

            HttpServletRequest httpRequest
    ) {
        Page<WebsiteFaqResponse> response =
                websiteFaqService
                        .searchFaqs(
                                keyword,
                                faqStatus,
                                pageable
                        )
                        .map(websiteFaqMapper::toResponse);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Website FAQs retrieved successfully.",
                        response,
                        httpRequest.getRequestURI()
                )
        );
    }

    @GetMapping("/deleted")
    public ResponseEntity<
            ApiResponse<Page<WebsiteFaqResponse>>
            > getDeletedFaqs(
            @PageableDefault(
                    size = 10,
                    sort = "deletedAt",
                    direction = Sort.Direction.DESC
            )
            Pageable pageable,

            HttpServletRequest httpRequest
    ) {
        Page<WebsiteFaqResponse> response =
                websiteFaqService
                        .getDeletedFaqs(pageable)
                        .map(websiteFaqMapper::toResponse);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Deleted website FAQs retrieved successfully.",
                        response,
                        httpRequest.getRequestURI()
                )
        );
    }

    @GetMapping("/{faqId}")
    public ResponseEntity<ApiResponse<WebsiteFaqResponse>>
    getFaq(
            @PathVariable
            UUID faqId,

            HttpServletRequest httpRequest
    ) {
        return successfulResponse(
                "Website FAQ retrieved successfully.",
                websiteFaqService.getFaq(faqId),
                httpRequest
        );
    }

    @GetMapping("/by-key/{faqKey}")
    public ResponseEntity<ApiResponse<WebsiteFaqResponse>>
    getFaqByKey(
            @PathVariable
            String faqKey,

            HttpServletRequest httpRequest
    ) {
        return successfulResponse(
                "Website FAQ retrieved successfully.",
                websiteFaqService.getFaqByKey(faqKey),
                httpRequest
        );
    }

    @PatchMapping("/{faqId}/status")
    public ResponseEntity<ApiResponse<WebsiteFaqResponse>>
    updateFaqStatus(
            @AuthenticationPrincipal
            AdminJwtPrincipal principal,

            @PathVariable
            UUID faqId,

            @Valid
            @RequestBody
            WebsiteFaqStatusRequest request,

            HttpServletRequest httpRequest
    ) {
        WebsiteFaq faq =
                websiteFaqService.updateFaqStatus(
                        faqId,
                        request.faqStatus(),
                        requireAdministratorId(principal)
                );

        return successfulResponse(
                "Website FAQ status updated successfully.",
                faq,
                httpRequest
        );
    }

    @PatchMapping("/{faqId}/activate")
    public ResponseEntity<ApiResponse<WebsiteFaqResponse>>
    activateFaq(
            @AuthenticationPrincipal
            AdminJwtPrincipal principal,

            @PathVariable
            UUID faqId,

            HttpServletRequest httpRequest
    ) {
        WebsiteFaq faq =
                websiteFaqService.activateFaq(
                        faqId,
                        requireAdministratorId(principal)
                );

        return successfulResponse(
                "Website FAQ activated successfully.",
                faq,
                httpRequest
        );
    }

    @PatchMapping("/{faqId}/deactivate")
    public ResponseEntity<ApiResponse<WebsiteFaqResponse>>
    deactivateFaq(
            @AuthenticationPrincipal
            AdminJwtPrincipal principal,

            @PathVariable
            UUID faqId,

            HttpServletRequest httpRequest
    ) {
        WebsiteFaq faq =
                websiteFaqService.deactivateFaq(
                        faqId,
                        requireAdministratorId(principal)
                );

        return successfulResponse(
                "Website FAQ deactivated successfully.",
                faq,
                httpRequest
        );
    }

    @PatchMapping("/{faqId}/archive")
    public ResponseEntity<ApiResponse<WebsiteFaqResponse>>
    archiveFaq(
            @AuthenticationPrincipal
            AdminJwtPrincipal principal,

            @PathVariable
            UUID faqId,

            HttpServletRequest httpRequest
    ) {
        WebsiteFaq faq =
                websiteFaqService.archiveFaq(
                        faqId,
                        requireAdministratorId(principal)
                );

        return successfulResponse(
                "Website FAQ archived successfully.",
                faq,
                httpRequest
        );
    }

    @DeleteMapping("/{faqId}")
    public ResponseEntity<Void> deleteFaq(
            @AuthenticationPrincipal
            AdminJwtPrincipal principal,

            @PathVariable
            UUID faqId
    ) {
        websiteFaqService.deleteFaq(
                faqId,
                requireAdministratorId(principal)
        );

        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{faqId}/restore")
    public ResponseEntity<ApiResponse<WebsiteFaqResponse>>
    restoreFaq(
            @AuthenticationPrincipal
            AdminJwtPrincipal principal,

            @PathVariable
            UUID faqId,

            HttpServletRequest httpRequest
    ) {
        WebsiteFaq faq =
                websiteFaqService.restoreFaq(
                        faqId,
                        requireAdministratorId(principal)
                );

        return successfulResponse(
                "Website FAQ restored successfully.",
                faq,
                httpRequest
        );
    }

    @GetMapping("/counts/status/{faqStatus}")
    public ResponseEntity<ApiResponse<Long>>
    countFaqsByStatus(
            @PathVariable
            WebsiteFaqStatus faqStatus,

            HttpServletRequest httpRequest
    ) {
        long count =
                websiteFaqService.countFaqsByStatus(
                        faqStatus
                );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Website FAQ count retrieved successfully.",
                        count,
                        httpRequest.getRequestURI()
                )
        );
    }

    @GetMapping("/counts/deleted")
    public ResponseEntity<ApiResponse<Long>>
    countDeletedFaqs(
            HttpServletRequest httpRequest
    ) {
        long count =
                websiteFaqService.countDeletedFaqs();

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Deleted website FAQ count retrieved successfully.",
                        count,
                        httpRequest.getRequestURI()
                )
        );
    }

    private ResponseEntity<ApiResponse<WebsiteFaqResponse>>
    successfulResponse(
            String message,
            WebsiteFaq faq,
            HttpServletRequest request
    ) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        message,
                        websiteFaqMapper.toResponse(faq),
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