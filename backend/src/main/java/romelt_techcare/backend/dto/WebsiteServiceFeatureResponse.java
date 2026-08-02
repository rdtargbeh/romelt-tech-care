package romelt_techcare.backend.dto;

import romelt_techcare.backend.enums.WebsiteServiceVersionStatus;

import java.time.Instant;
import java.util.UUID;

/**
 * ================================================================
 * ROMELT TECHCARE — SERVICE FEATURE RESPONSE
 * ================================================================
 *
 * Purpose:
 * Returns complete administrator-facing service-feature information.
 * ================================================================
 */
public record WebsiteServiceFeatureResponse(

        UUID serviceFeatureId,

        UUID serviceVersionId,

        UUID serviceId,

        String serviceCode,

        String serviceSlug,

        Integer versionNumber,

        WebsiteServiceVersionStatus versionStatus,

        String featureText,

        String iconKey,

        Integer displayOrder,

        Boolean isActive,

        UUID createdByAdminUserId,

        String createdByAdminUserDisplayName,

        UUID updatedByAdminUserId,

        String updatedByAdminUserDisplayName,

        Instant createdAt,

        Instant updatedAt,

        Long rowVersion
) {
}