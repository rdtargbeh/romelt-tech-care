package romelt_techcare.backend.dto;

import romelt_techcare.backend.entity.Customer;

/**
 * ================================================================
 * ROMELT TECHCARE — CUSTOMER RESOLUTION RESULT
 * ================================================================
 *
 * Purpose:
 * Returns the result of resolving a booking or inquiry contact to a
 * reusable customer profile.
 *
 * Responsibilities:
 * - Returns the resolved customer entity.
 * - Indicates whether a new customer was created.
 *
 * Internal use:
 * This DTO is used inside backend services and is not returned by a
 * controller.
 * ================================================================
 */
public record CustomerResolutionResult(

        Customer customer,

        boolean created
) {

    public CustomerResolutionResult {
        if (customer == null) {
            throw new IllegalArgumentException(
                    "Resolved customer must not be null."
            );
        }
    }
}