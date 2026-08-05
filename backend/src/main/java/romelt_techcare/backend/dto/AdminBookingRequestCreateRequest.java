package romelt_techcare.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import romelt_techcare.backend.enums.BookingFor;
import romelt_techcare.backend.enums.BookingSource;
import romelt_techcare.backend.enums.ContactMethod;
import romelt_techcare.backend.enums.PreferredServiceTime;
import romelt_techcare.backend.enums.ServiceMethod;

import java.time.LocalDate;
import java.util.UUID;

/**
 * ================================================================
 * ROMELT TECHCARE — ADMIN BOOKING REQUEST CREATE DTO
 * ================================================================
 *
 * Purpose:
 * Allows an authenticated administrator to enter a booking on behalf
 * of a customer.
 *
 * Responsibilities:
 * - Preserves the existing administrator booking workflow.
 * - Supports personal and business service requests.
 * - Supports selecting an existing customer when known.
 * - Collects contact-person and business snapshots.
 * - Collects service, scheduling, location, and private admin notes.
 * - Identifies the non-WEBSITE source of the booking.
 *
 * Important:
 * Administrator-created bookings do not represent direct customer
 * acceptance of the public booking-form consent checkbox.
 * ================================================================
 */
public record AdminBookingRequestCreateRequest(

        UUID customerId,

        @NotNull(
                message = "Select whether the service is personal or business."
        )
        BookingFor bookingFor,

        @NotBlank(message = "Enter the customer's full name.")
        @Size(
                min = 2,
                max = 120,
                message = "Full name must contain between 2 and 120 characters."
        )
        String fullName,

        @NotBlank(message = "Enter the customer's email address.")
        @Email(message = "Enter a valid email address.")
        @Size(
                max = 254,
                message = "Email address cannot exceed 254 characters."
        )
        String email,

        @NotBlank(message = "Enter the customer's telephone number.")
        @Pattern(
                regexp = "^[0-9+()\\-\\.\\s]{10,40}$",
                message = "Enter a valid telephone number."
        )
        String phone,

        @NotNull(message = "Select the preferred contact method.")
        ContactMethod preferredContactMethod,

        @Email(message = "Enter a valid notification email address.")
        @Size(
                max = 254,
                message = "Notification email cannot exceed 254 characters."
        )
        String notificationEmail,

        @Pattern(
                regexp = "^$|^[0-9+()\\-\\.\\s]{10,40}$",
                message = "Enter a valid notification telephone number."
        )
        String notificationPhone,

        @Size(
                max = 180,
                message = "Business name cannot exceed 180 characters."
        )
        String businessName,

        @Email(message = "Enter a valid business email address.")
        @Size(
                max = 254,
                message = "Business email cannot exceed 254 characters."
        )
        String businessEmail,

        @Pattern(
                regexp = "^$|^[0-9+()\\-\\.\\s]{10,40}$",
                message = "Enter a valid business telephone number."
        )
        String businessPhone,

        @Size(
                max = 180,
                message = "Business street address cannot exceed 180 characters."
        )
        String businessStreetAddress,

        @Size(
                max = 100,
                message = "Business city cannot exceed 100 characters."
        )
        String businessCity,

        @Size(
                max = 100,
                message = "Business state cannot exceed 100 characters."
        )
        String businessState,

        @Size(
                max = 30,
                message = "Business postal code cannot exceed 30 characters."
        )
        String businessPostalCode,

        @Pattern(
                regexp = "^$|^[A-Za-z]{2}$",
                message = "Business country code must contain two letters."
        )
        String businessCountryCode,

        @Size(
                max = 120,
                message = "Business contact role cannot exceed 120 characters."
        )
        String businessContactRole,

        UUID serviceId,

        @NotBlank(message = "Select the requested service.")
        @Size(
                max = 120,
                message = "Service type cannot exceed 120 characters."
        )
        String serviceType,

        @NotNull(message = "Select a service method.")
        ServiceMethod serviceMethod,

        @NotNull(message = "Select the preferred service date.")
        @Future(message = "Preferred date must be a future date.")
        LocalDate preferredDate,

        @NotNull(message = "Select the preferred service time.")
        PreferredServiceTime preferredTime,

        @Future(message = "Alternate date must be a future date.")
        LocalDate alternateDate,

        @Size(
                max = 120,
                message = "Device type cannot exceed 120 characters."
        )
        String deviceType,

        @NotBlank(message = "Describe the requested service.")
        @Size(
                min = 20,
                max = 2000,
                message = "Description must contain between 20 and 2,000 characters."
        )
        String problemDescription,

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
                message = "State cannot exceed 100 characters."
        )
        String stateRegion,

        @Size(
                max = 30,
                message = "Postal code cannot exceed 30 characters."
        )
        String postalCode,

        @Pattern(
                regexp = "^$|^[A-Za-z]{2}$",
                message = "Country code must contain two letters."
        )
        String countryCode,

        @NotNull(message = "Select the booking source.")
        BookingSource bookingSource,

        UUID assignedAdminUserId,

        @Size(
                max = 10000,
                message = "Administrator notes cannot exceed 10,000 characters."
        )
        String adminNotes
) {
}