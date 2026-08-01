package romelt_techcare.backend.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * ================================================================
 * ROMELT TECHCARE — WEBSITE BUSINESS HOURS BULK UPDATE REQUEST
 * ================================================================
 *
 * Purpose:
 * Carries one to seven normal weekly schedule entries for a single
 * transactional update.
 *
 * Rules:
 * - At least one day is required.
 * - No more than seven entries are allowed.
 * - Each day may appear only once.
 * - Duplicate-day validation is enforced by the service.
 * ================================================================
 */
public record WebsiteBusinessHoursBulkUpdateRequest(

        @NotEmpty(
                message = "At least one business-hour entry is required."
        )
        @Size(
                max = 7,
                message = "A weekly schedule cannot contain more than "
                        + "seven entries."
        )
        List<
                @Valid WebsiteBusinessHourUpsertRequest
                > businessHours
) {
}