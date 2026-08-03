package romelt_techcare.backend.dto;

import jakarta.validation.constraints.NotNull;
import romelt_techcare.backend.enums.WebsiteFaqStatus;

/**
 * ================================================================
 * ROMELT TECHCARE — WEBSITE FAQ STATUS REQUEST
 * ================================================================
 *
 * Purpose:
 * Updates the lifecycle status of one stable website FAQ.
 * ================================================================
 */
public record WebsiteFaqStatusRequest(

        @NotNull(message = "FAQ status is required.")
        WebsiteFaqStatus faqStatus
) {
}