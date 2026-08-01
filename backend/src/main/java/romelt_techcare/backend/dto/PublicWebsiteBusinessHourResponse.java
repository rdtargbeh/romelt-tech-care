package romelt_techcare.backend.dto;

import java.time.LocalTime;

/**
 * ================================================================
 * ROMELT TECHCARE — PUBLIC WEBSITE BUSINESS HOUR RESPONSE
 * ================================================================
 *
 * Purpose:
 * Provides the public website with one normal weekly schedule row.
 *
 * Security:
 * Internal identifiers, administrator attribution, timestamps, and
 * optimistic-lock values are excluded.
 * ================================================================
 */
public record PublicWebsiteBusinessHourResponse(

        Short dayOfWeek,

        String dayName,

        Boolean isClosed,

        Boolean isByAppointment,

        LocalTime openingTime,

        LocalTime closingTime,

        String displayText,

        Integer displayOrder
) {
}