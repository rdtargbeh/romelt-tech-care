package romelt_techcare.backend.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import romelt_techcare.backend.enums.ContactMethod;
import romelt_techcare.backend.enums.PreferredServiceTime;
import romelt_techcare.backend.enums.ServiceMethod;

import java.time.LocalDate;

/**
 * ================================================================
 * ROMELT TECHCARE — BOOKING REQUEST CREATE DTO
 * ================================================================
 *
 * Purpose:
 * Validates public booking-form submissions.
 *
 * Responsibilities:
 * - Validates customer information.
 * - Validates requested service information.
 * - Ensures the preferred date is in the future.
 * - Limits public text lengths.
 * - Requires booking-request acknowledgement.
 *
 * Additional validation:
 * Service-method-specific rules, such as requiring an address for
 * ON_SITE service, are enforced in the service layer.
 * ================================================================
 */
public record BookingRequestCreateRequest(

        @NotBlank(message = "Enter your full name.")
        @Size(
                min = 2,
                max = 120,
                message = "Full name must contain between 2 and 120 characters."
        )
        String fullName,

        @NotBlank(message = "Enter your email address.")
        @Email(message = "Enter a valid email address.")
        @Size(
                max = 254,
                message = "Email address cannot exceed 254 characters."
        )
        String email,

        @NotBlank(message = "Enter your telephone number.")
        @Pattern(
                regexp = "^[0-9+()\\-\\.\\s]{10,30}$",
                message = "Enter a valid telephone number."
        )
        String phone,

        @NotBlank(message = "Select the service you need.")
        @Size(
                max = 120,
                message = "Service type cannot exceed 120 characters."
        )
        String serviceType,

        @NotNull(
                message = "Select a preferred service method."
        )
        ServiceMethod serviceMethod,

        @NotNull(
                message = "Select your preferred service date."
        )
        @Future(
                message = "Preferred date must be a future date."
        )
        LocalDate preferredDate,

        @NotNull(
                message = "Select your preferred service time."
        )
        PreferredServiceTime preferredTime,

        @Future(
                message = "Alternate date must be a future date."
        )
        LocalDate alternateDate,

        @Size(
                max = 180,
                message = "Street address cannot exceed 180 characters."
        )
        String streetAddress,

        @Size(
                max = 100,
                message = "City cannot exceed 100 characters."
        )
        String city,

        @Size(
                max = 100,
                message = "State cannot exceed 100 characters."
        )
        String state,

        @Pattern(
                regexp = "^$|^\\d{5}(?:-\\d{4})?$",
                message = "Enter a valid postal code."
        )
        String postalCode,

        @Size(
                max = 120,
                message = "Device type cannot exceed 120 characters."
        )
        String deviceType,

        @NotBlank(
                message = "Describe the problem or requested service."
        )
        @Size(
                min = 20,
                max = 2000,
                message = "Description must contain between 20 and 2,000 characters."
        )
        String problemDescription,

        @NotNull(
                message = "Select how you prefer to be contacted."
        )
        ContactMethod preferredContactMethod,

        @AssertTrue(
                message = "Confirm that you understand this is a booking request."
        )
        boolean consentAccepted
) {
}