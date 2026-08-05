package romelt_techcare.backend.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import romelt_techcare.backend.dto.AdminContactInquiryResponse;
import romelt_techcare.backend.dto.AdminContactInquiryStatusUpdateRequest;
import romelt_techcare.backend.dto.AdminJwtPrincipal;

import java.util.UUID;

/**
 * ================================================================
 * ROMELT TECHCARE — ADMIN CONTACT INQUIRY SERVICE
 * ================================================================
 *
 * Purpose:
 * Defines authenticated administrator operations for contact
 * inquiries submitted through the public website.
 *
 * Responsibilities:
 * - Returns paginated contact inquiries.
 * - Returns one complete contact inquiry.
 * - Updates inquiry lifecycle status.
 * ================================================================
 */
public interface AdminContactInquiryService {

    Page<AdminContactInquiryResponse> getContactInquiries(
            Pageable pageable
    );

    AdminContactInquiryResponse getContactInquiry(
            UUID contactInquiryId
    );

    AdminContactInquiryResponse updateContactInquiryStatus(
            AdminJwtPrincipal principal,
            UUID contactInquiryId,
            AdminContactInquiryStatusUpdateRequest request
    );
}