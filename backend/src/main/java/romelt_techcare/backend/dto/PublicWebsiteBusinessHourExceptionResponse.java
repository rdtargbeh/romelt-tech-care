package romelt_techcare.backend.dto;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * ================================================================
 * ROMELT TECHCARE — PUBLIC BUSINESS-HOUR EXCEPTION RESPONSE
 * ================================================================
 *
 * Purpose:
 * Provides the React public website with safe date-specific schedule
 * override information.
 *
 * Security:
 * Internal identifiers, administrator attribution, timestamps, and
 * optimistic-lock information are excluded.
 * ================================================================
 */
public record PublicWebsiteBusinessHourExceptionResponse(

        LocalDate exceptionDate,

        String exceptionName,

        Boolean isClosed,

        Boolean isByAppointment,

        LocalTime openingTime,

        LocalTime closingTime,

        String displayText
) {
}