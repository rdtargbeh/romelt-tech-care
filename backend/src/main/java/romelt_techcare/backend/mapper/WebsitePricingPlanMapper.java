package romelt_techcare.backend.mapper;

import org.springframework.stereotype.Component;
import romelt_techcare.backend.dto.PublicWebsitePricingPlanResponse;
import romelt_techcare.backend.dto.WebsitePricingPlanCreateRequest;
import romelt_techcare.backend.dto.WebsitePricingPlanResponse;
import romelt_techcare.backend.dto.WebsitePricingPlanUpdateRequest;
import romelt_techcare.backend.entity.AdminUser;
import romelt_techcare.backend.entity.WebsitePricingPlan;
import romelt_techcare.backend.enums.WebsitePricingPlanStatus;

import java.util.UUID;

/**
 * ================================================================
 * ROMELT TECHCARE — PRICING PLAN MAPPER
 * ================================================================
 *
 * Purpose:
 * Converts pricing-plan requests into service-compatible entities and
 * persisted plans into administrator and public responses.
 *
 * Version lifecycle:
 * Draft and published pointers are intentionally excluded from create
 * and update request mapping.
 * ================================================================
 */
@Component
public class WebsitePricingPlanMapper {

    public WebsitePricingPlan toEntity(
            WebsitePricingPlanCreateRequest request
    ) {
        if (request == null) {
            return null;
        }

        return WebsitePricingPlan.builder()
                .planCode(request.planCode())
                .planSlug(request.planSlug())
                .planStatus(
                        request.planStatus() == null
                                ? WebsitePricingPlanStatus.ACTIVE
                                : request.planStatus()
                )
                .build();
    }

    public WebsitePricingPlan toUpdateEntity(
            WebsitePricingPlanUpdateRequest request
    ) {
        if (request == null) {
            return null;
        }

        return WebsitePricingPlan.builder()
                .planCode(request.planCode())
                .planSlug(request.planSlug())
                .planStatus(request.planStatus())
                .build();
    }

    public WebsitePricingPlanResponse toResponse(
            WebsitePricingPlan pricingPlan
    ) {
        if (pricingPlan == null) {
            return null;
        }

        AdminUser createdBy =
                pricingPlan.getCreatedByAdminUser();

        AdminUser updatedBy =
                pricingPlan.getUpdatedByAdminUser();

        AdminUser deletedBy =
                pricingPlan.getDeletedByAdminUser();

        return new WebsitePricingPlanResponse(
                pricingPlan.getPricingPlanId(),
                pricingPlan.getPlanCode(),
                pricingPlan.getPlanSlug(),
                pricingPlan.getDraftVersionId(),
                pricingPlan.getPublishedVersionId(),
                pricingPlan.getPlanStatus(),
                pricingPlan.isDeleted(),
                pricingPlan.isPubliclyAvailable(),
                getAdminUserId(createdBy),
                getAdminUserDisplayName(createdBy),
                getAdminUserId(updatedBy),
                getAdminUserDisplayName(updatedBy),
                getAdminUserId(deletedBy),
                getAdminUserDisplayName(deletedBy),
                pricingPlan.getDeletedAt(),
                pricingPlan.getCreatedAt(),
                pricingPlan.getUpdatedAt(),
                pricingPlan.getRowVersion()
        );
    }

    public PublicWebsitePricingPlanResponse toPublicResponse(
            WebsitePricingPlan pricingPlan
    ) {
        if (pricingPlan == null) {
            return null;
        }

        return new PublicWebsitePricingPlanResponse(
                pricingPlan.getPricingPlanId(),
                pricingPlan.getPlanCode(),
                pricingPlan.getPlanSlug(),
                pricingPlan.getPublishedVersionId()
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