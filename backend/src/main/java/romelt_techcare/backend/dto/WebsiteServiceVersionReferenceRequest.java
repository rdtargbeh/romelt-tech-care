package romelt_techcare.backend.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * ================================================================
 * ROMELT TECHCARE — SERVICE VERSION REFERENCE REQUEST
 * ================================================================
 *
 * Purpose:
 * Temporarily supports assigning a draft or published service-version
 * identifier before the service-version lifecycle module is added.
 *
 * Important:
 * Remove direct pointer-management controller endpoints after
 * WebsiteServiceVersion is implemented. Version ownership and
 * publication rules must then be controlled transactionally by that
 * service.
 * ================================================================
 */
public record WebsiteServiceVersionReferenceRequest(

        @NotNull(message = "Service version ID is required.")
        UUID serviceVersionId
) {
}