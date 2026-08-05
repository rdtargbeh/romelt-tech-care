package romelt_techcare.backend.mapper;

import org.springframework.stereotype.Component;
import romelt_techcare.backend.dto.PublicWebsitePricingPlanVersionResponse;
import romelt_techcare.backend.dto.WebsitePricingPlanVersionCreateRequest;
import romelt_techcare.backend.dto.WebsitePricingPlanVersionResponse;
import romelt_techcare.backend.dto.WebsitePricingPlanVersionUpdateRequest;
import romelt_techcare.backend.entity.AdminUser;
import romelt_techcare.backend.entity.WebsitePricingPlan;
import romelt_techcare.backend.entity.WebsitePricingPlanVersion;

import java.util.UUID;

/**
 * ================================================================
 * ROMELT TECHCARE — PRICING PLAN VERSION MAPPER
 * ================================================================
 *
 * Purpose:
 * Converts pricing-plan version requests into service-compatible
 * entities and persisted versions into administrator and public
 * responses.
 *
 * Responsibilities:
 * - Maps editable pricing-plan content.
 * - Applies safe create defaults.
 * - Excludes lifecycle and audit fields from request mapping.
 * - Prevents direct JPA relationship serialization.
 * ================================================================
 */
@Component
public class WebsitePricingPlanVersionMapper {

    public WebsitePricingPlanVersion toEntity(
            WebsitePricingPlanVersionCreateRequest request
    ) {
        if (request == null) {
            return null;
        }

        return WebsitePricingPlanVersion.builder()
                .planName(request.planName())
                .shortDescription(request.shortDescription())
                .fullDescription(request.fullDescription())
                .pricingModel(request.pricingModel())
                .amount(request.amount())
                .currencyCode(
                        request.currencyCode() == null
                                || request.currencyCode().isBlank()
                                ? "USD"
                                : request.currencyCode()
                )
                .pricePrefix(request.pricePrefix())
                .priceSuffix(request.priceSuffix())
                .billingInterval(request.billingInterval())
                .callToActionLabel(
                        request.callToActionLabel()
                )
                .callToActionUrl(
                        request.callToActionUrl()
                )
                .displayOrder(
                        request.displayOrder() == null
                                ? 0
                                : request.displayOrder()
                )
                .isRecommended(
                        Boolean.TRUE.equals(
                                request.isRecommended()
                        )
                )
                .isFeatured(
                        Boolean.TRUE.equals(
                                request.isFeatured()
                        )
                )
                .isPublic(
                        request.isPublic() == null
                                || request.isPublic()
                )
                .effectiveFrom(request.effectiveFrom())
                .effectiveUntil(request.effectiveUntil())
                .changeSummary(request.changeSummary())
                .build();
    }

    public WebsitePricingPlanVersion toUpdateEntity(
            WebsitePricingPlanVersionUpdateRequest request
    ) {
        if (request == null) {
            return null;
        }

        return WebsitePricingPlanVersion.builder()
                .planName(request.planName())
                .shortDescription(request.shortDescription())
                .fullDescription(request.fullDescription())
                .pricingModel(request.pricingModel())
                .amount(request.amount())
                .currencyCode(request.currencyCode())
                .pricePrefix(request.pricePrefix())
                .priceSuffix(request.priceSuffix())
                .billingInterval(request.billingInterval())
                .callToActionLabel(
                        request.callToActionLabel()
                )
                .callToActionUrl(
                        request.callToActionUrl()
                )
                .displayOrder(request.displayOrder())
                .isRecommended(request.isRecommended())
                .isFeatured(request.isFeatured())
                .isPublic(request.isPublic())
                .effectiveFrom(request.effectiveFrom())
                .effectiveUntil(request.effectiveUntil())
                .changeSummary(request.changeSummary())
                .build();
    }

    public WebsitePricingPlanVersionResponse toResponse(
            WebsitePricingPlanVersion version
    ) {
        if (version == null) {
            return null;
        }

        WebsitePricingPlan pricingPlan =
                version.getPricingPlan();

        AdminUser createdBy =
                version.getCreatedByAdminUser();

        AdminUser updatedBy =
                version.getUpdatedByAdminUser();

        AdminUser publishedBy =
                version.getPublishedByAdminUser();

        AdminUser archivedBy =
                version.getArchivedByAdminUser();

        return new WebsitePricingPlanVersionResponse(
                version.getPricingPlanVersionId(),
                pricingPlan == null
                        ? null
                        : pricingPlan.getPricingPlanId(),
                pricingPlan == null
                        ? null
                        : pricingPlan.getPlanCode(),
                pricingPlan == null
                        ? null
                        : pricingPlan.getPlanSlug(),
                pricingPlan == null
                        ? null
                        : pricingPlan.getPlanStatus(),
                version.getVersionNumber(),
                version.getVersionStatus(),
                version.getPlanName(),
                version.getShortDescription(),
                version.getFullDescription(),
                version.getPricingModel(),
                version.getAmount(),
                version.getCurrencyCode(),
                version.getPricePrefix(),
                version.getPriceSuffix(),
                version.getBillingInterval(),
                version.resolveDisplayPrice(),
                version.getCallToActionLabel(),
                version.getCallToActionUrl(),
                version.getDisplayOrder(),
                version.getIsRecommended(),
                version.getIsFeatured(),
                version.getIsPublic(),
                version.getEffectiveFrom(),
                version.getEffectiveUntil(),
                version.isCurrentlyEffective(),
                version.isPubliclyAvailable(),
                version.getChangeSummary(),
                version.getPublishedAt(),
                version.getArchivedAt(),
                getAdminUserId(createdBy),
                getAdminUserDisplayName(createdBy),
                getAdminUserId(updatedBy),
                getAdminUserDisplayName(updatedBy),
                getAdminUserId(publishedBy),
                getAdminUserDisplayName(publishedBy),
                getAdminUserId(archivedBy),
                getAdminUserDisplayName(archivedBy),
                version.getCreatedAt(),
                version.getUpdatedAt(),
                version.getRowVersion()
        );
    }

    public PublicWebsitePricingPlanVersionResponse toPublicResponse(
            WebsitePricingPlanVersion version
    ) {
        if (version == null) {
            return null;
        }

        WebsitePricingPlan pricingPlan =
                version.getPricingPlan();

        return new PublicWebsitePricingPlanVersionResponse(
                pricingPlan == null
                        ? null
                        : pricingPlan.getPricingPlanId(),
                pricingPlan == null
                        ? null
                        : pricingPlan.getPlanCode(),
                pricingPlan == null
                        ? null
                        : pricingPlan.getPlanSlug(),
                version.getPricingPlanVersionId(),
                version.getVersionNumber(),
                version.getPlanName(),
                version.getShortDescription(),
                version.getFullDescription(),
                version.getPricingModel(),
                version.getAmount(),
                version.getCurrencyCode(),
                version.getPricePrefix(),
                version.getPriceSuffix(),
                version.getBillingInterval(),
                version.resolveDisplayPrice(),
                version.getCallToActionLabel(),
                version.getCallToActionUrl(),
                version.getDisplayOrder(),
                version.getIsRecommended(),
                version.getIsFeatured(),
                version.getEffectiveFrom(),
                version.getEffectiveUntil(),
                version.getPublishedAt()
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