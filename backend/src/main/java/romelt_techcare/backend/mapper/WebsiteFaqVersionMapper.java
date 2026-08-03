package romelt_techcare.backend.mapper;

import org.springframework.stereotype.Component;
import romelt_techcare.backend.dto.PublicWebsiteFaqVersionResponse;
import romelt_techcare.backend.dto.WebsiteFaqVersionCreateRequest;
import romelt_techcare.backend.dto.WebsiteFaqVersionResponse;
import romelt_techcare.backend.dto.WebsiteFaqVersionUpdateRequest;
import romelt_techcare.backend.entity.AdminUser;
import romelt_techcare.backend.entity.WebsiteFaq;
import romelt_techcare.backend.entity.WebsiteFaqVersion;

import java.util.UUID;

/**
 * ================================================================
 * ROMELT TECHCARE — FAQ VERSION MAPPER
 * ================================================================
 *
 * Purpose:
 * Converts FAQ version requests into service-compatible entities and
 * persisted versions into administrator and public responses.
 *
 * Responsibilities:
 * - Applies safe defaults for draft creation.
 * - Excludes lifecycle and ownership fields from request mapping.
 * - Prevents direct JPA entity serialization.
 * - Maps administrator attribution safely.
 * ================================================================
 */
@Component
public class WebsiteFaqVersionMapper {

    public WebsiteFaqVersion toEntity(
            WebsiteFaqVersionCreateRequest request
    ) {
        if (request == null) {
            return null;
        }

        return WebsiteFaqVersion.builder()
                .faqCategory(request.faqCategory())
                .question(request.question())
                .answer(request.answer())
                .displayOrder(
                        request.displayOrder() == null
                                ? 0
                                : request.displayOrder()
                )
                .isFeatured(
                        Boolean.TRUE.equals(
                                request.isFeatured()
                        )
                )
                .isPublic(
                        request.isPublic() == null
                                || request.isPublic()
                )
                .changeSummary(request.changeSummary())
                .build();
    }

    public WebsiteFaqVersion toUpdateEntity(
            WebsiteFaqVersionUpdateRequest request
    ) {
        if (request == null) {
            return null;
        }

        return WebsiteFaqVersion.builder()
                .faqCategory(request.faqCategory())
                .question(request.question())
                .answer(request.answer())
                .displayOrder(request.displayOrder())
                .isFeatured(request.isFeatured())
                .isPublic(request.isPublic())
                .changeSummary(request.changeSummary())
                .build();
    }

    public WebsiteFaqVersionResponse toResponse(
            WebsiteFaqVersion version
    ) {
        if (version == null) {
            return null;
        }

        WebsiteFaq faq = version.getFaq();

        AdminUser createdBy =
                version.getCreatedByAdminUser();

        AdminUser updatedBy =
                version.getUpdatedByAdminUser();

        AdminUser publishedBy =
                version.getPublishedByAdminUser();

        AdminUser archivedBy =
                version.getArchivedByAdminUser();

        return new WebsiteFaqVersionResponse(
                version.getFaqVersionId(),
                faq == null
                        ? null
                        : faq.getFaqId(),
                faq == null
                        ? null
                        : faq.getFaqKey(),
                faq == null
                        ? null
                        : faq.getFaqStatus(),
                version.getVersionNumber(),
                version.getVersionStatus(),
                version.getFaqCategory(),
                version.getQuestion(),
                version.getAnswer(),
                version.getDisplayOrder(),
                version.getIsFeatured(),
                version.getIsPublic(),
                version.isPubliclyAvailable(),
                version.getChangeSummary(),
                version.getPublishedAt(),
                version.getArchivedAt(),
                getAdminUserId(createdBy),
                getAdminUserDisplayName(createdBy),
                getAdminUserId(updatedBy),
                getAdminUserDisplayName(updatedBy),
                getAdminUserId(publishedBy),
                getAdminUserDisplayName(publishedBy),
                getAdminUserId(archivedBy),
                getAdminUserDisplayName(archivedBy),
                version.getCreatedAt(),
                version.getUpdatedAt(),
                version.getRowVersion()
        );
    }

    public PublicWebsiteFaqVersionResponse toPublicResponse(
            WebsiteFaqVersion version
    ) {
        if (version == null) {
            return null;
        }

        WebsiteFaq faq = version.getFaq();

        return new PublicWebsiteFaqVersionResponse(
                faq == null
                        ? null
                        : faq.getFaqId(),
                faq == null
                        ? null
                        : faq.getFaqKey(),
                version.getFaqVersionId(),
                version.getVersionNumber(),
                version.getFaqCategory(),
                version.getQuestion(),
                version.getAnswer(),
                version.getDisplayOrder(),
                version.getIsFeatured(),
                version.getPublishedAt()
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