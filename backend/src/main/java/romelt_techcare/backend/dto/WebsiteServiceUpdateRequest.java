package romelt_techcare.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import romelt_techcare.backend.enums.WebsiteServiceStatus;

/**
 * ================================================================
 * ROMELT TECHCARE — WEBSITE SERVICE UPDATE REQUEST
 * ================================================================
 *
 * Purpose:
 * Carries the complete editable stable identity of one website
 * service.
 *
 * Version pointers are excluded and must be controlled by the service
 * version lifecycle.
 * ================================================================
 */
public record WebsiteServiceUpdateRequest(

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

        @NotNull(message = "Service status is required.")
        WebsiteServiceStatus serviceStatus
) {
}