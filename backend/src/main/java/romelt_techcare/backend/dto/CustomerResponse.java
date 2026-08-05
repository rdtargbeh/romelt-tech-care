package romelt_techcare.backend.dto;

import romelt_techcare.backend.enums.ContactMethod;
import romelt_techcare.backend.enums.CustomerSource;
import romelt_techcare.backend.enums.CustomerStatus;

import java.time.Instant;
import java.util.UUID;

/**
 * ================================================================
 * ROMELT TECHCARE — CUSTOMER RESPONSE
 * ================================================================
 *
 * Purpose:
 * Returns the complete reusable customer profile to authenticated
 * administrators.
 *
 * Responsibilities:
 * - Returns current customer identity and contact information.
 * - Returns communication preferences and restrictions.
 * - Returns customer activity timestamps.
 * - Returns lifecycle and duplicate-merge information.
 * - Returns administrator attribution and optimistic-locking data.
 *
 * Security:
 * This response is administrator-only. It includes internal notes and
 * must never be returned through a public controller.
 * ================================================================
 */
public record CustomerResponse(

        UUID customerId,

        String customerNumber,

        String firstName,

        String lastName,

        String preferredName,

        String displayName,

        String primaryEmail,

        String primaryPhone,

        ContactMethod preferredContactMethod,

        String streetAddress,

        String addressLine2,

        String city,

        String stateRegion,

        String postalCode,

        String countryCode,

        CustomerStatus customerStatus,

        CustomerSource customerSource,

        boolean marketingConsent,

        Instant marketingConsentAt,

        String marketingConsentSource,

        boolean emailVerified,

        Instant emailVerifiedAt,

        boolean phoneVerified,

        Instant phoneVerifiedAt,

        boolean doNotEmail,

        boolean doNotCall,

        boolean doNotText,

        String communicationNotes,

        String internalNotes,

        Instant firstContactAt,

        Instant lastContactedAt,

        Instant lastBookingAt,

        Instant lastServiceCompletedAt,

        Instant lastActivityAt,

        UUID mergedIntoCustomerId,

        Instant mergedAt,

        UUID mergedByAdminUserId,

        UUID createdByAdminUserId,

        UUID updatedByAdminUserId,

        UUID archivedByAdminUserId,

        UUID deletedByAdminUserId,

        Instant archivedAt,

        Instant deletedAt,

        Instant createdAt,

        Instant updatedAt,

        Long rowVersion
) {
}