package romelt_techcare.backend.dto;

import romelt_techcare.backend.enums.CustomerReviewInvitationStatus;

import java.time.Instant;
import java.util.UUID;

/**
 * Administrator-facing review-invitation response.
 *
 * The plain token is intentionally excluded.
 */
public record CustomerReviewInvitationResponse(

        UUID reviewInvitationId,

        UUID bookingRequestId,

        String bookingReferenceNumber,

        String customerName,

        String customerEmail,

        String serviceType,

        CustomerReviewInvitationStatus invitationStatus,

        Instant expiresAt,

        Instant sentAt,

        Instant usedAt,

        Instant revokedAt,

        UUID createdByAdminUserId,

        String createdByAdminUserDisplayName,

        UUID revokedByAdminUserId,

        String revokedByAdminUserDisplayName,

        Instant createdAt,

        Boolean usable,

        Boolean expired
) {
}