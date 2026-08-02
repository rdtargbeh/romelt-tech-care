package romelt_techcare.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import romelt_techcare.backend.enums.WebsiteServiceStatus;

/**
 * ================================================================
 * ROMELT TECHCARE — WEBSITE SERVICE CREATE REQUEST
 * ================================================================
 *
 * Purpose:
 * Carries administrator input for creating the stable identity of a
 * website service.
 *
 * Responsibilities:
 * - Accepts the stable internal service code.
 * - Accepts the stable public service slug.
 * - Accepts the initial lifecycle status.
 *
 * Version lifecycle:
 * Draft and published version identifiers are intentionally excluded.
 * WebsiteServiceVersionService owns draft creation, publishing,
 * archiving, and synchronization of version pointers.
 * ================================================================
 */
public record WebsiteServiceCreateRequest(

        @NotBlank(message = "Service code is required.")
        @Size(
                max = 100,
                message = "Service code must not exceed 100 characters."
        )
        String serviceCode,

        @NotBlank(message = "Service slug is required.")
        @Size(
                max = 180,
                message = "Service slug must not exceed 180 characters."
        )
        String serviceSlug,

        WebsiteServiceStatus serviceStatus
) {
}