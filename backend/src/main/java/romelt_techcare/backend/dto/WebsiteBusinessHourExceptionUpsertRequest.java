package romelt_techcare.backend.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * ================================================================
 * ROMELT TECHCARE — BUSINESS-HOUR EXCEPTION UPSERT REQUEST
 * ================================================================
 *
 * Purpose:
 * Carries the complete editable state of one date-specific schedule
 * exception.
 *
 * Rules:
 * - Exception date is required.
 * - Closed exceptions ignore and clear supplied times.
 * - Open exceptions may omit both times for appointment-only service.
 * - Opening and closing times must either both be present or both null.
 * - Opening time must be before closing time.
 * ================================================================
 */
public record WebsiteBusinessHourExceptionUpsertRequest(

        @NotNull(
                message = "Exception date is required."
        )
        LocalDate exceptionDate,

        @Size(
                max = 180,
                message = "Exception name must not exceed 180 characters."
        )
        String exceptionName,

        @NotNull(
                message = "Closed status is required."
        )
        Boolean isClosed,

        @NotNull(
                message = "Appointment status is required."
        )
        Boolean isByAppointment,

        LocalTime openingTime,

        LocalTime closingTime,

        @Size(
                max = 180,
                message = "Display text must not exceed 180 characters."
        )
        String displayText
) {
}