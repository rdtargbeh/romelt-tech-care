package romelt_techcare.backend.dto;

import java.util.UUID;

/**
 * ================================================================
 * ROMELT TECHCARE — PUBLIC WEBSITE FAQ RESPONSE
 * ================================================================
 *
 * Purpose:
 * Returns the stable identity and current published-version pointer
 * of an active public FAQ.
 *
 * Actual question-and-answer content will be returned by the
 * WebsiteFaqVersion public API.
 * ================================================================
 */
public record PublicWebsiteFaqResponse(

        UUID faqId,

        String faqKey,

        UUID publishedVersionId
) {
}