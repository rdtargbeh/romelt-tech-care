package romelt_techcare.backend.mapper;

import org.springframework.stereotype.Component;
import romelt_techcare.backend.dto.ContactInquiryConfirmationResponse;
import romelt_techcare.backend.dto.ContactInquiryCreateRequest;
import romelt_techcare.backend.entity.ContactInquiry;
import romelt_techcare.backend.enums.ContactInquiryStatus;

/**
 * ================================================================
 * ROMELT TECHCARE — CONTACT INQUIRY MAPPER
 * ================================================================
 *
 * Purpose:
 * Converts public contact DTOs to and from persistence entities.
 *
 * Responsibilities:
 * - Normalizes customer text before storage.
 * - Assigns the initial inquiry status.
 * - Prevents controller and service classes from manually copying
 *   fields.
 * ================================================================
 */
@Component
public class ContactInquiryMapper {

    public ContactInquiry toEntity(
            ContactInquiryCreateRequest request,
            String referenceNumber
    ) {
        return ContactInquiry.builder()
                .referenceNumber(referenceNumber)
                .fullName(normalizeRequired(request.fullName()))
                .email(
                        normalizeRequired(request.email())
                                .toLowerCase()
                )
                .phone(normalizeOptional(request.phone()))
                .subject(normalizeRequired(request.subject()))
                .serviceType(
                        normalizeOptional(request.serviceType())
                )
                .message(normalizeRequired(request.message()))
                .preferredContactMethod(
                        request.preferredContactMethod()
                )
                .consentAccepted(request.consentAccepted())
                .status(ContactInquiryStatus.NEW)
                .build();
    }

    public ContactInquiryConfirmationResponse toConfirmationResponse(
            ContactInquiry inquiry
    ) {
        return new ContactInquiryConfirmationResponse(
                inquiry.getContactInquiryId(),
                inquiry.getReferenceNumber(),
                "Your message was received successfully. Romelt TechCare will review your inquiry and respond using your preferred contact method.",
                inquiry.getSubmittedAt()
        );
    }

    private String normalizeRequired(
            String value
    ) {
        return value.trim();
    }

    private String normalizeOptional(
            String value
    ) {
        if (value == null) {
            return null;
        }

        String normalizedValue = value.trim();

        return normalizedValue.isEmpty()
                ? null
                : normalizedValue;
    }
}