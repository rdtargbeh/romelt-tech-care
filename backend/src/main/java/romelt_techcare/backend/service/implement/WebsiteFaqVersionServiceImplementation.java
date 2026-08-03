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
import romelt_techcare.backend.entity.WebsiteFaqVersion;
import romelt_techcare.backend.enums.WebsiteContentAuditAction;
import romelt_techcare.backend.enums.WebsiteContentAuditResourceType;
import romelt_techcare.backend.enums.WebsiteFaqStatus;
import romelt_techcare.backend.enums.WebsiteFaqVersionStatus;
import romelt_techcare.backend.repository.AdminUserRepository;
import romelt_techcare.backend.repository.WebsiteFaqRepository;
import romelt_techcare.backend.repository.WebsiteFaqVersionRepository;
import romelt_techcare.backend.service.WebsiteContentAuditLogService;
import romelt_techcare.backend.service.WebsiteContentAuditSnapshotService;
import romelt_techcare.backend.service.WebsiteFaqVersionService;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * ================================================================
 * ROMELT TECHCARE — FAQ VERSION SERVICE IMPLEMENTATION
 * ================================================================
 *
 * Purpose:
 * Implements production lifecycle rules for versioned website FAQ
 * content.
 *
 * Publication transaction:
 * 1. Locks the stable FAQ.
 * 2. Locks the selected draft.
 * 3. Archives the current published version when one exists.
 * 4. Publishes the selected draft.
 * 5. Clears the stable FAQ's draft pointer.
 * 6. Assigns the stable FAQ's published pointer.
 * 7. Records all lifecycle changes in the website content audit log.
 *
 * Mutation rules:
 * - Only DRAFT versions may be edited, published, or deleted.
 * - Published and archived versions remain immutable.
 *
 * Public rules:
 * Public content must:
 * - belong to an active, non-deleted FAQ;
 * - be the stable FAQ's current published version;
 * - have PUBLISHED status;
 * - be marked public.
 *
 * Audit behavior:
 * - Draft creation and editing use SAVE_DRAFT.
 * - Publication uses PUBLISH.
 * - Replaced published versions use ARCHIVE.
 * - Manual archival uses ARCHIVE.
 * - Permanent draft deletion uses DELETE.
 *
 * Audit security:
 * FAQ answers may contain large public content. The audit snapshot
 * stores the answer length and a limited preview rather than copying
 * an unlimited answer into every audit record.
 * ================================================================
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WebsiteFaqVersionServiceImplementation
        implements WebsiteFaqVersionService {

    private static final int AUDIT_ANSWER_PREVIEW_LENGTH = 500;

    private final WebsiteFaqVersionRepository
            websiteFaqVersionRepository;

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
    public WebsiteFaqVersion createDraft(
            UUID faqId,
            WebsiteFaqVersion requestedDraft,
            UUID administratorId
    ) {
        requireIdentifier(
                faqId,
                "FAQ ID"
        );

        if (requestedDraft == null) {
            throw badRequest(
                    "FAQ draft information is required."
            );
        }

        AdminUser administrator =
                getRequiredAdministrator(administratorId);

        WebsiteFaq faq =
                getFaqForUpdate(faqId);

        ensureFaqUsable(faq);
        ensureNoExistingDraft(faqId);

        normalizeAndValidateDraft(requestedDraft);

        int nextVersionNumber =
                websiteFaqVersionRepository
                        .findMaximumVersionNumber(faqId)
                        + 1;

        WebsiteFaqVersion draft =
                WebsiteFaqVersion.builder()
                        .faq(faq)
                        .versionNumber(nextVersionNumber)
                        .versionStatus(
                                WebsiteFaqVersionStatus.DRAFT
                        )
                        .faqCategory(
                                normalizeOptional(
                                        requestedDraft
                                                .getFaqCategory()
                                )
                        )
                        .question(
                                normalizeRequired(
                                        requestedDraft.getQuestion(),
                                        "FAQ question"
                                )
                        )
                        .answer(
                                normalizeRequired(
                                        requestedDraft.getAnswer(),
                                        "FAQ answer"
                                )
                        )
                        .displayOrder(
                                requestedDraft.getDisplayOrder()
                        )
                        .isFeatured(
                                requestedDraft.getIsFeatured()
                        )
                        .isPublic(
                                requestedDraft.getIsPublic()
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

        WebsiteFaqVersion savedDraft =
                saveVersion(
                        draft,
                        "Unable to create the FAQ draft. "
                                + "The FAQ may already have a current draft."
                );

        faq.assignDraftVersion(
                savedDraft.getFaqVersionId(),
                administrator
        );

        websiteFaqRepository.saveAndFlush(faq);

        recordFaqVersionAudit(
                administratorId,
                WebsiteContentAuditAction.SAVE_DRAFT,
                savedDraft,
                null,
                "Created FAQ draft version "
                        + savedDraft.getVersionNumber()
                        + "."
        );

        return getFaqVersion(
                savedDraft.getFaqVersionId()
        );
    }

    @Override
    @Transactional
    public WebsiteFaqVersion createDraftFromPublishedVersion(
            UUID faqId,
            String changeSummary,
            UUID administratorId
    ) {
        requireIdentifier(
                faqId,
                "FAQ ID"
        );

        validateLength(
                changeSummary,
                1000,
                "Change summary",
                false
        );

        AdminUser administrator =
                getRequiredAdministrator(administratorId);

        WebsiteFaq faq =
                getFaqForUpdate(faqId);

        ensureFaqUsable(faq);
        ensureNoExistingDraft(faqId);

        WebsiteFaqVersion publishedVersion =
                websiteFaqVersionRepository
                        .findByFaqAndStatusForUpdate(
                                faqId,
                                WebsiteFaqVersionStatus.PUBLISHED
                        )
                        .orElseThrow(() -> conflict(
                                "The FAQ has no published version to copy."
                        ));

        int nextVersionNumber =
                websiteFaqVersionRepository
                        .findMaximumVersionNumber(faqId)
                        + 1;

        WebsiteFaqVersion draft =
                WebsiteFaqVersion.builder()
                        .faq(faq)
                        .versionNumber(nextVersionNumber)
                        .versionStatus(
                                WebsiteFaqVersionStatus.DRAFT
                        )
                        .faqCategory(
                                publishedVersion.getFaqCategory()
                        )
                        .question(
                                publishedVersion.getQuestion()
                        )
                        .answer(
                                publishedVersion.getAnswer()
                        )
                        .displayOrder(
                                publishedVersion.getDisplayOrder()
                        )
                        .isFeatured(
                                publishedVersion.getIsFeatured()
                        )
                        .isPublic(
                                publishedVersion.getIsPublic()
                        )
                        .changeSummary(
                                normalizeOptional(changeSummary)
                        )
                        .createdByAdminUser(administrator)
                        .updatedByAdminUser(administrator)
                        .build();

        WebsiteFaqVersion savedDraft =
                saveVersion(
                        draft,
                        "Unable to create a draft from the published FAQ version."
                );

        faq.assignDraftVersion(
                savedDraft.getFaqVersionId(),
                administrator
        );

        websiteFaqRepository.saveAndFlush(faq);

        recordFaqVersionAudit(
                administratorId,
                WebsiteContentAuditAction.SAVE_DRAFT,
                savedDraft,
                createFaqVersionSnapshot(publishedVersion),
                "Created FAQ draft version "
                        + savedDraft.getVersionNumber()
                        + " from published version "
                        + publishedVersion.getVersionNumber()
                        + "."
        );

        return getFaqVersion(
                savedDraft.getFaqVersionId()
        );
    }

    @Override
    @Transactional
    public WebsiteFaqVersion updateDraft(
            UUID faqVersionId,
            WebsiteFaqVersion requestedUpdate,
            UUID administratorId
    ) {
        requireIdentifier(
                faqVersionId,
                "FAQ version ID"
        );

        if (requestedUpdate == null) {
            throw badRequest(
                    "Updated FAQ draft information is required."
            );
        }

        AdminUser administrator =
                getRequiredAdministrator(administratorId);

        WebsiteFaqVersion draft =
                getVersionForUpdate(faqVersionId);

        if (!draft.isDraft()) {
            throw conflict(
                    "Only a draft FAQ version may be edited."
            );
        }

        WebsiteFaq faq = draft.getFaq();

        ensureFaqUsable(faq);

        if (
                faq.getDraftVersionId() == null
                        || !faqVersionId.equals(
                        faq.getDraftVersionId()
                )
        ) {
            throw conflict(
                    "The selected version is not the FAQ's current draft."
            );
        }

        JsonNode beforeSnapshot =
                createFaqVersionSnapshot(draft);

        normalizeAndValidateDraft(requestedUpdate);

        draft.updateDraft(
                normalizeOptional(
                        requestedUpdate.getFaqCategory()
                ),
                normalizeRequired(
                        requestedUpdate.getQuestion(),
                        "FAQ question"
                ),
                normalizeRequired(
                        requestedUpdate.getAnswer(),
                        "FAQ answer"
                ),
                requestedUpdate.getDisplayOrder(),
                requestedUpdate.getIsFeatured(),
                requestedUpdate.getIsPublic(),
                normalizeOptional(
                        requestedUpdate.getChangeSummary()
                ),
                administrator
        );

        WebsiteFaqVersion savedDraft =
                saveVersion(
                        draft,
                        "Unable to update the FAQ draft."
                );

        recordFaqVersionAudit(
                administratorId,
                WebsiteContentAuditAction.SAVE_DRAFT,
                savedDraft,
                beforeSnapshot,
                "Updated FAQ draft version "
                        + savedDraft.getVersionNumber()
                        + "."
        );

        return getFaqVersion(
                savedDraft.getFaqVersionId()
        );
    }

    @Override
    public WebsiteFaqVersion getFaqVersion(
            UUID faqVersionId
    ) {
        requireIdentifier(
                faqVersionId,
                "FAQ version ID"
        );

        return websiteFaqVersionRepository
                .findByFaqVersionId(faqVersionId)
                .orElseThrow(() -> notFound(
                        "Website FAQ version was not found."
                ));
    }

    @Override
    public WebsiteFaqVersion getCurrentDraft(
            UUID faqId
    ) {
        requireIdentifier(
                faqId,
                "FAQ ID"
        );

        return websiteFaqVersionRepository
                .findByFaq_FaqIdAndVersionStatus(
                        faqId,
                        WebsiteFaqVersionStatus.DRAFT
                )
                .orElseThrow(() -> notFound(
                        "The FAQ has no current draft."
                ));
    }

    @Override
    public WebsiteFaqVersion getCurrentPublishedVersion(
            UUID faqId
    ) {
        requireIdentifier(
                faqId,
                "FAQ ID"
        );

        return websiteFaqVersionRepository
                .findByFaq_FaqIdAndVersionStatus(
                        faqId,
                        WebsiteFaqVersionStatus.PUBLISHED
                )
                .orElseThrow(() -> notFound(
                        "The FAQ has no published version."
                ));
    }

    @Override
    public Page<WebsiteFaqVersion> getVersionHistory(
            UUID faqId,
            WebsiteFaqVersionStatus versionStatus,
            Pageable pageable
    ) {
        requireIdentifier(
                faqId,
                "FAQ ID"
        );

        requirePageable(pageable);

        if (!websiteFaqRepository.existsById(faqId)) {
            throw notFound(
                    "Website FAQ was not found."
            );
        }

        if (versionStatus == null) {
            return websiteFaqVersionRepository
                    .findAllByFaq_FaqIdOrderByVersionNumberDesc(
                            faqId,
                            pageable
                    );
        }

        return websiteFaqVersionRepository
                .findAllByFaq_FaqIdAndVersionStatusOrderByVersionNumberDesc(
                        faqId,
                        versionStatus,
                        pageable
                );
    }

    @Override
    @Transactional
    public WebsiteFaqVersion publishDraft(
            UUID faqVersionId,
            UUID administratorId
    ) {
        AdminUser administrator =
                getRequiredAdministrator(administratorId);

        WebsiteFaqVersion draft =
                getVersionForUpdate(faqVersionId);

        if (!draft.isDraft()) {
            throw conflict(
                    "Only a draft FAQ version may be published."
            );
        }

        WebsiteFaq faq =
                getFaqForUpdate(
                        draft.getFaq().getFaqId()
                );

        ensureFaqUsable(faq);
        normalizeAndValidateDraft(draft);

        if (
                faq.getDraftVersionId() == null
                        || !faqVersionId.equals(
                        faq.getDraftVersionId()
                )
        ) {
            throw conflict(
                    "The selected version is not the FAQ's current draft."
            );
        }

        JsonNode draftBeforeSnapshot =
                createFaqVersionSnapshot(draft);

        WebsiteFaqVersion currentPublished =
                websiteFaqVersionRepository
                        .findByFaqAndStatusForUpdate(
                                faq.getFaqId(),
                                WebsiteFaqVersionStatus.PUBLISHED
                        )
                        .orElse(null);

        if (currentPublished != null) {
            JsonNode publishedBeforeSnapshot =
                    createFaqVersionSnapshot(
                            currentPublished
                    );

            currentPublished.archive(administrator);

            WebsiteFaqVersion archivedPublished =
                    websiteFaqVersionRepository
                            .saveAndFlush(currentPublished);

            recordFaqVersionAudit(
                    administratorId,
                    WebsiteContentAuditAction.ARCHIVE,
                    archivedPublished,
                    publishedBeforeSnapshot,
                    "Archived FAQ version "
                            + archivedPublished.getVersionNumber()
                            + " because version "
                            + draft.getVersionNumber()
                            + " was published."
            );
        }

        draft.publish(administrator);

        WebsiteFaqVersion publishedDraft =
                websiteFaqVersionRepository
                        .saveAndFlush(draft);

        faq.assignPublishedVersion(
                publishedDraft.getFaqVersionId(),
                administrator
        );

        faq.clearDraftVersion(administrator);

        websiteFaqRepository.saveAndFlush(faq);

        recordFaqVersionAudit(
                administratorId,
                WebsiteContentAuditAction.PUBLISH,
                publishedDraft,
                draftBeforeSnapshot,
                "Published FAQ version "
                        + publishedDraft.getVersionNumber()
                        + "."
        );

        return getFaqVersion(
                publishedDraft.getFaqVersionId()
        );
    }

    @Override
    @Transactional
    public WebsiteFaqVersion archiveVersion(
            UUID faqVersionId,
            UUID administratorId
    ) {
        AdminUser administrator =
                getRequiredAdministrator(administratorId);

        WebsiteFaqVersion version =
                getVersionForUpdate(faqVersionId);

        WebsiteFaq faq =
                getFaqForUpdate(
                        version.getFaq().getFaqId()
                );

        if (version.isArchived()) {
            return getFaqVersion(faqVersionId);
        }

        JsonNode beforeSnapshot =
                createFaqVersionSnapshot(version);

        boolean wasDraft = version.isDraft();
        boolean wasPublished = version.isPublished();

        version.archive(administrator);

        WebsiteFaqVersion archivedVersion =
                websiteFaqVersionRepository
                        .saveAndFlush(version);

        if (
                wasDraft
                        && faqVersionId.equals(
                        faq.getDraftVersionId()
                )
        ) {
            faq.clearDraftVersion(administrator);
        }

        if (
                wasPublished
                        && faqVersionId.equals(
                        faq.getPublishedVersionId()
                )
        ) {
            faq.clearPublishedVersion(administrator);
        }

        websiteFaqRepository.saveAndFlush(faq);

        recordFaqVersionAudit(
                administratorId,
                WebsiteContentAuditAction.ARCHIVE,
                archivedVersion,
                beforeSnapshot,
                "Archived FAQ version "
                        + archivedVersion.getVersionNumber()
                        + "."
        );

        return getFaqVersion(faqVersionId);
    }

    @Override
    @Transactional
    public void deleteDraft(
            UUID faqVersionId,
            UUID administratorId
    ) {
        AdminUser administrator =
                getRequiredAdministrator(administratorId);

        WebsiteFaqVersion draft =
                getVersionForUpdate(faqVersionId);

        if (!draft.isDraft()) {
            throw conflict(
                    "Only a draft FAQ version may be permanently deleted."
            );
        }

        JsonNode beforeSnapshot =
                createFaqVersionSnapshot(draft);

        UUID deletedVersionId =
                draft.getFaqVersionId();

        String resourceName =
                createFaqVersionResourceName(draft);

        int versionNumber =
                draft.getVersionNumber();

        WebsiteFaq faq =
                getFaqForUpdate(
                        draft.getFaq().getFaqId()
                );

        if (
                faqVersionId.equals(
                        faq.getDraftVersionId()
                )
        ) {
            faq.clearDraftVersion(administrator);

            websiteFaqRepository.saveAndFlush(faq);
        }

        websiteFaqVersionRepository.delete(draft);
        websiteFaqVersionRepository.flush();

        websiteContentAuditLogService.recordAudit(
                administratorId,
                WebsiteContentAuditAction.DELETE,
                WebsiteContentAuditResourceType.FAQ_VERSION,
                deletedVersionId,
                resourceName,
                beforeSnapshot,
                null,
                "Permanently deleted FAQ draft version "
                        + versionNumber
                        + ".",
                null
        );
    }

    @Override
    public WebsiteFaqVersion getPublicFaqByKey(
            String faqKey
    ) {
        return websiteFaqVersionRepository
                .findPublicByFaqKey(
                        normalizeFaqKey(faqKey)
                )
                .orElseThrow(() -> notFound(
                        "Public website FAQ was not found."
                ));
    }

    @Override
    public List<WebsiteFaqVersion> getPublicFaqs() {
        return websiteFaqVersionRepository
                .findAllPublicFaqs();
    }

    @Override
    public List<WebsiteFaqVersion> getFeaturedPublicFaqs() {
        return websiteFaqVersionRepository
                .findAllFeaturedPublicFaqs();
    }

    @Override
    public List<WebsiteFaqVersion> getPublicFaqsByCategory(
            String faqCategory
    ) {
        String normalizedCategory =
                normalizeRequired(
                        faqCategory,
                        "FAQ category"
                );

        if (normalizedCategory.length() > 120) {
            throw badRequest(
                    "FAQ category must not exceed 120 characters."
            );
        }

        return websiteFaqVersionRepository
                .findAllPublicFaqsByCategory(
                        normalizedCategory
                );
    }

    @Override
    public List<String> getPublicFaqCategories() {
        return websiteFaqVersionRepository
                .findPublicFaqCategories();
    }

    private void recordFaqVersionAudit(
            UUID administratorId,
            WebsiteContentAuditAction action,
            WebsiteFaqVersion version,
            JsonNode beforeSnapshot,
            String changeSummary
    ) {
        websiteContentAuditLogService.recordAudit(
                administratorId,
                action,
                WebsiteContentAuditResourceType.FAQ_VERSION,
                version.getFaqVersionId(),
                createFaqVersionResourceName(version),
                beforeSnapshot,
                createFaqVersionSnapshot(version),
                changeSummary,
                null
        );
    }

    private JsonNode createFaqVersionSnapshot(
            WebsiteFaqVersion version
    ) {
        Map<String, Object> fields =
                new LinkedHashMap<>();

        fields.put(
                "faqVersionId",
                version.getFaqVersionId()
        );

        fields.put(
                "faqId",
                version.getFaq() == null
                        ? null
                        : version.getFaq().getFaqId()
        );

        fields.put(
                "faqKey",
                version.getFaq() == null
                        ? null
                        : version.getFaq().getFaqKey()
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
                "faqCategory",
                version.getFaqCategory()
        );

        fields.put(
                "question",
                version.getQuestion()
        );

        String answer =
                normalizeOptional(version.getAnswer());

        fields.put(
                "answerPreview",
                truncate(
                        answer,
                        AUDIT_ANSWER_PREVIEW_LENGTH
                )
        );

        fields.put(
                "answerLength",
                answer == null
                        ? 0
                        : answer.length()
        );

        fields.put(
                "displayOrder",
                version.getDisplayOrder()
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

    private String createFaqVersionResourceName(
            WebsiteFaqVersion version
    ) {
        String faqKey =
                version.getFaq() == null
                        ? null
                        : normalizeOptional(
                        version.getFaq().getFaqKey()
                );

        if (faqKey == null) {
            faqKey = "FAQ";
        }

        return faqKey
                + " - Version "
                + version.getVersionNumber();
    }

    private WebsiteFaqVersion getVersionForUpdate(
            UUID faqVersionId
    ) {
        requireIdentifier(
                faqVersionId,
                "FAQ version ID"
        );

        return websiteFaqVersionRepository
                .findByIdForUpdate(faqVersionId)
                .orElseThrow(() -> notFound(
                        "Website FAQ version was not found."
                ));
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

    private void ensureNoExistingDraft(
            UUID faqId
    ) {
        if (
                websiteFaqVersionRepository
                        .existsByFaq_FaqIdAndVersionStatus(
                                faqId,
                                WebsiteFaqVersionStatus.DRAFT
                        )
        ) {
            throw conflict(
                    "The FAQ already has a current draft."
            );
        }
    }

    private void ensureFaqUsable(
            WebsiteFaq faq
    ) {
        if (faq == null || faq.isDeleted()) {
            throw conflict(
                    "A deleted FAQ cannot manage content versions."
            );
        }

        if (faq.getFaqStatus() == WebsiteFaqStatus.ARCHIVED) {
            throw conflict(
                    "An archived FAQ cannot manage new content versions."
            );
        }
    }

    private void normalizeAndValidateDraft(
            WebsiteFaqVersion version
    ) {
        validateLength(
                version.getFaqCategory(),
                120,
                "FAQ category",
                false
        );

        validateLength(
                version.getQuestion(),
                500,
                "FAQ question",
                true
        );

        validateLength(
                version.getAnswer(),
                Integer.MAX_VALUE,
                "FAQ answer",
                true
        );

        validateLength(
                version.getChangeSummary(),
                1000,
                "Change summary",
                false
        );

        if (
                version.getDisplayOrder() == null
                        || version.getDisplayOrder() < 0
        ) {
            throw badRequest(
                    "Display order must not be negative."
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

    private WebsiteFaqVersion saveVersion(
            WebsiteFaqVersion version,
            String conflictMessage
    ) {
        try {
            return websiteFaqVersionRepository
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

    private String truncate(
            String value,
            int maximumLength
    ) {
        if (
                value == null
                        || value.length() <= maximumLength
        ) {
            return value;
        }

        return value.substring(
                0,
                maximumLength
        );
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