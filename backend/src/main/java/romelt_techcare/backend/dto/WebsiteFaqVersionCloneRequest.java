package romelt_techcare.backend.dto;

import jakarta.validation.constraints.Size;

/**
 * ================================================================
 * ROMELT TECHCARE — FAQ VERSION CLONE REQUEST
 * ================================================================
 *
 * Purpose:
 * Carries an optional change summary when creating a new draft by
 * copying the current published FAQ version.
 * ================================================================
 */
public record WebsiteFaqVersionCloneRequest(

        @Size(
                max = 1000,
                message = "Change summary must not exceed 1000 characters."
        )
        String changeSummary
) {
}