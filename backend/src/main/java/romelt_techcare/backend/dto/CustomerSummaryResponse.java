package romelt_techcare.backend.dto;

import romelt_techcare.backend.enums.ContactMethod;
import romelt_techcare.backend.enums.CustomerSource;
import romelt_techcare.backend.enums.CustomerStatus;

import java.time.Instant;
import java.util.UUID;

/**
 * ================================================================
 * ROMELT TECHCARE — CUSTOMER SUMMARY RESPONSE
 * ================================================================
 *
 * Purpose:
 * Returns compact customer information for administrator lists,
 * customer selectors, booking screens, and search results.
 *
 * Security:
 * Internal notes, communication notes, verification timestamps, and
 * lifecycle attribution are intentionally excluded.
 * ================================================================
 */
public record CustomerSummaryResponse(

        UUID customerId,

        String customerNumber,

        String displayName,

        String preferredName,

        String primaryEmail,

        String primaryPhone,

        ContactMethod preferredContactMethod,

        CustomerStatus customerStatus,

        CustomerSource customerSource,

        Instant lastBookingAt,

        Instant lastServiceCompletedAt,

        Instant lastActivityAt
) {
}