package romelt_techcare.backend.service;

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
import romelt_techcare.backend.exception.AdminAuthenticationException;
import romelt_techcare.backend.exception.PublicRequestRejectedException;
import romelt_techcare.backend.repository.AdminUserRepository;
import romelt_techcare.backend.repository.ContactInquiryRepository;

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

        ContactInquiryStatus previousStatus =
                contactInquiry.getStatus();

        contactInquiry.setStatus(
                request.status()
        );

        ContactInquiry savedInquiry =
                contactInquiryRepository.save(
                        contactInquiry
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

        if (administrator.getAdminUserId() == null
                || !administrator.getAdminUserId()
                .equals(principal.adminUserId())) {

            throw AdminAuthenticationException
                    .staleAuthentication();
        }

        if (administrator.getEmail() == null
                || principal.email() == null
                || !administrator.getEmail()
                .trim()
                .equalsIgnoreCase(
                        principal.email().trim()
                )) {

            throw AdminAuthenticationException
                    .staleAuthentication();
        }

        if (administrator.getRole() == null
                || principal.role() == null
                || administrator.getRole()
                != principal.role()) {

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
        if (principal == null
                || principal.adminUserId() == null
                || principal.email() == null
                || principal.email().isBlank()
                || principal.role() == null) {

            throw AdminAuthenticationException
                    .staleAuthentication();
        }
    }
}