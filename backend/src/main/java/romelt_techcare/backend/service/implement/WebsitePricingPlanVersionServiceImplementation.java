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
import romelt_techcare.backend.entity.WebsitePricingPlanVersion;
import romelt_techcare.backend.enums.WebsiteBillingInterval;
import romelt_techcare.backend.enums.WebsiteContentAuditAction;
import romelt_techcare.backend.enums.WebsiteContentAuditResourceType;
import romelt_techcare.backend.enums.WebsitePricingModel;
import romelt_techcare.backend.enums.WebsitePricingPlanStatus;
import romelt_techcare.backend.enums.WebsitePricingPlanVersionStatus;
import romelt_techcare.backend.repository.AdminUserRepository;
import romelt_techcare.backend.repository.WebsitePricingPlanRepository;
import romelt_techcare.backend.repository.WebsitePricingPlanVersionRepository;
import romelt_techcare.backend.service.WebsiteContentAuditLogService;
import romelt_techcare.backend.service.WebsiteContentAuditSnapshotService;
import romelt_techcare.backend.service.WebsitePricingPlanVersionService;

import java.math.BigDecimal;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * ================================================================
 * ROMELT TECHCARE — PRICING PLAN VERSION SERVICE IMPLEMENTATION
 * ================================================================
 *
 * Purpose:
 * Implements production lifecycle and public-retrieval rules for
 * website pricing-plan versions.
 *
 * Publication transaction:
 * 1. Lock the stable pricing plan.
 * 2. Lock the selected draft.
 * 3. Archive the current published version when present.
 * 4. Publish the selected draft.
 * 5. Clear the stable plan's draft pointer.
 * 6. Assign the stable plan's published pointer.
 *
 * Mutation rules:
 * - Only DRAFT versions may be edited, published, or deleted.
 * - Published and archived versions remain immutable.
 *
 * Public rules:
 * Public versions must:
 * - belong to active, non-deleted pricing plans;
 * - be the stable plan's current published version;
 * - have PUBLISHED status;
 * - be marked public;
 * - be currently effective.
 * ================================================================
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WebsitePricingPlanVersionServiceImplementation
        implements WebsitePricingPlanVersionService {

    private final WebsitePricingPlanVersionRepository
            websitePricingPlanVersionRepository;

    private final WebsitePricingPlanRepository
            websitePricingPlanRepository;

    private final AdminUserRepository adminUserRepository;

    private final WebsiteContentAuditLogService
            websiteContentAuditLogService;

    private final WebsiteContentAuditSnapshotService
            websiteContentAuditSnapshotService;

    @Override
    @Transactional
    public WebsitePricingPlanVersion createDraft(
            UUID pricingPlanId,
            WebsitePricingPlanVersion requestedDraft,
            UUID administratorId
    ) {
        requireIdentifier(
                pricingPlanId,
                "Pricing plan ID"
        );

        if (requestedDraft == null) {
            throw badRequest(
                    "Pricing-plan draft information is required."
            );
        }

        AdminUser administrator =
                getRequiredAdministrator(administratorId);

        WebsitePricingPlan pricingPlan =
                getPricingPlanForUpdate(pricingPlanId);

        ensurePricingPlanUsable(pricingPlan);
        ensureNoExistingDraft(pricingPlanId);

        normalizeAndValidateDraft(requestedDraft);

        int nextVersionNumber =
                websitePricingPlanVersionRepository
                        .findMaximumVersionNumber(
                                pricingPlanId
                        )
                        + 1;

        WebsitePricingPlanVersion draft =
                WebsitePricingPlanVersion.builder()
                        .pricingPlan(pricingPlan)
                        .versionNumber(nextVersionNumber)
                        .versionStatus(
                                WebsitePricingPlanVersionStatus.DRAFT
                        )
                        .planName(
                                normalizeRequired(
                                        requestedDraft.getPlanName(),
                                        "Plan name"
                                )
                        )
                        .shortDescription(
                                normalizeOptional(
                                        requestedDraft
                                                .getShortDescription()
                                )
                        )
                        .fullDescription(
                                normalizeOptional(
                                        requestedDraft
                                                .getFullDescription()
                                )
                        )
                        .pricingModel(
                                requestedDraft.getPricingModel()
                        )
                        .amount(requestedDraft.getAmount())
                        .currencyCode(
                                normalizeCurrencyCode(
                                        requestedDraft
                                                .getCurrencyCode()
                                )
                        )
                        .pricePrefix(
                                normalizeOptional(
                                        requestedDraft
                                                .getPricePrefix()
                                )
                        )
                        .priceSuffix(
                                normalizeOptional(
                                        requestedDraft
                                                .getPriceSuffix()
                                )
                        )
                        .billingInterval(
                                requestedDraft
                                        .getBillingInterval()
                        )
                        .callToActionLabel(
                                normalizeOptional(
                                        requestedDraft
                                                .getCallToActionLabel()
                                )
                        )
                        .callToActionUrl(
                                normalizeCallToActionUrl(
                                        requestedDraft
                                                .getCallToActionUrl()
                                )
                        )
                        .displayOrder(
                                requestedDraft
                                        .getDisplayOrder()
                        )
                        .isRecommended(
                                requestedDraft
                                        .getIsRecommended()
                        )
                        .isFeatured(
                                requestedDraft
                                        .getIsFeatured()
                        )
                        .isPublic(
                                requestedDraft.getIsPublic()
                        )
                        .effectiveFrom(
                                requestedDraft
                                        .getEffectiveFrom()
                        )
                        .effectiveUntil(
                                requestedDraft
                                        .getEffectiveUntil()
                        )
                        .changeSummary(
                                normalizeOptional(
                                        requestedDraft
                                                .getChangeSummary()
                                )
                        )
                        .createdByAdminUser(administrator)
                        .updatedByAdminUser(administrator)
                        .build();

        WebsitePricingPlanVersion savedDraft =
                saveVersion(
                        draft,
                        "Unable to create the pricing-plan draft. "
                                + "The plan may already have a current draft."
                );

        pricingPlan.assignDraftVersion(
                savedDraft.getPricingPlanVersionId(),
                administrator
        );

        websitePricingPlanRepository.saveAndFlush(
                pricingPlan
        );

        recordPricingPlanVersionAudit(
                administratorId,
                WebsiteContentAuditAction.SAVE_DRAFT,
                savedDraft,
                null,
                "Created pricing-plan draft version "
                        + savedDraft.getVersionNumber()
                        + "."
        );

        return getPricingPlanVersion(
                savedDraft.getPricingPlanVersionId()
        );
    }

    @Override
    @Transactional
    public WebsitePricingPlanVersion
    createDraftFromPublishedVersion(
            UUID pricingPlanId,
            String changeSummary,
            UUID administratorId
    ) {
        requireIdentifier(
                pricingPlanId,
                "Pricing plan ID"
        );

        validateLength(
                changeSummary,
                1000,
                "Change summary",
                false
        );

        AdminUser administrator =
                getRequiredAdministrator(administratorId);

        WebsitePricingPlan pricingPlan =
                getPricingPlanForUpdate(pricingPlanId);

        ensurePricingPlanUsable(pricingPlan);
        ensureNoExistingDraft(pricingPlanId);

        WebsitePricingPlanVersion publishedVersion =
                websitePricingPlanVersionRepository
                        .findByPricingPlanAndStatusForUpdate(
                                pricingPlanId,
                                WebsitePricingPlanVersionStatus
                                        .PUBLISHED
                        )
                        .orElseThrow(() -> conflict(
                                "The pricing plan has no published "
                                        + "version to copy."
                        ));

        int nextVersionNumber =
                websitePricingPlanVersionRepository
                        .findMaximumVersionNumber(
                                pricingPlanId
                        )
                        + 1;

        WebsitePricingPlanVersion draft =
                WebsitePricingPlanVersion.builder()
                        .pricingPlan(pricingPlan)
                        .versionNumber(nextVersionNumber)
                        .versionStatus(
                                WebsitePricingPlanVersionStatus.DRAFT
                        )
                        .planName(
                                publishedVersion.getPlanName()
                        )
                        .shortDescription(
                                publishedVersion
                                        .getShortDescription()
                        )
                        .fullDescription(
                                publishedVersion
                                        .getFullDescription()
                        )
                        .pricingModel(
                                publishedVersion
                                        .getPricingModel()
                        )
                        .amount(
                                publishedVersion.getAmount()
                        )
                        .currencyCode(
                                publishedVersion
                                        .getCurrencyCode()
                        )
                        .pricePrefix(
                                publishedVersion
                                        .getPricePrefix()
                        )
                        .priceSuffix(
                                publishedVersion
                                        .getPriceSuffix()
                        )
                        .billingInterval(
                                publishedVersion
                                        .getBillingInterval()
                        )
                        .callToActionLabel(
                                publishedVersion
                                        .getCallToActionLabel()
                        )
                        .callToActionUrl(
                                publishedVersion
                                        .getCallToActionUrl()
                        )
                        .displayOrder(
                                publishedVersion
                                        .getDisplayOrder()
                        )
                        .isRecommended(
                                publishedVersion
                                        .getIsRecommended()
                        )
                        .isFeatured(
                                publishedVersion
                                        .getIsFeatured()
                        )
                        .isPublic(
                                publishedVersion.getIsPublic()
                        )
                        .effectiveFrom(
                                publishedVersion
                                        .getEffectiveFrom()
                        )
                        .effectiveUntil(
                                publishedVersion
                                        .getEffectiveUntil()
                        )
                        .changeSummary(
                                normalizeOptional(changeSummary)
                        )
                        .createdByAdminUser(administrator)
                        .updatedByAdminUser(administrator)
                        .build();

        WebsitePricingPlanVersion savedDraft =
                saveVersion(
                        draft,
                        "Unable to create a draft from the published "
                                + "pricing-plan version."
                );

        pricingPlan.assignDraftVersion(
                savedDraft.getPricingPlanVersionId(),
                administrator
        );

        websitePricingPlanRepository.saveAndFlush(
                pricingPlan
        );

        recordPricingPlanVersionAudit(
                administratorId,
                WebsiteContentAuditAction.SAVE_DRAFT,
                savedDraft,
                createPricingPlanVersionSnapshot(
                        publishedVersion
                ),
                "Created pricing-plan draft version "
                        + savedDraft.getVersionNumber()
                        + " from published version "
                        + publishedVersion.getVersionNumber()
                        + "."
        );

        return getPricingPlanVersion(
                savedDraft.getPricingPlanVersionId()
        );
    }

    @Override
    @Transactional
    public WebsitePricingPlanVersion updateDraft(
            UUID pricingPlanVersionId,
            WebsitePricingPlanVersion requestedUpdate,
            UUID administratorId
    ) {
        requireIdentifier(
                pricingPlanVersionId,
                "Pricing plan version ID"
        );

        if (requestedUpdate == null) {
            throw badRequest(
                    "Updated pricing-plan draft information is required."
            );
        }

        AdminUser administrator =
                getRequiredAdministrator(administratorId);

        WebsitePricingPlanVersion draft =
                getVersionForUpdate(
                        pricingPlanVersionId
                );

        if (!draft.isDraft()) {
            throw conflict(
                    "Only a draft pricing-plan version may be edited."
            );
        }

        ensurePricingPlanUsable(
                draft.getPricingPlan()
        );

        JsonNode beforeSnapshot =
                createPricingPlanVersionSnapshot(draft);

        normalizeAndValidateDraft(requestedUpdate);

        draft.updateDraft(
                normalizeRequired(
                        requestedUpdate.getPlanName(),
                        "Plan name"
                ),
                normalizeOptional(
                        requestedUpdate.getShortDescription()
                ),
                normalizeOptional(
                        requestedUpdate.getFullDescription()
                ),
                requestedUpdate.getPricingModel(),
                requestedUpdate.getAmount(),
                normalizeCurrencyCode(
                        requestedUpdate.getCurrencyCode()
                ),
                normalizeOptional(
                        requestedUpdate.getPricePrefix()
                ),
                normalizeOptional(
                        requestedUpdate.getPriceSuffix()
                ),
                requestedUpdate.getBillingInterval(),
                normalizeOptional(
                        requestedUpdate
                                .getCallToActionLabel()
                ),
                normalizeCallToActionUrl(
                        requestedUpdate
                                .getCallToActionUrl()
                ),
                requestedUpdate.getDisplayOrder(),
                requestedUpdate.getIsRecommended(),
                requestedUpdate.getIsFeatured(),
                requestedUpdate.getIsPublic(),
                requestedUpdate.getEffectiveFrom(),
                requestedUpdate.getEffectiveUntil(),
                normalizeOptional(
                        requestedUpdate.getChangeSummary()
                ),
                administrator
        );

        WebsitePricingPlanVersion savedDraft =
                saveVersion(
                        draft,
                        "Unable to update the pricing-plan draft."
                );

        recordPricingPlanVersionAudit(
                administratorId,
                WebsiteContentAuditAction.SAVE_DRAFT,
                savedDraft,
                beforeSnapshot,
                "Updated pricing-plan draft version "
                        + savedDraft.getVersionNumber()
                        + "."
        );

        return getPricingPlanVersion(
                savedDraft.getPricingPlanVersionId()
        );
    }

    @Override
    public WebsitePricingPlanVersion getPricingPlanVersion(
            UUID pricingPlanVersionId
    ) {
        requireIdentifier(
                pricingPlanVersionId,
                "Pricing plan version ID"
        );

        return websitePricingPlanVersionRepository
                .findByPricingPlanVersionId(
                        pricingPlanVersionId
                )
                .orElseThrow(() -> notFound(
                        "Website pricing-plan version was not found."
                ));
    }

    @Override
    public WebsitePricingPlanVersion getCurrentDraft(
            UUID pricingPlanId
    ) {
        requireIdentifier(
                pricingPlanId,
                "Pricing plan ID"
        );

        return websitePricingPlanVersionRepository
                .findByPricingPlan_PricingPlanIdAndVersionStatus(
                        pricingPlanId,
                        WebsitePricingPlanVersionStatus.DRAFT
                )
                .orElseThrow(() -> notFound(
                        "The pricing plan has no current draft."
                ));
    }

    @Override
    public WebsitePricingPlanVersion
    getCurrentPublishedVersion(
            UUID pricingPlanId
    ) {
        requireIdentifier(
                pricingPlanId,
                "Pricing plan ID"
        );

        return websitePricingPlanVersionRepository
                .findByPricingPlan_PricingPlanIdAndVersionStatus(
                        pricingPlanId,
                        WebsitePricingPlanVersionStatus
                                .PUBLISHED
                )
                .orElseThrow(() -> notFound(
                        "The pricing plan has no published version."
                ));
    }

    @Override
    public Page<WebsitePricingPlanVersion> getVersionHistory(
            UUID pricingPlanId,
            WebsitePricingPlanVersionStatus versionStatus,
            Pageable pageable
    ) {
        requireIdentifier(
                pricingPlanId,
                "Pricing plan ID"
        );

        requirePageable(pageable);

        if (
                !websitePricingPlanRepository.existsById(
                        pricingPlanId
                )
        ) {
            throw notFound(
                    "Website pricing plan was not found."
            );
        }

        if (versionStatus == null) {
            return websitePricingPlanVersionRepository
                    .findAllByPricingPlan_PricingPlanIdOrderByVersionNumberDesc(
                            pricingPlanId,
                            pageable
                    );
        }

        return websitePricingPlanVersionRepository
                .findAllByPricingPlan_PricingPlanIdAndVersionStatusOrderByVersionNumberDesc(
                        pricingPlanId,
                        versionStatus,
                        pageable
                );
    }

    @Override
    @Transactional
    public WebsitePricingPlanVersion publishDraft(
            UUID pricingPlanVersionId,
            UUID administratorId
    ) {
        AdminUser administrator =
                getRequiredAdministrator(administratorId);

        WebsitePricingPlanVersion draft =
                getVersionForUpdate(
                        pricingPlanVersionId
                );

        if (!draft.isDraft()) {
            throw conflict(
                    "Only a draft pricing-plan version may be published."
            );
        }

        WebsitePricingPlan pricingPlan =
                getPricingPlanForUpdate(
                        draft.getPricingPlan()
                                .getPricingPlanId()
                );

        ensurePricingPlanUsable(pricingPlan);
        normalizeAndValidateDraft(draft);

        if (
                !pricingPlanVersionId.equals(
                        pricingPlan.getDraftVersionId()
                )
        ) {
            throw conflict(
                    "The selected version is not the pricing plan's "
                            + "current draft."
            );
        }

        JsonNode draftBeforeSnapshot =
                createPricingPlanVersionSnapshot(draft);

        WebsitePricingPlanVersion currentPublished =
                websitePricingPlanVersionRepository
                        .findByPricingPlanAndStatusForUpdate(
                                pricingPlan.getPricingPlanId(),
                                WebsitePricingPlanVersionStatus
                                        .PUBLISHED
                        )
                        .orElse(null);

        if (currentPublished != null) {
            JsonNode publishedBeforeSnapshot =
                    createPricingPlanVersionSnapshot(
                            currentPublished
                    );

            currentPublished.archive(administrator);

            WebsitePricingPlanVersion archivedVersion =
                    websitePricingPlanVersionRepository
                            .saveAndFlush(currentPublished);

            recordPricingPlanVersionAudit(
                    administratorId,
                    WebsiteContentAuditAction.ARCHIVE,
                    archivedVersion,
                    publishedBeforeSnapshot,
                    "Archived pricing-plan version "
                            + archivedVersion.getVersionNumber()
                            + " because version "
                            + draft.getVersionNumber()
                            + " was published."
            );
        }

        draft.publish(administrator);

        WebsitePricingPlanVersion publishedDraft =
                websitePricingPlanVersionRepository
                        .saveAndFlush(draft);

        pricingPlan.assignPublishedVersion(
                publishedDraft
                        .getPricingPlanVersionId(),
                administrator
        );

        pricingPlan.clearDraftVersion(
                administrator
        );

        websitePricingPlanRepository.saveAndFlush(
                pricingPlan
        );

        recordPricingPlanVersionAudit(
                administratorId,
                WebsiteContentAuditAction.PUBLISH,
                publishedDraft,
                draftBeforeSnapshot,
                "Published pricing-plan version "
                        + publishedDraft.getVersionNumber()
                        + "."
        );

        return getPricingPlanVersion(
                publishedDraft.getPricingPlanVersionId()
        );
    }

    @Override
    @Transactional
    public WebsitePricingPlanVersion archiveVersion(
            UUID pricingPlanVersionId,
            UUID administratorId
    ) {
        AdminUser administrator =
                getRequiredAdministrator(administratorId);

        WebsitePricingPlanVersion version =
                getVersionForUpdate(
                        pricingPlanVersionId
                );

        WebsitePricingPlan pricingPlan =
                getPricingPlanForUpdate(
                        version.getPricingPlan()
                                .getPricingPlanId()
                );

        if (version.isArchived()) {
            return getPricingPlanVersion(
                    pricingPlanVersionId
            );
        }

        JsonNode beforeSnapshot =
                createPricingPlanVersionSnapshot(version);

        boolean wasDraft = version.isDraft();
        boolean wasPublished = version.isPublished();

        version.archive(administrator);

        WebsitePricingPlanVersion archivedVersion =
                websitePricingPlanVersionRepository
                        .saveAndFlush(version);

        if (
                wasDraft
                        && pricingPlanVersionId.equals(
                        pricingPlan.getDraftVersionId()
                )
        ) {
            pricingPlan.clearDraftVersion(
                    administrator
            );
        }

        if (
                wasPublished
                        && pricingPlanVersionId.equals(
                        pricingPlan
                                .getPublishedVersionId()
                )
        ) {
            pricingPlan.clearPublishedVersion(
                    administrator
            );
        }

        websitePricingPlanRepository.saveAndFlush(
                pricingPlan
        );

        recordPricingPlanVersionAudit(
                administratorId,
                WebsiteContentAuditAction.ARCHIVE,
                archivedVersion,
                beforeSnapshot,
                "Archived pricing-plan version "
                        + archivedVersion.getVersionNumber()
                        + "."
        );

        return getPricingPlanVersion(
                pricingPlanVersionId
        );
    }

    @Override
    @Transactional
    public void deleteDraft(
            UUID pricingPlanVersionId,
            UUID administratorId
    ) {
        AdminUser administrator =
                getRequiredAdministrator(administratorId);

        WebsitePricingPlanVersion draft =
                getVersionForUpdate(
                        pricingPlanVersionId
                );

        if (!draft.isDraft()) {
            throw conflict(
                    "Only a draft pricing-plan version may be permanently deleted."
            );
        }

        JsonNode beforeSnapshot =
                createPricingPlanVersionSnapshot(draft);

        UUID deletedVersionId =
                draft.getPricingPlanVersionId();

        int deletedVersionNumber =
                draft.getVersionNumber();

        String resourceName =
                createPricingPlanVersionResourceName(draft);

        WebsitePricingPlan pricingPlan =
                getPricingPlanForUpdate(
                        draft.getPricingPlan()
                                .getPricingPlanId()
                );

        if (
                pricingPlanVersionId.equals(
                        pricingPlan.getDraftVersionId()
                )
        ) {
            pricingPlan.clearDraftVersion(
                    administrator
            );

            websitePricingPlanRepository
                    .saveAndFlush(pricingPlan);
        }

        websitePricingPlanVersionRepository
                .delete(draft);

        websitePricingPlanVersionRepository.flush();

        websiteContentAuditLogService.recordAudit(
                administratorId,
                WebsiteContentAuditAction.DELETE,
                WebsiteContentAuditResourceType
                        .PRICING_PLAN_VERSION,
                deletedVersionId,
                resourceName,
                beforeSnapshot,
                null,
                "Permanently deleted pricing-plan draft version "
                        + deletedVersionNumber
                        + ".",
                null
        );
    }

    @Override
    public WebsitePricingPlanVersion
    getPublicPricingPlanByCode(
            String planCode
    ) {
        return websitePricingPlanVersionRepository
                .findPublicByPlanCode(
                        normalizePlanCode(planCode),
                        Instant.now()
                )
                .orElseThrow(() -> notFound(
                        "Public website pricing plan was not found."
                ));
    }

    @Override
    public WebsitePricingPlanVersion
    getPublicPricingPlanBySlug(
            String planSlug
    ) {
        return websitePricingPlanVersionRepository
                .findPublicByPlanSlug(
                        normalizePlanSlug(planSlug),
                        Instant.now()
                )
                .orElseThrow(() -> notFound(
                        "Public website pricing plan was not found."
                ));
    }

    @Override
    public List<WebsitePricingPlanVersion>
    getPublicPricingPlans() {
        return websitePricingPlanVersionRepository
                .findAllPublicPricingPlans(
                        Instant.now()
                );
    }

    @Override
    public List<WebsitePricingPlanVersion>
    getFeaturedPublicPricingPlans() {
        return websitePricingPlanVersionRepository
                .findAllFeaturedPublicPricingPlans(
                        Instant.now()
                );
    }

    @Override
    public List<WebsitePricingPlanVersion>
    getRecommendedPublicPricingPlans() {
        return websitePricingPlanVersionRepository
                .findAllRecommendedPublicPricingPlans(
                        Instant.now()
                );
    }

    private void recordPricingPlanVersionAudit(
            UUID administratorId,
            WebsiteContentAuditAction action,
            WebsitePricingPlanVersion version,
            JsonNode beforeSnapshot,
            String changeSummary
    ) {
        websiteContentAuditLogService.recordAudit(
                administratorId,
                action,
                WebsiteContentAuditResourceType
                        .PRICING_PLAN_VERSION,
                version.getPricingPlanVersionId(),
                createPricingPlanVersionResourceName(version),
                beforeSnapshot,
                createPricingPlanVersionSnapshot(version),
                changeSummary,
                null
        );
    }

    private JsonNode createPricingPlanVersionSnapshot(
            WebsitePricingPlanVersion version
    ) {
        Map<String, Object> fields =
                new LinkedHashMap<>();

        fields.put(
                "pricingPlanVersionId",
                version.getPricingPlanVersionId()
        );

        fields.put(
                "pricingPlanId",
                version.getPricingPlan() == null
                        ? null
                        : version.getPricingPlan()
                        .getPricingPlanId()
        );

        fields.put(
                "planCode",
                version.getPricingPlan() == null
                        ? null
                        : version.getPricingPlan()
                        .getPlanCode()
        );

        fields.put(
                "planSlug",
                version.getPricingPlan() == null
                        ? null
                        : version.getPricingPlan()
                        .getPlanSlug()
        );

        fields.put(
                "versionNumber",
                version.getVersionNumber()
        );

        fields.put(
                "versionStatus",
                version.getVersionStatus()
        );

        fields.put(
                "planName",
                version.getPlanName()
        );

        fields.put(
                "shortDescription",
                version.getShortDescription()
        );

        fields.put(
                "fullDescription",
                version.getFullDescription()
        );

        fields.put(
                "pricingModel",
                version.getPricingModel()
        );

        fields.put(
                "amount",
                version.getAmount()
        );

        fields.put(
                "currencyCode",
                version.getCurrencyCode()
        );

        fields.put(
                "pricePrefix",
                version.getPricePrefix()
        );

        fields.put(
                "priceSuffix",
                version.getPriceSuffix()
        );

        fields.put(
                "billingInterval",
                version.getBillingInterval()
        );

        fields.put(
                "callToActionLabel",
                version.getCallToActionLabel()
        );

        fields.put(
                "callToActionUrl",
                version.getCallToActionUrl()
        );

        fields.put(
                "displayOrder",
                version.getDisplayOrder()
        );

        fields.put(
                "isRecommended",
                version.getIsRecommended()
        );

        fields.put(
                "isFeatured",
                version.getIsFeatured()
        );

        fields.put(
                "isPublic",
                version.getIsPublic()
        );

        fields.put(
                "effectiveFrom",
                version.getEffectiveFrom()
        );

        fields.put(
                "effectiveUntil",
                version.getEffectiveUntil()
        );

        fields.put(
                "changeSummary",
                version.getChangeSummary()
        );

        fields.put(
                "publishedAt",
                version.getPublishedAt()
        );

        fields.put(
                "archivedAt",
                version.getArchivedAt()
        );

        return websiteContentAuditSnapshotService
                .createSnapshot(fields);
    }

    private String createPricingPlanVersionResourceName(
            WebsitePricingPlanVersion version
    ) {
        String planCode =
                version.getPricingPlan() == null
                        ? null
                        : normalizeOptional(
                        version.getPricingPlan()
                                .getPlanCode()
                );

        if (planCode == null) {
            planCode = "PRICING_PLAN";
        }

        return planCode
                + " - Version "
                + version.getVersionNumber();
    }

    private WebsitePricingPlanVersion getVersionForUpdate(
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

    private void ensureNoExistingDraft(
            UUID pricingPlanId
    ) {
        if (
                websitePricingPlanVersionRepository
                        .existsByPricingPlan_PricingPlanIdAndVersionStatus(
                                pricingPlanId,
                                WebsitePricingPlanVersionStatus.DRAFT
                        )
        ) {
            throw conflict(
                    "The pricing plan already has a current draft."
            );
        }
    }

    private void ensurePricingPlanUsable(
            WebsitePricingPlan pricingPlan
    ) {
        if (
                pricingPlan == null
                        || pricingPlan.isDeleted()
        ) {
            throw conflict(
                    "A deleted pricing plan cannot manage content versions."
            );
        }

        if (
                pricingPlan.getPlanStatus()
                        == WebsitePricingPlanStatus.ARCHIVED
        ) {
            throw conflict(
                    "An archived pricing plan cannot manage new content versions."
            );
        }
    }

    private void normalizeAndValidateDraft(
            WebsitePricingPlanVersion version
    ) {
        validateLength(
                version.getPlanName(),
                180,
                "Plan name",
                true
        );

        validateLength(
                version.getShortDescription(),
                500,
                "Short description",
                false
        );

        validateLength(
                version.getPricePrefix(),
                100,
                "Price prefix",
                false
        );

        validateLength(
                version.getPriceSuffix(),
                100,
                "Price suffix",
                false
        );

        validateLength(
                version.getCallToActionLabel(),
                180,
                "Call-to-action label",
                false
        );

        validateLength(
                version.getCallToActionUrl(),
                1000,
                "Call-to-action URL",
                false
        );

        validateLength(
                version.getChangeSummary(),
                1000,
                "Change summary",
                false
        );

        if (version.getPricingModel() == null) {
            throw badRequest(
                    "Pricing model is required."
            );
        }

        BigDecimal amount = version.getAmount();

        if (
                amount != null
                        && amount.compareTo(BigDecimal.ZERO) < 0
        ) {
            throw badRequest(
                    "Pricing amount must not be negative."
            );
        }

        normalizeCurrencyCode(
                version.getCurrencyCode()
        );

        if (
                version.getDisplayOrder() == null
                        || version.getDisplayOrder() < 0
        ) {
            throw badRequest(
                    "Display order must not be negative."
            );
        }

        if (version.getIsRecommended() == null) {
            throw badRequest(
                    "Recommended status is required."
            );
        }

        if (version.getIsFeatured() == null) {
            throw badRequest(
                    "Featured status is required."
            );
        }

        if (version.getIsPublic() == null) {
            throw badRequest(
                    "Public status is required."
            );
        }

        if (
                version.getEffectiveUntil() != null
                        && version.getEffectiveFrom() != null
                        && !version.getEffectiveUntil()
                        .isAfter(
                                version.getEffectiveFrom()
                        )
        ) {
            throw badRequest(
                    "Effective-until time must be after effective-from time."
            );
        }

        validatePricingModelRules(version);

        normalizeCallToActionUrl(
                version.getCallToActionUrl()
        );
    }

    private void validatePricingModelRules(
            WebsitePricingPlanVersion version
    ) {
        WebsitePricingModel pricingModel =
                version.getPricingModel();

        BigDecimal amount = version.getAmount();

        if (
                pricingModel
                        == WebsitePricingModel.CONTACT_FOR_PRICE
                        && amount != null
        ) {
            throw badRequest(
                    "CONTACT_FOR_PRICE pricing must not contain an amount."
            );
        }

        if (
                requiresAmount(pricingModel)
                        && amount == null
        ) {
            throw badRequest(
                    "The selected pricing model requires an amount."
            );
        }

        WebsiteBillingInterval billingInterval =
                version.getBillingInterval();

        if (
                pricingModel == WebsitePricingModel.ONE_TIME
                        && billingInterval != null
                        && billingInterval
                        != WebsiteBillingInterval.ONE_TIME
        ) {
            throw badRequest(
                    "ONE_TIME pricing must use the ONE_TIME billing interval."
            );
        }
    }

    private boolean requiresAmount(
            WebsitePricingModel pricingModel
    ) {
        return switch (pricingModel) {
            case ONE_TIME,
                 HOURLY,
                 DAILY,
                 WEEKLY,
                 MONTHLY,
                 QUARTERLY,
                 ANNUAL -> true;

            case CUSTOM,
                 CONTACT_FOR_PRICE -> false;
        };
    }

    private String normalizeCallToActionUrl(
            String value
    ) {
        String normalized = normalizeOptional(value);

        if (normalized == null) {
            return null;
        }

        if (
                normalized.startsWith("/")
                        || normalized.startsWith("#")
        ) {
            return normalized;
        }

        try {
            URI uri = new URI(normalized);

            String scheme = uri.getScheme();
            String host = uri.getHost();

            if (
                    scheme == null
                            || host == null
                            || (
                            !"http".equalsIgnoreCase(scheme)
                                    && !"https".equalsIgnoreCase(
                                    scheme
                            )
                    )
            ) {
                throw badRequest(
                        "Call-to-action URL must be an internal route, "
                                + "anchor, or valid HTTP/HTTPS URL."
                );
            }

            return uri.normalize().toString();
        } catch (URISyntaxException exception) {
            throw badRequest(
                    "Call-to-action URL is invalid."
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

    private String normalizeCurrencyCode(
            String value
    ) {
        String normalized =
                normalizeOptional(value);

        if (normalized == null) {
            normalized = "USD";
        }

        normalized =
                normalized.toUpperCase(Locale.ROOT);

        if (!normalized.matches("[A-Z]{3}")) {
            throw badRequest(
                    "Currency code must contain exactly three letters."
            );
        }

        return normalized;
    }

    private WebsitePricingPlanVersion saveVersion(
            WebsitePricingPlanVersion version,
            String conflictMessage
    ) {
        try {
            return websitePricingPlanVersionRepository
                    .saveAndFlush(version);
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

        String normalized = value.trim();

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