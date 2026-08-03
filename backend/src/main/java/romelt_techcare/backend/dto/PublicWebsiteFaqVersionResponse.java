package romelt_techcare.backend.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * ================================================================
 * ROMELT TECHCARE — PUBLIC FAQ VERSION RESPONSE
 * ================================================================
 *
 * Purpose:
 * Returns current published FAQ content to the public website.
 *
 * Security:
 * Internal lifecycle details, change summaries, administrator
 * attribution, archival metadata, and optimistic-lock information are
 * excluded.
 * ================================================================
 */
public record PublicWebsiteFaqVersionResponse(

        UUID faqId,

        String faqKey,

        UUID faqVersionId,

        Integer versionNumber,

        String faqCategory,

        String question,

        String answer,

        Integer displayOrder,

        Boolean isFeatured,

        Instant publishedAt
) {
}