package romelt_techcare.backend.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * ================================================================
 * ROMELT TECHCARE — CUSTOMER MERGE REQUEST
 * ================================================================
 *
 * Purpose:
 * Merges a duplicate customer into one surviving customer profile.
 *
 * Responsibilities:
 * - Identifies the surviving customer.
 * - Records an optional administrator explanation.
 *
 * Important:
 * The customer identified in the endpoint path is the duplicate
 * customer being merged.
 *
 * Existing bookings, inquiries, reviews, and notification records
 * should be reassigned to the surviving customer by the customer
 * service before the duplicate is marked MERGED.
 * ================================================================
 */
public record CustomerMergeRequest(

        @NotNull(
                message = "Surviving customer ID is required."
        )
        UUID survivingCustomerId,

        @Size(
                max = 1000,
                message = "Merge reason cannot exceed 1,000 characters."
        )
        String mergeReason
) {
}