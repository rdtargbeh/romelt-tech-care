package romelt_techcare.backend.mapper;

import org.springframework.stereotype.Component;
import romelt_techcare.backend.dto.CustomerReviewInvitationResponse;
import romelt_techcare.backend.dto.PublicCustomerReviewInvitationResponse;
import romelt_techcare.backend.entity.AdminUser;
import romelt_techcare.backend.entity.BookingRequest;
import romelt_techcare.backend.entity.CustomerReviewInvitation;

import java.util.UUID;

/**
 * Maps customer-review invitations into safe responses.
 */
@Component
public class CustomerReviewInvitationMapper {

    public CustomerReviewInvitationResponse toResponse(
            CustomerReviewInvitation invitation
    ) {
        if (invitation == null) {
            return null;
        }

        BookingRequest booking =
                invitation.getBookingRequest();

        AdminUser createdBy =
                invitation.getCreatedByAdminUser();

        AdminUser revokedBy =
                invitation.getRevokedByAdminUser();

        return new CustomerReviewInvitationResponse(
                invitation.getReviewInvitationId(),
                booking == null
                        ? null
                        : booking.getBookingRequestId(),
                booking == null
                        ? null
                        : booking.getReferenceNumber(),
                booking == null
                        ? null
                        : booking.getFullName(),
                invitation.getCustomerEmail(),
                booking == null
                        ? null
                        : booking.getServiceType(),
                invitation.getInvitationStatus(),
                invitation.getExpiresAt(),
                invitation.getSentAt(),
                invitation.getUsedAt(),
                invitation.getRevokedAt(),
                adminId(createdBy),
                adminName(createdBy),
                adminId(revokedBy),
                adminName(revokedBy),
                invitation.getCreatedAt(),
                invitation.isUsable(),
                invitation.isExpired()
        );
    }

    public PublicCustomerReviewInvitationResponse
    toPublicResponse(
            CustomerReviewInvitation invitation
    ) {
        if (invitation == null) {
            return null;
        }

        BookingRequest booking =
                invitation.getBookingRequest();

        return new PublicCustomerReviewInvitationResponse(
                invitation.getReviewInvitationId(),
                booking == null
                        ? null
                        : booking.getBookingRequestId(),
                booking == null
                        ? null
                        : booking.getReferenceNumber(),
                booking == null
                        ? null
                        : firstName(booking.getFullName()),
                booking == null
                        ? null
                        : booking.getServiceType(),
                invitation.getExpiresAt(),
                invitation.isUsable()
        );
    }

    private UUID adminId(
            AdminUser administrator
    ) {
        return administrator == null
                ? null
                : administrator.getAdminUserId();
    }

    private String adminName(
            AdminUser administrator
    ) {
        return administrator == null
                ? null
                : administrator.getFullName();
    }

    private String firstName(
            String fullName
    ) {
        if (
                fullName == null
                        || fullName.isBlank()
        ) {
            return null;
        }

        String normalized = fullName.trim();
        int firstSpace = normalized.indexOf(' ');

        return firstSpace < 0
                ? normalized
                : normalized.substring(0, firstSpace);
    }
}