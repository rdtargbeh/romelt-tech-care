package romelt_techcare.backend.mapper;

import org.springframework.stereotype.Component;
import romelt_techcare.backend.dto.PublicWebsiteServiceResponse;
import romelt_techcare.backend.dto.WebsiteServiceCreateRequest;
import romelt_techcare.backend.dto.WebsiteServiceResponse;
import romelt_techcare.backend.dto.WebsiteServiceUpdateRequest;
import romelt_techcare.backend.entity.AdminUser;
import romelt_techcare.backend.entity.WebsiteService;
import romelt_techcare.backend.enums.WebsiteServiceStatus;

import java.util.UUID;

/**
 * ================================================================
 * ROMELT TECHCARE — WEBSITE SERVICE MAPPER
 * ================================================================
 *
 * Purpose:
 * Converts website-service request DTOs into service-compatible
 * entities and persisted services into administrator and public
 * responses.
 *
 * Responsibilities:
 * - Maps stable service identity fields.
 * - Excludes draft and published pointers from create and update input.
 * - Preserves version pointers in administrator responses.
 * - Prevents direct JPA entity serialization.
 *
 * Version lifecycle:
 * Draft and published pointers are managed exclusively by
 * WebsiteServiceVersionService.
 * ================================================================
 */
@Component
public class WebsiteServiceMapper {

    /**
     * Converts a create request into a new unsaved stable service.
     */
    public WebsiteService toEntity(
            WebsiteServiceCreateRequest request
    ) {
        if (request == null) {
            return null;
        }

        return WebsiteService.builder()
                .serviceCode(request.serviceCode())
                .serviceSlug(request.serviceSlug())
                .serviceStatus(
                        request.serviceStatus() == null
                                ? WebsiteServiceStatus.ACTIVE
                                : request.serviceStatus()
                )
                .build();
    }

    /**
     * Converts a full identity update request into a detached entity.
     */
    public WebsiteService toUpdateEntity(
            WebsiteServiceUpdateRequest request
    ) {
        if (request == null) {
            return null;
        }

        return WebsiteService.builder()
                .serviceCode(request.serviceCode())
                .serviceSlug(request.serviceSlug())
                .serviceStatus(request.serviceStatus())
                .build();
    }

    /**
     * Converts a persisted service into an administrator response.
     */
    public WebsiteServiceResponse toResponse(
            WebsiteService service
    ) {
        if (service == null) {
            return null;
        }

        AdminUser createdBy =
                service.getCreatedByAdminUser();

        AdminUser updatedBy =
                service.getUpdatedByAdminUser();

        AdminUser deletedBy =
                service.getDeletedByAdminUser();

        return new WebsiteServiceResponse(
                service.getServiceId(),
                service.getServiceCode(),
                service.getServiceSlug(),
                service.getDraftVersionId(),
                service.getPublishedVersionId(),
                service.getServiceStatus(),
                service.isDeleted(),
                service.isPubliclyAvailable(),
                getAdminUserId(createdBy),
                getAdminUserDisplayName(createdBy),
                getAdminUserId(updatedBy),
                getAdminUserDisplayName(updatedBy),
                getAdminUserId(deletedBy),
                getAdminUserDisplayName(deletedBy),
                service.getDeletedAt(),
                service.getCreatedAt(),
                service.getUpdatedAt(),
                service.getRowVersion()
        );
    }

    /**
     * Converts an active public stable service into a public identity
     * response.
     *
     * The actual published service content is exposed through the
     * WebsiteServiceVersion public API.
     */
    public PublicWebsiteServiceResponse toPublicResponse(
            WebsiteService service
    ) {
        if (service == null) {
            return null;
        }

        return new PublicWebsiteServiceResponse(
                service.getServiceId(),
                service.getServiceCode(),
                service.getServiceSlug(),
                service.getPublishedVersionId()
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