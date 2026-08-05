package romelt_techcare.backend.mapper;

import org.springframework.stereotype.Component;
import romelt_techcare.backend.dto.AdminBookingRequestCreateRequest;
import romelt_techcare.backend.dto.BookingRequestConfirmationResponse;
import romelt_techcare.backend.dto.BookingRequestCreateRequest;
import romelt_techcare.backend.entity.BookingRequest;
import romelt_techcare.backend.enums.BookingFor;
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
 * Converts public and administrator booking DTOs into the existing
 * BookingRequest entity while integrating the new fields.
 *
 * Normalization:
 * - Original values remain available for display.
 * - Email matching values are converted to lowercase.
 * - Phone matching values contain digits only.
 * ================================================================
 */
@Component
public class BookingRequestMapper {

    private static final String PUBLIC_CONSENT_VERSION =
            "PUBLIC_BOOKING_CONSENT_V1";

    public BookingRequest toEntity(
            BookingRequestCreateRequest request,
            String referenceNumber
    ) {
        requirePublicRequest(request);

        String email = normalizeEmail(request.email());
        String phone = normalizeRequired(request.phone());

        BookingRequest bookingRequest = BookingRequest.builder()
                .referenceNumber(normalizeRequired(referenceNumber))
                .bookingFor(request.bookingFor())
                .fullName(normalizeRequired(request.fullName()))
                .email(email)
                .normalizedEmail(normalizeEmail(email))
                .phone(phone)
                .normalizedPhone(normalizePhone(phone))
                .preferredContactMethod(
                        request.preferredContactMethod()
                )
                .notificationEmail(
                        normalizeEmail(
                                request.resolvedNotificationEmail()
                        )
                )
                .notificationPhone(
                        normalizeOptional(
                                request.resolvedNotificationPhone()
                        )
                )
                .businessName(
                        normalizeOptional(request.businessName())
                )
                .businessEmail(
                        normalizeOptionalEmail(request.businessEmail())
                )
                .normalizedBusinessEmail(
                        normalizeOptionalEmail(request.businessEmail())
                )
                .businessPhone(
                        normalizeOptional(request.businessPhone())
                )
                .normalizedBusinessPhone(
                        normalizeOptionalPhone(request.businessPhone())
                )
                .businessStreetAddress(
                        normalizeOptional(
                                request.businessStreetAddress()
                        )
                )
                .businessCity(
                        normalizeOptional(request.businessCity())
                )
                .businessState(
                        normalizeOptional(request.businessState())
                )
                .businessPostalCode(
                        normalizeOptional(
                                request.businessPostalCode()
                        )
                )
                .businessCountryCode(
                        request.resolvedBusinessCountryCode()
                )
                .businessContactRole(
                        normalizeOptional(
                                request.businessContactRole()
                        )
                )
                .serviceType(
                        normalizeRequired(request.serviceType())
                )
                .serviceMethod(request.serviceMethod())
                .preferredDate(request.preferredDate())
                .preferredTime(request.preferredTime())
                .alternateDate(request.alternateDate())
                .deviceType(
                        normalizeOptional(request.deviceType())
                )
                .problemDescription(
                        normalizeRequired(
                                request.problemDescription()
                        )
                )
                .streetAddress(
                        normalizeOptional(request.streetAddress())
                )
                .addressLine2(
                        normalizeOptional(request.addressLine2())
                )
                .city(normalizeOptional(request.city()))
                .stateRegion(
                        normalizeOptional(request.stateRegion())
                )
                .postalCode(
                        normalizeOptional(request.postalCode())
                )
                .countryCode(request.resolvedCountryCode())
                .consentAccepted(request.consentAccepted())
                .consentVersion(PUBLIC_CONSENT_VERSION)
                .status(BookingRequestStatus.PENDING)
                .bookingSource(BookingSource.WEBSITE)
                .reviewEligible(true)
                .build();

        if (bookingRequest.getBookingFor() == BookingFor.PERSONAL) {
            bookingRequest.clearBusinessDetails();
        }

        return bookingRequest;
    }

    public BookingRequest toAdminEntity(
            AdminBookingRequestCreateRequest request,
            String referenceNumber,
            UUID administratorId,
            String administratorName
    ) {
        requireAdminRequest(request);

        String email = normalizeEmail(request.email());
        String phone = normalizeRequired(request.phone());

        BookingRequest bookingRequest = BookingRequest.builder()
                .customerId(request.customerId())
                .referenceNumber(normalizeRequired(referenceNumber))
                .bookingFor(request.bookingFor())
                .fullName(normalizeRequired(request.fullName()))
                .email(email)
                .normalizedEmail(normalizeEmail(email))
                .phone(phone)
                .normalizedPhone(normalizePhone(phone))
                .preferredContactMethod(
                        request.preferredContactMethod()
                )
                .notificationEmail(
                        normalizeOptionalEmail(
                                hasText(request.notificationEmail())
                                        ? request.notificationEmail()
                                        : request.email()
                        )
                )
                .notificationPhone(
                        normalizeOptional(
                                hasText(request.notificationPhone())
                                        ? request.notificationPhone()
                                        : request.phone()
                        )
                )
                .businessName(
                        normalizeOptional(request.businessName())
                )
                .businessEmail(
                        normalizeOptionalEmail(request.businessEmail())
                )
                .normalizedBusinessEmail(
                        normalizeOptionalEmail(request.businessEmail())
                )
                .businessPhone(
                        normalizeOptional(request.businessPhone())
                )
                .normalizedBusinessPhone(
                        normalizeOptionalPhone(request.businessPhone())
                )
                .businessStreetAddress(
                        normalizeOptional(
                                request.businessStreetAddress()
                        )
                )
                .businessCity(
                        normalizeOptional(request.businessCity())
                )
                .businessState(
                        normalizeOptional(request.businessState())
                )
                .businessPostalCode(
                        normalizeOptional(
                                request.businessPostalCode()
                        )
                )
                .businessCountryCode(
                        request.bookingFor() == BookingFor.BUSINESS
                                ? normalizeCountryCode(
                                request.businessCountryCode()
                        )
                                : null
                )
                .businessContactRole(
                        normalizeOptional(
                                request.businessContactRole()
                        )
                )
                .serviceId(request.serviceId())
                .serviceType(
                        normalizeRequired(request.serviceType())
                )
                .serviceMethod(request.serviceMethod())
                .preferredDate(request.preferredDate())
                .preferredTime(request.preferredTime())
                .alternateDate(request.alternateDate())
                .deviceType(
                        normalizeOptional(request.deviceType())
                )
                .problemDescription(
                        normalizeRequired(
                                request.problemDescription()
                        )
                )
                .streetAddress(
                        normalizeOptional(request.streetAddress())
                )
                .addressLine2(
                        normalizeOptional(request.addressLine2())
                )
                .city(normalizeOptional(request.city()))
                .stateRegion(
                        normalizeOptional(request.stateRegion())
                )
                .postalCode(
                        normalizeOptional(request.postalCode())
                )
                .countryCode(
                        normalizeCountryCode(request.countryCode())
                )
                .consentAccepted(false)
                .consentVersion(null)
                .status(BookingRequestStatus.PENDING)
                .bookingSource(request.bookingSource())
                .assignedAdminUserId(
                        request.assignedAdminUserId()
                )
                .createdByAdminUserId(administratorId)
                .createdByAdminName(
                        normalizeRequired(administratorName)
                )
                .adminNotes(
                        normalizeOptional(request.adminNotes())
                )
                .reviewEligible(true)
                .build();

        if (bookingRequest.getBookingFor() == BookingFor.PERSONAL) {
            bookingRequest.clearBusinessDetails();
        }

        return bookingRequest;
    }

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

    public String normalizeEmail(String value) {
        return normalizeRequired(value)
                .toLowerCase(Locale.ROOT);
    }

    public String normalizePhone(String value) {
        String requiredValue = normalizeRequired(value);
        String normalized = requiredValue.replaceAll("\\D", "");

        if (normalized.isBlank()) {
            throw new IllegalArgumentException(
                    "Telephone number cannot be normalized."
            );
        }

        return normalized;
    }

    public String normalizeOptionalPhone(String value) {
        String optional = normalizeOptional(value);

        if (optional == null) {
            return null;
        }

        String normalized = optional.replaceAll("\\D", "");

        return normalized.isBlank() ? null : normalized;
    }

    public String normalizeOptionalEmail(String value) {
        String optional = normalizeOptional(value);

        return optional == null
                ? null
                : optional.toLowerCase(Locale.ROOT);
    }

    public String normalizeCountryCode(String value) {
        String optional = normalizeOptional(value);

        return optional == null
                ? "US"
                : optional.toUpperCase(Locale.ROOT);
    }

    public String normalizeRequired(String value) {
        if (value == null) {
            throw new IllegalArgumentException(
                    "Required booking value must not be null."
            );
        }

        String normalized = value.trim();

        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(
                    "Required booking value must not be blank."
            );
        }

        return normalized;
    }

    public String normalizeOptional(String value) {
        if (value == null) {
            return null;
        }

        String normalized = value.trim();

        return normalized.isEmpty() ? null : normalized;
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
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
}