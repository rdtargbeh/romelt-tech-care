package romelt_techcare.backend.service.implement;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import romelt_techcare.backend.dto.AdminContactInquiryResponse;
import romelt_techcare.backend.dto.AdminContactInquiryStatusUpdateRequest;
import romelt_techcare.backend.dto.AdminJwtPrincipal;
import romelt_techcare.backend.entity.AdminUser;
import romelt_techcare.backend.entity.ContactInquiry;
import romelt_techcare.backend.enums.ContactInquiryStatus;
import romelt_techcare.backend.enums.WebsiteContentAuditAction;
import romelt_techcare.backend.enums.WebsiteContentAuditResourceType;
import romelt_techcare.backend.exception.AdminAuthenticationException;
import romelt_techcare.backend.exception.PublicRequestRejectedException;
import romelt_techcare.backend.repository.AdminUserRepository;
import romelt_techcare.backend.repository.ContactInquiryRepository;
import romelt_techcare.backend.service.AdminContactInquiryService;
import romelt_techcare.backend.service.WebsiteContentAuditLogService;
import romelt_techcare.backend.service.WebsiteContentAuditSnapshotService;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * ================================================================
 * ROMELT TECHCARE — ADMIN CONTACT INQUIRY SERVICE IMPLEMENTATION
 * ================================================================
 *
 * Purpose:
 * Implements administrator operations for customer contact inquiries.
 *
 * Responsibilities:
 * - Retrieves contact inquiry list and detail records.
 * - Updates inquiry lifecycle status.
 * - Verifies the authenticated administrator before write operations.
 * - Records immutable audit events for inquiry status changes.
 *
 * Inquiry lifecycle:
 * NEW → IN_PROGRESS → RESPONDED → CLOSED
 *
 * Alternative status:
 * SPAM
 * ================================================================
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminContactInquiryServiceImpl
        implements AdminContactInquiryService {

    private final ContactInquiryRepository
            contactInquiryRepository;

    private final AdminUserRepository
            adminUserRepository;

    private final WebsiteContentAuditLogService
            websiteContentAuditLogService;

    private final WebsiteContentAuditSnapshotService
            websiteContentAuditSnapshotService;

    @Override
    @Transactional(readOnly = true)
    public Page<AdminContactInquiryResponse> getContactInquiries(
            Pageable pageable
    ) {
        if (pageable == null) {
            throw new IllegalArgumentException(
                    "Contact inquiry pagination information is required."
            );
        }

        return contactInquiryRepository
                .findAll(pageable)
                .map(AdminContactInquiryResponse::from);
    }

    @Override
    @Transactional(readOnly = true)
    public AdminContactInquiryResponse getContactInquiry(
            UUID contactInquiryId
    ) {
        ContactInquiry contactInquiry =
                findContactInquiry(contactInquiryId);

        return AdminContactInquiryResponse.from(
                contactInquiry
        );
    }

    @Override
    @Transactional
    public AdminContactInquiryResponse updateContactInquiryStatus(
            AdminJwtPrincipal principal,
            UUID contactInquiryId,
            AdminContactInquiryStatusUpdateRequest request
    ) {
        requirePrincipal(principal);
        requireStatusUpdateRequest(request);

        AdminUser administrator =
                findAuthenticatedAdministrator(principal);

        ContactInquiry contactInquiry =
                findContactInquiry(contactInquiryId);

        JsonNode beforeSnapshot =
                createInquirySnapshot(contactInquiry);

        ContactInquiryStatus previousStatus =
                contactInquiry.getStatus();

        contactInquiry.setStatus(
                request.status()
        );

        ContactInquiry savedInquiry =
                contactInquiryRepository.saveAndFlush(
                        contactInquiry
                );

        JsonNode afterSnapshot =
                createInquirySnapshot(savedInquiry);

        websiteContentAuditLogService.recordAudit(
                administrator.getAdminUserId(),
                WebsiteContentAuditAction.UPDATE,
                WebsiteContentAuditResourceType.CONTACT_INQUIRY,
                savedInquiry.getContactInquiryId(),
                savedInquiry.getReferenceNumber(),
                beforeSnapshot,
                afterSnapshot,
                "Contact inquiry status changed from "
                        + previousStatus
                        + " to "
                        + savedInquiry.getStatus()
                        + ".",
                null
        );

        log.info(
                "Contact inquiry status updated. contactInquiryId={}, referenceNumber={}, previousStatus={}, newStatus={}, updatedByAdminUserId={}",
                savedInquiry.getContactInquiryId(),
                savedInquiry.getReferenceNumber(),
                previousStatus,
                savedInquiry.getStatus(),
                administrator.getAdminUserId()
        );

        return AdminContactInquiryResponse.from(
                savedInquiry
        );
    }

    private JsonNode createInquirySnapshot(
            ContactInquiry contactInquiry
    ) {
        Map<String, Object> fields =
                new LinkedHashMap<>();

        fields.put(
                "contactInquiryId",
                contactInquiry.getContactInquiryId()
        );

        fields.put(
                "referenceNumber",
                contactInquiry.getReferenceNumber()
        );

        fields.put(
                "status",
                contactInquiry.getStatus()
        );

        return websiteContentAuditSnapshotService
                .createSnapshot(fields);
    }

    private ContactInquiry findContactInquiry(
            UUID contactInquiryId
    ) {
        if (contactInquiryId == null) {
            throw new PublicRequestRejectedException(
                    HttpStatus.BAD_REQUEST,
                    "Contact inquiry ID is required."
            );
        }

        return contactInquiryRepository
                .findById(contactInquiryId)
                .orElseThrow(() ->
                        new PublicRequestRejectedException(
                                HttpStatus.NOT_FOUND,
                                "Contact inquiry was not found."
                        )
                );
    }

    private AdminUser findAuthenticatedAdministrator(
            AdminJwtPrincipal principal
    ) {
        AdminUser administrator =
                adminUserRepository
                        .findById(
                                principal.adminUserId()
                        )
                        .orElseThrow(
                                AdminAuthenticationException
                                        ::accountNotFound
                        );

        if (
                administrator.getAdminUserId() == null
                        || !administrator.getAdminUserId()
                        .equals(principal.adminUserId())
        ) {
            throw AdminAuthenticationException
                    .staleAuthentication();
        }

        if (
                administrator.getEmail() == null
                        || principal.email() == null
                        || !administrator.getEmail()
                        .trim()
                        .equalsIgnoreCase(
                                principal.email().trim()
                        )
        ) {
            throw AdminAuthenticationException
                    .staleAuthentication();
        }

        if (
                administrator.getRole() == null
                        || principal.role() == null
                        || administrator.getRole()
                        != principal.role()
        ) {
            throw AdminAuthenticationException
                    .staleAuthentication();
        }

        if (!administrator.isActive()) {
            throw AdminAuthenticationException
                    .inactiveAccount();
        }

        return administrator;
    }

    private void requireStatusUpdateRequest(
            AdminContactInquiryStatusUpdateRequest request
    ) {
        if (request == null || request.status() == null) {
            throw new PublicRequestRejectedException(
                    HttpStatus.BAD_REQUEST,
                    "Contact inquiry status is required."
            );
        }
    }

    private void requirePrincipal(
            AdminJwtPrincipal principal
    ) {
        if (
                principal == null
                        || principal.adminUserId() == null
                        || principal.email() == null
                        || principal.email().isBlank()
                        || principal.role() == null
        ) {
            throw AdminAuthenticationException
                    .staleAuthentication();
        }
    }
}