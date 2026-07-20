package com.romelttechcare.backend.contact.dto;

import com.romelttechcare.backend.contact.enums.ContactMethod;
import jakarta.validation.constraints.*;

/**
 * ================================================================
 * ROMELT TECHCARE — CONTACT INQUIRY CREATE REQUEST
 * ================================================================
 *
 * Purpose:
 * Validates information submitted through the public contact form.
 *
 * Responsibilities:
 * - Enforces required customer information.
 * - Validates email and optional telephone values.
 * - Limits public text lengths.
 * - Requires acknowledgement of the contact-form disclaimer.
 * ================================================================
 */
public record ContactInquiryCreateRequest(

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

        @Pattern(
                regexp = "^$|^[0-9+()\\-\\.\\s]{10,30}$",
                message = "Enter a valid telephone number."
        )
        String phone,

        @NotBlank(message = "Enter a subject.")
        @Size(
                min = 3,
                max = 120,
                message = "Subject must contain between 3 and 120 characters."
        )
        String subject,

        @Size(
                max = 100,
                message = "Service type cannot exceed 100 characters."
        )
        String serviceType,

        @NotBlank(message = "Enter your message.")
        @Size(
                min = 20,
                max = 2000,
                message = "Message must contain between 20 and 2,000 characters."
        )
        String message,

        @NotNull(
                message = "Select how you prefer to be contacted."
        )
        ContactMethod preferredContactMethod,

        @AssertTrue(
                message = "Confirm that the submitted information is accurate."
        )
        boolean consentAccepted
) {
}