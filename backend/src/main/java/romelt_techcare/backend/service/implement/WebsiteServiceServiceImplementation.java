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
import romelt_techcare.backend.entity.WebsiteService;
import romelt_techcare.backend.enums.WebsiteContentAuditAction;
import romelt_techcare.backend.enums.WebsiteContentAuditResourceType;
import romelt_techcare.backend.enums.WebsiteServiceStatus;
import romelt_techcare.backend.repository.AdminUserRepository;
import romelt_techcare.backend.repository.WebsiteServiceRepository;
import romelt_techcare.backend.service.WebsiteContentAuditLogService;
import romelt_techcare.backend.service.WebsiteContentAuditSnapshotService;
import romelt_techcare.backend.service.WebsiteServiceService;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * ================================================================
 * ROMELT TECHCARE — WEBSITE SERVICE SERVICE IMPLEMENTATION
 * ================================================================
 *
 * Purpose:
 * Implements production business rules for stable website-service
 * identities.
 *
 * Responsibilities:
 * - Creates stable service records.
 * - Updates service codes, public slugs, and lifecycle status.
 * - Preserves service-code and service-slug uniqueness.
 * - Retrieves administrator and public service identities.
 * - Supports administrator search and pagination.
 * - Supports activation, deactivation, and archival.
 * - Supports soft deletion and restoration.
 * - Records administrator attribution.
 * - Records immutable content audit entries for service mutations.
 *
 * Version lifecycle:
 * WebsiteServiceVersionServiceImplementation exclusively manages:
 * - draft creation;
 * - draft content changes;
 * - publication;
 * - version archival;
 * - version history;
 * - draftVersionId synchronization;
 * - publishedVersionId synchronization.
 *
 * This implementation intentionally contains no public methods for
 * assigning or clearing version pointers.
 * ================================================================
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WebsiteServiceServiceImplementation
        implements WebsiteServiceService {

    private final WebsiteServiceRepository
            websiteServiceRepository;

    private final AdminUserRepository adminUserRepository;

    private final WebsiteContentAuditLogService
            websiteContentAuditLogService;

    private final WebsiteContentAuditSnapshotService
            websiteContentAuditSnapshotService;

    @Override
    @Transactional
    public WebsiteService createWebsiteService(
            WebsiteService websiteService,
            UUID administratorId
    ) {
        if (websiteService == null) {
            throw badRequest(
                    "Website service information is required."
            );
        }

        AdminUser administrator =
                getRequiredAdministrator(administratorId);

        validateEditableFields(websiteService);

        String serviceCode =
                normalizeServiceCode(
                        websiteService.getServiceCode()
                );

        String serviceSlug =
                normalizeServiceSlug(
                        websiteService.getServiceSlug()
                );

        validateIdentityAvailability(
                serviceCode,
                serviceSlug,
                null
        );

        WebsiteServiceStatus initialStatus =
                websiteService.getServiceStatus() == null
                        ? WebsiteServiceStatus.ACTIVE
                        : websiteService.getServiceStatus();

        WebsiteService serviceToCreate =
                WebsiteService.builder()
                        .serviceCode(serviceCode)
                        .serviceSlug(serviceSlug)
                        .serviceStatus(initialStatus)
                        .createdByAdminUser(administrator)
                        .updatedByAdminUser(administrator)
                        .build();

        WebsiteService savedService =
                saveWebsiteService(
                        serviceToCreate,
                        "Unable to create the website service because its "
                                + "code or slug is already in use."
                );

        recordServiceAudit(
                administratorId,
                WebsiteContentAuditAction.CREATE,
                savedService,
                null,
                "Website service created."
        );

        return savedService;
    }

    @Override
    @Transactional
    public WebsiteService updateWebsiteService(
            UUID serviceId,
            WebsiteService requestedUpdate,
            UUID administratorId
    ) {
        requireIdentifier(
                serviceId,
                "Service ID"
        );

        if (requestedUpdate == null) {
            throw badRequest(
                    "Updated website service information is required."
            );
        }

        AdminUser administrator =
                getRequiredAdministrator(administratorId);

        WebsiteService existingService =
                getWebsiteServiceForUpdate(serviceId);

        JsonNode beforeSnapshot =
                createServiceSnapshot(existingService);

        validateEditableFields(requestedUpdate);

        String serviceCode =
                normalizeServiceCode(
                        requestedUpdate.getServiceCode()
                );

        String serviceSlug =
                normalizeServiceSlug(
                        requestedUpdate.getServiceSlug()
                );

        validateIdentityAvailability(
                serviceCode,
                serviceSlug,
                serviceId
        );

        existingService.updateIdentity(
                serviceCode,
                serviceSlug,
                requestedUpdate.getServiceStatus(),
                administrator
        );

        WebsiteService savedService =
                saveWebsiteService(
                        existingService,
                        "Unable to update the website service because its "
                                + "code or slug is already in use."
                );

        recordServiceAudit(
                administratorId,
                WebsiteContentAuditAction.UPDATE,
                savedService,
                beforeSnapshot,
                "Website service identity updated."
        );

        return savedService;
    }

    @Override
    public WebsiteService getWebsiteService(
            UUID serviceId
    ) {
        requireIdentifier(
                serviceId,
                "Service ID"
        );

        return websiteServiceRepository
                .findByServiceIdAndDeletedAtIsNull(
                        serviceId
                )
                .orElseThrow(() -> notFound(
                        "Website service was not found."
                ));
    }

    @Override
    public WebsiteService getWebsiteServiceByCode(
            String serviceCode
    ) {
        String normalizedCode =
                normalizeServiceCode(serviceCode);

        return websiteServiceRepository
                .findByServiceCodeAndDeletedAtIsNull(
                        normalizedCode
                )
                .orElseThrow(() -> notFound(
                        "Website service was not found."
                ));
    }

    @Override
    public WebsiteService getWebsiteServiceBySlug(
            String serviceSlug
    ) {
        String normalizedSlug =
                normalizeServiceSlug(serviceSlug);

        return websiteServiceRepository
                .findByServiceSlugAndDeletedAtIsNull(
                        normalizedSlug
                )
                .orElseThrow(() -> notFound(
                        "Website service was not found."
                ));
    }

    @Override
    public Page<WebsiteService> searchWebsiteServices(
            String keyword,
            WebsiteServiceStatus serviceStatus,
            Pageable pageable
    ) {
        requirePageable(pageable);

        return websiteServiceRepository
                .searchWebsiteServices(
                        normalizeOptional(keyword),
                        serviceStatus,
                        pageable
                );
    }

    @Override
    public Page<WebsiteService> getDeletedWebsiteServices(
            Pageable pageable
    ) {
        requirePageable(pageable);

        return websiteServiceRepository
                .findAllByDeletedAtIsNotNullOrderByDeletedAtDesc(
                        pageable
                );
    }

    @Override
    @Transactional
    public WebsiteService updateWebsiteServiceStatus(
            UUID serviceId,
            WebsiteServiceStatus serviceStatus,
            UUID administratorId
    ) {
        if (serviceStatus == null) {
            throw badRequest(
                    "Service status is required."
            );
        }

        AdminUser administrator =
                getRequiredAdministrator(administratorId);

        WebsiteService service =
                getWebsiteServiceForUpdate(serviceId);

        JsonNode beforeSnapshot =
                createServiceSnapshot(service);

        WebsiteServiceStatus previousStatus =
                service.getServiceStatus();

        switch (serviceStatus) {
            case ACTIVE ->
                    service.activate(administrator);

            case INACTIVE ->
                    service.deactivate(administrator);

            case ARCHIVED ->
                    service.archive(administrator);
        }

        WebsiteService savedService =
                websiteServiceRepository.saveAndFlush(service);

        WebsiteContentAuditAction auditAction =
                serviceStatus == WebsiteServiceStatus.ARCHIVED
                        ? WebsiteContentAuditAction.ARCHIVE
                        : WebsiteContentAuditAction.UPDATE;

        recordServiceAudit(
                administratorId,
                auditAction,
                savedService,
                beforeSnapshot,
                "Website service status changed from "
                        + previousStatus
                        + " to "
                        + savedService.getServiceStatus()
                        + "."
        );

        return savedService;
    }

    @Override
    @Transactional
    public WebsiteService activateWebsiteService(
            UUID serviceId,
            UUID administratorId
    ) {
        return updateWebsiteServiceStatus(
                serviceId,
                WebsiteServiceStatus.ACTIVE,
                administratorId
        );
    }

    @Override
    @Transactional
    public WebsiteService deactivateWebsiteService(
            UUID serviceId,
            UUID administratorId
    ) {
        return updateWebsiteServiceStatus(
                serviceId,
                WebsiteServiceStatus.INACTIVE,
                administratorId
        );
    }

    @Override
    @Transactional
    public WebsiteService archiveWebsiteService(
            UUID serviceId,
            UUID administratorId
    ) {
        return updateWebsiteServiceStatus(
                serviceId,
                WebsiteServiceStatus.ARCHIVED,
                administratorId
        );
    }

    @Override
    @Transactional
    public void deleteWebsiteService(
            UUID serviceId,
            UUID administratorId
    ) {
        AdminUser administrator =
                getRequiredAdministrator(administratorId);

        WebsiteService service =
                getWebsiteServiceForUpdate(serviceId);

        JsonNode beforeSnapshot =
                createServiceSnapshot(service);

        service.softDelete(administrator);

        WebsiteService deletedService =
                websiteServiceRepository.saveAndFlush(service);

        recordServiceAudit(
                administratorId,
                WebsiteContentAuditAction.DELETE,
                deletedService,
                beforeSnapshot,
                "Website service soft deleted."
        );
    }

    @Override
    @Transactional
    public WebsiteService restoreWebsiteService(
            UUID serviceId,
            UUID administratorId
    ) {
        AdminUser administrator =
                getRequiredAdministrator(administratorId);

        WebsiteService service =
                getWebsiteServiceIncludingDeletedForUpdate(
                        serviceId
                );

        if (!service.isDeleted()) {
            throw conflict(
                    "The website service is not deleted."
            );
        }

        JsonNode beforeSnapshot =
                createServiceSnapshot(service);

        service.restore(administrator);

        /*
         * Restored services remain inactive until an administrator
         * explicitly activates them.
         */
        WebsiteService restoredService =
                websiteServiceRepository.saveAndFlush(service);

        recordServiceAudit(
                administratorId,
                WebsiteContentAuditAction.RESTORE,
                restoredService,
                beforeSnapshot,
                "Website service restored. "
                        + "The restored service remains inactive."
        );

        return restoredService;
    }

    @Override
    public WebsiteService getPublicWebsiteServiceByCode(
            String serviceCode
    ) {
        String normalizedCode =
                normalizeServiceCode(serviceCode);

        return websiteServiceRepository
                .findByServiceCodeAndServiceStatusAndDeletedAtIsNullAndPublishedVersionIdIsNotNull(
                        normalizedCode,
                        WebsiteServiceStatus.ACTIVE
                )
                .orElseThrow(() -> notFound(
                        "Public website service was not found."
                ));
    }

    @Override
    public WebsiteService getPublicWebsiteServiceBySlug(
            String serviceSlug
    ) {
        String normalizedSlug =
                normalizeServiceSlug(serviceSlug);

        return websiteServiceRepository
                .findByServiceSlugAndServiceStatusAndDeletedAtIsNullAndPublishedVersionIdIsNotNull(
                        normalizedSlug,
                        WebsiteServiceStatus.ACTIVE
                )
                .orElseThrow(() -> notFound(
                        "Public website service was not found."
                ));
    }

    @Override
    public List<WebsiteService> getPublicWebsiteServices() {
        return websiteServiceRepository
                .findAllByServiceStatusAndDeletedAtIsNullAndPublishedVersionIdIsNotNullOrderByServiceCodeAsc(
                        WebsiteServiceStatus.ACTIVE
                );
    }

    @Override
    public long countWebsiteServicesByStatus(
            WebsiteServiceStatus serviceStatus
    ) {
        if (serviceStatus == null) {
            throw badRequest(
                    "Service status is required."
            );
        }

        return websiteServiceRepository
                .countByServiceStatusAndDeletedAtIsNull(
                        serviceStatus
                );
    }

    @Override
    public long countDeletedWebsiteServices() {
        return websiteServiceRepository
                .countByDeletedAtIsNotNull();
    }

    private void recordServiceAudit(
            UUID administratorId,
            WebsiteContentAuditAction action,
            WebsiteService service,
            JsonNode beforeSnapshot,
            String changeSummary
    ) {
        websiteContentAuditLogService.recordAudit(
                administratorId,
                action,
                WebsiteContentAuditResourceType.SERVICE,
                service.getServiceId(),
                service.getServiceCode(),
                beforeSnapshot,
                createServiceSnapshot(service),
                changeSummary,
                null
        );
    }

    private JsonNode createServiceSnapshot(
            WebsiteService service
    ) {
        Map<String, Object> fields =
                new LinkedHashMap<>();

        fields.put(
                "serviceId",
                service.getServiceId()
        );

        fields.put(
                "serviceCode",
                service.getServiceCode()
        );

        fields.put(
                "serviceSlug",
                service.getServiceSlug()
        );

        fields.put(
                "draftVersionId",
                service.getDraftVersionId()
        );

        fields.put(
                "publishedVersionId",
                service.getPublishedVersionId()
        );

        fields.put(
                "serviceStatus",
                service.getServiceStatus()
        );

        fields.put(
                "deleted",
                service.isDeleted()
        );

        fields.put(
                "deletedAt",
                service.getDeletedAt()
        );

        fields.put(
                "createdAt",
                service.getCreatedAt()
        );

        fields.put(
                "updatedAt",
                service.getUpdatedAt()
        );

        return websiteContentAuditSnapshotService
                .createSnapshot(fields);
    }

    /**
     * Retrieves and locks one active, non-deleted service for writing.
     */
    private WebsiteService getWebsiteServiceForUpdate(
            UUID serviceId
    ) {
        requireIdentifier(
                serviceId,
                "Service ID"
        );

        return websiteServiceRepository
                .findByIdForUpdate(serviceId)
                .orElseThrow(() -> notFound(
                        "Website service was not found."
                ));
    }

    /**
     * Retrieves and locks a service regardless of deletion status.
     */
    private WebsiteService
    getWebsiteServiceIncludingDeletedForUpdate(
            UUID serviceId
    ) {
        requireIdentifier(
                serviceId,
                "Service ID"
        );

        return websiteServiceRepository
                .findIncludingDeletedByIdForUpdate(
                        serviceId
                )
                .orElseThrow(() -> notFound(
                        "Website service was not found."
                ));
    }

    private void validateEditableFields(
            WebsiteService service
    ) {
        validateRequiredLength(
                service.getServiceCode(),
                100,
                "Service code"
        );

        validateRequiredLength(
                service.getServiceSlug(),
                180,
                "Service slug"
        );

        if (service.getServiceStatus() == null) {
            throw badRequest(
                    "Service status is required."
            );
        }
    }

    private void validateIdentityAvailability(
            String serviceCode,
            String serviceSlug,
            UUID currentServiceId
    ) {
        boolean codeExists;
        boolean slugExists;

        if (currentServiceId == null) {
            codeExists =
                    websiteServiceRepository
                            .existsByServiceCode(serviceCode);

            slugExists =
                    websiteServiceRepository
                            .existsByServiceSlug(serviceSlug);
        } else {
            codeExists =
                    websiteServiceRepository
                            .existsByServiceCodeAndServiceIdNot(
                                    serviceCode,
                                    currentServiceId
                            );

            slugExists =
                    websiteServiceRepository
                            .existsByServiceSlugAndServiceIdNot(
                                    serviceSlug,
                                    currentServiceId
                            );
        }

        if (codeExists) {
            throw conflict(
                    "A website service already uses this service code."
            );
        }

        if (slugExists) {
            throw conflict(
                    "A website service already uses this service slug."
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

    private WebsiteService saveWebsiteService(
            WebsiteService service,
            String conflictMessage
    ) {
        try {
            return websiteServiceRepository
                    .saveAndFlush(service);
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