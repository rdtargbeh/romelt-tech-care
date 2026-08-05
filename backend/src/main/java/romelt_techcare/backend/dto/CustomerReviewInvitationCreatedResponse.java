package romelt_techcare.backend.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Returned once immediately after invitation creation.
 *
 * Security:
 * The plain token and review URL must not be persisted or logged.
 */
public record CustomerReviewInvitationCreatedResponse(

        UUID reviewInvitationId,

        UUID bookingRequestId,

        String bookingReferenceNumber,

        String customerEmail,

        String token,

        String reviewUrl,

        Instant expiresAt,

        String message
) {
}