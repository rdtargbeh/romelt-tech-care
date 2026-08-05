package romelt_techcare.backend.dto;

import java.util.UUID;

/**
 * ================================================================
 * ROMELT TECHCARE — PUBLIC SERVICE FEATURE RESPONSE
 * ================================================================
 *
 * Purpose:
 * Returns one active feature belonging to the current published
 * service version.
 *
 * Security:
 * Administrator attribution, timestamps, version lifecycle metadata,
 * and optimistic-lock information are excluded.
 * ================================================================
 */
public record PublicWebsiteServiceFeatureResponse(

        UUID serviceFeatureId,

        String featureText,

        String iconKey,

        Integer displayOrder
) {
}