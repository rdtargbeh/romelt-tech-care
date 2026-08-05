package romelt_techcare.backend.dto;

import romelt_techcare.backend.entity.ContactInquiry;
import romelt_techcare.backend.enums.ContactInquiryStatus;
import romelt_techcare.backend.enums.ContactMethod;

import java.time.Instant;
import java.util.UUID;

/**
 * ================================================================
 * ROMELT TECHCARE — ADMIN CONTACT INQUIRY RESPONSE
 * ================================================================
 *
 * Purpose:
 * Returns complete contact-inquiry information to authenticated
 * Romelt TechCare administrators.
 *
 * Responsibilities:
 * - Supports administrator inquiry list pages.
 * - Supports administrator inquiry detail pages.
 * - Exposes customer contact information and submitted messages.
 * - Never exposes authentication credentials or security secrets.
 *
 * Real-data integration:
 * Records originate from the contact_inquiries database table.
 * ================================================================
 */
public record AdminContactInquiryResponse(

        UUID contactInquiryId,

        String referenceNumber,

        String fullName,

        String email,

        String phone,

        String subject,

        String serviceType,

        String message,

        ContactMethod preferredContactMethod,

        ContactInquiryStatus status,

        boolean consentAccepted,

        Instant submittedAt,

        Instant createdAt,

        Instant updatedAt
) {

    public static AdminContactInquiryResponse from(
            ContactInquiry contactInquiry
    ) {
        if (contactInquiry == null) {
            throw new IllegalArgumentException(
                    "Contact inquiry must not be null."
            );
        }

        return new AdminContactInquiryResponse(
                contactInquiry.getContactInquiryId(),
                contactInquiry.getReferenceNumber(),
                contactInquiry.getFullName(),
                contactInquiry.getEmail(),
                contactInquiry.getPhone(),
                contactInquiry.getSubject(),
                contactInquiry.getServiceType(),
                contactInquiry.getMessage(),
                contactInquiry.getPreferredContactMethod(),
                contactInquiry.getStatus(),
                contactInquiry.isConsentAccepted(),
                contactInquiry.getSubmittedAt(),
                contactInquiry.getCreatedAt(),
                contactInquiry.getUpdatedAt()
        );
    }
}