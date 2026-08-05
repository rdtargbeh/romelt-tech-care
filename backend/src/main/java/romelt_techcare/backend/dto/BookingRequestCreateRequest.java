package romelt_techcare.backend.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import romelt_techcare.backend.enums.BookingFor;
import romelt_techcare.backend.enums.ContactMethod;
import romelt_techcare.backend.enums.PreferredServiceTime;
import romelt_techcare.backend.enums.ServiceMethod;

import java.time.LocalDate;
import java.util.Locale;

/**
 * ================================================================
 * ROMELT TECHCARE — PUBLIC BOOKING REQUEST CREATE DTO
 * ================================================================
 *
 * Purpose:
 * Validates service-booking requests submitted through the public
 * Romelt TechCare website.
 *
 * Responsibilities:
 * - Collects the contact-person snapshot.
 * - Identifies whether the service is personal or business-related.
 * - Collects required business details for business bookings.
 * - Collects the requested service, schedule, and service location.
 * - Collects the customer's preferred notification destinations.
 * - Requires acknowledgement of the booking-request disclaimer.
 * - Applies conditional validation without changing the existing
 *   public booking endpoint.
 *
 * Booking classification:
 *
 * PERSONAL:
 * - Contact-person information is required.
 * - Business fields must be empty.
 *
 * BUSINESS:
 * - Contact-person information is required.
 * - Business name, email, phone, street address, city, state,
 *   postal code, and country code are required.
 *
 * Service location:
 * - Address fields are required when serviceMethod is ON_SITE.
 * - Service-location fields remain separate from the business
 *   address because the work may occur at another location.
 *
 * Important:
 * This request creates only a booking request. It does not confirm
 * an appointment.
 * ================================================================
 */
public record BookingRequestCreateRequest(

        // ============================================================
        // BOOKING CLASSIFICATION
        // ============================================================

        @NotNull(
                message = "Select whether this service is for personal or business use."
        )
        BookingFor bookingFor,

        // ============================================================
        // CONTACT-PERSON INFORMATION
        // ============================================================

        @NotBlank(
                message = "Enter your full name."
        )
        @Size(
                min = 2,
                max = 120,
                message = "Full name must contain between 2 and 120 characters."
        )
        String fullName,

        @NotBlank(
                message = "Enter your email address."
        )
        @Email(
                message = "Enter a valid email address."
        )
        @Size(
                max = 254,
                message = "Email address cannot exceed 254 characters."
        )
        String email,

        @NotBlank(
                message = "Enter your telephone number."
        )
        @Pattern(
                regexp = "^[0-9+()\\-\\.\\s]{10,40}$",
                message = "Enter a valid telephone number."
        )
        String phone,

        @NotNull(
                message = "Select how you prefer to be contacted."
        )
        ContactMethod preferredContactMethod,

        @Email(
                message = "Enter a valid notification email address."
        )
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

        // ============================================================
        // BUSINESS INFORMATION
        // ============================================================

        @Size(
                max = 180,
                message = "Business name cannot exceed 180 characters."
        )
        String businessName,

        @Email(
                message = "Enter a valid business email address."
        )
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
                message = "Business country code must contain exactly two letters."
        )
        String businessCountryCode,

        @Size(
                max = 120,
                message = "Business contact role cannot exceed 120 characters."
        )
        String businessContactRole,

        // ============================================================
        // REQUESTED SERVICE
        // ============================================================

        @NotBlank(
                message = "Select the service you need."
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

        // ============================================================
        // SERVICE LOCATION
        // ============================================================

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
                message = "Country code must contain exactly two letters."
        )
        String countryCode,

        // ============================================================
        // CUSTOMER ACKNOWLEDGEMENT
        // ============================================================

        @AssertTrue(
                message = "Confirm that you understand this is a booking request."
        )
        boolean consentAccepted
) {

        /**
         * Ensures a business booking includes all required business
         * details.
         */
        @AssertTrue(
                message = "Complete all required business information."
        )
        public boolean isBusinessInformationComplete() {
                if (bookingFor != BookingFor.BUSINESS) {
                        return true;
                }

                return hasText(businessName)
                        && hasText(businessEmail)
                        && hasText(businessPhone)
                        && hasText(businessStreetAddress)
                        && hasText(businessCity)
                        && hasText(businessState)
                        && hasText(businessPostalCode)
                        && hasText(businessCountryCode);
        }

        /**
         * Prevents business data from being submitted for a personal
         * booking.
         */
        @AssertTrue(
                message = "Business information must be empty for a personal booking."
        )
        public boolean isBusinessInformationEmptyForPersonalBooking() {
                if (bookingFor != BookingFor.PERSONAL) {
                        return true;
                }

                return !hasText(businessName)
                        && !hasText(businessEmail)
                        && !hasText(businessPhone)
                        && !hasText(businessStreetAddress)
                        && !hasText(businessCity)
                        && !hasText(businessState)
                        && !hasText(businessPostalCode)
                        && !hasText(businessCountryCode)
                        && !hasText(businessContactRole);
        }

        /**
         * Ensures on-site requests include a complete service location.
         */
        @AssertTrue(
                message = "Complete the service address for on-site service."
        )
        public boolean isOnSiteServiceAddressComplete() {
                if (serviceMethod != ServiceMethod.ON_SITE) {
                        return true;
                }

                return hasText(streetAddress)
                        && hasText(city)
                        && hasText(stateRegion)
                        && hasText(postalCode)
                        && hasText(countryCode);
        }

        /**
         * Prevents the alternate date from matching the preferred date.
         */
        @AssertTrue(
                message = "Alternate date must be different from the preferred date."
        )
        public boolean isAlternateDateDifferent() {
                return alternateDate == null
                        || preferredDate == null
                        || !alternateDate.equals(preferredDate);
        }

        /**
         * Ensures an EMAIL preference has a usable email destination.
         */
        @AssertTrue(
                message = "An email address is required when email is the preferred contact method."
        )
        public boolean isPreferredEmailAvailable() {
                if (preferredContactMethod != ContactMethod.EMAIL) {
                        return true;
                }

                return hasText(notificationEmail)
                        || hasText(email);
        }

        /**
         * Ensures a PHONE or TEXT preference has a usable telephone
         * destination.
         */
        @AssertTrue(
                message = "A telephone number is required for phone or text contact."
        )
        public boolean isPreferredPhoneAvailable() {
                if (
                        preferredContactMethod != ContactMethod.PHONE
                                && preferredContactMethod != ContactMethod.TEXT
                ) {
                        return true;
                }

                return hasText(notificationPhone)
                        || hasText(phone);
        }

        /**
         * Returns the normalized country code used for the service
         * location.
         */
        public String resolvedCountryCode() {
                if (!hasText(countryCode)) {
                        return "US";
                }

                return countryCode
                        .trim()
                        .toUpperCase(Locale.ROOT);
        }

        /**
         * Returns the normalized business country code.
         */
        public String resolvedBusinessCountryCode() {
                if (bookingFor != BookingFor.BUSINESS) {
                        return null;
                }

                if (!hasText(businessCountryCode)) {
                        return "US";
                }

                return businessCountryCode
                        .trim()
                        .toUpperCase(Locale.ROOT);
        }

        /**
         * Uses the main email when a separate notification email was not
         * supplied.
         */
        public String resolvedNotificationEmail() {
                if (hasText(notificationEmail)) {
                        return notificationEmail.trim();
                }

                return email == null
                        ? null
                        : email.trim();
        }

        /**
         * Uses the main phone when a separate notification phone was not
         * supplied.
         */
        public String resolvedNotificationPhone() {
                if (hasText(notificationPhone)) {
                        return notificationPhone.trim();
                }

                return phone == null
                        ? null
                        : phone.trim();
        }

        private boolean hasText(
                String value
        ) {
                return value != null
                        && !value.trim().isEmpty();
        }
}