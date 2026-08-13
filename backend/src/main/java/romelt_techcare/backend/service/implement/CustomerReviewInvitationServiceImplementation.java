package romelt_techcare.backend.service.implement;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import romelt_techcare.backend.dto.CustomerReviewInvitationCreatedResponse;
import romelt_techcare.backend.dto.EmailSendResult;
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
import romelt_techcare.backend.service.EmailService;
import romelt_techcare.backend.service.WebsiteContentAuditLogService;
import romelt_techcare.backend.service.WebsiteContentAuditSnapshotService;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
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
 * Implements secure review-invitation creation, email delivery,
 * delivery-state tracking, public-token validation, consumption,
 * revocation, expiration, and immutable audit logging.
 *
 * Review invitation workflow:
 *
 * 1. Administrator selects a COMPLETED booking.
 * 2. Backend verifies that the booking is eligible for review.
 * 3. Backend generates a cryptographically secure review token.
 * 4. Only the SHA-256 token hash is stored in the database.
 * 5. A secure public review URL is created using the plain token.
 * 6. The review invitation email is sent directly through EmailService.
 * 7. If email delivery is accepted, the invitation becomes SENT.
 * 8. If email delivery fails, the invitation remains PENDING.
 * 9. Customer follows the secure link and submits one verified review.
 * 10. Successful review submission consumes the invitation.
 *
 * Security:
 * - Generates a high-entropy token.
 * - Persists only its SHA-256 hash.
 * - Returns the plain token only from the creation response.
 * - Never writes the plain token to audit records.
 * - Never writes the plain token to application logs.
 * - Never persists the secure review URL in the notifications table.
 * - Uses pessimistic locking for invitation state transitions.
 *
 * Important:
 *
 * Review invitation email is sent directly through EmailService rather
 * than NotificationService because the current NotificationService
 * persists messageText/messageHtml before sending. A review email body
 * contains the plain invitation token inside the review URL and that
 * token must not be persisted.
 *
 * Normal booking/contact notifications may continue using the standard
 * NotificationService because they do not contain one-time security
 * tokens.
 * ================================================================
 */
@Slf4j
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

    private static final DateTimeFormatter
            REVIEW_EXPIRATION_FORMATTER =
            DateTimeFormatter
                    .ofPattern(
                            "MMMM d, yyyy 'at' h:mm a 'UTC'",
                            Locale.US
                    )
                    .withZone(ZoneOffset.UTC);

    private final CustomerReviewInvitationRepository
            invitationRepository;

    private final BookingRequestRepository
            bookingRequestRepository;

    private final AdminUserRepository
            adminUserRepository;

    private final CustomerReviewInvitationTokenService
            tokenService;

    private final EmailService
            emailService;

    private final WebsiteContentAuditLogService
            websiteContentAuditLogService;

    private final WebsiteContentAuditSnapshotService
            websiteContentAuditSnapshotService;

    @Value("${app.public-site-url:http://localhost:5173}")
    private String publicSiteUrl;


    // =================================================================
    // CREATE + SEND REVIEW INVITATION
    // =================================================================

    /**
     * Creates a secure review invitation and immediately attempts to
     * deliver it to the booking customer's email address.
     *
     * Business rules:
     * - Booking must exist.
     * - Booking must be COMPLETED.
     * - Booking must have an email address.
     * - Booking cannot already have an active invitation.
     * - Expiration must be in the future.
     *
     * Delivery behavior:
     * - Invitation is first persisted as PENDING.
     * - Email is then sent using the one-time plain token.
     * - Successful provider acceptance changes status to SENT.
     * - Failed email delivery leaves invitation PENDING.
     *
     * Security:
     * The plain token is used only in memory while constructing the
     * outbound email. It is never persisted by this service.
     */
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

        String customerEmail =
                normalizeEmail(
                        bookingRequest.getEmail()
                );

        CustomerReviewInvitation invitation =
                CustomerReviewInvitation.builder()
                        .bookingRequest(bookingRequest)
                        .customerEmail(customerEmail)
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

        /*
         * IMPORTANT:
         *
         * The plain token exists only in memory.
         *
         * Do not place reviewUrl or plainToken in:
         * - audit snapshots;
         * - database notification records;
         * - log statements;
         * - exception messages.
         */
        String reviewUrl =
                normalizePublicSiteUrl(publicSiteUrl)
                        + "/review?token="
                        + generatedToken.plainToken();

        ReviewInvitationDeliveryResult deliveryResult =
                sendReviewInvitationEmail(
                        savedInvitation,
                        bookingRequest,
                        reviewUrl
                );

        if (deliveryResult.successful()) {
            JsonNode beforeSentSnapshot =
                    createInvitationSnapshot(
                            savedInvitation
                    );

            try {
                savedInvitation.markSent();

            } catch (IllegalStateException exception) {
                throw conflict(
                        exception.getMessage()
                );
            }

            savedInvitation =
                    invitationRepository.saveAndFlush(
                            savedInvitation
                    );

            recordInvitationAudit(
                    administratorId,
                    WebsiteContentAuditAction.MARK_SENT,
                    savedInvitation,
                    beforeSentSnapshot,
                    "Customer review invitation email sent."
            );
        }

        return new CustomerReviewInvitationCreatedResponse(
                savedInvitation.getReviewInvitationId(),
                bookingRequest.getBookingRequestId(),
                bookingRequest.getReferenceNumber(),
                savedInvitation.getCustomerEmail(),
                generatedToken.plainToken(),
                reviewUrl,
                savedInvitation.getExpiresAt(),
                deliveryResult.successful()
                        ? "Review invitation created and emailed successfully."
                        : "Review invitation was created, but the email could not "
                        + "be sent. The invitation remains pending."
        );
    }


    // =================================================================
    // GET INVITATION
    // =================================================================

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


    // =================================================================
    // SEARCH INVITATIONS
    // =================================================================

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


    // =================================================================
    // GET BOOKING INVITATIONS
    // =================================================================

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


    // =================================================================
    // MANUALLY MARK SENT
    // =================================================================

    /**
     * Preserves the existing administrative operation for cases where an
     * invitation was delivered manually outside the automated email
     * workflow.
     *
     * Normal invitations created through createInvitation() are marked
     * SENT automatically after successful email provider acceptance.
     */
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

        try {
            invitation.markSent();

        } catch (IllegalStateException exception) {
            throw conflict(
                    exception.getMessage()
            );
        }

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


    // =================================================================
    // REVOKE INVITATION
    // =================================================================

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
            throw conflict(
                    exception.getMessage()
            );
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


    // =================================================================
    // VALIDATE PUBLIC TOKEN
    // =================================================================

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


    // =================================================================
    // CONSUME INVITATION
    // =================================================================

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
            throw conflict(
                    exception.getMessage()
            );
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


    // =================================================================
    // EXPIRE INVITATIONS
    // =================================================================

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


    // =================================================================
    // REVIEW INVITATION EMAIL
    // =================================================================

    /**
     * Sends the secure review invitation without persisting the email
     * body or secure URL.
     *
     * The EmailService sends directly through the configured SMTP
     * provider and returns a safe delivery result.
     */
    private ReviewInvitationDeliveryResult sendReviewInvitationEmail(
            CustomerReviewInvitation invitation,
            BookingRequest bookingRequest,
            String reviewUrl
    ) {
        String customerName =
                resolveCustomerName(
                        bookingRequest
                );

        String referenceNumber =
                resolveReferenceNumber(
                        bookingRequest
                );

        String subject =
                "How was your Romelt TechCare service? — "
                        + referenceNumber;

        String textBody =
                buildReviewInvitationText(
                        customerName,
                        referenceNumber,
                        bookingRequest,
                        invitation.getExpiresAt(),
                        reviewUrl
                );

        String htmlBody =
                buildReviewInvitationHtml(
                        customerName,
                        referenceNumber,
                        bookingRequest,
                        invitation.getExpiresAt(),
                        reviewUrl
                );

        try {
            EmailSendResult result =
                    emailService.sendEmail(
                            invitation.getCustomerEmail(),
                            customerName,
                            subject,
                            textBody,
                            htmlBody
                    );

            if (
                    result != null
                            && result.successful()
            ) {
                log.info(
                        "Review invitation email accepted by provider. reviewInvitationId={}, bookingRequestId={}, referenceNumber={}, provider={}",
                        invitation.getReviewInvitationId(),
                        bookingRequest.getBookingRequestId(),
                        referenceNumber,
                        normalizeOptional(
                                result.providerName()
                        )
                );

                return new ReviewInvitationDeliveryResult(
                        true,
                        null,
                        null
                );
            }

            String failureCode =
                    result == null
                            ? "EMAIL_EMPTY_RESULT"
                            : normalizeOptional(
                            result.failureCode()
                    );

            String failureMessage =
                    result == null
                            ? "Email provider returned no delivery result."
                            : normalizeOptional(
                            result.failureMessage()
                    );

            log.warn(
                    "Review invitation email was not sent. reviewInvitationId={}, bookingRequestId={}, referenceNumber={}, failureCode={}",
                    invitation.getReviewInvitationId(),
                    bookingRequest.getBookingRequestId(),
                    referenceNumber,
                    failureCode == null
                            ? "UNKNOWN"
                            : failureCode
            );

            return new ReviewInvitationDeliveryResult(
                    false,
                    failureCode,
                    failureMessage
            );

        } catch (Exception exception) {
            /*
             * Never log:
             * - reviewUrl;
             * - plain token;
             * - complete email body.
             */
            log.error(
                    "Review invitation email failed unexpectedly. reviewInvitationId={}, bookingRequestId={}, referenceNumber={}, errorType={}",
                    invitation.getReviewInvitationId(),
                    bookingRequest.getBookingRequestId(),
                    referenceNumber,
                    exception
                            .getClass()
                            .getSimpleName()
            );

            return new ReviewInvitationDeliveryResult(
                    false,
                    "REVIEW_INVITATION_EMAIL_ERROR",
                    safeFailureMessage(
                            exception.getMessage(),
                            "An unexpected review invitation email error occurred."
                    )
            );
        }
    }


    // =================================================================
    // EMAIL TEXT BODY
    // =================================================================

    private String buildReviewInvitationText(
            String customerName,
            String referenceNumber,
            BookingRequest bookingRequest,
            Instant expiresAt,
            String reviewUrl
    ) {
        String serviceName =
                resolveServiceName(
                        bookingRequest
                );

        return """
                Hi %s,

                Thank you for choosing Romelt TechCare.

                Your service request %s has been completed. We would appreciate your feedback about your experience with our service.

                Service:
                %s

                Share your experience:
                %s

                This is a secure, single-use review link. It expires on %s.

                Your review will be submitted to Romelt TechCare for moderation before it may appear publicly on our website.

                You may choose how your name is displayed when submitting your review.

                If you did not receive service from Romelt TechCare or you believe this message was sent to you by mistake, you may ignore this email.

                Thank you for trusting Romelt TechCare.

                Romelt TechCare
                Hassle-free technology support.
                """
                .formatted(
                        customerName,
                        referenceNumber,
                        serviceName,
                        reviewUrl,
                        formatExpiration(expiresAt)
                );
    }


    // =================================================================
    // EMAIL HTML BODY
    // =================================================================

    private String buildReviewInvitationHtml(
            String customerName,
            String referenceNumber,
            BookingRequest bookingRequest,
            Instant expiresAt,
            String reviewUrl
    ) {
        String safeCustomerName =
                escapeHtml(customerName);

        String safeReferenceNumber =
                escapeHtml(referenceNumber);

        String safeServiceName =
                escapeHtml(
                        resolveServiceName(
                                bookingRequest
                        )
                );

        String safeExpiration =
                escapeHtml(
                        formatExpiration(expiresAt)
                );

        String safeReviewUrl =
                escapeHtml(reviewUrl);

        return """
                <!doctype html>
                <html lang="en">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>Romelt TechCare Review Invitation</title>
                </head>

                <body style="
                    margin:0;
                    padding:0;
                    background:#f8fafc;
                    font-family:Arial,Helvetica,sans-serif;
                    color:#0f172a;
                ">

                    <table
                        role="presentation"
                        width="100%%"
                        cellspacing="0"
                        cellpadding="0"
                        border="0"
                        style="background:#f8fafc;padding:32px 16px;"
                    >
                        <tr>
                            <td align="center">

                                <table
                                    role="presentation"
                                    width="100%%"
                                    cellspacing="0"
                                    cellpadding="0"
                                    border="0"
                                    style="
                                        max-width:640px;
                                        background:#ffffff;
                                        border:1px solid #e2e8f0;
                                        border-radius:16px;
                                        overflow:hidden;
                                    "
                                >

                                    <tr>
                                        <td style="
                                            background:#0f172a;
                                            padding:28px 32px;
                                            color:#ffffff;
                                        ">
                                            <div style="
                                                font-size:12px;
                                                font-weight:700;
                                                text-transform:uppercase;
                                                letter-spacing:1.5px;
                                                color:#93c5fd;
                                            ">
                                                Romelt TechCare
                                            </div>

                                            <div style="
                                                margin-top:8px;
                                                font-size:25px;
                                                line-height:1.25;
                                                font-weight:800;
                                            ">
                                                How was your service?
                                            </div>
                                        </td>
                                    </tr>

                                    <tr>
                                        <td style="padding:32px;">

                                            <p style="
                                                margin:0 0 18px;
                                                font-size:16px;
                                                line-height:1.7;
                                            ">
                                                Hi <strong>%s</strong>,
                                            </p>

                                            <p style="
                                                margin:0 0 18px;
                                                font-size:15px;
                                                line-height:1.7;
                                                color:#334155;
                                            ">
                                                Thank you for choosing Romelt TechCare.
                                                Your service request has been completed,
                                                and we would appreciate your feedback
                                                about your experience.
                                            </p>

                                            <table
                                                role="presentation"
                                                width="100%%"
                                                cellspacing="0"
                                                cellpadding="0"
                                                border="0"
                                                style="
                                                    margin:24px 0;
                                                    background:#f8fafc;
                                                    border:1px solid #e2e8f0;
                                                    border-radius:12px;
                                                "
                                            >
                                                <tr>
                                                    <td style="padding:18px 20px;">

                                                        <div style="
                                                            font-size:11px;
                                                            font-weight:700;
                                                            text-transform:uppercase;
                                                            letter-spacing:1px;
                                                            color:#64748b;
                                                        ">
                                                            Service request
                                                        </div>

                                                        <div style="
                                                            margin-top:5px;
                                                            font-size:16px;
                                                            font-weight:700;
                                                            color:#0f172a;
                                                        ">
                                                            %s
                                                        </div>

                                                        <div style="
                                                            margin-top:12px;
                                                            font-size:11px;
                                                            font-weight:700;
                                                            text-transform:uppercase;
                                                            letter-spacing:1px;
                                                            color:#64748b;
                                                        ">
                                                            Service
                                                        </div>

                                                        <div style="
                                                            margin-top:5px;
                                                            font-size:15px;
                                                            color:#334155;
                                                        ">
                                                            %s
                                                        </div>

                                                    </td>
                                                </tr>
                                            </table>

                                            <p style="
                                                margin:0 0 22px;
                                                font-size:15px;
                                                line-height:1.7;
                                                color:#334155;
                                            ">
                                                Your feedback helps us improve our service
                                                and helps future customers understand what
                                                they can expect from Romelt TechCare.
                                            </p>

                                            <table
                                                role="presentation"
                                                cellspacing="0"
                                                cellpadding="0"
                                                border="0"
                                                style="margin:0 auto 26px;"
                                            >
                                                <tr>
                                                    <td
                                                        align="center"
                                                        bgcolor="#1976D2"
                                                        style="border-radius:10px;"
                                                    >
                                                        <a
                                                            href="%s"
                                                            style="
                                                                display:inline-block;
                                                                padding:14px 28px;
                                                                font-size:15px;
                                                                font-weight:700;
                                                                color:#ffffff;
                                                                text-decoration:none;
                                                            "
                                                        >
                                                            Leave a Review
                                                        </a>
                                                    </td>
                                                </tr>
                                            </table>

                                            <div style="
                                                margin-top:8px;
                                                padding:16px;
                                                background:#eff6ff;
                                                border:1px solid #bfdbfe;
                                                border-radius:10px;
                                                font-size:13px;
                                                line-height:1.6;
                                                color:#1e3a8a;
                                            ">
                                                <strong>Secure review link</strong><br>
                                                This link can be used once and expires on
                                                %s.
                                            </div>

                                            <p style="
                                                margin:24px 0 0;
                                                font-size:13px;
                                                line-height:1.7;
                                                color:#64748b;
                                            ">
                                                Your review will be submitted to Romelt
                                                TechCare for moderation before it may
                                                appear publicly on our website.
                                            </p>

                                            <p style="
                                                margin:14px 0 0;
                                                font-size:13px;
                                                line-height:1.7;
                                                color:#64748b;
                                            ">
                                                If you did not receive service from Romelt
                                                TechCare or believe this email was sent to
                                                you by mistake, you may safely ignore it.
                                            </p>

                                        </td>
                                    </tr>

                                    <tr>
                                        <td style="
                                            border-top:1px solid #e2e8f0;
                                            padding:22px 32px;
                                            font-size:12px;
                                            line-height:1.6;
                                            color:#64748b;
                                        ">
                                            Thank you for trusting
                                            <strong style="color:#0f172a;">
                                                Romelt TechCare
                                            </strong>.
                                        </td>
                                    </tr>

                                </table>

                            </td>
                        </tr>
                    </table>

                </body>
                </html>
                """
                .formatted(
                        safeCustomerName,
                        safeReferenceNumber,
                        safeServiceName,
                        safeReviewUrl,
                        safeExpiration
                );
    }


    // =================================================================
    // AUDIT
    // =================================================================

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
            return invitation
                    .getBookingRequest()
                    .getReferenceNumber();
        }

        return "Customer Review Invitation";
    }


    // =================================================================
    // INVITATION LOOKUP
    // =================================================================

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


    // =================================================================
    // PUBLIC INVITATION VALIDATION
    // =================================================================

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


    // =================================================================
    // BOOKING ELIGIBILITY
    // =================================================================

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


    // =================================================================
    // TOKEN GENERATION
    // =================================================================

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


    // =================================================================
    // ADMINISTRATOR
    // =================================================================

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


    // =================================================================
    // BOOKING DISPLAY HELPERS
    // =================================================================

    private String resolveCustomerName(
            BookingRequest bookingRequest
    ) {
        String customerName =
                normalizeOptional(
                        bookingRequest.getFullName()
                );

        return customerName == null
                ? "Customer"
                : customerName;
    }


    private String resolveReferenceNumber(
            BookingRequest bookingRequest
    ) {
        String referenceNumber =
                normalizeOptional(
                        bookingRequest.getReferenceNumber()
                );

        return referenceNumber == null
                ? "your completed service request"
                : referenceNumber;
    }


    private String resolveServiceName(
            BookingRequest bookingRequest
    ) {
        String serviceType =
                normalizeOptional(
                        bookingRequest.getServiceType()
                );

        if (serviceType == null) {
            return "Technology support service";
        }

        return serviceType
                .replace('_', ' ')
                .toLowerCase(Locale.ROOT)
                .replaceFirst(
                        "^.",
                        serviceType
                                .substring(0, 1)
                                .toUpperCase(Locale.ROOT)
                );
    }


    // =================================================================
    // EMAIL / URL HELPERS
    // =================================================================

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


    private String formatExpiration(
            Instant expiresAt
    ) {
        if (expiresAt == null) {
            return "the invitation expiration time";
        }

        return REVIEW_EXPIRATION_FORMATTER
                .format(expiresAt);
    }


    private String escapeHtml(
            String value
    ) {
        if (value == null) {
            return "";
        }

        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }


    // =================================================================
    // VALIDATION
    // =================================================================

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


    // =================================================================
    // NORMALIZATION
    // =================================================================

    private String normalizeOptional(
            String value
    ) {
        if (value == null) {
            return null;
        }

        String normalized =
                value.trim();

        return normalized.isEmpty()
                ? null
                : normalized;
    }


    private String safeFailureMessage(
            String value,
            String fallback
    ) {
        String normalized =
                normalizeOptional(value);

        if (normalized == null) {
            return fallback;
        }

        return normalized.length() <= 1000
                ? normalized
                : normalized.substring(
                0,
                1000
        );
    }


    // =================================================================
    // HTTP EXCEPTIONS
    // =================================================================

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


    // =================================================================
    // INTERNAL RECORDS
    // =================================================================

    private record GeneratedToken(
            String plainToken,
            String tokenHash
    ) {
    }


    private record ReviewInvitationDeliveryResult(
            boolean successful,
            String failureCode,
            String failureMessage
    ) {
    }
}