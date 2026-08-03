package romelt_techcare.backend.service.implement;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import romelt_techcare.backend.entity.AdminUser;
import romelt_techcare.backend.entity.WebsitePricingPlan;
import romelt_techcare.backend.entity.WebsitePricingPlanFeature;
import romelt_techcare.backend.entity.WebsitePricingPlanVersion;
import romelt_techcare.backend.enums.WebsiteContentAuditAction;
import romelt_techcare.backend.enums.WebsiteContentAuditResourceType;
import romelt_techcare.backend.enums.WebsitePricingPlanStatus;
import romelt_techcare.backend.enums.WebsitePricingPlanVersionStatus;
import romelt_techcare.backend.repository.AdminUserRepository;
import romelt_techcare.backend.repository.WebsitePricingPlanFeatureRepository;
import romelt_techcare.backend.repository.WebsitePricingPlanVersionRepository;
import romelt_techcare.backend.service.WebsiteContentAuditLogService;
import romelt_techcare.backend.service.WebsiteContentAuditSnapshotService;
import romelt_techcare.backend.service.WebsitePricingPlanFeatureService;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * ================================================================
 * ROMELT TECHCARE — PRICING PLAN FEATURE SERVICE IMPLEMENTATION
 * ================================================================
 *
 * Purpose:
 * Implements production rules for pricing-plan version features.
 *
 * Ownership:
 * Every feature belongs to one WebsitePricingPlanVersion.
 *
 * Mutation rules:
 * Only features belonging to DRAFT pricing-plan versions may be
 * created, edited, reordered, activated, deactivated, or deleted.
 *
 * Published and archived features remain immutable to preserve
 * historical and publicly published content.
 *
 * Public retrieval:
 * Public queries return active features belonging to the current
 * published version of an active, non-deleted, public, and currently
 * effective pricing plan.
 *
 * Audit behavior:
 * Pricing-plan features are subordinate content belonging to a
 * pricing-plan version. Feature mutations are therefore recorded
 * against the PRICING_PLAN_VERSION audit resource using the owning
 * pricing-plan version ID.
 *
 * Existing service behavior:
 * Audit integration does not change existing public methods,
 * repository calls, validation rules, entity operations, or return
 * types.
 * ================================================================
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WebsitePricingPlanFeatureServiceImplementation
        implements WebsitePricingPlanFeatureService {

    private final WebsitePricingPlanFeatureRepository
            websitePricingPlanFeatureRepository;

    private final WebsitePricingPlanVersionRepository
            websitePricingPlanVersionRepository;

    private final AdminUserRepository adminUserRepository;

    private final WebsiteContentAuditLogService
            websiteContentAuditLogService;

    private final WebsiteContentAuditSnapshotService
            websiteContentAuditSnapshotService;

    @Override
    @Transactional
    public WebsitePricingPlanFeature createPricingPlanFeature(
            UUID pricingPlanVersionId,
            WebsitePricingPlanFeature requestedFeature,
            UUID administratorId
    ) {
        requireIdentifier(
                pricingPlanVersionId,
                "Pricing plan version ID"
        );

        if (requestedFeature == null) {
            throw badRequest(
                    "Pricing-plan feature information is required."
            );
        }

        AdminUser administrator =
                getRequiredAdministrator(administratorId);

        WebsitePricingPlanVersion pricingPlanVersion =
                getPricingPlanVersionForUpdate(
                        pricingPlanVersionId
                );

        requireEditableDraft(pricingPlanVersion);
        validateEditableFields(requestedFeature);

        WebsitePricingPlanFeature feature =
                WebsitePricingPlanFeature.builder()
                        .pricingPlanVersion(pricingPlanVersion)
                        .featureText(
                                normalizeRequired(
                                        requestedFeature.getFeatureText(),
                                        "Feature text"
                                )
                        )
                        .iconKey(
                                normalizeIconKey(
                                        requestedFeature.getIconKey()
                                )
                        )
                        .displayOrder(
                                requestedFeature.getDisplayOrder() == null
                                        ? 0
                                        : requestedFeature.getDisplayOrder()
                        )
                        .isActive(
                                requestedFeature.getIsActive() == null
                                        || requestedFeature.getIsActive()
                        )
                        .createdByAdminUser(administrator)
                        .updatedByAdminUser(administrator)
                        .build();

        WebsitePricingPlanFeature savedFeature =
                websitePricingPlanFeatureRepository
                        .saveAndFlush(feature);

        recordFeatureAudit(
                administratorId,
                WebsiteContentAuditAction.CREATE,
                savedFeature,
                null,
                "Pricing-plan feature created."
        );

        return savedFeature;
    }

    @Override
    @Transactional
    public WebsitePricingPlanFeature updatePricingPlanFeature(
            UUID pricingPlanFeatureId,
            WebsitePricingPlanFeature requestedUpdate,
            UUID administratorId
    ) {
        requireIdentifier(
                pricingPlanFeatureId,
                "Pricing plan feature ID"
        );

        if (requestedUpdate == null) {
            throw badRequest(
                    "Updated pricing-plan feature information is required."
            );
        }

        AdminUser administrator =
                getRequiredAdministrator(administratorId);

        WebsitePricingPlanFeature existingFeature =
                getFeatureForUpdate(pricingPlanFeatureId);

        requireEditableDraft(
                existingFeature.getPricingPlanVersion()
        );

        validateEditableFields(requestedUpdate);

        JsonNode beforeSnapshot =
                createFeatureSnapshot(existingFeature);

        existingFeature.updateDetails(
                normalizeRequired(
                        requestedUpdate.getFeatureText(),
                        "Feature text"
                ),
                normalizeIconKey(
                        requestedUpdate.getIconKey()
                ),
                requestedUpdate.getDisplayOrder(),
                requestedUpdate.getIsActive(),
                administrator
        );

        WebsitePricingPlanFeature savedFeature =
                websitePricingPlanFeatureRepository
                        .saveAndFlush(existingFeature);

        recordFeatureAudit(
                administratorId,
                WebsiteContentAuditAction.UPDATE,
                savedFeature,
                beforeSnapshot,
                "Pricing-plan feature updated."
        );

        return savedFeature;
    }

    @Override
    public WebsitePricingPlanFeature getPricingPlanFeature(
            UUID pricingPlanFeatureId
    ) {
        requireIdentifier(
                pricingPlanFeatureId,
                "Pricing plan feature ID"
        );

        return websitePricingPlanFeatureRepository
                .findByPricingPlanFeatureId(
                        pricingPlanFeatureId
                )
                .orElseThrow(() -> notFound(
                        "Website pricing-plan feature was not found."
                ));
    }

    @Override
    public List<WebsitePricingPlanFeature> getPricingPlanFeatures(
            UUID pricingPlanVersionId
    ) {
        requireExistingPricingPlanVersion(
                pricingPlanVersionId
        );

        return websitePricingPlanFeatureRepository
                .findAllByPricingPlanVersion_PricingPlanVersionIdOrderByDisplayOrderAscCreatedAtAsc(
                        pricingPlanVersionId
                );
    }

    @Override
    public Page<WebsitePricingPlanFeature> searchPricingPlanFeatures(
            UUID pricingPlanVersionId,
            String keyword,
            Boolean isActive,
            Pageable pageable
    ) {
        requireExistingPricingPlanVersion(
                pricingPlanVersionId
        );

        requirePageable(pageable);

        return websitePricingPlanFeatureRepository
                .searchPricingPlanFeatures(
                        pricingPlanVersionId,
                        normalizeOptional(keyword),
                        isActive,
                        pageable
                );
    }

    @Override
    @Transactional
    public WebsitePricingPlanFeature
    updatePricingPlanFeatureStatus(
            UUID pricingPlanFeatureId,
            boolean isActive,
            UUID administratorId
    ) {
        AdminUser administrator =
                getRequiredAdministrator(administratorId);

        WebsitePricingPlanFeature feature =
                getFeatureForUpdate(pricingPlanFeatureId);

        requireEditableDraft(
                feature.getPricingPlanVersion()
        );

        JsonNode beforeSnapshot =
                createFeatureSnapshot(feature);

        feature.updateStatus(
                isActive,
                administrator
        );

        WebsitePricingPlanFeature savedFeature =
                websitePricingPlanFeatureRepository
                        .saveAndFlush(feature);

        recordFeatureAudit(
                administratorId,
                WebsiteContentAuditAction.UPDATE,
                savedFeature,
                beforeSnapshot,
                isActive
                        ? "Pricing-plan feature activated."
                        : "Pricing-plan feature deactivated."
        );

        return savedFeature;
    }

    @Override
    @Transactional
    public WebsitePricingPlanFeature
    updatePricingPlanFeatureOrder(
            UUID pricingPlanFeatureId,
            Integer displayOrder,
            UUID administratorId
    ) {
        if (displayOrder == null || displayOrder < 0) {
            throw badRequest(
                    "Display order must not be negative."
            );
        }

        AdminUser administrator =
                getRequiredAdministrator(administratorId);

        WebsitePricingPlanFeature feature =
                getFeatureForUpdate(pricingPlanFeatureId);

        requireEditableDraft(
                feature.getPricingPlanVersion()
        );

        JsonNode beforeSnapshot =
                createFeatureSnapshot(feature);

        feature.updateDisplayOrder(
                displayOrder,
                administrator
        );

        WebsitePricingPlanFeature savedFeature =
                websitePricingPlanFeatureRepository
                        .saveAndFlush(feature);

        recordFeatureAudit(
                administratorId,
                WebsiteContentAuditAction.UPDATE,
                savedFeature,
                beforeSnapshot,
                "Pricing-plan feature display order updated."
        );

        return savedFeature;
    }

    @Override
    @Transactional
    public void deletePricingPlanFeature(
            UUID pricingPlanFeatureId,
            UUID administratorId
    ) {
        getRequiredAdministrator(administratorId);

        WebsitePricingPlanFeature feature =
                getFeatureForUpdate(pricingPlanFeatureId);

        requireEditableDraft(
                feature.getPricingPlanVersion()
        );

        JsonNode beforeSnapshot =
                createFeatureSnapshot(feature);

        UUID pricingPlanVersionId =
                feature.getPricingPlanVersion()
                        .getPricingPlanVersionId();

        String resourceName =
                createPricingPlanVersionResourceName(
                        feature.getPricingPlanVersion()
                );

        websitePricingPlanFeatureRepository.delete(feature);
        websitePricingPlanFeatureRepository.flush();

        websiteContentAuditLogService.recordAudit(
                administratorId,
                WebsiteContentAuditAction.DELETE,
                WebsiteContentAuditResourceType
                        .PRICING_PLAN_VERSION,
                pricingPlanVersionId,
                resourceName,
                beforeSnapshot,
                null,
                "Pricing-plan feature deleted.",
                null
        );
    }

    @Override
    public List<WebsitePricingPlanFeature>
    getPublicFeaturesByPlanCode(
            String planCode
    ) {
        return websitePricingPlanFeatureRepository
                .findPublicByPlanCode(
                        normalizePlanCode(planCode),
                        Instant.now()
                );
    }

    @Override
    public List<WebsitePricingPlanFeature>
    getPublicFeaturesByPlanSlug(
            String planSlug
    ) {
        return websitePricingPlanFeatureRepository
                .findPublicByPlanSlug(
                        normalizePlanSlug(planSlug),
                        Instant.now()
                );
    }

    @Override
    public long countPricingPlanFeatures(
            UUID pricingPlanVersionId
    ) {
        requireExistingPricingPlanVersion(
                pricingPlanVersionId
        );

        return websitePricingPlanFeatureRepository
                .countByPricingPlanVersion_PricingPlanVersionId(
                        pricingPlanVersionId
                );
    }

    @Override
    public long countActivePricingPlanFeatures(
            UUID pricingPlanVersionId
    ) {
        requireExistingPricingPlanVersion(
                pricingPlanVersionId
        );

        return websitePricingPlanFeatureRepository
                .countByPricingPlanVersion_PricingPlanVersionIdAndIsActiveTrue(
                        pricingPlanVersionId
                );
    }

    /**
     * Records a feature change against its owning pricing-plan
     * version.
     */
    private void recordFeatureAudit(
            UUID administratorId,
            WebsiteContentAuditAction action,
            WebsitePricingPlanFeature feature,
            JsonNode beforeSnapshot,
            String changeSummary
    ) {
        WebsitePricingPlanVersion pricingPlanVersion =
                feature.getPricingPlanVersion();

        websiteContentAuditLogService.recordAudit(
                administratorId,
                action,
                WebsiteContentAuditResourceType
                        .PRICING_PLAN_VERSION,
                pricingPlanVersion.getPricingPlanVersionId(),
                createPricingPlanVersionResourceName(
                        pricingPlanVersion
                ),
                beforeSnapshot,
                createFeatureSnapshot(feature),
                changeSummary,
                null
        );
    }

    /**
     * Creates a controlled feature snapshot without serializing the
     * complete entity graph.
     */
    private JsonNode createFeatureSnapshot(
            WebsitePricingPlanFeature feature
    ) {
        Map<String, Object> fields =
                new LinkedHashMap<>();

        fields.put(
                "pricingPlanFeatureId",
                feature.getPricingPlanFeatureId()
        );

        fields.put(
                "pricingPlanVersionId",
                feature.getPricingPlanVersion() == null
                        ? null
                        : feature.getPricingPlanVersion()
                        .getPricingPlanVersionId()
        );

        fields.put(
                "pricingPlanId",
                feature.getPricingPlanVersion() == null
                        || feature.getPricingPlanVersion()
                        .getPricingPlan() == null
                        ? null
                        : feature.getPricingPlanVersion()
                        .getPricingPlan()
                        .getPricingPlanId()
        );

        fields.put(
                "versionNumber",
                feature.getPricingPlanVersion() == null
                        ? null
                        : feature.getPricingPlanVersion()
                        .getVersionNumber()
        );

        fields.put(
                "featureText",
                feature.getFeatureText()
        );

        fields.put(
                "iconKey",
                feature.getIconKey()
        );

        fields.put(
                "displayOrder",
                feature.getDisplayOrder()
        );

        fields.put(
                "isActive",
                feature.getIsActive()
        );

        return websiteContentAuditSnapshotService
                .createSnapshot(fields);
    }

    /**
     * Produces an audit resource name for the owning pricing-plan
     * version.
     */
    private String createPricingPlanVersionResourceName(
            WebsitePricingPlanVersion pricingPlanVersion
    ) {
        if (pricingPlanVersion == null) {
            return "Pricing Plan Version";
        }

        WebsitePricingPlan pricingPlan =
                pricingPlanVersion.getPricingPlan();

        String planCode =
                pricingPlan == null
                        ? null
                        : normalizeOptional(
                        pricingPlan.getPlanCode()
                );

        if (planCode == null) {
            planCode = "PRICING_PLAN";
        }

        return planCode
                + " - Version "
                + pricingPlanVersion.getVersionNumber();
    }

    private WebsitePricingPlanFeature getFeatureForUpdate(
            UUID pricingPlanFeatureId
    ) {
        requireIdentifier(
                pricingPlanFeatureId,
                "Pricing plan feature ID"
        );

        return websitePricingPlanFeatureRepository
                .findByIdForUpdate(pricingPlanFeatureId)
                .orElseThrow(() -> notFound(
                        "Website pricing-plan feature was not found."
                ));
    }

    private WebsitePricingPlanVersion
    getPricingPlanVersionForUpdate(
            UUID pricingPlanVersionId
    ) {
        requireIdentifier(
                pricingPlanVersionId,
                "Pricing plan version ID"
        );

        return websitePricingPlanVersionRepository
                .findByIdForUpdate(pricingPlanVersionId)
                .orElseThrow(() -> notFound(
                        "Website pricing-plan version was not found."
                ));
    }

    private void requireExistingPricingPlanVersion(
            UUID pricingPlanVersionId
    ) {
        requireIdentifier(
                pricingPlanVersionId,
                "Pricing plan version ID"
        );

        if (
                !websitePricingPlanVersionRepository
                        .existsById(pricingPlanVersionId)
        ) {
            throw notFound(
                    "Website pricing-plan version was not found."
            );
        }
    }

    private void requireEditableDraft(
            WebsitePricingPlanVersion pricingPlanVersion
    ) {
        if (pricingPlanVersion == null) {
            throw notFound(
                    "Website pricing-plan version was not found."
            );
        }

        if (
                pricingPlanVersion.getVersionStatus()
                        != WebsitePricingPlanVersionStatus.DRAFT
        ) {
            throw conflict(
                    "Only features belonging to a draft pricing-plan "
                            + "version may be modified."
            );
        }

        WebsitePricingPlan pricingPlan =
                pricingPlanVersion.getPricingPlan();

        if (
                pricingPlan == null
                        || pricingPlan.isDeleted()
        ) {
            throw conflict(
                    "Features cannot be modified because the owning "
                            + "pricing plan is deleted."
            );
        }

        if (
                pricingPlan.getPlanStatus()
                        == WebsitePricingPlanStatus.ARCHIVED
        ) {
            throw conflict(
                    "Features cannot be modified because the owning "
                            + "pricing plan is archived."
            );
        }

        if (
                pricingPlan.getDraftVersionId() != null
                        && !pricingPlanVersion
                        .getPricingPlanVersionId()
                        .equals(pricingPlan.getDraftVersionId())
        ) {
            throw conflict(
                    "The selected version is not the pricing plan's "
                            + "current draft."
            );
        }
    }

    private void validateEditableFields(
            WebsitePricingPlanFeature feature
    ) {
        validateLength(
                feature.getFeatureText(),
                500,
                "Feature text",
                true
        );

        validateLength(
                feature.getIconKey(),
                100,
                "Icon key",
                false
        );

        if (
                feature.getDisplayOrder() == null
                        || feature.getDisplayOrder() < 0
        ) {
            throw badRequest(
                    "Display order must not be negative."
            );
        }

        if (feature.getIsActive() == null) {
            throw badRequest(
                    "Active status is required."
            );
        }
    }

    private String normalizePlanCode(
            String value
    ) {
        String normalized =
                normalizeRequired(
                        value,
                        "Plan code"
                )
                        .toUpperCase(Locale.ROOT)
                        .replaceAll("[^A-Z0-9]+", "_")
                        .replaceAll("^_+|_+$", "");

        if (normalized.isBlank()) {
            throw badRequest(
                    "Plan code is required."
            );
        }

        if (normalized.length() > 100) {
            throw badRequest(
                    "Plan code must not exceed 100 characters."
            );
        }

        return normalized;
    }

    private String normalizePlanSlug(
            String value
    ) {
        String normalized =
                normalizeRequired(
                        value,
                        "Plan slug"
                )
                        .toLowerCase(Locale.ROOT)
                        .replaceAll("[^a-z0-9]+", "-")
                        .replaceAll("^-+|-+$", "");

        if (normalized.isBlank()) {
            throw badRequest(
                    "Plan slug is required."
            );
        }

        if (normalized.length() > 180) {
            throw badRequest(
                    "Plan slug must not exceed 180 characters."
            );
        }

        return normalized;
    }

    private String normalizeIconKey(
            String value
    ) {
        String normalized =
                normalizeOptional(value);

        if (normalized == null) {
            return null;
        }

        normalized =
                normalized
                        .toLowerCase(Locale.ROOT)
                        .replaceAll("[^a-z0-9]+", "-")
                        .replaceAll("^-+|-+$", "");

        if (normalized.isBlank()) {
            return null;
        }

        if (normalized.length() > 100) {
            throw badRequest(
                    "Icon key must not exceed 100 characters."
            );
        }

        return normalized;
    }

    private AdminUser getRequiredAdministrator(
            UUID administratorId
    ) {
        requireIdentifier(
                administratorId,
                "Administrator ID"
        );

        return adminUserRepository
                .findById(administratorId)
                .orElseThrow(() -> notFound(
                        "Administrator account was not found."
                ));
    }

    private void validateLength(
            String value,
            int maximumLength,
            String fieldName,
            boolean required
    ) {
        String normalized =
                normalizeOptional(value);

        if (required && normalized == null) {
            throw badRequest(
                    fieldName + " is required."
            );
        }

        if (
                normalized != null
                        && normalized.length() > maximumLength
        ) {
            throw badRequest(
                    fieldName
                            + " must not exceed "
                            + maximumLength
                            + " characters."
            );
        }
    }

    private void requirePageable(
            Pageable pageable
    ) {
        if (pageable == null) {
            throw badRequest(
                    "Pagination information is required."
            );
        }
    }

    private void requireIdentifier(
            UUID identifier,
            String fieldName
    ) {
        if (identifier == null) {
            throw badRequest(
                    fieldName + " is required."
            );
        }
    }

    private String normalizeRequired(
            String value,
            String fieldName
    ) {
        String normalized =
                normalizeOptional(value);

        if (normalized == null) {
            throw badRequest(
                    fieldName + " is required."
            );
        }

        return normalized;
    }

    private String normalizeOptional(
            String value
    ) {
        if (value == null) {
            return null;
        }

        String normalized =
                value.trim();

        return normalized.isEmpty()
                ? null
                : normalized;
    }

    private ResponseStatusException badRequest(
            String message
    ) {
        return new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                message
        );
    }

    private ResponseStatusException notFound(
            String message
    ) {
        return new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                message
        );
    }

    private ResponseStatusException conflict(
            String message
    ) {
        return new ResponseStatusException(
                HttpStatus.CONFLICT,
                message
        );
    }
}