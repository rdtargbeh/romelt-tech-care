package romelt_techcare.backend.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import romelt_techcare.backend.api.ApiResponse;
import romelt_techcare.backend.dto.ContactInquiryConfirmationResponse;
import romelt_techcare.backend.dto.ContactInquiryCreateRequest;
import romelt_techcare.backend.service.ContactInquiryService;

/**
 * ================================================================
 * ROMELT TECHCARE — PUBLIC CONTACT INQUIRY CONTROLLER
 * ================================================================
 *
 * Purpose:
 * Exposes the public API endpoint used by the website contact form.
 *
 * Endpoint:
 * POST /api/v1/public/contact-inquiries
 *
 * Security:
 * - This endpoint is public and must not require authentication.
 * - Production security should include CORS restrictions.
 * - Production protection should include request throttling.
 * - Public requests must remain subject to server-side validation.
 * ================================================================
 */
@RestController
@RequestMapping("/api/v1/public/contact-inquiries")
@RequiredArgsConstructor
public class PublicContactInquiryController {

    private final ContactInquiryService contactInquiryService;

    @PostMapping
    public ResponseEntity<
            ApiResponse<ContactInquiryConfirmationResponse>
            > createInquiry(
            @Valid
            @RequestBody
            ContactInquiryCreateRequest request,
            HttpServletRequest httpRequest
    ) {
        ContactInquiryConfirmationResponse response =
                contactInquiryService.createInquiry(request);

        ApiResponse<ContactInquiryConfirmationResponse> body =
                ApiResponse.success(
                        "Contact inquiry submitted successfully.",
                        response,
                        httpRequest.getRequestURI()
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(body);
    }
}