package romelt_techcare.backend.dto;

import romelt_techcare.backend.entity.BookingRequest;
import romelt_techcare.backend.enums.BookingFor;
import romelt_techcare.backend.enums.BookingRequestStatus;
import romelt_techcare.backend.enums.BookingSource;
import romelt_techcare.backend.enums.ContactMethod;
import romelt_techcare.backend.enums.PreferredServiceTime;
import romelt_techcare.backend.enums.ServiceMethod;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * ================================================================
 * ROMELT TECHCARE — ADMIN BOOKING REQUEST RESPONSE
 * ================================================================
 *
 * Purpose:
 * Returns complete booking information to authenticated
 * administrators.
 *
 * Security:
 * This response is administrator-only and must never be returned
 * through the public booking controller.
 * ================================================================
 */
public record AdminBookingRequestResponse(

        UUID bookingRequestId,

        UUID customerId,

        String referenceNumber,

        BookingFor bookingFor,

        String fullName,

        String email,

        String phone,

        ContactMethod preferredContactMethod,

        String notificationEmail,

        String notificationPhone,

        String businessName,

        String businessEmail,

        String businessPhone,

        String businessStreetAddress,

        String businessCity,

        String businessState,

        String businessPostalCode,

        String businessCountryCode,

        String businessContactRole,

        UUID serviceId,

        String serviceType,

        ServiceMethod serviceMethod,

        LocalDate preferredDate,

        PreferredServiceTime preferredTime,

        LocalDate alternateDate,

        String deviceType,

        String problemDescription,

        String streetAddress,

        String addressLine2,

        String city,

        String stateRegion,

        String postalCode,

        String countryCode,

        Instant scheduledStartAt,

        Instant scheduledEndAt,

        String scheduledTimezone,

        boolean consentAccepted,

        Instant consentAcceptedAt,

        String consentVersion,

        BookingRequestStatus status,

        BookingSource bookingSource,

        Instant confirmedAt,

        Instant completedAt,

        Instant cancelledAt,

        Instant declinedAt,

        Instant expiredAt,

        String cancellationReason,

        String declineReason,

        String expirationReason,

        String completionSummary,

        String completionNotes,

        boolean reviewEligible,

        String reviewEligibilityNotes,

        UUID assignedAdminUserId,

        UUID createdByAdminUserId,

        String createdByAdminName,

        UUID updatedByAdminUserId,

        UUID confirmedByAdminUserId,

        UUID completedByAdminUserId,

        UUID cancelledByAdminUserId,

        UUID declinedByAdminUserId,

        UUID expiredByAdminUserId,

        String adminNotes,

        Instant submittedAt,

        Instant createdAt,

        Instant updatedAt,

        Long rowVersion
) {

    public static AdminBookingRequestResponse from(
            BookingRequest bookingRequest
    ) {
        if (bookingRequest == null) {
            throw new IllegalArgumentException(
                    "Booking request must not be null."
            );
        }

        return new AdminBookingRequestResponse(
                bookingRequest.getBookingRequestId(),
                bookingRequest.getCustomerId(),
                bookingRequest.getReferenceNumber(),
                bookingRequest.getBookingFor(),
                bookingRequest.getFullName(),
                bookingRequest.getEmail(),
                bookingRequest.getPhone(),
                bookingRequest.getPreferredContactMethod(),
                bookingRequest.getNotificationEmail(),
                bookingRequest.getNotificationPhone(),
                bookingRequest.getBusinessName(),
                bookingRequest.getBusinessEmail(),
                bookingRequest.getBusinessPhone(),
                bookingRequest.getBusinessStreetAddress(),
                bookingRequest.getBusinessCity(),
                bookingRequest.getBusinessState(),
                bookingRequest.getBusinessPostalCode(),
                bookingRequest.getBusinessCountryCode(),
                bookingRequest.getBusinessContactRole(),
                bookingRequest.getServiceId(),
                bookingRequest.getServiceType(),
                bookingRequest.getServiceMethod(),
                bookingRequest.getPreferredDate(),
                bookingRequest.getPreferredTime(),
                bookingRequest.getAlternateDate(),
                bookingRequest.getDeviceType(),
                bookingRequest.getProblemDescription(),
                bookingRequest.getStreetAddress(),
                bookingRequest.getAddressLine2(),
                bookingRequest.getCity(),
                bookingRequest.getStateRegion(),
                bookingRequest.getPostalCode(),
                bookingRequest.getCountryCode(),
                bookingRequest.getScheduledStartAt(),
                bookingRequest.getScheduledEndAt(),
                bookingRequest.getScheduledTimezone(),
                bookingRequest.isConsentAccepted(),
                bookingRequest.getConsentAcceptedAt(),
                bookingRequest.getConsentVersion(),
                bookingRequest.getStatus(),
                bookingRequest.getBookingSource(),
                bookingRequest.getConfirmedAt(),
                bookingRequest.getCompletedAt(),
                bookingRequest.getCancelledAt(),
                bookingRequest.getDeclinedAt(),
                bookingRequest.getExpiredAt(),
                bookingRequest.getCancellationReason(),
                bookingRequest.getDeclineReason(),
                bookingRequest.getExpirationReason(),
                bookingRequest.getCompletionSummary(),
                bookingRequest.getCompletionNotes(),
                bookingRequest.isReviewEligible(),
                bookingRequest.getReviewEligibilityNotes(),
                bookingRequest.getAssignedAdminUserId(),
                bookingRequest.getCreatedByAdminUserId(),
                bookingRequest.getCreatedByAdminName(),
                bookingRequest.getUpdatedByAdminUserId(),
                bookingRequest.getConfirmedByAdminUserId(),
                bookingRequest.getCompletedByAdminUserId(),
                bookingRequest.getCancelledByAdminUserId(),
                bookingRequest.getDeclinedByAdminUserId(),
                bookingRequest.getExpiredByAdminUserId(),
                bookingRequest.getAdminNotes(),
                bookingRequest.getSubmittedAt(),
                bookingRequest.getCreatedAt(),
                bookingRequest.getUpdatedAt(),
                bookingRequest.getRowVersion()
        );
    }
}