package romelt_techcare.backend.mapper;

import org.springframework.stereotype.Component;
import romelt_techcare.backend.dto.PublicWebsiteServiceFeatureResponse;
import romelt_techcare.backend.dto.WebsiteServiceFeatureCreateRequest;
import romelt_techcare.backend.dto.WebsiteServiceFeatureResponse;
import romelt_techcare.backend.dto.WebsiteServiceFeatureUpdateRequest;
import romelt_techcare.backend.entity.AdminUser;
import romelt_techcare.backend.entity.WebsiteService;
import romelt_techcare.backend.entity.WebsiteServiceFeature;
import romelt_techcare.backend.entity.WebsiteServiceVersion;

import java.util.UUID;

/**
 * ================================================================
 * ROMELT TECHCARE — SERVICE FEATURE MAPPER
 * ================================================================
 *
 * Purpose:
 * Converts service-feature requests into service-compatible entities
 * and persisted features into administrator and public responses.
 *
 * Responsibilities:
 * - Maps editable feature fields.
 * - Excludes service-version ownership from request-body mapping.
 * - Prevents direct JPA relationship serialization.
 * - Maps administrator attribution safely.
 * ================================================================
 */
@Component
public class WebsiteServiceFeatureMapper {

    public WebsiteServiceFeature toEntity(
            WebsiteServiceFeatureCreateRequest request
    ) {
        if (request == null) {
            return null;
        }

        return WebsiteServiceFeature.builder()
                .featureText(request.featureText())
                .iconKey(request.iconKey())
                .displayOrder(
                        request.displayOrder() == null
                                ? 0
                                : request.displayOrder()
                )
                .isActive(
                        request.isActive() == null
                                || request.isActive()
                )
                .build();
    }

    public WebsiteServiceFeature toUpdateEntity(
            WebsiteServiceFeatureUpdateRequest request
    ) {
        if (request == null) {
            return null;
        }

        return WebsiteServiceFeature.builder()
                .featureText(request.featureText())
                .iconKey(request.iconKey())
                .displayOrder(request.displayOrder())
                .isActive(request.isActive())
                .build();
    }

    public WebsiteServiceFeatureResponse toResponse(
            WebsiteServiceFeature feature
    ) {
        if (feature == null) {
            return null;
        }

        WebsiteServiceVersion version =
                feature.getServiceVersion();

        WebsiteService service =
                version == null
                        ? null
                        : version.getWebsiteService();

        AdminUser createdBy =
                feature.getCreatedByAdminUser();

        AdminUser updatedBy =
                feature.getUpdatedByAdminUser();

        return new WebsiteServiceFeatureResponse(
                feature.getServiceFeatureId(),
                version == null
                        ? null
                        : version.getServiceVersionId(),
                service == null
                        ? null
                        : service.getServiceId(),
                service == null
                        ? null
                        : service.getServiceCode(),
                service == null
                        ? null
                        : service.getServiceSlug(),
                version == null
                        ? null
                        : version.getVersionNumber(),
                version == null
                        ? null
                        : version.getVersionStatus(),
                feature.getFeatureText(),
                feature.getIconKey(),
                feature.getDisplayOrder(),
                feature.getIsActive(),
                getAdminUserId(createdBy),
                getAdminUserDisplayName(createdBy),
                getAdminUserId(updatedBy),
                getAdminUserDisplayName(updatedBy),
                feature.getCreatedAt(),
                feature.getUpdatedAt(),
                feature.getRowVersion()
        );
    }

    public PublicWebsiteServiceFeatureResponse toPublicResponse(
            WebsiteServiceFeature feature
    ) {
        if (feature == null) {
            return null;
        }

        return new PublicWebsiteServiceFeatureResponse(
                feature.getServiceFeatureId(),
                feature.getFeatureText(),
                feature.getIconKey(),
                feature.getDisplayOrder()
        );
    }

    private UUID getAdminUserId(
            AdminUser administrator
    ) {
        return administrator == null
                ? null
                : administrator.getAdminUserId();
    }

    private String getAdminUserDisplayName(
            AdminUser administrator
    ) {
        if (administrator == null) {
            return null;
        }

        String firstName =
                normalizeOptional(
                        administrator.getFirstName()
                );

        String lastName =
                normalizeOptional(
                        administrator.getLastName()
                );

        if (firstName != null && lastName != null) {
            return firstName + " " + lastName;
        }

        if (firstName != null) {
            return firstName;
        }

        if (lastName != null) {
            return lastName;
        }

        return normalizeOptional(
                administrator.getEmail()
        );
    }

    private String normalizeOptional(
            String value
    ) {
        if (value == null) {
            return null;
        }

        String normalized = value.trim();

        return normalized.isEmpty()
                ? null
                : normalized;
    }
}