package romelt_techcare.backend.mapper;

import org.springframework.stereotype.Component;
import romelt_techcare.backend.dto.PublicWebsiteFaqResponse;
import romelt_techcare.backend.dto.WebsiteFaqCreateRequest;
import romelt_techcare.backend.dto.WebsiteFaqResponse;
import romelt_techcare.backend.dto.WebsiteFaqUpdateRequest;
import romelt_techcare.backend.entity.AdminUser;
import romelt_techcare.backend.entity.WebsiteFaq;
import romelt_techcare.backend.enums.WebsiteFaqStatus;

import java.util.UUID;

/**
 * ================================================================
 * ROMELT TECHCARE — WEBSITE FAQ MAPPER
 * ================================================================
 *
 * Purpose:
 * Converts FAQ request DTOs into service-compatible entities and
 * persisted FAQs into administrator and public responses.
 *
 * Version lifecycle:
 * Draft and published pointers are intentionally excluded from create
 * and update request mappings.
 * ================================================================
 */
@Component
public class WebsiteFaqMapper {

    public WebsiteFaq toEntity(
            WebsiteFaqCreateRequest request
    ) {
        if (request == null) {
            return null;
        }

        return WebsiteFaq.builder()
                .faqKey(request.faqKey())
                .faqStatus(
                        request.faqStatus() == null
                                ? WebsiteFaqStatus.ACTIVE
                                : request.faqStatus()
                )
                .build();
    }

    public WebsiteFaq toUpdateEntity(
            WebsiteFaqUpdateRequest request
    ) {
        if (request == null) {
            return null;
        }

        return WebsiteFaq.builder()
                .faqKey(request.faqKey())
                .faqStatus(request.faqStatus())
                .build();
    }

    public WebsiteFaqResponse toResponse(
            WebsiteFaq faq
    ) {
        if (faq == null) {
            return null;
        }

        AdminUser createdBy =
                faq.getCreatedByAdminUser();

        AdminUser updatedBy =
                faq.getUpdatedByAdminUser();

        AdminUser deletedBy =
                faq.getDeletedByAdminUser();

        return new WebsiteFaqResponse(
                faq.getFaqId(),
                faq.getFaqKey(),
                faq.getDraftVersionId(),
                faq.getPublishedVersionId(),
                faq.getFaqStatus(),
                faq.isDeleted(),
                faq.isPubliclyAvailable(),
                getAdminUserId(createdBy),
                getAdminUserDisplayName(createdBy),
                getAdminUserId(updatedBy),
                getAdminUserDisplayName(updatedBy),
                getAdminUserId(deletedBy),
                getAdminUserDisplayName(deletedBy),
                faq.getDeletedAt(),
                faq.getCreatedAt(),
                faq.getUpdatedAt(),
                faq.getRowVersion()
        );
    }

    public PublicWebsiteFaqResponse toPublicResponse(
            WebsiteFaq faq
    ) {
        if (faq == null) {
            return null;
        }

        return new PublicWebsiteFaqResponse(
                faq.getFaqId(),
                faq.getFaqKey(),
                faq.getPublishedVersionId()
        );
    }

    private UUID getAdminUserId(
            AdminUser administrator
    ) {
        return administrator == null
                ? null
                : administrator.getAdminUserId();
    }

    private String getAdminUserDisplayName(
            AdminUser administrator
    ) {
        if (administrator == null) {
            return null;
        }

        String firstName =
                normalizeOptional(
                        administrator.getFirstName()
                );

        String lastName =
                normalizeOptional(
                        administrator.getLastName()
                );

        if (firstName != null && lastName != null) {
            return firstName + " " + lastName;
        }

        if (firstName != null) {
            return firstName;
        }

        if (lastName != null) {
            return lastName;
        }

        return normalizeOptional(
                administrator.getEmail()
        );
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
}