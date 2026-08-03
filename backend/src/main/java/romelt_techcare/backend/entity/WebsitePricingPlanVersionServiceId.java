package romelt_techcare.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.UUID;

/**
 * ================================================================
 * ROMELT TECHCARE — PRICING PLAN VERSION SERVICE COMPOSITE KEY
 * ================================================================
 *
 * Purpose:
 * Identifies one relationship between a pricing-plan version and a
 * stable website service.
 *
 * Database mapping:
 * - pricing_plan_version_id
 * - service_id
 *
 * Both identifiers form the primary key of
 * website_pricing_plan_version_services.
 * ================================================================
 */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class WebsitePricingPlanVersionServiceId
        implements Serializable {

    @Column(
            name = "pricing_plan_version_id",
            nullable = false
    )
    private UUID pricingPlanVersionId;

    @Column(
            name = "service_id",
            nullable = false
    )
    private UUID serviceId;
}