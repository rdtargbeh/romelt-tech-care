package romelt_techcare.backend.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import romelt_techcare.backend.entity.WebsiteFaq;
import romelt_techcare.backend.enums.WebsiteFaqStatus;

import java.util.List;
import java.util.UUID;

/**
 * ================================================================
 * ROMELT TECHCARE — WEBSITE FAQ SERVICE
 * ================================================================
 *
 * Purpose:
 * Defines business operations for stable website FAQ identities.
 *
 * Responsibilities:
 * - Creates stable FAQ identities.
 * - Updates FAQ keys and lifecycle status.
 * - Retrieves FAQs by identifier and key.
 * - Searches administrator-facing FAQs.
 * - Activates, deactivates, and archives FAQs.
 * - Soft-deletes and restores FAQs.
 * - Retrieves active public FAQ identities.
 * - Returns status and deletion counts.
 *
 * Version lifecycle:
 * WebsiteFaqVersionService exclusively manages:
 * - draft creation;
 * - draft content updates;
 * - publication;
 * - version archival;
 * - version history;
 * - draftVersionId synchronization;
 * - publishedVersionId synchronization.
 *
 * This service must not expose direct version-pointer operations.
 * ================================================================
 */
public interface WebsiteFaqService {

    WebsiteFaq createFaq(
            WebsiteFaq faq,
            UUID administratorId
    );

    WebsiteFaq updateFaq(
            UUID faqId,
            WebsiteFaq requestedUpdate,
            UUID administratorId
    );

    WebsiteFaq getFaq(
            UUID faqId
    );

    WebsiteFaq getFaqByKey(
            String faqKey
    );

    Page<WebsiteFaq> searchFaqs(
            String keyword,
            WebsiteFaqStatus faqStatus,
            Pageable pageable
    );

    Page<WebsiteFaq> getDeletedFaqs(
            Pageable pageable
    );

    WebsiteFaq updateFaqStatus(
            UUID faqId,
            WebsiteFaqStatus faqStatus,
            UUID administratorId
    );

    WebsiteFaq activateFaq(
            UUID faqId,
            UUID administratorId
    );

    WebsiteFaq deactivateFaq(
            UUID faqId,
            UUID administratorId
    );

    WebsiteFaq archiveFaq(
            UUID faqId,
            UUID administratorId
    );

    void deleteFaq(
            UUID faqId,
            UUID administratorId
    );

    WebsiteFaq restoreFaq(
            UUID faqId,
            UUID administratorId
    );

    WebsiteFaq getPublicFaqByKey(
            String faqKey
    );

    List<WebsiteFaq> getPublicFaqs();

    long countFaqsByStatus(
            WebsiteFaqStatus faqStatus
    );

    long countDeletedFaqs();
}