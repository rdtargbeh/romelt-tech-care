package romelt_techcare.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import romelt_techcare.backend.enums.ContactMethod;
import romelt_techcare.backend.enums.CustomerStatus;

/**
 * ================================================================
 * ROMELT TECHCARE — CUSTOMER UPDATE REQUEST
 * ================================================================
 *
 * Purpose:
 * Updates the current reusable customer/contact profile.
 *
 * Responsibilities:
 * - Updates names and contact information.
 * - Updates the current address.
 * - Updates communication preferences and restrictions.
 * - Updates internal administrator notes.
 * - Supports ACTIVE, INACTIVE, and BLOCKED status changes.
 *
 * Important:
 * This DTO does not merge, archive, restore, or delete customers.
 * Those operations use dedicated service methods so lifecycle
 * attribution and timestamps remain correct.
 *
 * Booking history:
 * Updating a customer does not modify contact or business snapshots
 * stored on existing BookingRequest records.
 * ================================================================
 */
public record CustomerUpdateRequest(

        @Size(
                max = 100,
                message = "First name cannot exceed 100 characters."
        )
        String firstName,

        @Size(
                max = 100,
                message = "Last name cannot exceed 100 characters."
        )
        String lastName,

        @Size(
                max = 100,
                message = "Preferred name cannot exceed 100 characters."
        )
        String preferredName,

        @Size(
                min = 2,
                max = 180,
                message = "Display name must contain between 2 and 180 characters."
        )
        String displayName,

        @Email(
                message = "Enter a valid email address."
        )
        @Size(
                max = 254,
                message = "Email address cannot exceed 254 characters."
        )
        String primaryEmail,

        @Pattern(
                regexp = "^$|^[0-9+()\\-\\.\\s]{10,40}$",
                message = "Enter a valid telephone number."
        )
        String primaryPhone,

        ContactMethod preferredContactMethod,

        @Size(
                max = 180,
                message = "Street address cannot exceed 180 characters."
        )
        String streetAddress,

        @Size(
                max = 180,
                message = "Address line 2 cannot exceed 180 characters."
        )
        String addressLine2,

        @Size(
                max = 100,
                message = "City cannot exceed 100 characters."
        )
        String city,

        @Size(
                max = 100,
                message = "State or region cannot exceed 100 characters."
        )
        String stateRegion,

        @Size(
                max = 30,
                message = "Postal code cannot exceed 30 characters."
        )
        String postalCode,

        @Pattern(
                regexp = "^$|^[A-Za-z]{2}$",
                message = "Country code must contain exactly two letters."
        )
        String countryCode,

        CustomerStatus customerStatus,

        Boolean marketingConsent,

        @Size(
                max = 50,
                message = "Marketing consent source cannot exceed 50 characters."
        )
        String marketingConsentSource,

        Boolean emailVerified,

        Boolean phoneVerified,

        Boolean doNotEmail,

        Boolean doNotCall,

        Boolean doNotText,

        @Size(
                max = 1000,
                message = "Communication notes cannot exceed 1,000 characters."
        )
        String communicationNotes,

        @Size(
                max = 20000,
                message = "Internal notes cannot exceed 20,000 characters."
        )
        String internalNotes
) {
}