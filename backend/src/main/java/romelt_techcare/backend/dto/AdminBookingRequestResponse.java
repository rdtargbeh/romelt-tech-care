package romelt_techcare.backend.dto;

import romelt_techcare.backend.entity.BookingRequest;
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
 * Returns complete booking information to authenticated Romelt
 * TechCare administrators.
 *
 * Responsibilities:
 * - Supports administrator booking list pages.
 * - Supports administrator booking detail pages.
 * - Supports administrator-created booking confirmations.
 * - Exposes customer, service, schedule, and address information.
 * - Exposes booking source and creating administrator information.
 * - Exposes private operational notes only to administrators.
 *
 * Real-data integration:
 * Records originate from the shared booking_requests database table.
 * ================================================================
 */
public record AdminBookingRequestResponse(

        UUID bookingRequestId,

        String referenceNumber,

        String fullName,

        String email,

        String phone,

        String serviceType,

        ServiceMethod serviceMethod,

        LocalDate preferredDate,

        PreferredServiceTime preferredTime,

        LocalDate alternateDate,

        String streetAddress,

        String city,

        String state,

        String postalCode,

        String deviceType,

        String problemDescription,

        ContactMethod preferredContactMethod,

        BookingRequestStatus status,

        BookingSource bookingSource,

        UUID createdByAdminUserId,

        String createdByAdminName,

        String adminNotes,

        boolean consentAccepted,

        Instant submittedAt,

        Instant createdAt,

        Instant updatedAt
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
                bookingRequest.getReferenceNumber(),
                bookingRequest.getFullName(),
                bookingRequest.getEmail(),
                bookingRequest.getPhone(),
                bookingRequest.getServiceType(),
                bookingRequest.getServiceMethod(),
                bookingRequest.getPreferredDate(),
                bookingRequest.getPreferredTime(),
                bookingRequest.getAlternateDate(),
                bookingRequest.getStreetAddress(),
                bookingRequest.getCity(),
                bookingRequest.getState(),
                bookingRequest.getPostalCode(),
                bookingRequest.getDeviceType(),
                bookingRequest.getProblemDescription(),
                bookingRequest.getPreferredContactMethod(),
                bookingRequest.getStatus(),
                bookingRequest.getBookingSource(),
                bookingRequest.getCreatedByAdminUserId(),
                bookingRequest.getCreatedByAdminName(),
                bookingRequest.getAdminNotes(),
                bookingRequest.isConsentAccepted(),
                bookingRequest.getSubmittedAt(),
                bookingRequest.getCreatedAt(),
                bookingRequest.getUpdatedAt()
        );
    }
}