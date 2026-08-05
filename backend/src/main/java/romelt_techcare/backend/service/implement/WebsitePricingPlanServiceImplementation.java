package romelt_techcare.backend.service.implement;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import romelt_techcare.backend.entity.AdminUser;
import romelt_techcare.backend.entity.WebsitePricingPlan;
import romelt_techcare.backend.enums.WebsiteContentAuditAction;
import romelt_techcare.backend.enums.WebsiteContentAuditResourceType;
import romelt_techcare.backend.enums.WebsitePricingPlanStatus;
import romelt_techcare.backend.repository.AdminUserRepository;
import romelt_techcare.backend.repository.WebsitePricingPlanRepository;
import romelt_techcare.backend.service.WebsiteContentAuditLogService;
import romelt_techcare.backend.service.WebsiteContentAuditSnapshotService;
import romelt_techcare.backend.service.WebsitePricingPlanService;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * ================================================================
 * ROMELT TECHCARE — PRICING PLAN SERVICE IMPLEMENTATION
 * ================================================================
 *
 * Purpose:
 * Implements production business rules for stable website
 * pricing-plan identities.
 *
 * Responsibilities:
 * - Creates and updates stable pricing-plan records.
 * - Normalizes internal plan codes and public slugs.
 * - Preserves plan-code and plan-slug uniqueness.
 * - Supports administrator filtering and pagination.
 * - Supports activation, deactivation, and archival.
 * - Supports soft deletion and restoration.
 * - Retrieves active public pricing-plan identities.
 * - Records administrator attribution.
 * - Records immutable audit entries for pricing-plan mutations.
 *
 * Version lifecycle:
 * Pricing-plan content versions and draft/published pointer changes
 * are not exposed here. They must be controlled by
 * WebsitePricingPlanVersionService.
 *
 * Audit integration:
 * Existing public methods, repository calls, entity methods,
 * validations, and return types are preserved.
 * ================================================================
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WebsitePricingPlanServiceImplementation
        implements WebsitePricingPlanService {

    private final WebsitePricingPlanRepository
            websitePricingPlanRepository;

    private final AdminUserRepository adminUserRepository;

    private final WebsiteContentAuditLogService
            websiteContentAuditLogService;

    private final WebsiteContentAuditSnapshotService
            websiteContentAuditSnapshotService;

    @Override
    @Transactional
    public WebsitePricingPlan createPricingPlan(
            WebsitePricingPlan pricingPlan,
            UUID administratorId
    ) {
        if (pricingPlan == null) {
            throw badRequest(
                    "Website pricing-plan information is required."
            );
        }

        AdminUser administrator =
                getRequiredAdministrator(administratorId);

        validateEditableFields(pricingPlan);

        String planCode =
                normalizePlanCode(
                        pricingPlan.getPlanCode()
                );

        String planSlug =
                normalizePlanSlug(
                        pricingPlan.getPlanSlug()
                );

        validateIdentityAvailability(
                planCode,
                planSlug,
                null
        );

        WebsitePricingPlanStatus initialStatus =
                pricingPlan.getPlanStatus() == null
                        ? WebsitePricingPlanStatus.ACTIVE
                        : pricingPlan.getPlanStatus();

        WebsitePricingPlan planToCreate =
                WebsitePricingPlan.builder()
                        .planCode(planCode)
                        .planSlug(planSlug)
                        .planStatus(initialStatus)
                        .createdByAdminUser(administrator)
                        .updatedByAdminUser(administrator)
                        .build();

        WebsitePricingPlan savedPricingPlan =
                savePricingPlan(
                        planToCreate,
                        "Unable to create the pricing plan because its code "
                                + "or slug is already in use."
                );

        recordPricingPlanAudit(
                administratorId,
                WebsiteContentAuditAction.CREATE,
                savedPricingPlan,
                null,
                "Website pricing plan created."
        );

        return savedPricingPlan;
    }

    @Override
    @Transactional
    public WebsitePricingPlan updatePricingPlan(
            UUID pricingPlanId,
            WebsitePricingPlan requestedUpdate,
            UUID administratorId
    ) {
        requireIdentifier(
                pricingPlanId,
                "Pricing plan ID"
        );

        if (requestedUpdate == null) {
            throw badRequest(
                    "Updated website pricing-plan information is required."
            );
        }

        AdminUser administrator =
                getRequiredAdministrator(administratorId);

        WebsitePricingPlan existingPlan =
                getPricingPlanForUpdate(pricingPlanId);

        JsonNode beforeSnapshot =
                createPricingPlanSnapshot(existingPlan);

        validateEditableFields(requestedUpdate);

        String planCode =
                normalizePlanCode(
                        requestedUpdate.getPlanCode()
                );

        String planSlug =
                normalizePlanSlug(
                        requestedUpdate.getPlanSlug()
                );

        validateIdentityAvailability(
                planCode,
                planSlug,
                pricingPlanId
        );

        existingPlan.updateIdentity(
                planCode,
                planSlug,
                requestedUpdate.getPlanStatus(),
                administrator
        );

        WebsitePricingPlan savedPricingPlan =
                savePricingPlan(
                        existingPlan,
                        "Unable to update the pricing plan because its code "
                                + "or slug is already in use."
                );

        recordPricingPlanAudit(
                administratorId,
                WebsiteContentAuditAction.UPDATE,
                savedPricingPlan,
                beforeSnapshot,
                "Website pricing plan identity updated."
        );

        return savedPricingPlan;
    }

    @Override
    public WebsitePricingPlan getPricingPlan(
            UUID pricingPlanId
    ) {
        requireIdentifier(
                pricingPlanId,
                "Pricing plan ID"
        );

        return websitePricingPlanRepository
                .findByPricingPlanIdAndDeletedAtIsNull(
                        pricingPlanId
                )
                .orElseThrow(() -> notFound(
                        "Website pricing plan was not found."
                ));
    }

    @Override
    public WebsitePricingPlan getPricingPlanByCode(
            String planCode
    ) {
        return websitePricingPlanRepository
                .findByPlanCodeAndDeletedAtIsNull(
                        normalizePlanCode(planCode)
                )
                .orElseThrow(() -> notFound(
                        "Website pricing plan was not found."
                ));
    }

    @Override
    public WebsitePricingPlan getPricingPlanBySlug(
            String planSlug
    ) {
        return websitePricingPlanRepository
                .findByPlanSlugAndDeletedAtIsNull(
                        normalizePlanSlug(planSlug)
                )
                .orElseThrow(() -> notFound(
                        "Website pricing plan was not found."
                ));
    }

    @Override
    public Page<WebsitePricingPlan> searchPricingPlans(
            String keyword,
            WebsitePricingPlanStatus planStatus,
            Pageable pageable
    ) {
        requirePageable(pageable);

        return websitePricingPlanRepository.searchPricingPlans(
                normalizeOptional(keyword),
                planStatus,
                pageable
        );
    }

    @Override
    public Page<WebsitePricingPlan> getDeletedPricingPlans(
            Pageable pageable
    ) {
        requirePageable(pageable);

        return websitePricingPlanRepository
                .findAllByDeletedAtIsNotNullOrderByDeletedAtDesc(
                        pageable
                );
    }

    @Override
    @Transactional
    public WebsitePricingPlan updatePricingPlanStatus(
            UUID pricingPlanId,
            WebsitePricingPlanStatus planStatus,
            UUID administratorId
    ) {
        if (planStatus == null) {
            throw badRequest(
                    "Pricing-plan status is required."
            );
        }

        AdminUser administrator =
                getRequiredAdministrator(administratorId);

        WebsitePricingPlan pricingPlan =
                getPricingPlanForUpdate(pricingPlanId);

        JsonNode beforeSnapshot =
                createPricingPlanSnapshot(pricingPlan);

        WebsitePricingPlanStatus previousStatus =
                pricingPlan.getPlanStatus();

        switch (planStatus) {
            case ACTIVE ->
                    pricingPlan.activate(administrator);

            case INACTIVE ->
                    pricingPlan.deactivate(administrator);

            case ARCHIVED ->
                    pricingPlan.archive(administrator);
        }

        WebsitePricingPlan savedPricingPlan =
                websitePricingPlanRepository.saveAndFlush(
                        pricingPlan
                );

        WebsiteContentAuditAction auditAction =
                planStatus == WebsitePricingPlanStatus.ARCHIVED
                        ? WebsiteContentAuditAction.ARCHIVE
                        : WebsiteContentAuditAction.UPDATE;

        recordPricingPlanAudit(
                administratorId,
                auditAction,
                savedPricingPlan,
                beforeSnapshot,
                "Website pricing plan status changed from "
                        + previousStatus
                        + " to "
                        + savedPricingPlan.getPlanStatus()
                        + "."
        );

        return savedPricingPlan;
    }

    @Override
    @Transactional
    public WebsitePricingPlan activatePricingPlan(
            UUID pricingPlanId,
            UUID administratorId
    ) {
        return updatePricingPlanStatus(
                pricingPlanId,
                WebsitePricingPlanStatus.ACTIVE,
                administratorId
        );
    }

    @Override
    @Transactional
    public WebsitePricingPlan deactivatePricingPlan(
            UUID pricingPlanId,
            UUID administratorId
    ) {
        return updatePricingPlanStatus(
                pricingPlanId,
                WebsitePricingPlanStatus.INACTIVE,
                administratorId
        );
    }

    @Override
    @Transactional
    public WebsitePricingPlan archivePricingPlan(
            UUID pricingPlanId,
            UUID administratorId
    ) {
        return updatePricingPlanStatus(
                pricingPlanId,
                WebsitePricingPlanStatus.ARCHIVED,
                administratorId
        );
    }

    @Override
    @Transactional
    public void deletePricingPlan(
            UUID pricingPlanId,
            UUID administratorId
    ) {
        AdminUser administrator =
                getRequiredAdministrator(administratorId);

        WebsitePricingPlan pricingPlan =
                getPricingPlanForUpdate(pricingPlanId);

        JsonNode beforeSnapshot =
                createPricingPlanSnapshot(pricingPlan);

        pricingPlan.softDelete(administrator);

        WebsitePricingPlan deletedPricingPlan =
                websitePricingPlanRepository.saveAndFlush(
                        pricingPlan
                );

        recordPricingPlanAudit(
                administratorId,
                WebsiteContentAuditAction.DELETE,
                deletedPricingPlan,
                beforeSnapshot,
                "Website pricing plan soft deleted."
        );
    }

    @Override
    @Transactional
    public WebsitePricingPlan restorePricingPlan(
            UUID pricingPlanId,
            UUID administratorId
    ) {
        AdminUser administrator =
                getRequiredAdministrator(administratorId);

        WebsitePricingPlan pricingPlan =
                getPricingPlanIncludingDeletedForUpdate(
                        pricingPlanId
                );

        if (!pricingPlan.isDeleted()) {
            throw conflict(
                    "The website pricing plan is not deleted."
            );
        }

        JsonNode beforeSnapshot =
                createPricingPlanSnapshot(pricingPlan);

        pricingPlan.restore(administrator);

        /*
         * Restored plans remain inactive until an administrator
         * explicitly activates them.
         */
        WebsitePricingPlan restoredPricingPlan =
                websitePricingPlanRepository.saveAndFlush(
                        pricingPlan
                );

        recordPricingPlanAudit(
                administratorId,
                WebsiteContentAuditAction.RESTORE,
                restoredPricingPlan,
                beforeSnapshot,
                "Website pricing plan restored. "
                        + "The restored pricing plan remains inactive."
        );

        return restoredPricingPlan;
    }

    @Override
    public WebsitePricingPlan getPublicPricingPlanByCode(
            String planCode
    ) {
        return websitePricingPlanRepository
                .findByPlanCodeAndPlanStatusAndDeletedAtIsNullAndPublishedVersionIdIsNotNull(
                        normalizePlanCode(planCode),
                        WebsitePricingPlanStatus.ACTIVE
                )
                .orElseThrow(() -> notFound(
                        "Public website pricing plan was not found."
                ));
    }

    @Override
    public WebsitePricingPlan getPublicPricingPlanBySlug(
            String planSlug
    ) {
        return websitePricingPlanRepository
                .findByPlanSlugAndPlanStatusAndDeletedAtIsNullAndPublishedVersionIdIsNotNull(
                        normalizePlanSlug(planSlug),
                        WebsitePricingPlanStatus.ACTIVE
                )
                .orElseThrow(() -> notFound(
                        "Public website pricing plan was not found."
                ));
    }

    @Override
    public List<WebsitePricingPlan> getPublicPricingPlans() {
        return websitePricingPlanRepository
                .findAllByPlanStatusAndDeletedAtIsNullAndPublishedVersionIdIsNotNullOrderByPlanCodeAsc(
                        WebsitePricingPlanStatus.ACTIVE
                );
    }

    @Override
    public long countPricingPlansByStatus(
            WebsitePricingPlanStatus planStatus
    ) {
        if (planStatus == null) {
            throw badRequest(
                    "Pricing-plan status is required."
            );
        }

        return websitePricingPlanRepository
                .countByPlanStatusAndDeletedAtIsNull(
                        planStatus
                );
    }

    @Override
    public long countDeletedPricingPlans() {
        return websitePricingPlanRepository
                .countByDeletedAtIsNotNull();
    }

    /**
     * Records one pricing-plan mutation in the content audit log.
     */
    private void recordPricingPlanAudit(
            UUID administratorId,
            WebsiteContentAuditAction action,
            WebsitePricingPlan pricingPlan,
            JsonNode beforeSnapshot,
            String changeSummary
    ) {
        websiteContentAuditLogService.recordAudit(
                administratorId,
                action,
                WebsiteContentAuditResourceType.PRICING_PLAN,
                pricingPlan.getPricingPlanId(),
                pricingPlan.getPlanCode(),
                beforeSnapshot,
                createPricingPlanSnapshot(pricingPlan),
                changeSummary,
                null
        );
    }

    /**
     * Creates a controlled JSON snapshot without serializing the full
     * entity graph.
     */
    private JsonNode createPricingPlanSnapshot(
            WebsitePricingPlan pricingPlan
    ) {
        Map<String, Object> fields =
                new LinkedHashMap<>();

        fields.put(
                "pricingPlanId",
                pricingPlan.getPricingPlanId()
        );

        fields.put(
                "planCode",
                pricingPlan.getPlanCode()
        );

        fields.put(
                "planSlug",
                pricingPlan.getPlanSlug()
        );

        fields.put(
                "draftVersionId",
                pricingPlan.getDraftVersionId()
        );

        fields.put(
                "publishedVersionId",
                pricingPlan.getPublishedVersionId()
        );

        fields.put(
                "planStatus",
                pricingPlan.getPlanStatus()
        );

        fields.put(
                "deleted",
                pricingPlan.isDeleted()
        );

        fields.put(
                "deletedAt",
                pricingPlan.getDeletedAt()
        );

        fields.put(
                "createdAt",
                pricingPlan.getCreatedAt()
        );

        fields.put(
                "updatedAt",
                pricingPlan.getUpdatedAt()
        );

        return websiteContentAuditSnapshotService
                .createSnapshot(fields);
    }

    private WebsitePricingPlan getPricingPlanForUpdate(
            UUID pricingPlanId
    ) {
        requireIdentifier(
                pricingPlanId,
                "Pricing plan ID"
        );

        return websitePricingPlanRepository
                .findByIdForUpdate(pricingPlanId)
                .orElseThrow(() -> notFound(
                        "Website pricing plan was not found."
                ));
    }

    private WebsitePricingPlan
    getPricingPlanIncludingDeletedForUpdate(
            UUID pricingPlanId
    ) {
        requireIdentifier(
                pricingPlanId,
                "Pricing plan ID"
        );

        return websitePricingPlanRepository
                .findIncludingDeletedByIdForUpdate(
                        pricingPlanId
                )
                .orElseThrow(() -> notFound(
                        "Website pricing plan was not found."
                ));
    }

    private void validateEditableFields(
            WebsitePricingPlan pricingPlan
    ) {
        validateRequiredLength(
                pricingPlan.getPlanCode(),
                100,
                "Plan code"
        );

        validateRequiredLength(
                pricingPlan.getPlanSlug(),
                180,
                "Plan slug"
        );

        if (pricingPlan.getPlanStatus() == null) {
            throw badRequest(
                    "Pricing-plan status is required."
            );
        }
    }

    private void validateIdentityAvailability(
            String planCode,
            String planSlug,
            UUID currentPricingPlanId
    ) {
        boolean codeExists;
        boolean slugExists;

        if (currentPricingPlanId == null) {
            codeExists =
                    websitePricingPlanRepository
                            .existsByPlanCode(planCode);

            slugExists =
                    websitePricingPlanRepository
                            .existsByPlanSlug(planSlug);
        } else {
            codeExists =
                    websitePricingPlanRepository
                            .existsByPlanCodeAndPricingPlanIdNot(
                                    planCode,
                                    currentPricingPlanId
                            );

            slugExists =
                    websitePricingPlanRepository
                            .existsByPlanSlugAndPricingPlanIdNot(
                                    planSlug,
                                    currentPricingPlanId
                            );
        }

        if (codeExists) {
            throw conflict(
                    "A website pricing plan already uses this plan code."
            );
        }

        if (slugExists) {
            throw conflict(
                    "A website pricing plan already uses this plan slug."
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

    private WebsitePricingPlan savePricingPlan(
            WebsitePricingPlan pricingPlan,
            String conflictMessage
    ) {
        try {
            return websitePricingPlanRepository
                    .saveAndFlush(pricingPlan);
        } catch (DataIntegrityViolationException exception) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    conflictMessage,
                    exception
            );
        }
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

    private void validateRequiredLength(
            String value,
            int maximumLength,
            String fieldName
    ) {
        String normalized =
                normalizeOptional(value);

        if (normalized == null) {
            throw badRequest(
                    fieldName + " is required."
            );
        }

        if (normalized.length() > maximumLength) {
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