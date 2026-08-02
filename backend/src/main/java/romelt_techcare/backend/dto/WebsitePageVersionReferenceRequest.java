package romelt_techcare.backend.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * ================================================================
 * ROMELT TECHCARE — WEBSITE PAGE VERSION REFERENCE REQUEST
 * ================================================================
 *
 * Purpose:
 * Assigns a draft or published page-version identifier to a stable
 * website page.
 *
 * The page-version service will later validate that the supplied
 * version belongs to the same page before assignment.
 * ================================================================
 */
public record WebsitePageVersionReferenceRequest(

        @NotNull(message = "Page version ID is required.")
        UUID pageVersionId
) {
}