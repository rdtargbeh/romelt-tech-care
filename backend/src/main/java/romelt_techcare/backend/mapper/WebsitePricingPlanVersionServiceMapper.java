package romelt_techcare.backend.mapper;

import org.springframework.stereotype.Component;
import romelt_techcare.backend.dto.PublicWebsitePricingPlanVersionServiceResponse;
import romelt_techcare.backend.dto.WebsitePricingPlanVersionServiceResponse;
import romelt_techcare.backend.entity.WebsitePricingPlan;
import romelt_techcare.backend.entity.WebsitePricingPlanVersion;
import romelt_techcare.backend.entity.WebsitePricingPlanVersionService;
import romelt_techcare.backend.entity.WebsiteService;

/**
 * ================================================================
 * ROMELT TECHCARE — PRICING PLAN VERSION SERVICE MAPPER
 * ================================================================
 *
 * Purpose:
 * Converts pricing-plan version service relationships into safe
 * administrator and public responses.
 *
 * Responsibilities:
 * - Prevents direct JPA relationship serialization.
 * - Includes stable plan and service identity information.
 * - Excludes internal pricing-plan lifecycle information from public
 *   responses.
 * ================================================================
 */
@Component
public class WebsitePricingPlanVersionServiceMapper {

    public WebsitePricingPlanVersionServiceResponse toResponse(
            WebsitePricingPlanVersionService relationship
    ) {
        if (relationship == null) {
            return null;
        }

        WebsitePricingPlanVersion pricingPlanVersion =
                relationship.getPricingPlanVersion();

        WebsitePricingPlan pricingPlan =
                pricingPlanVersion == null
                        ? null
                        : pricingPlanVersion.getPricingPlan();

        WebsiteService websiteService =
                relationship.getWebsiteService();

        return new WebsitePricingPlanVersionServiceResponse(
                pricingPlanVersion == null
                        ? null
                        : pricingPlanVersion
                        .getPricingPlanVersionId(),
                pricingPlan == null
                        ? null
                        : pricingPlan.getPricingPlanId(),
                pricingPlan == null
                        ? null
                        : pricingPlan.getPlanCode(),
                pricingPlan == null
                        ? null
                        : pricingPlan.getPlanSlug(),
                pricingPlanVersion == null
                        ? null
                        : pricingPlanVersion.getVersionNumber(),
                pricingPlanVersion == null
                        ? null
                        : pricingPlanVersion.getVersionStatus(),
                websiteService == null
                        ? null
                        : websiteService.getServiceId(),
                websiteService == null
                        ? null
                        : websiteService.getServiceCode(),
                websiteService == null
                        ? null
                        : websiteService.getServiceSlug(),
                websiteService == null
                        ? null
                        : websiteService.getServiceStatus(),
                websiteService == null
                        ? null
                        : websiteService.getDraftVersionId(),
                websiteService == null
                        ? null
                        : websiteService.getPublishedVersionId(),
                websiteService != null
                        && websiteService.isDeleted(),
                websiteService != null
                        && websiteService.isPubliclyAvailable(),
                relationship.getCreatedAt()
        );
    }

    public PublicWebsitePricingPlanVersionServiceResponse
    toPublicResponse(
            WebsitePricingPlanVersionService relationship
    ) {
        if (
                relationship == null
                        || relationship.getWebsiteService() == null
        ) {
            return null;
        }

        WebsiteService service =
                relationship.getWebsiteService();

        return new PublicWebsitePricingPlanVersionServiceResponse(
                service.getServiceId(),
                service.getServiceCode(),
                service.getServiceSlug(),
                service.getPublishedVersionId()
        );
    }
}