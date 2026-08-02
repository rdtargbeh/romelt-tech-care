package romelt_techcare.backend.dto;

import java.util.UUID;

/**
 * ================================================================
 * ROMELT TECHCARE — PUBLIC WEBSITE SERVICE RESPONSE
 * ================================================================
 *
 * Purpose:
 * Returns the stable public identity and published-version pointer for
 * an active website service.
 *
 * Actual service content will be supplied by the future
 * WebsiteServiceVersion public endpoint.
 * ================================================================
 */
public record PublicWebsiteServiceResponse(

        UUID serviceId,

        String serviceCode,

        String serviceSlug,

        UUID publishedVersionId
) {
}