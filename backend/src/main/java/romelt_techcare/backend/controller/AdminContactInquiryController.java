package romelt_techcare.backend.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import romelt_techcare.backend.api.ApiResponse;
import romelt_techcare.backend.dto.AdminContactInquiryResponse;
import romelt_techcare.backend.dto.AdminContactInquiryStatusUpdateRequest;
import romelt_techcare.backend.dto.AdminJwtPrincipal;
import romelt_techcare.backend.service.AdminContactInquiryService;

import java.util.UUID;

/**
 * ================================================================
 * ROMELT TECHCARE — ADMIN CONTACT INQUIRY CONTROLLER
 * ================================================================
 *
 * Purpose:
 * Allows authenticated administrators to manage customer contact
 * inquiries submitted through the public website.
 *
 * Responsibilities:
 * - Returns paginated contact inquiries.
 * - Returns one complete inquiry.
 * - Updates inquiry lifecycle status.
 *
 * Endpoints:
 * GET   /api/v1/admin/contact-inquiries
 * GET   /api/v1/admin/contact-inquiries/{contactInquiryId}
 * PATCH /api/v1/admin/contact-inquiries/{contactInquiryId}/status
 * ================================================================
 */
@RestController
@RequestMapping("/api/v1/admin/contact-inquiries")
@RequiredArgsConstructor
public class AdminContactInquiryController {

    private final AdminContactInquiryService
            adminContactInquiryService;

    /**
     * Returns contact inquiries from newest to oldest.
     */
    @GetMapping
    public ResponseEntity<
            ApiResponse<Page<AdminContactInquiryResponse>>
            > getContactInquiries(
            @PageableDefault(
                    size = 10,
                    sort = "submittedAt",
                    direction = Sort.Direction.DESC
            )
            Pageable pageable,
            HttpServletRequest httpRequest
    ) {
        Page<AdminContactInquiryResponse> response =
                adminContactInquiryService
                        .getContactInquiries(
                                pageable
                        );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Contact inquiries retrieved successfully.",
                        response,
                        httpRequest.getRequestURI()
                )
        );
    }

    /**
     * Returns one complete contact inquiry.
     */
    @GetMapping("/{contactInquiryId}")
    public ResponseEntity<
            ApiResponse<AdminContactInquiryResponse>
            > getContactInquiry(
            @PathVariable
            UUID contactInquiryId,
            HttpServletRequest httpRequest
    ) {
        AdminContactInquiryResponse response =
                adminContactInquiryService
                        .getContactInquiry(
                                contactInquiryId
                        );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Contact inquiry retrieved successfully.",
                        response,
                        httpRequest.getRequestURI()
                )
        );
    }

    /**
     * Updates a contact inquiry's lifecycle status.
     */
    @PatchMapping("/{contactInquiryId}/status")
    public ResponseEntity<
            ApiResponse<AdminContactInquiryResponse>
            > updateContactInquiryStatus(
            @AuthenticationPrincipal
            AdminJwtPrincipal principal,
            @PathVariable
            UUID contactInquiryId,
            @Valid
            @RequestBody
            AdminContactInquiryStatusUpdateRequest request,
            HttpServletRequest httpRequest
    ) {
        AdminContactInquiryResponse response =
                adminContactInquiryService
                        .updateContactInquiryStatus(
                                principal,
                                contactInquiryId,
                                request
                        );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Contact inquiry status updated successfully.",
                        response,
                        httpRequest.getRequestURI()
                )
        );
    }
}