package romelt_techcare.backend.service;

import romelt_techcare.backend.dto.ContactInquiryConfirmationResponse;
import romelt_techcare.backend.dto.ContactInquiryCreateRequest;

/**
 * ================================================================
 * ROMELT TECHCARE — CONTACT INQUIRY SERVICE
 * ================================================================
 *
 * Purpose:
 * Defines business operations for public contact inquiries.
 * ================================================================
 */
public interface ContactInquiryService {

    ContactInquiryConfirmationResponse createInquiry(
            ContactInquiryCreateRequest request
    );
}