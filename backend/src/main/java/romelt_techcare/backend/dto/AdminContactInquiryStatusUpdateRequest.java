package romelt_techcare.backend.dto;

import jakarta.validation.constraints.NotNull;
import romelt_techcare.backend.enums.ContactInquiryStatus;

/**
 * ================================================================
 * ROMELT TECHCARE — CONTACT INQUIRY STATUS UPDATE REQUEST
 * ================================================================
 *
 * Purpose:
 * Validates an administrator request to update the operational status
 * of a contact inquiry.
 *
 * Endpoint:
 * PATCH /api/v1/admin/contact-inquiries/{contactInquiryId}/status
 * ================================================================
 */
public record AdminContactInquiryStatusUpdateRequest(

        @NotNull(
                message = "Select a contact inquiry status."
        )
        ContactInquiryStatus status
) {
}