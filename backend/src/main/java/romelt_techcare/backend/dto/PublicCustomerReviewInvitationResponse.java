package romelt_techcare.backend.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Safe public response returned after token validation.
 */
public record PublicCustomerReviewInvitationResponse(

        UUID reviewInvitationId,

        UUID bookingRequestId,

        String bookingReferenceNumber,

        String customerDisplayName,

        String serviceType,

        Instant expiresAt,

        Boolean valid
) {
}