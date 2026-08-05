package romelt_techcare.backend.service.implement;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import romelt_techcare.backend.dto.CustomerReviewInvitationCreatedResponse;
import romelt_techcare.backend.entity.AdminUser;
import romelt_techcare.backend.entity.BookingRequest;
import romelt_techcare.backend.entity.CustomerReviewInvitation;
import romelt_techcare.backend.enums.CustomerReviewInvitationStatus;
import romelt_techcare.backend.enums.WebsiteContentAuditAction;
import romelt_techcare.backend.enums.WebsiteContentAuditResourceType;
import romelt_techcare.backend.repository.AdminUserRepository;
import romelt_techcare.backend.repository.BookingRequestRepository;
import romelt_techcare.backend.repository.CustomerReviewInvitationRepository;
import romelt_techcare.backend.service.CustomerReviewInvitationService;
import romelt_techcare.backend.service.CustomerReviewInvitationTokenService;
import romelt_techcare.backend.service.WebsiteContentAuditLogService;
import romelt_techcare.backend.service.WebsiteContentAuditSnapshotService;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * ================================================================
 * ROMELT TECHCARE — REVIEW INVITATION SERVICE IMPLEMENTATION
 * ================================================================
 *
 * Purpose:
 * Implements secure review-invitation creation, delivery state,
 * validation, consumption, revocation, expiration, and audit logging.
 *
 * Security:
 * - Generates a high-entropy token.
 * - Persists only its SHA-256 hash.
 * - Returns the plain token once.
 * - Never logs or includes token values in audit records.
 * - Uses pessimistic locking for state transitions.
 * ================================================================
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CustomerReviewInvitationServiceImplementation
        implements CustomerReviewInvitationService {

    private static final int MAX_TOKEN_GENERATION_ATTEMPTS =
            10;

    private static final List<CustomerReviewInvitationStatus>
            ACTIVE_STATUSES =
            List.of(
                    CustomerReviewInvitationStatus.PENDING,
                    CustomerReviewInvitationStatus.SENT
            );

    private final CustomerReviewInvitationRepository
            invitationRepository;

    private final BookingRequestRepository
            bookingRequestRepository;

    private final AdminUserRepository
            adminUserRepository;

    private final CustomerReviewInvitationTokenService
            tokenService;

    private final WebsiteContentAuditLogService
            websiteContentAuditLogService;

    private final WebsiteContentAuditSnapshotService
            websiteContentAuditSnapshotService;

    @Value("${app.public-site-url:http://localhost:5173}")
    private String publicSiteUrl;

    @Override
    @Transactional
    public CustomerReviewInvitationCreatedResponse createInvitation(
            UUID bookingRequestId,
            Instant expiresAt,
            UUID administratorId
    ) {
        requireIdentifier(
                bookingRequestId,
                "Booking request ID"
        );

        if (
                expiresAt == null
                        || !expiresAt.isAfter(Instant.now())
        ) {
            throw badRequest(
                    "Invitation expiration must be in the future."
            );
        }

        AdminUser administrator =
                getRequiredAdministrator(administratorId);

        BookingRequest bookingRequest =
                bookingRequestRepository
                        .findById(bookingRequestId)
                        .orElseThrow(() -> notFound(
                                "Booking request was not found."
                        ));

        requireReviewEligibleBooking(bookingRequest);

        if (
                invitationRepository
                        .existsByBookingRequest_BookingRequestIdAndInvitationStatusIn(
                                bookingRequestId,
                                ACTIVE_STATUSES
                        )
        ) {
            throw conflict(
                    "The booking request already has an active "
                            + "review invitation."
            );
        }

        GeneratedToken generatedToken =
                generateUniqueToken();

        CustomerReviewInvitation invitation =
                CustomerReviewInvitation.builder()
                        .bookingRequest(bookingRequest)
                        .customerEmail(
                                normalizeEmail(
                                        bookingRequest.getEmail()
                                )
                        )
                        .tokenHash(
                                generatedToken.tokenHash()
                        )
                        .invitationStatus(
                                CustomerReviewInvitationStatus.PENDING
                        )
                        .expiresAt(expiresAt)
                        .createdByAdminUser(administrator)
                        .build();

        CustomerReviewInvitation savedInvitation;

        try {
            savedInvitation =
                    invitationRepository.saveAndFlush(
                            invitation
                    );
        } catch (DataIntegrityViolationException exception) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Unable to create the review invitation.",
                    exception
            );
        }

        websiteContentAuditLogService.recordAudit(
                administratorId,
                WebsiteContentAuditAction.CREATE,
                WebsiteContentAuditResourceType.REVIEW_INVITATION,
                savedInvitation.getReviewInvitationId(),
                bookingRequest.getReferenceNumber(),
                null,
                createInvitationSnapshot(savedInvitation),
                "Customer review invitation created.",
                null
        );

        String reviewUrl =
                normalizePublicSiteUrl(publicSiteUrl)
                        + "/review?token="
                        + generatedToken.plainToken();

        return new CustomerReviewInvitationCreatedResponse(
                savedInvitation.getReviewInvitationId(),
                bookingRequest.getBookingRequestId(),
                bookingRequest.getReferenceNumber(),
                savedInvitation.getCustomerEmail(),
                generatedToken.plainToken(),
                reviewUrl,
                savedInvitation.getExpiresAt(),
                "Review invitation created. The plain token is "
                        + "shown only in this response."
        );
    }

    @Override
    @Transactional
    public CustomerReviewInvitation getInvitation(
            UUID reviewInvitationId
    ) {
        requireIdentifier(
                reviewInvitationId,
                "Review invitation ID"
        );

        CustomerReviewInvitation invitation =
                invitationRepository
                        .findByReviewInvitationId(
                                reviewInvitationId
                        )
                        .orElseThrow(() -> notFound(
                                "Review invitation was not found."
                        ));

        if (
                invitation.isExpired()
                        && ACTIVE_STATUSES.contains(
                        invitation.getInvitationStatus()
                )
        ) {
            JsonNode beforeSnapshot =
                    createInvitationSnapshot(invitation);

            invitation.markExpired();

            CustomerReviewInvitation savedInvitation =
                    invitationRepository
                            .saveAndFlush(invitation);

            websiteContentAuditLogService.recordAudit(
                    null,
                    WebsiteContentAuditAction.EXPIRE,
                    WebsiteContentAuditResourceType.REVIEW_INVITATION,
                    savedInvitation.getReviewInvitationId(),
                    createInvitationResourceName(savedInvitation),
                    beforeSnapshot,
                    createInvitationSnapshot(savedInvitation),
                    "Review invitation automatically expired during retrieval.",
                    null
            );

            return savedInvitation;
        }

        return invitation;
    }

    @Override
    public Page<CustomerReviewInvitation> searchInvitations(
            String keyword,
            CustomerReviewInvitationStatus status,
            Pageable pageable
    ) {
        requirePageable(pageable);

        return invitationRepository.searchInvitations(
                normalizeOptional(keyword),
                status,
                pageable
        );
    }

    @Override
    public Page<CustomerReviewInvitation> getBookingInvitations(
            UUID bookingRequestId,
            Pageable pageable
    ) {
        requireIdentifier(
                bookingRequestId,
                "Booking request ID"
        );

        requirePageable(pageable);

        if (
                !bookingRequestRepository
                        .existsById(bookingRequestId)
        ) {
            throw notFound(
                    "Booking request was not found."
            );
        }

        return invitationRepository
                .findAllByBookingRequest_BookingRequestIdOrderByCreatedAtDesc(
                        bookingRequestId,
                        pageable
                );
    }

    @Override
    @Transactional
    public CustomerReviewInvitation markInvitationSent(
            UUID reviewInvitationId,
            UUID administratorId
    ) {
        getRequiredAdministrator(administratorId);

        CustomerReviewInvitation invitation =
                getInvitationForUpdate(
                        reviewInvitationId
                );

        JsonNode beforeSnapshot =
                createInvitationSnapshot(invitation);

        if (invitation.isExpired()) {
            invitation.markExpired();

            CustomerReviewInvitation expiredInvitation =
                    invitationRepository
                            .saveAndFlush(invitation);

            websiteContentAuditLogService.recordAudit(
                    administratorId,
                    WebsiteContentAuditAction.EXPIRE,
                    WebsiteContentAuditResourceType.REVIEW_INVITATION,
                    expiredInvitation.getReviewInvitationId(),
                    createInvitationResourceName(expiredInvitation),
                    beforeSnapshot,
                    createInvitationSnapshot(expiredInvitation),
                    "Review invitation expired before it could be marked sent.",
                    null
            );

            throw conflict(
                    "The review invitation has expired."
            );
        }

        invitation.markSent();

        CustomerReviewInvitation savedInvitation =
                invitationRepository
                        .saveAndFlush(invitation);

        recordInvitationAudit(
                administratorId,
                WebsiteContentAuditAction.MARK_SENT,
                savedInvitation,
                beforeSnapshot,
                "Review invitation marked as sent."
        );

        return savedInvitation;
    }

    @Override
    @Transactional
    public CustomerReviewInvitation revokeInvitation(
            UUID reviewInvitationId,
            UUID administratorId
    ) {
        AdminUser administrator =
                getRequiredAdministrator(administratorId);

        CustomerReviewInvitation invitation =
                getInvitationForUpdate(
                        reviewInvitationId
                );

        JsonNode beforeSnapshot =
                createInvitationSnapshot(invitation);

        try {
            invitation.revoke(administrator);
        } catch (IllegalStateException exception) {
            throw conflict(exception.getMessage());
        }

        CustomerReviewInvitation savedInvitation =
                invitationRepository
                        .saveAndFlush(invitation);

        recordInvitationAudit(
                administratorId,
                WebsiteContentAuditAction.REVOKE,
                savedInvitation,
                beforeSnapshot,
                "Review invitation revoked."
        );

        return savedInvitation;
    }

    @Override
    @Transactional
    public CustomerReviewInvitation validatePublicToken(
            String plainToken
    ) {
        String tokenHash =
                tokenService.hashToken(plainToken);

        CustomerReviewInvitation invitation =
                invitationRepository
                        .findByTokenHashForUpdate(tokenHash)
                        .orElseThrow(() -> notFound(
                                "Review invitation is invalid."
                        ));

        requireUsableInvitation(invitation);

        return invitation;
    }

    @Override
    @Transactional
    public CustomerReviewInvitation consumeInvitation(
            String plainToken
    ) {
        String tokenHash =
                tokenService.hashToken(plainToken);

        CustomerReviewInvitation invitation =
                invitationRepository
                        .findByTokenHashForUpdate(tokenHash)
                        .orElseThrow(() -> notFound(
                                "Review invitation is invalid."
                        ));

        requireUsableInvitation(invitation);

        JsonNode beforeSnapshot =
                createInvitationSnapshot(invitation);

        try {
            invitation.markUsed();
        } catch (IllegalStateException exception) {
            throw conflict(exception.getMessage());
        }

        CustomerReviewInvitation savedInvitation =
                invitationRepository
                        .saveAndFlush(invitation);

        recordInvitationAudit(
                null,
                WebsiteContentAuditAction.CONSUME,
                savedInvitation,
                beforeSnapshot,
                "Review invitation consumed by a verified review submission."
        );

        return savedInvitation;
    }

    @Override
    @Transactional
    public int expireInvitations() {
        List<UUID> invitationIds =
                invitationRepository.findExpiredInvitationIds(
                        ACTIVE_STATUSES,
                        Instant.now()
                );

        int updatedCount = 0;

        for (UUID invitationId : invitationIds) {
            CustomerReviewInvitation invitation =
                    invitationRepository
                            .findByIdForUpdate(invitationId)
                            .orElse(null);

            if (
                    invitation == null
                            || !invitation.isExpired()
                            || !ACTIVE_STATUSES.contains(
                            invitation
                                    .getInvitationStatus()
                    )
            ) {
                continue;
            }

            JsonNode beforeSnapshot =
                    createInvitationSnapshot(invitation);

            invitation.markExpired();

            CustomerReviewInvitation savedInvitation =
                    invitationRepository.saveAndFlush(
                            invitation
                    );

            recordInvitationAudit(
                    null,
                    WebsiteContentAuditAction.EXPIRE,
                    savedInvitation,
                    beforeSnapshot,
                    "Review invitation automatically expired."
            );

            updatedCount++;
        }

        return updatedCount;
    }

    private void recordInvitationAudit(
            UUID administratorId,
            WebsiteContentAuditAction action,
            CustomerReviewInvitation invitation,
            JsonNode beforeSnapshot,
            String summary
    ) {
        websiteContentAuditLogService.recordAudit(
                administratorId,
                action,
                WebsiteContentAuditResourceType.REVIEW_INVITATION,
                invitation.getReviewInvitationId(),
                createInvitationResourceName(invitation),
                beforeSnapshot,
                createInvitationSnapshot(invitation),
                summary,
                null
        );
    }

    private JsonNode createInvitationSnapshot(
            CustomerReviewInvitation invitation
    ) {
        Map<String, Object> fields =
                new LinkedHashMap<>();

        fields.put(
                "reviewInvitationId",
                invitation.getReviewInvitationId()
        );

        fields.put(
                "bookingRequestId",
                invitation.getBookingRequest() == null
                        ? null
                        : invitation.getBookingRequest()
                        .getBookingRequestId()
        );

        fields.put(
                "bookingReferenceNumber",
                invitation.getBookingRequest() == null
                        ? null
                        : invitation.getBookingRequest()
                        .getReferenceNumber()
        );

        fields.put(
                "invitationStatus",
                invitation.getInvitationStatus()
        );

        fields.put(
                "expiresAt",
                invitation.getExpiresAt()
        );

        fields.put(
                "sentAt",
                invitation.getSentAt()
        );

        fields.put(
                "usedAt",
                invitation.getUsedAt()
        );

        fields.put(
                "revokedAt",
                invitation.getRevokedAt()
        );

        fields.put(
                "createdAt",
                invitation.getCreatedAt()
        );

        return websiteContentAuditSnapshotService
                .createSnapshot(fields);
    }

    private String createInvitationResourceName(
            CustomerReviewInvitation invitation
    ) {
        if (
                invitation.getBookingRequest() != null
                        && invitation.getBookingRequest()
                        .getReferenceNumber() != null
        ) {
            return invitation.getBookingRequest()
                    .getReferenceNumber();
        }

        return "Customer Review Invitation";
    }

    private CustomerReviewInvitation getInvitationForUpdate(
            UUID reviewInvitationId
    ) {
        requireIdentifier(
                reviewInvitationId,
                "Review invitation ID"
        );

        return invitationRepository
                .findByIdForUpdate(reviewInvitationId)
                .orElseThrow(() -> notFound(
                        "Review invitation was not found."
                ));
    }

    private void requireUsableInvitation(
            CustomerReviewInvitation invitation
    ) {
        if (invitation.isExpired()) {
            JsonNode beforeSnapshot =
                    createInvitationSnapshot(invitation);

            invitation.markExpired();

            CustomerReviewInvitation savedInvitation =
                    invitationRepository
                            .saveAndFlush(invitation);

            recordInvitationAudit(
                    null,
                    WebsiteContentAuditAction.EXPIRE,
                    savedInvitation,
                    beforeSnapshot,
                    "Review invitation expired during token validation."
            );

            throw conflict(
                    "Review invitation has expired."
            );
        }

        if (
                invitation.getInvitationStatus()
                        == CustomerReviewInvitationStatus.USED
        ) {
            throw conflict(
                    "Review invitation has already been used."
            );
        }

        if (
                invitation.getInvitationStatus()
                        == CustomerReviewInvitationStatus.REVOKED
        ) {
            throw conflict(
                    "Review invitation has been revoked."
            );
        }

        if (!invitation.isUsable()) {
            throw conflict(
                    "Review invitation is not usable."
            );
        }
    }

    private void requireReviewEligibleBooking(
            BookingRequest bookingRequest
    ) {
        if (
                bookingRequest.getStatus() == null
                        || !"COMPLETED".equals(
                        bookingRequest
                                .getStatus()
                                .name()
                                .toUpperCase(Locale.ROOT)
                )
        ) {
            throw conflict(
                    "A review invitation may be created only after "
                            + "the booking is completed."
            );
        }

        normalizeEmail(
                bookingRequest.getEmail()
        );
    }

    private GeneratedToken generateUniqueToken() {
        for (
                int attempt = 0;
                attempt < MAX_TOKEN_GENERATION_ATTEMPTS;
                attempt++
        ) {
            String plainToken =
                    tokenService.generateToken();

            String tokenHash =
                    tokenService.hashToken(plainToken);

            if (
                    !invitationRepository
                            .existsByTokenHash(tokenHash)
            ) {
                return new GeneratedToken(
                        plainToken,
                        tokenHash
                );
            }
        }

        throw new IllegalStateException(
                "Unable to generate a unique review token."
        );
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

    private String normalizeEmail(
            String value
    ) {
        String normalized =
                normalizeOptional(value);

        if (normalized == null) {
            throw conflict(
                    "The booking request does not contain a "
                            + "customer email address."
            );
        }

        return normalized.toLowerCase(
                Locale.ROOT
        );
    }

    private String normalizePublicSiteUrl(
            String value
    ) {
        String normalized =
                normalizeOptional(value);

        if (normalized == null) {
            return "http://localhost:5173";
        }

        return normalized.endsWith("/")
                ? normalized.substring(
                0,
                normalized.length() - 1
        )
                : normalized;
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

    private void requirePageable(
            Pageable pageable
    ) {
        if (pageable == null) {
            throw badRequest(
                    "Pagination information is required."
            );
        }
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

    private record GeneratedToken(
            String plainToken,
            String tokenHash
    ) {
    }
}