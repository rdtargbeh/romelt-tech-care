package com.romelttechcare.backend.contact.service;

import com.romelttechcare.backend.contact.dto.ContactInquiryConfirmationResponse;
import com.romelttechcare.backend.contact.dto.ContactInquiryCreateRequest;

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