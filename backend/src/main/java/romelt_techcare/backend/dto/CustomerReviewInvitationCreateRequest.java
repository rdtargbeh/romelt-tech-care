package romelt_techcare.backend.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.UUID;

/**
 * Creates a verified-review invitation for one booking request.
 */
public record CustomerReviewInvitationCreateRequest(

        @NotNull(message = "Booking request ID is required.")
        UUID bookingRequestId,

        @NotNull(message = "Expiration time is required.")
        @Future(message = "Expiration time must be in the future.")
        Instant expiresAt
) {
}