package romelt_techcare.backend.dto;

import jakarta.validation.constraints.Size;

/**
 * ================================================================
 * ROMELT TECHCARE — SERVICE VERSION CLONE REQUEST
 * ================================================================
 *
 * Purpose:
 * Carries an optional change summary when creating a draft by copying
 * the current published service version.
 * ================================================================
 */
public record WebsiteServiceVersionCloneRequest(

        @Size(
                max = 1000,
                message = "Change summary must not exceed 1000 characters."
        )
        String changeSummary
) {
}