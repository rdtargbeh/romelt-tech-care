package romelt_techcare.backend.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * ================================================================
 * ROMELT TECHCARE — CONTACT INQUIRY CONFIRMATION RESPONSE
 * ================================================================
 *
 * Purpose:
 * Returns safe confirmation information after a public inquiry is
 * accepted.
 *
 * Security:
 * This response intentionally excludes internal notes, assignment
 * data, administrative status history, and private processing details.
 * ================================================================
 */
public record ContactInquiryConfirmationResponse(
        UUID inquiryId,
        String referenceNumber,
        String message,
        Instant submittedAt
) {
}