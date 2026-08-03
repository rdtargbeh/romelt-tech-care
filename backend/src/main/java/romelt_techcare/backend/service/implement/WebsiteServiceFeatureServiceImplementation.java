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
import romelt_techcare.backend.entity.WebsiteService;
import romelt_techcare.backend.entity.WebsiteServiceFeature;
import romelt_techcare.backend.entity.WebsiteServiceVersion;
import romelt_techcare.backend.enums.WebsiteContentAuditAction;
import romelt_techcare.backend.enums.WebsiteContentAuditResourceType;
import romelt_techcare.backend.enums.WebsiteServiceVersionStatus;
import romelt_techcare.backend.repository.AdminUserRepository;
import romelt_techcare.backend.repository.WebsiteServiceFeatureRepository;
import romelt_techcare.backend.repository.WebsiteServiceVersionRepository;
import romelt_techcare.backend.service.WebsiteContentAuditLogService;
import romelt_techcare.backend.service.WebsiteContentAuditSnapshotService;
import romelt_techcare.backend.service.WebsiteServiceFeatureService;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * ================================================================
 * ROMELT TECHCARE — SERVICE FEATURE SERVICE IMPLEMENTATION
 * ================================================================
 *
 * Purpose:
 * Implements production rules for service-version feature management.
 *
 * Ownership:
 * Every feature belongs to one WebsiteServiceVersion.
 *
 * Immutability:
 * Only features belonging to DRAFT service versions may be created,
 * edited, reordered, activated, deactivated, or deleted.
 *
 * Published and archived service-version features remain immutable so
 * historical and public content cannot be changed unexpectedly.
 *
 * Public retrieval:
 * Public queries return only active features belonging to the current
 * published version of an active, non-deleted, public, and currently
 * effective service.
 *
 * Audit behavior:
 * Service-feature changes are audited against the owning
 * SERVICE_VERSION resource.
 * ================================================================
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WebsiteServiceFeatureServiceImplementation
        implements WebsiteServiceFeatureService {

    private final WebsiteServiceFeatureRepository
            websiteServiceFeatureRepository;

    private final WebsiteServiceVersionRepository
            websiteServiceVersionRepository;

    private final AdminUserRepository adminUserRepository;

    private final WebsiteContentAuditLogService
            websiteContentAuditLogService;

    private final WebsiteContentAuditSnapshotService
            websiteContentAuditSnapshotService;

    @Override
    @Transactional
    public WebsiteServiceFeature createServiceFeature(
            UUID serviceVersionId,
            WebsiteServiceFeature requestedFeature,
            UUID administratorId
    ) {
        requireIdentifier(
                serviceVersionId,
                "Service version ID"
        );

        if (requestedFeature == null) {
            throw badRequest(
                    "Service feature information is required."
            );
        }

        AdminUser administrator =
                getRequiredAdministrator(administratorId);

        WebsiteServiceVersion serviceVersion =
                getServiceVersionForUpdate(serviceVersionId);

        requireEditableDraft(serviceVersion);

        validateEditableFields(requestedFeature);

        WebsiteServiceFeature feature =
                WebsiteServiceFeature.builder()
                        .serviceVersion(serviceVersion)
                        .featureText(
                                normalizeRequired(
                                        requestedFeature
                                                .getFeatureText(),
                                        "Feature text"
                                )
                        )
                        .iconKey(
                                normalizeIconKey(
                                        requestedFeature
                                                .getIconKey()
                                )
                        )
                        .displayOrder(
                                requestedFeature
                                        .getDisplayOrder() == null
                                        ? 0
                                        : requestedFeature
                                        .getDisplayOrder()
                        )
                        .isActive(
                                requestedFeature
                                        .getIsActive() == null
                                        || requestedFeature
                                        .getIsActive()
                        )
                        .createdByAdminUser(administrator)
                        .updatedByAdminUser(administrator)
                        .build();

        WebsiteServiceFeature savedFeature =
                websiteServiceFeatureRepository
                        .saveAndFlush(feature);

        recordFeatureAudit(
                administratorId,
                WebsiteContentAuditAction.CREATE,
                savedFeature,
                null,
                "Website service feature created."
        );

        return savedFeature;
    }

    @Override
    @Transactional
    public WebsiteServiceFeature updateServiceFeature(
            UUID serviceFeatureId,
            WebsiteServiceFeature requestedUpdate,
            UUID administratorId
    ) {
        requireIdentifier(
                serviceFeatureId,
                "Service feature ID"
        );

        if (requestedUpdate == null) {
            throw badRequest(
                    "Updated service-feature information is required."
            );
        }

        AdminUser administrator =
                getRequiredAdministrator(administratorId);

        WebsiteServiceFeature existingFeature =
                getFeatureForUpdate(serviceFeatureId);

        requireEditableDraft(
                existingFeature.getServiceVersion()
        );

        JsonNode beforeSnapshot =
                createFeatureSnapshot(existingFeature);

        validateEditableFields(requestedUpdate);

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

        WebsiteServiceFeature savedFeature =
                websiteServiceFeatureRepository
                        .saveAndFlush(existingFeature);

        recordFeatureAudit(
                administratorId,
                WebsiteContentAuditAction.UPDATE,
                savedFeature,
                beforeSnapshot,
                "Website service feature updated."
        );

        return savedFeature;
    }

    @Override
    public WebsiteServiceFeature getServiceFeature(
            UUID serviceFeatureId
    ) {
        requireIdentifier(
                serviceFeatureId,
                "Service feature ID"
        );

        return websiteServiceFeatureRepository
                .findByServiceFeatureId(serviceFeatureId)
                .orElseThrow(() -> notFound(
                        "Website service feature was not found."
                ));
    }

    @Override
    public List<WebsiteServiceFeature> getServiceFeatures(
            UUID serviceVersionId
    ) {
        requireExistingServiceVersion(serviceVersionId);

        return websiteServiceFeatureRepository
                .findAllByServiceVersion_ServiceVersionIdOrderByDisplayOrderAscCreatedAtAsc(
                        serviceVersionId
                );
    }

    @Override
    public Page<WebsiteServiceFeature> searchServiceFeatures(
            UUID serviceVersionId,
            String keyword,
            Boolean isActive,
            Pageable pageable
    ) {
        requireExistingServiceVersion(serviceVersionId);
        requirePageable(pageable);

        return websiteServiceFeatureRepository
                .searchServiceFeatures(
                        serviceVersionId,
                        normalizeOptional(keyword),
                        isActive,
                        pageable
                );
    }

    @Override
    @Transactional
    public WebsiteServiceFeature updateServiceFeatureStatus(
            UUID serviceFeatureId,
            boolean isActive,
            UUID administratorId
    ) {
        AdminUser administrator =
                getRequiredAdministrator(administratorId);

        WebsiteServiceFeature feature =
                getFeatureForUpdate(serviceFeatureId);

        requireEditableDraft(
                feature.getServiceVersion()
        );

        JsonNode beforeSnapshot =
                createFeatureSnapshot(feature);

        feature.updateStatus(
                isActive,
                administrator
        );

        WebsiteServiceFeature savedFeature =
                websiteServiceFeatureRepository
                        .saveAndFlush(feature);

        recordFeatureAudit(
                administratorId,
                WebsiteContentAuditAction.UPDATE,
                savedFeature,
                beforeSnapshot,
                isActive
                        ? "Website service feature activated."
                        : "Website service feature deactivated."
        );

        return savedFeature;
    }

    @Override
    @Transactional
    public WebsiteServiceFeature updateServiceFeatureOrder(
            UUID serviceFeatureId,
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

        WebsiteServiceFeature feature =
                getFeatureForUpdate(serviceFeatureId);

        requireEditableDraft(
                feature.getServiceVersion()
        );

        JsonNode beforeSnapshot =
                createFeatureSnapshot(feature);

        feature.updateDisplayOrder(
                displayOrder,
                administrator
        );

        WebsiteServiceFeature savedFeature =
                websiteServiceFeatureRepository
                        .saveAndFlush(feature);

        recordFeatureAudit(
                administratorId,
                WebsiteContentAuditAction.UPDATE,
                savedFeature,
                beforeSnapshot,
                "Website service feature display order updated."
        );

        return savedFeature;
    }

    @Override
    @Transactional
    public void deleteServiceFeature(
            UUID serviceFeatureId,
            UUID administratorId
    ) {
        getRequiredAdministrator(administratorId);

        WebsiteServiceFeature feature =
                getFeatureForUpdate(serviceFeatureId);

        requireEditableDraft(
                feature.getServiceVersion()
        );

        JsonNode beforeSnapshot =
                createFeatureSnapshot(feature);

        WebsiteServiceVersion serviceVersion =
                feature.getServiceVersion();

        UUID serviceVersionId =
                serviceVersion.getServiceVersionId();

        String resourceName =
                createServiceVersionResourceName(
                        serviceVersion
                );

        websiteServiceFeatureRepository.delete(feature);
        websiteServiceFeatureRepository.flush();

        websiteContentAuditLogService.recordAudit(
                administratorId,
                WebsiteContentAuditAction.DELETE,
                WebsiteContentAuditResourceType.SERVICE_VERSION,
                serviceVersionId,
                resourceName,
                beforeSnapshot,
                null,
                "Website service feature deleted.",
                null
        );
    }

    @Override
    public List<WebsiteServiceFeature>
    getPublicFeaturesByServiceCode(
            String serviceCode
    ) {
        return websiteServiceFeatureRepository
                .findPublicByServiceCode(
                        normalizeServiceCode(serviceCode),
                        Instant.now()
                );
    }

    @Override
    public List<WebsiteServiceFeature>
    getPublicFeaturesByServiceSlug(
            String serviceSlug
    ) {
        return websiteServiceFeatureRepository
                .findPublicByServiceSlug(
                        normalizeServiceSlug(serviceSlug),
                        Instant.now()
                );
    }

    @Override
    public long countServiceFeatures(
            UUID serviceVersionId
    ) {
        requireExistingServiceVersion(serviceVersionId);

        return websiteServiceFeatureRepository
                .countByServiceVersion_ServiceVersionId(
                        serviceVersionId
                );
    }

    @Override
    public long countActiveServiceFeatures(
            UUID serviceVersionId
    ) {
        requireExistingServiceVersion(serviceVersionId);

        return websiteServiceFeatureRepository
                .countByServiceVersion_ServiceVersionIdAndIsActiveTrue(
                        serviceVersionId
                );
    }

    private void recordFeatureAudit(
            UUID administratorId,
            WebsiteContentAuditAction action,
            WebsiteServiceFeature feature,
            JsonNode beforeSnapshot,
            String changeSummary
    ) {
        WebsiteServiceVersion serviceVersion =
                feature.getServiceVersion();

        websiteContentAuditLogService.recordAudit(
                administratorId,
                action,
                WebsiteContentAuditResourceType.SERVICE_VERSION,
                serviceVersion.getServiceVersionId(),
                createServiceVersionResourceName(
                        serviceVersion
                ),
                beforeSnapshot,
                createFeatureSnapshot(feature),
                changeSummary,
                null
        );
    }

    private JsonNode createFeatureSnapshot(
            WebsiteServiceFeature feature
    ) {
        Map<String, Object> fields =
                new LinkedHashMap<>();

        fields.put(
                "serviceFeatureId",
                feature.getServiceFeatureId()
        );

        fields.put(
                "serviceVersionId",
                feature.getServiceVersion() == null
                        ? null
                        : feature.getServiceVersion()
                        .getServiceVersionId()
        );

        fields.put(
                "serviceId",
                feature.getServiceVersion() == null
                        || feature.getServiceVersion()
                        .getWebsiteService() == null
                        ? null
                        : feature.getServiceVersion()
                        .getWebsiteService()
                        .getServiceId()
        );

        fields.put(
                "versionNumber",
                feature.getServiceVersion() == null
                        ? null
                        : feature.getServiceVersion()
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

    private String createServiceVersionResourceName(
            WebsiteServiceVersion serviceVersion
    ) {
        if (serviceVersion == null) {
            return "Website Service Version";
        }

        WebsiteService websiteService =
                serviceVersion.getWebsiteService();

        String serviceCode =
                websiteService == null
                        ? null
                        : normalizeOptional(
                        websiteService.getServiceCode()
                );

        if (serviceCode == null) {
            serviceCode = "WEBSITE_SERVICE";
        }

        return serviceCode
                + " - Version "
                + serviceVersion.getVersionNumber();
    }

    private WebsiteServiceFeature getFeatureForUpdate(
            UUID serviceFeatureId
    ) {
        requireIdentifier(
                serviceFeatureId,
                "Service feature ID"
        );

        return websiteServiceFeatureRepository
                .findByIdForUpdate(serviceFeatureId)
                .orElseThrow(() -> notFound(
                        "Website service feature was not found."
                ));
    }

    private WebsiteServiceVersion getServiceVersionForUpdate(
            UUID serviceVersionId
    ) {
        requireIdentifier(
                serviceVersionId,
                "Service version ID"
        );

        return websiteServiceVersionRepository
                .findByIdForUpdate(serviceVersionId)
                .orElseThrow(() -> notFound(
                        "Website service version was not found."
                ));
    }

    private void requireExistingServiceVersion(
            UUID serviceVersionId
    ) {
        requireIdentifier(
                serviceVersionId,
                "Service version ID"
        );

        if (
                !websiteServiceVersionRepository
                        .existsById(serviceVersionId)
        ) {
            throw notFound(
                    "Website service version was not found."
            );
        }
    }

    private void requireEditableDraft(
            WebsiteServiceVersion serviceVersion
    ) {
        if (serviceVersion == null) {
            throw notFound(
                    "Website service version was not found."
            );
        }

        if (
                serviceVersion.getVersionStatus()
                        != WebsiteServiceVersionStatus.DRAFT
        ) {
            throw conflict(
                    "Only features belonging to a draft service "
                            + "version may be modified."
            );
        }

        if (
                serviceVersion.getWebsiteService() == null
                        || serviceVersion
                        .getWebsiteService()
                        .isDeleted()
        ) {
            throw conflict(
                    "Features cannot be modified because the owning "
                            + "website service is deleted."
            );
        }
    }

    private void validateEditableFields(
            WebsiteServiceFeature feature
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

    private String normalizeServiceCode(
            String value
    ) {
        String normalized =
                normalizeRequired(
                        value,
                        "Service code"
                )
                        .toUpperCase(Locale.ROOT)
                        .replaceAll("[^A-Z0-9]+", "_")
                        .replaceAll("^_+|_+$", "");

        if (normalized.isBlank()) {
            throw badRequest(
                    "Service code is required."
            );
        }

        if (normalized.length() > 100) {
            throw badRequest(
                    "Service code must not exceed 100 characters."
            );
        }

        return normalized;
    }

    private String normalizeServiceSlug(
            String value
    ) {
        String normalized =
                normalizeRequired(
                        value,
                        "Service slug"
                )
                        .toLowerCase(Locale.ROOT)
                        .replaceAll("[^a-z0-9]+", "-")
                        .replaceAll("^-+|-+$", "");

        if (normalized.isBlank()) {
            throw badRequest(
                    "Service slug is required."
            );
        }

        if (normalized.length() > 180) {
            throw badRequest(
                    "Service slug must not exceed 180 characters."
            );
        }

        return normalized;
    }

    private String normalizeIconKey(
            String value
    ) {
        String normalized = normalizeOptional(value);

        if (normalized == null) {
            return null;
        }

        normalized = normalized
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
        String normalized = normalizeOptional(value);

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
        String normalized = normalizeOptional(value);

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