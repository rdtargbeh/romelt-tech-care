package romelt_techcare.backend.mapper;

import org.springframework.stereotype.Component;
import romelt_techcare.backend.dto.PublicWebsitePricingPlanFeatureResponse;
import romelt_techcare.backend.dto.WebsitePricingPlanFeatureCreateRequest;
import romelt_techcare.backend.dto.WebsitePricingPlanFeatureResponse;
import romelt_techcare.backend.dto.WebsitePricingPlanFeatureUpdateRequest;
import romelt_techcare.backend.entity.AdminUser;
import romelt_techcare.backend.entity.WebsitePricingPlan;
import romelt_techcare.backend.entity.WebsitePricingPlanFeature;
import romelt_techcare.backend.entity.WebsitePricingPlanVersion;

import java.util.UUID;

/**
 * ================================================================
 * ROMELT TECHCARE — PRICING PLAN FEATURE MAPPER
 * ================================================================
 *
 * Purpose:
 * Converts pricing-plan feature requests into service-compatible
 * entities and persisted features into administrator and public
 * responses.
 *
 * Responsibilities:
 * - Maps editable feature fields.
 * - Excludes pricing-plan version ownership from request bodies.
 * - Prevents direct JPA relationship serialization.
 * - Maps administrator attribution safely.
 * ================================================================
 */
@Component
public class WebsitePricingPlanFeatureMapper {

    public WebsitePricingPlanFeature toEntity(
            WebsitePricingPlanFeatureCreateRequest request
    ) {
        if (request == null) {
            return null;
        }

        return WebsitePricingPlanFeature.builder()
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

    public WebsitePricingPlanFeature toUpdateEntity(
            WebsitePricingPlanFeatureUpdateRequest request
    ) {
        if (request == null) {
            return null;
        }

        return WebsitePricingPlanFeature.builder()
                .featureText(request.featureText())
                .iconKey(request.iconKey())
                .displayOrder(request.displayOrder())
                .isActive(request.isActive())
                .build();
    }

    public WebsitePricingPlanFeatureResponse toResponse(
            WebsitePricingPlanFeature feature
    ) {
        if (feature == null) {
            return null;
        }

        WebsitePricingPlanVersion version =
                feature.getPricingPlanVersion();

        WebsitePricingPlan pricingPlan =
                version == null
                        ? null
                        : version.getPricingPlan();

        AdminUser createdBy =
                feature.getCreatedByAdminUser();

        AdminUser updatedBy =
                feature.getUpdatedByAdminUser();

        return new WebsitePricingPlanFeatureResponse(
                feature.getPricingPlanFeatureId(),
                version == null
                        ? null
                        : version.getPricingPlanVersionId(),
                pricingPlan == null
                        ? null
                        : pricingPlan.getPricingPlanId(),
                pricingPlan == null
                        ? null
                        : pricingPlan.getPlanCode(),
                pricingPlan == null
                        ? null
                        : pricingPlan.getPlanSlug(),
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

    public PublicWebsitePricingPlanFeatureResponse toPublicResponse(
            WebsitePricingPlanFeature feature
    ) {
        if (feature == null) {
            return null;
        }

        return new PublicWebsitePricingPlanFeatureResponse(
                feature.getPricingPlanFeatureId(),
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