package romelt_techcare.backend.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.time.LocalTime;

/**
 * ================================================================
 * ROMELT TECHCARE — WEBSITE BUSINESS HOUR UPSERT REQUEST
 * ================================================================
 *
 * Purpose:
 * Carries the complete editable schedule for one normal weekday.
 *
 * Upsert behavior:
 * If a row already exists for the business profile and day, it is
 * updated. Otherwise, a new row is created.
 *
 * Time rules:
 * - Closed days ignore and clear opening and closing times.
 * - Open days may omit both times for appointment-only operations.
 * - When one time is supplied, both must be supplied.
 * - Opening time must be before closing time.
 * ================================================================
 */
public record WebsiteBusinessHourUpsertRequest(

        @NotNull(message = "Day of week is required.")
        @Min(
                value = 1,
                message = "Day of week must be at least 1."
        )
        @Max(
                value = 7,
                message = "Day of week must not exceed 7."
        )
        Short dayOfWeek,

        @NotNull(message = "Closed status is required.")
        Boolean isClosed,

        @NotNull(message = "Appointment status is required.")
        Boolean isByAppointment,

        LocalTime openingTime,

        LocalTime closingTime,

        @Size(
                max = 120,
                message = "Display text must not exceed 120 characters."
        )
        String displayText,

        @PositiveOrZero(
                message = "Display order must not be negative."
        )
        Integer displayOrder
) {
}