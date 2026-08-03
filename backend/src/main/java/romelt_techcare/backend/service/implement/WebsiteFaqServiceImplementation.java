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
import romelt_techcare.backend.entity.WebsiteFaq;
import romelt_techcare.backend.enums.WebsiteContentAuditAction;
import romelt_techcare.backend.enums.WebsiteContentAuditResourceType;
import romelt_techcare.backend.enums.WebsiteFaqStatus;
import romelt_techcare.backend.repository.AdminUserRepository;
import romelt_techcare.backend.repository.WebsiteFaqRepository;
import romelt_techcare.backend.service.WebsiteContentAuditLogService;
import romelt_techcare.backend.service.WebsiteContentAuditSnapshotService;
import romelt_techcare.backend.service.WebsiteFaqService;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * ================================================================
 * ROMELT TECHCARE — WEBSITE FAQ SERVICE IMPLEMENTATION
 * ================================================================
 *
 * Purpose:
 * Implements production business rules for stable website FAQ
 * identities.
 *
 * Responsibilities:
 * - Creates and updates stable FAQ records.
 * - Normalizes FAQ keys.
 * - Preserves FAQ-key uniqueness.
 * - Supports administrator filtering and pagination.
 * - Supports activation, deactivation, and archival.
 * - Supports soft deletion and restoration.
 * - Retrieves active public FAQ identities.
 * - Records administrator attribution.
 * - Records immutable content audit events for every mutation.
 *
 * Version lifecycle:
 * FAQ content versions and draft/published pointer changes are not
 * exposed by this service. They are controlled by
 * WebsiteFaqVersionService.
 *
 * Audit behavior:
 * - Identity creation uses CREATE.
 * - Identity updates and active/inactive transitions use UPDATE.
 * - Archival uses ARCHIVE.
 * - Soft deletion uses DELETE.
 * - Restoration uses RESTORE.
 * ================================================================
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WebsiteFaqServiceImplementation
        implements WebsiteFaqService {

    private final WebsiteFaqRepository
            websiteFaqRepository;

    private final AdminUserRepository
            adminUserRepository;

    private final WebsiteContentAuditLogService
            websiteContentAuditLogService;

    private final WebsiteContentAuditSnapshotService
            websiteContentAuditSnapshotService;

    @Override
    @Transactional
    public WebsiteFaq createFaq(
            WebsiteFaq faq,
            UUID administratorId
    ) {
        if (faq == null) {
            throw badRequest(
                    "Website FAQ information is required."
            );
        }

        AdminUser administrator =
                getRequiredAdministrator(administratorId);

        validateEditableFields(faq);

        String faqKey =
                normalizeFaqKey(faq.getFaqKey());

        validateFaqKeyAvailability(
                faqKey,
                null
        );

        WebsiteFaqStatus initialStatus =
                faq.getFaqStatus() == null
                        ? WebsiteFaqStatus.ACTIVE
                        : faq.getFaqStatus();

        WebsiteFaq faqToCreate =
                WebsiteFaq.builder()
                        .faqKey(faqKey)
                        .faqStatus(initialStatus)
                        .createdByAdminUser(administrator)
                        .updatedByAdminUser(administrator)
                        .build();

        WebsiteFaq savedFaq =
                saveFaq(
                        faqToCreate,
                        "Unable to create the website FAQ because its key "
                                + "is already in use."
                );

        recordFaqAudit(
                administratorId,
                WebsiteContentAuditAction.CREATE,
                savedFaq,
                null,
                "Website FAQ identity created."
        );

        return savedFaq;
    }

    @Override
    @Transactional
    public WebsiteFaq updateFaq(
            UUID faqId,
            WebsiteFaq requestedUpdate,
            UUID administratorId
    ) {
        requireIdentifier(
                faqId,
                "FAQ ID"
        );

        if (requestedUpdate == null) {
            throw badRequest(
                    "Updated website FAQ information is required."
            );
        }

        AdminUser administrator =
                getRequiredAdministrator(administratorId);

        WebsiteFaq existingFaq =
                getFaqForUpdate(faqId);

        JsonNode beforeSnapshot =
                createFaqSnapshot(existingFaq);

        validateEditableFields(requestedUpdate);

        String faqKey =
                normalizeFaqKey(
                        requestedUpdate.getFaqKey()
                );

        validateFaqKeyAvailability(
                faqKey,
                faqId
        );

        existingFaq.updateIdentity(
                faqKey,
                requestedUpdate.getFaqStatus(),
                administrator
        );

        WebsiteFaq savedFaq =
                saveFaq(
                        existingFaq,
                        "Unable to update the website FAQ because its key "
                                + "is already in use."
                );

        recordFaqAudit(
                administratorId,
                WebsiteContentAuditAction.UPDATE,
                savedFaq,
                beforeSnapshot,
                "Website FAQ identity updated."
        );

        return savedFaq;
    }

    @Override
    public WebsiteFaq getFaq(
            UUID faqId
    ) {
        requireIdentifier(
                faqId,
                "FAQ ID"
        );

        return websiteFaqRepository
                .findByFaqIdAndDeletedAtIsNull(faqId)
                .orElseThrow(() -> notFound(
                        "Website FAQ was not found."
                ));
    }

    @Override
    public WebsiteFaq getFaqByKey(
            String faqKey
    ) {
        return websiteFaqRepository
                .findByFaqKeyAndDeletedAtIsNull(
                        normalizeFaqKey(faqKey)
                )
                .orElseThrow(() -> notFound(
                        "Website FAQ was not found."
                ));
    }

    @Override
    public Page<WebsiteFaq> searchFaqs(
            String keyword,
            WebsiteFaqStatus faqStatus,
            Pageable pageable
    ) {
        requirePageable(pageable);

        return websiteFaqRepository.searchFaqs(
                normalizeOptional(keyword),
                faqStatus,
                pageable
        );
    }

    @Override
    public Page<WebsiteFaq> getDeletedFaqs(
            Pageable pageable
    ) {
        requirePageable(pageable);

        return websiteFaqRepository
                .findAllByDeletedAtIsNotNullOrderByDeletedAtDesc(
                        pageable
                );
    }

    @Override
    @Transactional
    public WebsiteFaq updateFaqStatus(
            UUID faqId,
            WebsiteFaqStatus faqStatus,
            UUID administratorId
    ) {
        if (faqStatus == null) {
            throw badRequest(
                    "FAQ status is required."
            );
        }

        AdminUser administrator =
                getRequiredAdministrator(administratorId);

        WebsiteFaq faq =
                getFaqForUpdate(faqId);

        JsonNode beforeSnapshot =
                createFaqSnapshot(faq);

        WebsiteFaqStatus previousStatus =
                faq.getFaqStatus();

        switch (faqStatus) {
            case ACTIVE ->
                    faq.activate(administrator);

            case INACTIVE ->
                    faq.deactivate(administrator);

            case ARCHIVED ->
                    faq.archive(administrator);
        }

        WebsiteFaq savedFaq =
                websiteFaqRepository.saveAndFlush(faq);

        WebsiteContentAuditAction auditAction =
                faqStatus == WebsiteFaqStatus.ARCHIVED
                        ? WebsiteContentAuditAction.ARCHIVE
                        : WebsiteContentAuditAction.UPDATE;

        recordFaqAudit(
                administratorId,
                auditAction,
                savedFaq,
                beforeSnapshot,
                "Website FAQ status changed from "
                        + previousStatus
                        + " to "
                        + savedFaq.getFaqStatus()
                        + "."
        );

        return savedFaq;
    }

    @Override
    @Transactional
    public WebsiteFaq activateFaq(
            UUID faqId,
            UUID administratorId
    ) {
        return updateFaqStatus(
                faqId,
                WebsiteFaqStatus.ACTIVE,
                administratorId
        );
    }

    @Override
    @Transactional
    public WebsiteFaq deactivateFaq(
            UUID faqId,
            UUID administratorId
    ) {
        return updateFaqStatus(
                faqId,
                WebsiteFaqStatus.INACTIVE,
                administratorId
        );
    }

    @Override
    @Transactional
    public WebsiteFaq archiveFaq(
            UUID faqId,
            UUID administratorId
    ) {
        return updateFaqStatus(
                faqId,
                WebsiteFaqStatus.ARCHIVED,
                administratorId
        );
    }

    @Override
    @Transactional
    public void deleteFaq(
            UUID faqId,
            UUID administratorId
    ) {
        AdminUser administrator =
                getRequiredAdministrator(administratorId);

        WebsiteFaq faq =
                getFaqForUpdate(faqId);

        JsonNode beforeSnapshot =
                createFaqSnapshot(faq);

        faq.softDelete(administrator);

        WebsiteFaq deletedFaq =
                websiteFaqRepository.saveAndFlush(faq);

        recordFaqAudit(
                administratorId,
                WebsiteContentAuditAction.DELETE,
                deletedFaq,
                beforeSnapshot,
                "Website FAQ soft deleted."
        );
    }

    @Override
    @Transactional
    public WebsiteFaq restoreFaq(
            UUID faqId,
            UUID administratorId
    ) {
        AdminUser administrator =
                getRequiredAdministrator(administratorId);

        WebsiteFaq faq =
                getFaqIncludingDeletedForUpdate(faqId);

        if (!faq.isDeleted()) {
            throw conflict(
                    "The website FAQ is not deleted."
            );
        }

        JsonNode beforeSnapshot =
                createFaqSnapshot(faq);

        faq.restore(administrator);

        /*
         * Restored FAQs remain inactive until an administrator
         * explicitly activates them.
         */
        WebsiteFaq restoredFaq =
                websiteFaqRepository.saveAndFlush(faq);

        recordFaqAudit(
                administratorId,
                WebsiteContentAuditAction.RESTORE,
                restoredFaq,
                beforeSnapshot,
                "Website FAQ restored. The restored FAQ remains inactive."
        );

        return restoredFaq;
    }

    @Override
    public WebsiteFaq getPublicFaqByKey(
            String faqKey
    ) {
        return websiteFaqRepository
                .findByFaqKeyAndFaqStatusAndDeletedAtIsNullAndPublishedVersionIdIsNotNull(
                        normalizeFaqKey(faqKey),
                        WebsiteFaqStatus.ACTIVE
                )
                .orElseThrow(() -> notFound(
                        "Public website FAQ was not found."
                ));
    }

    @Override
    public List<WebsiteFaq> getPublicFaqs() {
        return websiteFaqRepository
                .findAllByFaqStatusAndDeletedAtIsNullAndPublishedVersionIdIsNotNullOrderByFaqKeyAsc(
                        WebsiteFaqStatus.ACTIVE
                );
    }

    @Override
    public long countFaqsByStatus(
            WebsiteFaqStatus faqStatus
    ) {
        if (faqStatus == null) {
            throw badRequest(
                    "FAQ status is required."
            );
        }

        return websiteFaqRepository
                .countByFaqStatusAndDeletedAtIsNull(
                        faqStatus
                );
    }

    @Override
    public long countDeletedFaqs() {
        return websiteFaqRepository
                .countByDeletedAtIsNotNull();
    }

    private void recordFaqAudit(
            UUID administratorId,
            WebsiteContentAuditAction action,
            WebsiteFaq faq,
            JsonNode beforeSnapshot,
            String changeSummary
    ) {
        websiteContentAuditLogService.recordAudit(
                administratorId,
                action,
                WebsiteContentAuditResourceType.FAQ,
                faq.getFaqId(),
                faq.getFaqKey(),
                beforeSnapshot,
                createFaqSnapshot(faq),
                changeSummary,
                null
        );
    }

    private JsonNode createFaqSnapshot(
            WebsiteFaq faq
    ) {
        Map<String, Object> fields =
                new LinkedHashMap<>();

        fields.put(
                "faqId",
                faq.getFaqId()
        );

        fields.put(
                "faqKey",
                faq.getFaqKey()
        );

        fields.put(
                "faqStatus",
                faq.getFaqStatus()
        );

        fields.put(
                "draftVersionId",
                faq.getDraftVersionId()
        );

        fields.put(
                "publishedVersionId",
                faq.getPublishedVersionId()
        );

        fields.put(
                "deleted",
                faq.isDeleted()
        );

        fields.put(
                "deletedAt",
                faq.getDeletedAt()
        );

        fields.put(
                "createdAt",
                faq.getCreatedAt()
        );

        fields.put(
                "updatedAt",
                faq.getUpdatedAt()
        );

        return websiteContentAuditSnapshotService
                .createSnapshot(fields);
    }

    private WebsiteFaq getFaqForUpdate(
            UUID faqId
    ) {
        requireIdentifier(
                faqId,
                "FAQ ID"
        );

        return websiteFaqRepository
                .findByIdForUpdate(faqId)
                .orElseThrow(() -> notFound(
                        "Website FAQ was not found."
                ));
    }

    private WebsiteFaq getFaqIncludingDeletedForUpdate(
            UUID faqId
    ) {
        requireIdentifier(
                faqId,
                "FAQ ID"
        );

        return websiteFaqRepository
                .findIncludingDeletedByIdForUpdate(faqId)
                .orElseThrow(() -> notFound(
                        "Website FAQ was not found."
                ));
    }

    private void validateEditableFields(
            WebsiteFaq faq
    ) {
        validateRequiredLength(
                faq.getFaqKey(),
                120,
                "FAQ key"
        );

        if (faq.getFaqStatus() == null) {
            throw badRequest(
                    "FAQ status is required."
            );
        }
    }

    private void validateFaqKeyAvailability(
            String faqKey,
            UUID currentFaqId
    ) {
        boolean faqKeyExists;

        if (currentFaqId == null) {
            faqKeyExists =
                    websiteFaqRepository
                            .existsByFaqKey(faqKey);
        } else {
            faqKeyExists =
                    websiteFaqRepository
                            .existsByFaqKeyAndFaqIdNot(
                                    faqKey,
                                    currentFaqId
                            );
        }

        if (faqKeyExists) {
            throw conflict(
                    "A website FAQ already uses this FAQ key."
            );
        }
    }

    private String normalizeFaqKey(
            String value
    ) {
        String normalized =
                normalizeRequired(
                        value,
                        "FAQ key"
                )
                        .toUpperCase(Locale.ROOT)
                        .replaceAll("[^A-Z0-9]+", "_")
                        .replaceAll("^_+|_+$", "");

        if (normalized.isBlank()) {
            throw badRequest(
                    "FAQ key is required."
            );
        }

        if (normalized.length() > 120) {
            throw badRequest(
                    "FAQ key must not exceed 120 characters."
            );
        }

        return normalized;
    }

    private WebsiteFaq saveFaq(
            WebsiteFaq faq,
            String conflictMessage
    ) {
        try {
            return websiteFaqRepository.saveAndFlush(faq);
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