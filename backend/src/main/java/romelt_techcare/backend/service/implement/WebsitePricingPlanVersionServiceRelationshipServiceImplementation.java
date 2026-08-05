package romelt_techcare.backend.service.implement;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import romelt_techcare.backend.entity.AdminUser;
import romelt_techcare.backend.entity.WebsitePricingPlan;
import romelt_techcare.backend.entity.WebsitePricingPlanVersion;
import romelt_techcare.backend.entity.WebsitePricingPlanVersionService;
import romelt_techcare.backend.entity.WebsitePricingPlanVersionServiceId;
import romelt_techcare.backend.entity.WebsiteService;
import romelt_techcare.backend.enums.WebsiteContentAuditAction;
import romelt_techcare.backend.enums.WebsiteContentAuditResourceType;
import romelt_techcare.backend.enums.WebsitePricingPlanStatus;
import romelt_techcare.backend.enums.WebsitePricingPlanVersionStatus;
import romelt_techcare.backend.enums.WebsiteServiceStatus;
import romelt_techcare.backend.repository.AdminUserRepository;
import romelt_techcare.backend.repository.WebsitePricingPlanVersionRepository;
import romelt_techcare.backend.repository.WebsitePricingPlanVersionServiceRepository;
import romelt_techcare.backend.repository.WebsiteServiceRepository;
import romelt_techcare.backend.service.WebsiteContentAuditLogService;
import romelt_techcare.backend.service.WebsiteContentAuditSnapshotService;
import romelt_techcare.backend.service.WebsitePricingPlanVersionServiceRelationshipService;

import java.time.Instant;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * ================================================================
 * ROMELT TECHCARE — PRICING PLAN VERSION SERVICE RELATIONSHIP
 * SERVICE IMPLEMENTATION
 * ================================================================
 *
 * Purpose:
 * Implements production rules for assigning stable website services
 * to pricing-plan versions.
 *
 * Mutation rules:
 * - Only the current DRAFT pricing-plan version may be modified.
 * - Published and archived pricing-plan memberships are immutable.
 * - Deleted or archived stable services cannot be newly assigned.
 * - Duplicate relationships are rejected for single additions and
 *   ignored during bulk operations.
 *
 * Public rules:
 * Public retrieval includes only relationships where:
 * - the plan is active and not deleted;
 * - the pricing-plan version is current, published, public, and
 *   effective;
 * - the linked service is active and not deleted;
 * - the linked service has a published version.
 *
 * Audit behavior:
 * Relationship mutations are recorded against the owning
 * PRICING_PLAN_VERSION resource.
 *
 * Audit integration:
 * Existing public methods, repository calls, validation rules,
 * entity operations, and return types remain unchanged.
 * ================================================================
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WebsitePricingPlanVersionServiceRelationshipServiceImplementation
        implements WebsitePricingPlanVersionServiceRelationshipService {

    private final WebsitePricingPlanVersionServiceRepository
            relationshipRepository;

    private final WebsitePricingPlanVersionRepository
            pricingPlanVersionRepository;

    private final WebsiteServiceRepository
            websiteServiceRepository;

    private final AdminUserRepository adminUserRepository;

    private final WebsiteContentAuditLogService
            websiteContentAuditLogService;

    private final WebsiteContentAuditSnapshotService
            websiteContentAuditSnapshotService;

    @Override
    @Transactional
    public WebsitePricingPlanVersionService addService(
            UUID pricingPlanVersionId,
            UUID serviceId,
            UUID administratorId
    ) {
        getRequiredAdministrator(administratorId);

        WebsitePricingPlanVersion version =
                getEditablePricingPlanVersion(
                        pricingPlanVersionId
                );

        WebsiteService websiteService =
                getAssignableWebsiteService(serviceId);

        if (
                relationshipRepository
                        .existsByPricingPlanVersion_PricingPlanVersionIdAndWebsiteService_ServiceId(
                                pricingPlanVersionId,
                                serviceId
                        )
        ) {
            throw conflict(
                    "The website service is already assigned to this "
                            + "pricing-plan version."
            );
        }

        JsonNode beforeSnapshot =
                createRelationshipCollectionSnapshot(
                        version,
                        getCurrentRelationships(
                                pricingPlanVersionId
                        )
                );

        WebsitePricingPlanVersionService relationship =
                buildRelationship(
                        version,
                        websiteService
                );

        WebsitePricingPlanVersionService savedRelationship =
                saveRelationship(relationship);

        List<WebsitePricingPlanVersionService>
                updatedRelationships =
                getCurrentRelationships(
                        pricingPlanVersionId
                );

        recordRelationshipAudit(
                administratorId,
                WebsiteContentAuditAction.CREATE,
                version,
                beforeSnapshot,
                createRelationshipCollectionSnapshot(
                        version,
                        updatedRelationships
                ),
                "Website service "
                        + websiteService.getServiceCode()
                        + " assigned to pricing-plan version."
        );

        return savedRelationship;
    }

    @Override
    @Transactional
    public List<WebsitePricingPlanVersionService> addServices(
            UUID pricingPlanVersionId,
            Collection<UUID> serviceIds,
            UUID administratorId
    ) {
        getRequiredAdministrator(administratorId);

        WebsitePricingPlanVersion version =
                getEditablePricingPlanVersion(
                        pricingPlanVersionId
                );

        JsonNode beforeSnapshot =
                createRelationshipCollectionSnapshot(
                        version,
                        getCurrentRelationships(
                                pricingPlanVersionId
                        )
                );

        Set<UUID> normalizedServiceIds =
                normalizeServiceIds(serviceIds, false);

        List<WebsiteService> services =
                getAssignableWebsiteServices(
                        normalizedServiceIds
                );

        Set<UUID> existingServiceIds =
                relationshipRepository
                        .findAllByPricingPlanVersion_PricingPlanVersionIdAndWebsiteService_ServiceIdIn(
                                pricingPlanVersionId,
                                normalizedServiceIds
                        )
                        .stream()
                        .map(relationship ->
                                relationship
                                        .getWebsiteService()
                                        .getServiceId()
                        )
                        .collect(
                                java.util.stream.Collectors.toSet()
                        );

        List<WebsitePricingPlanVersionService>
                newRelationships =
                services.stream()
                        .filter(service ->
                                !existingServiceIds.contains(
                                        service.getServiceId()
                                )
                        )
                        .map(service ->
                                buildRelationship(
                                        version,
                                        service
                                )
                        )
                        .toList();

        if (!newRelationships.isEmpty()) {
            relationshipRepository.saveAll(
                    newRelationships
            );

            relationshipRepository.flush();
        }

        List<WebsitePricingPlanVersionService>
                updatedRelationships =
                relationshipRepository
                        .findAllByPricingPlanVersion_PricingPlanVersionIdOrderByWebsiteService_ServiceCodeAsc(
                                pricingPlanVersionId
                        );

        if (!newRelationships.isEmpty()) {
            recordRelationshipAudit(
                    administratorId,
                    WebsiteContentAuditAction.UPDATE,
                    version,
                    beforeSnapshot,
                    createRelationshipCollectionSnapshot(
                            version,
                            updatedRelationships
                    ),
                    "Added "
                            + newRelationships.size()
                            + " website service relationship(s) to "
                            + "the pricing-plan version."
            );
        }

        return updatedRelationships;
    }

    @Override
    @Transactional
    public List<WebsitePricingPlanVersionService> replaceServices(
            UUID pricingPlanVersionId,
            Collection<UUID> serviceIds,
            UUID administratorId
    ) {
        getRequiredAdministrator(administratorId);

        WebsitePricingPlanVersion version =
                getEditablePricingPlanVersion(
                        pricingPlanVersionId
                );

        JsonNode beforeSnapshot =
                createRelationshipCollectionSnapshot(
                        version,
                        getCurrentRelationships(
                                pricingPlanVersionId
                        )
                );

        Set<UUID> normalizedServiceIds =
                normalizeServiceIds(serviceIds, true);

        if (normalizedServiceIds.isEmpty()) {
            relationshipRepository
                    .deleteAllByPricingPlanVersion_PricingPlanVersionId(
                            pricingPlanVersionId
                    );

            relationshipRepository.flush();

            recordRelationshipAudit(
                    administratorId,
                    WebsiteContentAuditAction.UPDATE,
                    version,
                    beforeSnapshot,
                    createRelationshipCollectionSnapshot(
                            version,
                            List.of()
                    ),
                    "Cleared all website service relationships from "
                            + "the pricing-plan version."
            );

            return List.of();
        }

        List<WebsiteService> services =
                getAssignableWebsiteServices(
                        normalizedServiceIds
                );

        relationshipRepository
                .deleteAllByPricingPlanVersion_PricingPlanVersionIdAndWebsiteService_ServiceIdNotIn(
                        pricingPlanVersionId,
                        normalizedServiceIds
                );

        Set<UUID> existingServiceIds =
                relationshipRepository
                        .findAllByPricingPlanVersion_PricingPlanVersionIdAndWebsiteService_ServiceIdIn(
                                pricingPlanVersionId,
                                normalizedServiceIds
                        )
                        .stream()
                        .map(relationship ->
                                relationship
                                        .getWebsiteService()
                                        .getServiceId()
                        )
                        .collect(
                                java.util.stream.Collectors.toSet()
                        );

        List<WebsitePricingPlanVersionService>
                missingRelationships =
                services.stream()
                        .filter(service ->
                                !existingServiceIds.contains(
                                        service.getServiceId()
                                )
                        )
                        .map(service ->
                                buildRelationship(
                                        version,
                                        service
                                )
                        )
                        .toList();

        if (!missingRelationships.isEmpty()) {
            relationshipRepository.saveAll(
                    missingRelationships
            );
        }

        relationshipRepository.flush();

        List<WebsitePricingPlanVersionService>
                updatedRelationships =
                relationshipRepository
                        .findAllByPricingPlanVersion_PricingPlanVersionIdOrderByWebsiteService_ServiceCodeAsc(
                                pricingPlanVersionId
                        );

        recordRelationshipAudit(
                administratorId,
                WebsiteContentAuditAction.UPDATE,
                version,
                beforeSnapshot,
                createRelationshipCollectionSnapshot(
                        version,
                        updatedRelationships
                ),
                "Replaced website service relationships for the "
                        + "pricing-plan version."
        );

        return updatedRelationships;
    }

    @Override
    @Transactional
    public void removeService(
            UUID pricingPlanVersionId,
            UUID serviceId,
            UUID administratorId
    ) {
        getRequiredAdministrator(administratorId);

        WebsitePricingPlanVersion version =
                getEditablePricingPlanVersion(
                        pricingPlanVersionId
                );

        JsonNode beforeSnapshot =
                createRelationshipCollectionSnapshot(
                        version,
                        getCurrentRelationships(
                                pricingPlanVersionId
                        )
                );

        WebsitePricingPlanVersionService relationship =
                relationshipRepository
                        .findByPricingPlanVersionAndServiceForUpdate(
                                pricingPlanVersionId,
                                requireIdentifier(
                                        serviceId,
                                        "Website service ID"
                                )
                        )
                        .orElseThrow(() -> notFound(
                                "The website service is not assigned to "
                                        + "this pricing-plan version."
                        ));

        String serviceCode =
                relationship.getWebsiteService()
                        .getServiceCode();

        relationshipRepository.delete(relationship);
        relationshipRepository.flush();

        recordRelationshipAudit(
                administratorId,
                WebsiteContentAuditAction.DELETE,
                version,
                beforeSnapshot,
                createRelationshipCollectionSnapshot(
                        version,
                        getCurrentRelationships(
                                pricingPlanVersionId
                        )
                ),
                "Website service "
                        + serviceCode
                        + " removed from pricing-plan version."
        );
    }

    @Override
    @Transactional
    public void clearServices(
            UUID pricingPlanVersionId,
            UUID administratorId
    ) {
        getRequiredAdministrator(administratorId);

        WebsitePricingPlanVersion version =
                getEditablePricingPlanVersion(
                        pricingPlanVersionId
                );

        List<WebsitePricingPlanVersionService>
                existingRelationships =
                getCurrentRelationships(
                        pricingPlanVersionId
                );

        if (existingRelationships.isEmpty()) {
            return;
        }

        JsonNode beforeSnapshot =
                createRelationshipCollectionSnapshot(
                        version,
                        existingRelationships
                );

        relationshipRepository
                .deleteAllByPricingPlanVersion_PricingPlanVersionId(
                        pricingPlanVersionId
                );

        relationshipRepository.flush();

        recordRelationshipAudit(
                administratorId,
                WebsiteContentAuditAction.DELETE,
                version,
                beforeSnapshot,
                createRelationshipCollectionSnapshot(
                        version,
                        List.of()
                ),
                "All website service relationships cleared from the "
                        + "pricing-plan version."
        );
    }

    @Override
    public WebsitePricingPlanVersionService getRelationship(
            UUID pricingPlanVersionId,
            UUID serviceId
    ) {
        requireIdentifier(
                pricingPlanVersionId,
                "Pricing plan version ID"
        );

        requireIdentifier(
                serviceId,
                "Website service ID"
        );

        return relationshipRepository
                .findByPricingPlanVersion_PricingPlanVersionIdAndWebsiteService_ServiceId(
                        pricingPlanVersionId,
                        serviceId
                )
                .orElseThrow(() -> notFound(
                        "Pricing-plan version service relationship "
                                + "was not found."
                ));
    }

    @Override
    public List<WebsitePricingPlanVersionService>
    getServicesForPricingPlanVersion(
            UUID pricingPlanVersionId
    ) {
        requireExistingPricingPlanVersion(
                pricingPlanVersionId
        );

        return relationshipRepository
                .findAllByPricingPlanVersion_PricingPlanVersionIdOrderByWebsiteService_ServiceCodeAsc(
                        pricingPlanVersionId
                );
    }

    @Override
    public List<WebsitePricingPlanVersionService>
    getPricingPlanVersionsForService(
            UUID serviceId
    ) {
        requireExistingWebsiteService(serviceId);

        return relationshipRepository
                .findAllByWebsiteService_ServiceIdOrderByCreatedAtDesc(
                        serviceId
                );
    }

    @Override
    public List<WebsitePricingPlanVersionService>
    getPublicServicesByPlanCode(
            String planCode
    ) {
        return relationshipRepository
                .findPublicServicesByPlanCode(
                        normalizePlanCode(planCode),
                        Instant.now()
                );
    }

    @Override
    public List<WebsitePricingPlanVersionService>
    getPublicServicesByPlanSlug(
            String planSlug
    ) {
        return relationshipRepository
                .findPublicServicesByPlanSlug(
                        normalizePlanSlug(planSlug),
                        Instant.now()
                );
    }

    @Override
    public long countServicesForPricingPlanVersion(
            UUID pricingPlanVersionId
    ) {
        requireExistingPricingPlanVersion(
                pricingPlanVersionId
        );

        return relationshipRepository
                .countByPricingPlanVersion_PricingPlanVersionId(
                        pricingPlanVersionId
                );
    }

    private void recordRelationshipAudit(
            UUID administratorId,
            WebsiteContentAuditAction action,
            WebsitePricingPlanVersion version,
            JsonNode beforeSnapshot,
            JsonNode afterSnapshot,
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
                afterSnapshot,
                changeSummary,
                null
        );
    }

    private JsonNode createRelationshipCollectionSnapshot(
            WebsitePricingPlanVersion version,
            List<WebsitePricingPlanVersionService> relationships
    ) {
        Map<String, Object> fields =
                new LinkedHashMap<>();

        fields.put(
                "pricingPlanVersionId",
                version == null
                        ? null
                        : version.getPricingPlanVersionId()
        );

        fields.put(
                "pricingPlanId",
                version == null
                        || version.getPricingPlan() == null
                        ? null
                        : version.getPricingPlan()
                        .getPricingPlanId()
        );

        fields.put(
                "planCode",
                version == null
                        || version.getPricingPlan() == null
                        ? null
                        : version.getPricingPlan()
                        .getPlanCode()
        );

        fields.put(
                "versionNumber",
                version == null
                        ? null
                        : version.getVersionNumber()
        );

        fields.put(
                "versionStatus",
                version == null
                        ? null
                        : version.getVersionStatus()
        );

        List<Map<String, Object>> services =
                relationships == null
                        ? List.of()
                        : relationships.stream()
                        .map(this::createServiceRelationshipMap)
                        .toList();

        fields.put(
                "serviceCount",
                services.size()
        );

        fields.put(
                "services",
                services
        );

        return websiteContentAuditSnapshotService
                .createSnapshot(fields);
    }

    private Map<String, Object> createServiceRelationshipMap(
            WebsitePricingPlanVersionService relationship
    ) {
        Map<String, Object> serviceFields =
                new LinkedHashMap<>();

        WebsiteService service =
                relationship == null
                        ? null
                        : relationship.getWebsiteService();

        serviceFields.put(
                "serviceId",
                service == null
                        ? null
                        : service.getServiceId()
        );

        serviceFields.put(
                "serviceCode",
                service == null
                        ? null
                        : service.getServiceCode()
        );

        serviceFields.put(
                "serviceSlug",
                service == null
                        ? null
                        : service.getServiceSlug()
        );

        serviceFields.put(
                "serviceStatus",
                service == null
                        ? null
                        : service.getServiceStatus()
        );

        return serviceFields;
    }

    private String createPricingPlanVersionResourceName(
            WebsitePricingPlanVersion version
    ) {
        if (version == null) {
            return "Pricing Plan Version";
        }

        WebsitePricingPlan pricingPlan =
                version.getPricingPlan();

        String planCode =
                pricingPlan == null
                        ? null
                        : pricingPlan.getPlanCode();

        if (
                planCode == null
                        || planCode.trim().isEmpty()
        ) {
            planCode = "PRICING_PLAN";
        }

        return planCode
                + " - Version "
                + version.getVersionNumber();
    }

    private List<WebsitePricingPlanVersionService>
    getCurrentRelationships(
            UUID pricingPlanVersionId
    ) {
        return relationshipRepository
                .findAllByPricingPlanVersion_PricingPlanVersionIdOrderByWebsiteService_ServiceCodeAsc(
                        pricingPlanVersionId
                );
    }

    private WebsitePricingPlanVersion
    getEditablePricingPlanVersion(
            UUID pricingPlanVersionId
    ) {
        requireIdentifier(
                pricingPlanVersionId,
                "Pricing plan version ID"
        );

        WebsitePricingPlanVersion version =
                pricingPlanVersionRepository
                        .findByIdForUpdate(
                                pricingPlanVersionId
                        )
                        .orElseThrow(() -> notFound(
                                "Website pricing-plan version was not found."
                        ));

        if (
                version.getVersionStatus()
                        != WebsitePricingPlanVersionStatus.DRAFT
        ) {
            throw conflict(
                    "Services may be changed only on a draft "
                            + "pricing-plan version."
            );
        }

        WebsitePricingPlan pricingPlan =
                version.getPricingPlan();

        if (
                pricingPlan == null
                        || pricingPlan.isDeleted()
        ) {
            throw conflict(
                    "Services cannot be changed because the owning "
                            + "pricing plan is deleted."
            );
        }

        if (
                pricingPlan.getPlanStatus()
                        == WebsitePricingPlanStatus.ARCHIVED
        ) {
            throw conflict(
                    "Services cannot be changed because the owning "
                            + "pricing plan is archived."
            );
        }

        if (
                pricingPlan.getDraftVersionId() == null
                        || !pricingPlanVersionId.equals(
                        pricingPlan.getDraftVersionId()
                )
        ) {
            throw conflict(
                    "The selected version is not the pricing plan's "
                            + "current draft."
            );
        }

        return version;
    }

    private WebsiteService getAssignableWebsiteService(
            UUID serviceId
    ) {
        requireIdentifier(
                serviceId,
                "Website service ID"
        );

        WebsiteService service =
                websiteServiceRepository
                        .findByServiceIdAndDeletedAtIsNull(
                                serviceId
                        )
                        .orElseThrow(() -> notFound(
                                "Website service was not found."
                        ));

        validateAssignableWebsiteService(service);

        return service;
    }

    private List<WebsiteService>
    getAssignableWebsiteServices(
            Set<UUID> serviceIds
    ) {
        List<WebsiteService> services =
                websiteServiceRepository.findAllById(
                        serviceIds
                );

        if (services.size() != serviceIds.size()) {
            Set<UUID> foundIds =
                    services.stream()
                            .map(WebsiteService::getServiceId)
                            .collect(
                                    java.util.stream.Collectors.toSet()
                            );

            Set<UUID> missingIds =
                    new LinkedHashSet<>(serviceIds);

            missingIds.removeAll(foundIds);

            throw notFound(
                    "One or more website services were not found: "
                            + missingIds
            );
        }

        services.forEach(
                this::validateAssignableWebsiteService
        );

        return services;
    }

    private void validateAssignableWebsiteService(
            WebsiteService service
    ) {
        if (service == null || service.isDeleted()) {
            throw conflict(
                    "A deleted website service cannot be assigned "
                            + "to a pricing plan."
            );
        }

        if (
                service.getServiceStatus()
                        == WebsiteServiceStatus.ARCHIVED
        ) {
            throw conflict(
                    "An archived website service cannot be assigned "
                            + "to a pricing plan."
            );
        }
    }

    private WebsitePricingPlanVersionService buildRelationship(
            WebsitePricingPlanVersion version,
            WebsiteService service
    ) {
        return WebsitePricingPlanVersionService.builder()
                .id(
                        new WebsitePricingPlanVersionServiceId(
                                version.getPricingPlanVersionId(),
                                service.getServiceId()
                        )
                )
                .pricingPlanVersion(version)
                .websiteService(service)
                .build();
    }

    private WebsitePricingPlanVersionService saveRelationship(
            WebsitePricingPlanVersionService relationship
    ) {
        try {
            return relationshipRepository.saveAndFlush(
                    relationship
            );
        } catch (DataIntegrityViolationException exception) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Unable to assign the website service to the "
                            + "pricing-plan version.",
                    exception
            );
        }
    }

    private Set<UUID> normalizeServiceIds(
            Collection<UUID> serviceIds,
            boolean allowEmpty
    ) {
        if (serviceIds == null) {
            throw badRequest(
                    "Website service IDs are required."
            );
        }

        LinkedHashSet<UUID> normalizedIds =
                new LinkedHashSet<>();

        for (UUID serviceId : serviceIds) {
            if (serviceId == null) {
                throw badRequest(
                        "Website service IDs must not contain null."
                );
            }

            normalizedIds.add(serviceId);
        }

        if (!allowEmpty && normalizedIds.isEmpty()) {
            throw badRequest(
                    "At least one website service ID is required."
            );
        }

        return normalizedIds;
    }

    private void requireExistingPricingPlanVersion(
            UUID pricingPlanVersionId
    ) {
        requireIdentifier(
                pricingPlanVersionId,
                "Pricing plan version ID"
        );

        if (
                !pricingPlanVersionRepository.existsById(
                        pricingPlanVersionId
                )
        ) {
            throw notFound(
                    "Website pricing-plan version was not found."
            );
        }
    }

    private void requireExistingWebsiteService(
            UUID serviceId
    ) {
        requireIdentifier(
                serviceId,
                "Website service ID"
        );

        if (!websiteServiceRepository.existsById(serviceId)) {
            throw notFound(
                    "Website service was not found."
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

    private UUID requireIdentifier(
            UUID identifier,
            String fieldName
    ) {
        if (identifier == null) {
            throw badRequest(
                    fieldName + " is required."
            );
        }

        return identifier;
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

    private String normalizeRequired(
            String value,
            String fieldName
    ) {
        if (value == null || value.trim().isEmpty()) {
            throw badRequest(
                    fieldName + " is required."
            );
        }

        return value.trim();
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