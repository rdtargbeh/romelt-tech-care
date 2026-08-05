package romelt_techcare.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import romelt_techcare.backend.enums.ContactMethod;
import romelt_techcare.backend.enums.CustomerSource;

/**
 * ================================================================
 * ROMELT TECHCARE — CUSTOMER CREATE REQUEST
 * ================================================================
 *
 * Purpose:
 * Allows an authenticated administrator to manually create a reusable
 * customer/contact profile.
 *
 * Responsibilities:
 * - Collects the customer's current contact information.
 * - Supports optional address and communication preferences.
 * - Supports customer communication restrictions.
 * - Records how the customer entered the system.
 *
 * Booking integration:
 * Customers created automatically from bookings do not use this DTO.
 * BookingRequestService resolves or creates those customers internally
 * from the booking contact-person snapshot.
 *
 * Important:
 * Business information is not stored on Customer. Business details
 * remain on the individual BookingRequest as historical snapshots.
 * ================================================================
 */
public record CustomerCreateRequest(

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

        @NotBlank(
                message = "Customer display name is required."
        )
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

        CustomerSource customerSource,

        boolean marketingConsent,

        @Size(
                max = 50,
                message = "Marketing consent source cannot exceed 50 characters."
        )
        String marketingConsentSource,

        boolean doNotEmail,

        boolean doNotCall,

        boolean doNotText,

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

    /**
     * At least one reusable customer contact method is required.
     */
    public boolean hasContactInformation() {
        return hasText(primaryEmail)
                || hasText(primaryPhone);
    }

    private boolean hasText(
            String value
    ) {
        return value != null
                && !value.trim().isEmpty();
    }
}