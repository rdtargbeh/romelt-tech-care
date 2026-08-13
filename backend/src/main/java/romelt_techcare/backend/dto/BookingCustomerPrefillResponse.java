package romelt_techcare.backend.dto;

import romelt_techcare.backend.entity.Customer;
import romelt_techcare.backend.enums.ContactMethod;

import java.util.UUID;

/**
 * ================================================================
 * ROMELT TECHCARE — BOOKING CUSTOMER PREFILL RESPONSE
 * ================================================================
 *
 * Purpose:
 * Provides the reusable Customer information needed to prefill an
 * administrator Create Booking form when an existing customer is
 * selected.
 *
 * Business workflow:
 *
 * Administrator searches Customer
 *      -> selects Customer
 *          -> frontend requests booking customer prefill
 *              -> customer fields are populated automatically
 *                  -> administrator completes booking-specific fields
 *
 * This DTO intentionally exposes only customer information relevant
 * to booking creation.
 *
 * It does NOT expose:
 * - internal customer notes;
 * - communication notes;
 * - marketing metadata;
 * - merge metadata;
 * - audit metadata;
 * - lifecycle metadata unrelated to creating a booking.
 *
 * Important:
 * The values returned by this DTO are current reusable Customer
 * values.
 *
 * BookingRequest will still preserve its own contact/address snapshot
 * when the booking is created.
 *
 * customerId remains the authoritative link between the new booking
 * and the selected reusable Customer.
 * ================================================================
 */
public record BookingCustomerPrefillResponse(

        UUID customerId,

        String customerNumber,

        String displayName,

        String preferredName,

        String primaryEmail,

        String primaryPhone,

        ContactMethod preferredContactMethod,

        String streetAddress,

        String addressLine2,

        String city,

        String stateRegion,

        String postalCode,

        String countryCode
) {

    /**
     * Creates booking-prefill data from one reusable Customer.
     */
    public static BookingCustomerPrefillResponse from(
            Customer customer
    ) {
        if (customer == null) {
            throw new IllegalArgumentException(
                    "Customer is required."
            );
        }

        return new BookingCustomerPrefillResponse(
                customer.getCustomerId(),
                customer.getCustomerNumber(),
                customer.getDisplayName(),
                customer.getPreferredName(),
                customer.getPrimaryEmail(),
                customer.getPrimaryPhone(),
                customer.getPreferredContactMethod(),
                customer.getStreetAddress(),
                customer.getAddressLine2(),
                customer.getCity(),
                customer.getStateRegion(),
                customer.getPostalCode(),
                customer.getCountryCode()
        );
    }
}