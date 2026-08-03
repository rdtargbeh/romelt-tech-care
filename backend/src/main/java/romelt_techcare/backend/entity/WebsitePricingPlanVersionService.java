package romelt_techcare.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * ================================================================
 * ROMELT TECHCARE — PRICING PLAN VERSION SERVICE RELATIONSHIP
 * ================================================================
 *
 * Purpose:
 * Associates one stable website service with one specific
 * WebsitePricingPlanVersion.
 *
 * Responsibilities:
 * - Supports listing services included in a pricing plan.
 * - Preserves service membership independently for every version.
 * - Allows draft pricing-plan versions to change included services.
 * - Preserves published and archived version relationships as
 *   immutable historical content.
 *
 * Ownership:
 * This relationship belongs to WebsitePricingPlanVersion, not directly
 * to WebsitePricingPlan.
 *
 * Mutation rules:
 * - Relationships may be added or removed only from the pricing plan's
 *   current DRAFT version.
 * - PUBLISHED and ARCHIVED version relationships are immutable.
 *
 * Deletion behavior:
 * - Deleting a pricing-plan version removes its relationships.
 * - Deleting a stable service removes its relationships.
 * ================================================================
 */
@Entity
@Table(
        name = "website_pricing_plan_version_services",
        indexes = {
                @Index(
                        name = "idx_website_pricing_version_services_service",
                        columnList = "service_id"
                )
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebsitePricingPlanVersionService {

    @EmbeddedId
    private WebsitePricingPlanVersionServiceId id;

    @MapsId("pricingPlanVersionId")
    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "pricing_plan_version_id",
            nullable = false
    )
    private WebsitePricingPlanVersion pricingPlanVersion;

    @MapsId("serviceId")
    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "service_id",
            nullable = false
    )
    private WebsiteService websiteService;

    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        if (pricingPlanVersion == null) {
            throw new IllegalStateException(
                    "Pricing-plan version is required."
            );
        }

        if (pricingPlanVersion.getPricingPlanVersionId() == null) {
            throw new IllegalStateException(
                    "Pricing-plan version must be persisted before it can be linked."
            );
        }

        if (websiteService == null) {
            throw new IllegalStateException(
                    "Website service is required."
            );
        }

        if (websiteService.getServiceId() == null) {
            throw new IllegalStateException(
                    "Website service must be persisted before it can be linked."
            );
        }

        if (id == null) {
            id = new WebsitePricingPlanVersionServiceId(
                    pricingPlanVersion.getPricingPlanVersionId(),
                    websiteService.getServiceId()
            );
        }

        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}