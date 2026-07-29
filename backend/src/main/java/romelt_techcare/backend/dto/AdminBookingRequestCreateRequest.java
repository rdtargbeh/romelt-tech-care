package romelt_techcare.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import romelt_techcare.backend.enums.BookingSource;
import romelt_techcare.backend.enums.ContactMethod;
import romelt_techcare.backend.enums.PreferredServiceTime;
import romelt_techcare.backend.enums.ServiceMethod;

import java.time.LocalDate;

/**
 * ================================================================
 * ROMELT TECHCARE — ADMIN BOOKING REQUEST CREATE DTO
 * ================================================================
 *
 * Purpose:
 * Allows an authenticated administrator to create a booking request
 * for a customer.
 *
 * Typical scenarios:
 * - A customer calls Romelt TechCare.
 * - A customer sends an email requesting service.
 * - A customer visits in person.
 * - An administrator receives a request through another channel.
 *
 * Responsibilities:
 * - Validates customer contact information.
 * - Validates requested service information.
 * - Requires a future preferred service date.
 * - Records how the booking entered the system.
 * - Supports private administrator notes.
 *
 * Important:
 * The public website consent checkbox is not included because the
 * administrator is entering information on the customer's behalf.
 * ================================================================
 */
public record AdminBookingRequestCreateRequest(

        @NotBlank(
                message = "Enter the customer's full name."
        )
        @Size(
                min = 2,
                max = 120,
                message = "Customer name must contain between 2 and 120 characters."
        )
        String fullName,

        @NotBlank(
                message = "Enter the customer's email address."
        )
        @Email(
                message = "Enter a valid customer email address."
        )
        @Size(
                max = 254,
                message = "Email address cannot exceed 254 characters."
        )
        String email,

        @NotBlank(
                message = "Enter the customer's telephone number."
        )
        @Pattern(
                regexp = "^[0-9+()\\-\\.\\s]{10,30}$",
                message = "Enter a valid telephone number."
        )
        String phone,

        @NotBlank(
                message = "Select the service the customer needs."
        )
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
                message = "Select the preferred service date."
        )
        @Future(
                message = "Preferred date must be a future date."
        )
        LocalDate preferredDate,

        @NotNull(
                message = "Select the preferred service time."
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
                message = "Select how the customer prefers to be contacted."
        )
        ContactMethod preferredContactMethod,

        @NotNull(
                message = "Select how the customer contacted Romelt TechCare."
        )
        BookingSource bookingSource,

        @Size(
                max = 2000,
                message = "Administrator notes cannot exceed 2,000 characters."
        )
        String adminNotes
) {
}