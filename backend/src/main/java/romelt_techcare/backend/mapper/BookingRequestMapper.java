package romelt_techcare.backend.mapper;

import org.springframework.stereotype.Component;
import romelt_techcare.backend.dto.AdminBookingRequestCreateRequest;
import romelt_techcare.backend.dto.BookingRequestConfirmationResponse;
import romelt_techcare.backend.dto.BookingRequestCreateRequest;
import romelt_techcare.backend.entity.BookingRequest;
import romelt_techcare.backend.enums.BookingRequestStatus;
import romelt_techcare.backend.enums.BookingSource;

import java.util.Locale;
import java.util.UUID;

/**
 * ================================================================
 * ROMELT TECHCARE — BOOKING REQUEST MAPPER
 * ================================================================
 *
 * Purpose:
 * Converts public and administrator booking request DTOs into the
 * shared BookingRequest entity.
 *
 * Responsibilities:
 * - Normalizes customer-provided text.
 * - Maps public website booking requests.
 * - Maps administrator-created customer bookings.
 * - Records booking source and creating administrator.
 * - Assigns the initial PENDING status.
 * - Creates a safe public confirmation response.
 *
 * Security:
 * Private administrator notes are never included in the public
 * confirmation response.
 * ================================================================
 */
@Component
public class BookingRequestMapper {

    /**
     * Maps a public website booking request.
     */
    public BookingRequest toEntity(
            BookingRequestCreateRequest request,
            String referenceNumber
    ) {
        requirePublicRequest(request);

        return BookingRequest.builder()
                .referenceNumber(
                        normalizeRequired(referenceNumber)
                )
                .fullName(
                        normalizeRequired(request.fullName())
                )
                .email(
                        normalizeEmail(request.email())
                )
                .phone(
                        normalizeRequired(request.phone())
                )
                .serviceType(
                        normalizeRequired(request.serviceType())
                )
                .serviceMethod(
                        request.serviceMethod()
                )
                .preferredDate(
                        request.preferredDate()
                )
                .preferredTime(
                        request.preferredTime()
                )
                .alternateDate(
                        request.alternateDate()
                )
                .streetAddress(
                        normalizeOptional(request.streetAddress())
                )
                .city(
                        normalizeOptional(request.city())
                )
                .state(
                        normalizeOptional(request.state())
                )
                .postalCode(
                        normalizeOptional(request.postalCode())
                )
                .deviceType(
                        normalizeOptional(request.deviceType())
                )
                .problemDescription(
                        normalizeRequired(
                                request.problemDescription()
                        )
                )
                .preferredContactMethod(
                        request.preferredContactMethod()
                )
                .consentAccepted(
                        request.consentAccepted()
                )
                .status(
                        BookingRequestStatus.PENDING
                )
                .bookingSource(
                        BookingSource.WEBSITE
                )
                .createdByAdminUserId(null)
                .createdByAdminName(null)
                .adminNotes(null)
                .build();
    }

    /**
     * Maps a booking entered by an authenticated administrator for a
     * customer.
     */
    public BookingRequest toAdminEntity(
            AdminBookingRequestCreateRequest request,
            String referenceNumber,
            UUID administratorId,
            String administratorName
    ) {
        requireAdminRequest(request);

        return BookingRequest.builder()
                .referenceNumber(
                        normalizeRequired(referenceNumber)
                )
                .fullName(
                        normalizeRequired(request.fullName())
                )
                .email(
                        normalizeEmail(request.email())
                )
                .phone(
                        normalizeRequired(request.phone())
                )
                .serviceType(
                        normalizeRequired(request.serviceType())
                )
                .serviceMethod(
                        request.serviceMethod()
                )
                .preferredDate(
                        request.preferredDate()
                )
                .preferredTime(
                        request.preferredTime()
                )
                .alternateDate(
                        request.alternateDate()
                )
                .streetAddress(
                        normalizeOptional(request.streetAddress())
                )
                .city(
                        normalizeOptional(request.city())
                )
                .state(
                        normalizeOptional(request.state())
                )
                .postalCode(
                        normalizeOptional(request.postalCode())
                )
                .deviceType(
                        normalizeOptional(request.deviceType())
                )
                .problemDescription(
                        normalizeRequired(
                                request.problemDescription()
                        )
                )
                .preferredContactMethod(
                        request.preferredContactMethod()
                )
                /*
                 * The administrator entered this record on the
                 * customer's behalf, so the customer did not directly
                 * accept the public website disclaimer.
                 */
                .consentAccepted(false)
                .status(
                        BookingRequestStatus.PENDING
                )
                .bookingSource(
                        request.bookingSource()
                )
                .createdByAdminUserId(
                        administratorId
                )
                .createdByAdminName(
                        normalizeRequired(administratorName)
                )
                .adminNotes(
                        normalizeOptional(request.adminNotes())
                )
                .build();
    }

    /**
     * Creates the safe customer-facing booking confirmation.
     */
    public BookingRequestConfirmationResponse toConfirmationResponse(
            BookingRequest bookingRequest
    ) {
        if (bookingRequest == null) {
            throw new IllegalArgumentException(
                    "Booking request must not be null."
            );
        }

        return new BookingRequestConfirmationResponse(
                bookingRequest.getBookingRequestId(),
                bookingRequest.getReferenceNumber(),
                "Your booking request was received successfully. Romelt TechCare will review availability and contact you before the appointment is confirmed.",
                bookingRequest.getSubmittedAt(),
                bookingRequest.getPreferredDate(),
                bookingRequest.getPreferredTime(),
                bookingRequest.getStatus()
        );
    }

    private void requirePublicRequest(
            BookingRequestCreateRequest request
    ) {
        if (request == null) {
            throw new IllegalArgumentException(
                    "Public booking request must not be null."
            );
        }
    }

    private void requireAdminRequest(
            AdminBookingRequestCreateRequest request
    ) {
        if (request == null) {
            throw new IllegalArgumentException(
                    "Administrator booking request must not be null."
            );
        }
    }

    private String normalizeEmail(
            String value
    ) {
        return normalizeRequired(value)
                .toLowerCase(Locale.ROOT);
    }

    private String normalizeRequired(
            String value
    ) {
        if (value == null) {
            throw new IllegalArgumentException(
                    "Required booking value must not be null."
            );
        }

        String normalizedValue = value.trim();

        if (normalizedValue.isEmpty()) {
            throw new IllegalArgumentException(
                    "Required booking value must not be blank."
            );
        }

        return normalizedValue;
    }

    private String normalizeOptional(
            String value
    ) {
        if (value == null) {
            return null;
        }

        String normalizedValue = value.trim();

        return normalizedValue.isEmpty()
                ? null
                : normalizedValue;
    }
}