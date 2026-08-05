package romelt_techcare.backend.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import romelt_techcare.backend.dto.CustomerReviewInvitationCreatedResponse;
import romelt_techcare.backend.entity.CustomerReviewInvitation;
import romelt_techcare.backend.enums.CustomerReviewInvitationStatus;

import java.time.Instant;
import java.util.UUID;

/**
 * Defines secure review-invitation lifecycle operations.
 */
public interface CustomerReviewInvitationService {

    CustomerReviewInvitationCreatedResponse createInvitation(
            UUID bookingRequestId,
            Instant expiresAt,
            UUID administratorId
    );

    CustomerReviewInvitation getInvitation(
            UUID reviewInvitationId
    );

    Page<CustomerReviewInvitation> searchInvitations(
            String keyword,
            CustomerReviewInvitationStatus status,
            Pageable pageable
    );

    Page<CustomerReviewInvitation> getBookingInvitations(
            UUID bookingRequestId,
            Pageable pageable
    );

    CustomerReviewInvitation markInvitationSent(
            UUID reviewInvitationId,
            UUID administratorId
    );

    CustomerReviewInvitation revokeInvitation(
            UUID reviewInvitationId,
            UUID administratorId
    );

    CustomerReviewInvitation validatePublicToken(
            String plainToken
    );

    CustomerReviewInvitation consumeInvitation(
            String plainToken
    );

    int expireInvitations();
}