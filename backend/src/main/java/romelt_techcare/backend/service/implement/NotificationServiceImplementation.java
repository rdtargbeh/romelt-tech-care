package romelt_techcare.backend.service.implement;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import romelt_techcare.backend.dto.AdminInAppNotificationResponse;
import romelt_techcare.backend.dto.AdminNotificationCountResponse;
import romelt_techcare.backend.dto.EmailSendResult;
import romelt_techcare.backend.dto.NotificationCreateRequest;
import romelt_techcare.backend.dto.NotificationResponse;
import romelt_techcare.backend.dto.NotificationSummaryResponse;
import romelt_techcare.backend.dto.SmsSendResult;
import romelt_techcare.backend.entity.Notification;
import romelt_techcare.backend.enums.NotificationChannel;
import romelt_techcare.backend.enums.NotificationResourceType;
import romelt_techcare.backend.enums.NotificationStatus;
import romelt_techcare.backend.exception.PublicRequestRejectedException;
import romelt_techcare.backend.mapper.NotificationMapper;
import romelt_techcare.backend.repository.NotificationRepository;
import romelt_techcare.backend.service.AdminNotificationWebSocketService;
import romelt_techcare.backend.service.EmailService;
import romelt_techcare.backend.service.NotificationService;
import romelt_techcare.backend.service.SmsService;

import java.time.Instant;
import java.util.UUID;

/**
 * ================================================================
 * ROMELT TECHCARE — NOTIFICATION SERVICE IMPLEMENTATION
 * ================================================================
 *
 * Purpose:
 * Implements the simplified notification workflow for customer
 * EMAIL/SMS notifications and administrator IN_APP notifications.
 *
 * EMAIL flow:
 * 1. Start an independent notification transaction.
 * 2. Save a PENDING notification.
 * 3. Record the send attempt.
 * 4. Send through the configured email provider.
 * 5. Mark the notification SENT or FAILED.
 * 6. Commit independently from the originating business transaction.
 *
 * SMS flow:
 * 1. Start an independent notification transaction.
 * 2. Save a PENDING notification.
 * 3. Record the send attempt.
 * 4. Send through the configured SMS provider.
 * 5. Mark the notification SENT or FAILED.
 * 6. Commit independently from the originating business transaction.
 *
 * IN_APP flow:
 * 1. Start an independent notification transaction.
 * 2. Save the administrator notification.
 * 3. Mark it DELIVERED immediately.
 * 4. Persist the final notification state.
 * 5. Publish the WebSocket message as best-effort real-time delivery.
 *
 * Transaction behavior:
 *
 * EMAIL, SMS, and IN_APP notifications may be triggered from
 * AFTER_COMMIT application-event listeners.
 *
 * The originating booking/contact transaction has therefore already
 * completed before notification processing starts.
 *
 * All outbound/in-app notification creation methods use REQUIRES_NEW
 * so:
 *
 * - notification persistence always has an active transaction;
 * - notification failure cannot roll back a valid booking/inquiry;
 * - each notification channel remains operationally isolated;
 * - database-backed IN_APP notifications remain available even when
 *   no administrator WebSocket connection is active.
 *
 * The notifications table is the source of truth for persistent
 * notification state.
 * ================================================================
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImplementation
        implements NotificationService {

    private final NotificationRepository notificationRepository;

    private final NotificationMapper notificationMapper;

    private final AdminNotificationWebSocketService
            adminNotificationWebSocketService;

    private final EmailService emailService;

    private final SmsService smsService;

    // =================================================================
    // EMAIL
    // =================================================================

    /**
     * Creates and sends one customer EMAIL notification.
     *
     * REQUIRES_NEW is intentional because this method may execute from
     * an AFTER_COMMIT listener after the originating transaction has
     * already completed.
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public NotificationResponse sendEmail(
            NotificationCreateRequest request
    ) {
        requireChannel(
                request,
                NotificationChannel.EMAIL
        );

        Notification notification =
                savePendingNotification(request);

        try {
            notification.beginAttempt();

            notification =
                    notificationRepository.saveAndFlush(
                            notification
                    );

            EmailSendResult result =
                    emailService.sendEmail(
                            notification.getRecipientAddress(),
                            notification.getRecipientName(),
                            notification.getTitle(),
                            notification.getMessageText(),
                            notification.getMessageHtml()
                    );

            if (
                    result != null
                            && result.successful()
            ) {
                notification.markSent(
                        normalizeProviderName(
                                result.providerName(),
                                "SPRING_MAIL_SMTP"
                        ),
                        result.providerMessageId(),
                        null
                );

                log.info(
                        "Email notification sent. notificationId={}, provider={}, providerMessageId={}",
                        notification.getNotificationId(),
                        normalizeProviderName(
                                result.providerName(),
                                "SPRING_MAIL_SMTP"
                        ),
                        result.providerMessageId()
                );

            } else {
                notification.markFailed(
                        result == null
                                ? "EMAIL_EMPTY_RESULT"
                                : result.failureCode(),

                        resolveFailureMessage(
                                result == null
                                        ? null
                                        : result.failureMessage(),
                                "Email delivery failed."
                        ),

                        null
                );

                log.warn(
                        "Email notification provider reported failure. notificationId={}, failureCode={}",
                        notification.getNotificationId(),
                        result == null
                                ? "EMAIL_EMPTY_RESULT"
                                : result.failureCode()
                );
            }

        } catch (Exception exception) {
            notification.markFailed(
                    "EMAIL_SEND_ERROR",
                    safeFailureMessage(
                            exception.getMessage(),
                            "An unexpected email delivery error occurred."
                    ),
                    null
            );

            log.error(
                    "Email notification failed. notificationId={}, errorType={}, message={}",
                    notification.getNotificationId(),
                    exception
                            .getClass()
                            .getSimpleName(),
                    safeFailureMessage(
                            exception.getMessage(),
                            "No provider error message was available."
                    )
            );
        }

        Notification savedNotification =
                notificationRepository.saveAndFlush(
                        notification
                );

        return notificationMapper.toResponse(
                savedNotification
        );
    }

    // =================================================================
    // SMS
    // =================================================================

    /**
     * Creates and sends one customer SMS notification.
     *
     * REQUIRES_NEW is intentional because this method may execute from
     * an AFTER_COMMIT listener.
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public NotificationResponse sendSms(
            NotificationCreateRequest request
    ) {
        requireChannel(
                request,
                NotificationChannel.SMS
        );

        Notification notification =
                savePendingNotification(request);

        try {
            notification.beginAttempt();

            notification =
                    notificationRepository.saveAndFlush(
                            notification
                    );

            SmsSendResult result =
                    smsService.sendSms(
                            notification.getRecipientAddress(),
                            notification.getMessageText()
                    );

            if (
                    result != null
                            && result.successful()
            ) {
                notification.markSent(
                        normalizeProviderName(
                                result.providerName(),
                                "TWILIO"
                        ),
                        result.providerMessageId(),
                        null
                );

                log.info(
                        "SMS notification sent. notificationId={}, provider={}, providerMessageId={}",
                        notification.getNotificationId(),
                        normalizeProviderName(
                                result.providerName(),
                                "TWILIO"
                        ),
                        result.providerMessageId()
                );

            } else {
                notification.markFailed(
                        result == null
                                ? "SMS_EMPTY_RESULT"
                                : result.failureCode(),

                        resolveFailureMessage(
                                result == null
                                        ? null
                                        : result.failureMessage(),
                                "SMS delivery failed."
                        ),

                        null
                );

                log.warn(
                        "SMS notification provider reported failure. notificationId={}, failureCode={}",
                        notification.getNotificationId(),
                        result == null
                                ? "SMS_EMPTY_RESULT"
                                : result.failureCode()
                );
            }

        } catch (Exception exception) {
            notification.markFailed(
                    "SMS_SEND_ERROR",
                    safeFailureMessage(
                            exception.getMessage(),
                            "An unexpected SMS delivery error occurred."
                    ),
                    null
            );

            log.error(
                    "SMS notification failed. notificationId={}, errorType={}, message={}",
                    notification.getNotificationId(),
                    exception
                            .getClass()
                            .getSimpleName(),
                    safeFailureMessage(
                            exception.getMessage(),
                            "No provider error message was available."
                    )
            );
        }

        Notification savedNotification =
                notificationRepository.saveAndFlush(
                        notification
                );

        return notificationMapper.toResponse(
                savedNotification
        );
    }

    // =================================================================
    // IN-APP
    // =================================================================

    /**
     * Creates, persists, and publishes one administrator IN_APP
     * notification.
     *
     * IMPORTANT:
     *
     * REQUIRES_NEW is required because booking/contact notification
     * listeners execute AFTER_COMMIT.
     *
     * The originating business transaction no longer exists at this
     * point. The in-app notification therefore needs its own independent
     * transaction.
     *
     * Persistence happens before WebSocket publication so administrators
     * can still retrieve notifications through REST even if:
     *
     * - the administrator is offline;
     * - WebSocket is disconnected;
     * - the browser is closed;
     * - real-time publication fails.
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AdminInAppNotificationResponse createInApp(
            NotificationCreateRequest request
    ) {
        requireChannel(
                request,
                NotificationChannel.IN_APP
        );

        Notification notification =
                notificationMapper.toEntity(
                        request
                );

        /*
         * Persist first.
         *
         * This ensures the notification becomes durable before any
         * best-effort real-time WebSocket publication occurs.
         */
        Notification savedNotification =
                notificationRepository.saveAndFlush(
                        notification
                );

        /*
         * IN_APP notification does not require an external provider.
         *
         * Once stored successfully, it is considered delivered to the
         * administrator's persistent notification inbox.
         */
        savedNotification.markInAppDelivered();

        savedNotification =
                notificationRepository.saveAndFlush(
                        savedNotification
                );

        AdminInAppNotificationResponse response =
                notificationMapper.toAdminInAppResponse(
                        savedNotification
                );

        /*
         * Best-effort WebSocket publication.
         *
         * Database persistence remains the source of truth.
         *
         * An administrator who is not currently connected will still
         * receive this notification from:
         *
         * GET /api/v1/admin/notifications
         */
        try {
            adminNotificationWebSocketService
                    .publishNotification(
                            savedNotification.getAdminUserId(),
                            response
                    );

        } catch (Exception exception) {
            /*
             * WebSocket failure must never invalidate the persisted
             * notification.
             */
            log.warn(
                    "Administrator in-app notification persisted but WebSocket publication failed. notificationId={}, adminUserId={}, errorType={}, message={}",
                    savedNotification.getNotificationId(),
                    savedNotification.getAdminUserId(),
                    exception
                            .getClass()
                            .getSimpleName(),
                    safeFailureMessage(
                            exception.getMessage(),
                            "WebSocket publication failed."
                    )
            );
        }

        log.info(
                "Administrator in-app notification created. notificationId={}, adminUserId={}, resourceType={}, resourceId={}, status={}",
                savedNotification.getNotificationId(),
                savedNotification.getAdminUserId(),
                savedNotification.getResourceType(),
                savedNotification.getResourceId(),
                savedNotification.getNotificationStatus()
        );

        return response;
    }

    // =================================================================
    // GET ONE
    // =================================================================

    /**
     * Returns one complete notification record.
     */
    @Override
    @Transactional(readOnly = true)
    public NotificationResponse getNotification(
            UUID notificationId
    ) {
        Notification notification =
                findNotification(
                        notificationId
                );

        return notificationMapper.toResponse(
                notification
        );
    }

    // =================================================================
    // SEARCH
    // =================================================================

    /**
     * Searches notification history.
     */
    @Override
    @Transactional(readOnly = true)
    public Page<NotificationSummaryResponse> search(

            NotificationChannel channel,

            NotificationStatus status,

            NotificationResourceType resourceType,

            UUID resourceId,

            UUID customerId,

            UUID adminUserId,

            String keyword,

            Pageable pageable
    ) {
        requirePageable(
                pageable
        );

        return notificationRepository
                .search(
                        channel,
                        status,
                        resourceType,
                        resourceId,
                        customerId,
                        adminUserId,
                        normalizeOptional(
                                keyword
                        ),
                        pageable
                )
                .map(
                        notificationMapper
                                ::toSummaryResponse
                );
    }

    // =================================================================
    // ADMIN IN-APP LIST
    // =================================================================

    /**
     * Returns active administrator IN_APP notifications.
     *
     * Dismissed notifications are excluded by the repository query.
     */
    @Override
    @Transactional(readOnly = true)
    public Page<AdminInAppNotificationResponse>
    getAdminNotifications(

            UUID adminUserId,

            boolean unreadOnly,

            Pageable pageable
    ) {
        requireId(
                adminUserId,
                "Administrator ID is required."
        );

        requirePageable(
                pageable
        );

        Page<Notification> notifications =
                unreadOnly
                        ? notificationRepository
                        .findByAdminUserIdAndChannelAndReadAtIsNullAndDismissedAtIsNullOrderByCreatedAtDesc(
                                adminUserId,
                                NotificationChannel.IN_APP,
                                pageable
                        )
                        : notificationRepository
                        .findByAdminUserIdAndChannelAndDismissedAtIsNullOrderByCreatedAtDesc(
                                adminUserId,
                                NotificationChannel.IN_APP,
                                pageable
                        );

        return notifications.map(
                notificationMapper
                        ::toAdminInAppResponse
        );
    }

    // =================================================================
    // UNREAD COUNT
    // =================================================================

    /**
     * Returns the authenticated administrator's unread notification
     * count.
     */
    @Override
    @Transactional(readOnly = true)
    public AdminNotificationCountResponse getAdminUnreadCount(
            UUID adminUserId
    ) {
        requireId(
                adminUserId,
                "Administrator ID is required."
        );

        long unreadCount =
                notificationRepository
                        .countByAdminUserIdAndChannelAndReadAtIsNullAndDismissedAtIsNull(
                                adminUserId,
                                NotificationChannel.IN_APP
                        );

        return new AdminNotificationCountResponse(
                unreadCount
        );
    }

    // =================================================================
    // MARK READ
    // =================================================================

    /**
     * Marks one administrator IN_APP notification as read.
     */
    @Override
    @Transactional
    public AdminInAppNotificationResponse
    markAdminNotificationRead(

            UUID adminUserId,

            UUID notificationId
    ) {
        Notification notification =
                findAdminNotification(
                        adminUserId,
                        notificationId
                );

        notification.markRead();

        Notification savedNotification =
                notificationRepository.saveAndFlush(
                        notification
                );

        return notificationMapper
                .toAdminInAppResponse(
                        savedNotification
                );
    }

    // =================================================================
    // MARK UNREAD
    // =================================================================

    /**
     * Marks one administrator IN_APP notification as unread.
     */
    @Override
    @Transactional
    public AdminInAppNotificationResponse
    markAdminNotificationUnread(

            UUID adminUserId,

            UUID notificationId
    ) {
        Notification notification =
                findAdminNotification(
                        adminUserId,
                        notificationId
                );

        notification.markUnread();

        Notification savedNotification =
                notificationRepository.saveAndFlush(
                        notification
                );

        return notificationMapper
                .toAdminInAppResponse(
                        savedNotification
                );
    }

    // =================================================================
    // DISMISS
    // =================================================================

    /**
     * Dismisses one administrator IN_APP notification.
     */
    @Override
    @Transactional
    public AdminInAppNotificationResponse
    dismissAdminNotification(

            UUID adminUserId,

            UUID notificationId
    ) {
        Notification notification =
                findAdminNotification(
                        adminUserId,
                        notificationId
                );

        notification.dismiss();

        Notification savedNotification =
                notificationRepository.saveAndFlush(
                        notification
                );

        return notificationMapper
                .toAdminInAppResponse(
                        savedNotification
                );
    }

    // =================================================================
    // RESTORE
    // =================================================================

    /**
     * Restores one dismissed administrator IN_APP notification.
     */
    @Override
    @Transactional
    public AdminInAppNotificationResponse
    restoreAdminNotification(

            UUID adminUserId,

            UUID notificationId
    ) {
        Notification notification =
                findAdminNotification(
                        adminUserId,
                        notificationId
                );

        notification.restore();

        Notification savedNotification =
                notificationRepository.saveAndFlush(
                        notification
                );

        return notificationMapper
                .toAdminInAppResponse(
                        savedNotification
                );
    }

    // =================================================================
    // PROVIDER DELIVERED CALLBACK
    // =================================================================

    /**
     * Marks one externally delivered EMAIL/SMS notification as
     * DELIVERED.
     */
    @Override
    @Transactional
    public NotificationResponse markProviderDelivered(

            String providerMessageId,

            Instant deliveredAt
    ) {
        Notification notification =
                findByProviderMessageId(
                        providerMessageId
                );

        notification.markDelivered(
                deliveredAt
        );

        Notification savedNotification =
                notificationRepository.saveAndFlush(
                        notification
                );

        return notificationMapper.toResponse(
                savedNotification
        );
    }

    // =================================================================
    // PROVIDER FAILED CALLBACK
    // =================================================================

    /**
     * Records an externally reported provider delivery failure.
     */
    @Override
    @Transactional
    public NotificationResponse markProviderFailed(

            String providerMessageId,

            String failureCode,

            String failureMessage
    ) {
        Notification notification =
                findByProviderMessageId(
                        providerMessageId
                );

        notification.markFailed(
                normalizeOptional(
                        failureCode
                ),

                resolveFailureMessage(
                        failureMessage,
                        "The notification provider reported a delivery failure."
                ),

                null
        );

        Notification savedNotification =
                notificationRepository.saveAndFlush(
                        notification
                );

        return notificationMapper.toResponse(
                savedNotification
        );
    }

    // =================================================================
    // INTERNAL PERSISTENCE
    // =================================================================

    /**
     * Creates a PENDING EMAIL or SMS notification.
     *
     * This private helper always executes inside the transaction created
     * by the public sendEmail/sendSms operation.
     */
    private Notification savePendingNotification(
            NotificationCreateRequest request
    ) {
        Notification notification =
                notificationMapper.toEntity(
                        request
                );

        Notification savedNotification =
                notificationRepository.saveAndFlush(
                        notification
                );

        log.debug(
                "Pending notification created. notificationId={}, channel={}, resourceType={}, resourceId={}",
                savedNotification.getNotificationId(),
                savedNotification.getChannel(),
                savedNotification.getResourceType(),
                savedNotification.getResourceId()
        );

        return savedNotification;
    }

    // =================================================================
    // FIND NOTIFICATION
    // =================================================================

    private Notification findNotification(
            UUID notificationId
    ) {
        requireId(
                notificationId,
                "Notification ID is required."
        );

        return notificationRepository
                .findById(
                        notificationId
                )
                .orElseThrow(
                        () ->
                                new PublicRequestRejectedException(
                                        HttpStatus.NOT_FOUND,
                                        "Notification was not found."
                                )
                );
    }

    // =================================================================
    // FIND ADMIN NOTIFICATION
    // =================================================================

    private Notification findAdminNotification(

            UUID adminUserId,

            UUID notificationId
    ) {
        requireId(
                adminUserId,
                "Administrator ID is required."
        );

        requireId(
                notificationId,
                "Notification ID is required."
        );

        Notification notification =
                notificationRepository
                        .findByNotificationIdAndAdminUserId(
                                notificationId,
                                adminUserId
                        )
                        .orElseThrow(
                                () ->
                                        new PublicRequestRejectedException(
                                                HttpStatus.NOT_FOUND,
                                                "Administrator notification was not found."
                                        )
                        );

        if (
                notification.getChannel()
                        != NotificationChannel.IN_APP
        ) {
            throw new PublicRequestRejectedException(
                    HttpStatus.CONFLICT,
                    "The selected notification is not an in-app notification."
            );
        }

        return notification;
    }

    // =================================================================
    // FIND PROVIDER NOTIFICATION
    // =================================================================

    private Notification findByProviderMessageId(
            String providerMessageId
    ) {
        String normalizedProviderMessageId =
                requireText(
                        providerMessageId,
                        "Provider message ID is required."
                );

        return notificationRepository
                .findByProviderMessageId(
                        normalizedProviderMessageId
                )
                .orElseThrow(
                        () ->
                                new PublicRequestRejectedException(
                                        HttpStatus.NOT_FOUND,
                                        "Notification provider message was not found."
                                )
                );
    }

    // =================================================================
    // VALIDATION
    // =================================================================

    private void requireChannel(

            NotificationCreateRequest request,

            NotificationChannel expectedChannel
    ) {
        if (request == null) {
            throw new IllegalArgumentException(
                    "Notification request is required."
            );
        }

        if (
                request.channel()
                        != expectedChannel
        ) {
            throw new IllegalArgumentException(
                    "Notification channel must be "
                            + expectedChannel
                            + "."
            );
        }
    }

    private void requirePageable(
            Pageable pageable
    ) {
        if (pageable == null) {
            throw new IllegalArgumentException(
                    "Notification pagination information is required."
            );
        }
    }

    private void requireId(
            UUID id,
            String message
    ) {
        if (id == null) {
            throw new IllegalArgumentException(
                    message
            );
        }
    }

    private String requireText(

            String value,

            String message
    ) {
        String normalized =
                normalizeOptional(
                        value
                );

        if (normalized == null) {
            throw new IllegalArgumentException(
                    message
            );
        }

        return normalized;
    }

    // =================================================================
    // NORMALIZATION
    // =================================================================

    private String normalizeProviderName(

            String value,

            String fallback
    ) {
        String normalized =
                normalizeOptional(
                        value
                );

        return normalized == null
                ? fallback
                : normalized;
    }

    private String resolveFailureMessage(

            String value,

            String fallback
    ) {
        String normalized =
                normalizeOptional(
                        value
                );

        return normalized == null
                ? fallback
                : safeFailureMessage(
                normalized,
                fallback
        );
    }

    private String safeFailureMessage(

            String value,

            String fallback
    ) {
        String normalized =
                normalizeOptional(
                        value
                );

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
}