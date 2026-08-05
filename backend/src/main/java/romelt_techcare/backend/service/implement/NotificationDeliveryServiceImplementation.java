package romelt_techcare.backend.service.implement;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import romelt_techcare.backend.dto.AdminJwtPrincipal;
import romelt_techcare.backend.dto.NotificationDeliveryCreateRequest;
import romelt_techcare.backend.dto.NotificationDeliveryResponse;
import romelt_techcare.backend.dto.NotificationDeliverySummaryResponse;
import romelt_techcare.backend.entity.NotificationDelivery;
import romelt_techcare.backend.enums.NotificationChannel;
import romelt_techcare.backend.enums.NotificationDeliveryStatus;
import romelt_techcare.backend.exception.AdminAuthenticationException;
import romelt_techcare.backend.exception.PublicRequestRejectedException;
import romelt_techcare.backend.mapper.NotificationDeliveryMapper;
import romelt_techcare.backend.repository.NotificationDeliveryRepository;
import romelt_techcare.backend.service.NotificationDeliveryService;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * ================================================================
 * ROMELT TECHCARE — NOTIFICATION DELIVERY SERVICE IMPLEMENTATION
 * ================================================================
 *
 * Purpose:
 * Implements EMAIL, SMS, and administrator IN_APP delivery workflows.
 *
 * Important:
 * Provider-attempt persistence will be handled by the separate
 * NotificationDeliveryAttempt service.
 * ================================================================
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationDeliveryServiceImplementation
        implements NotificationDeliveryService {

    private static final int DEFAULT_BATCH_SIZE = 20;
    private static final int MAXIMUM_BATCH_SIZE = 100;

    private static final Duration DEFAULT_LEASE_DURATION =
            Duration.ofMinutes(5);

    private static final Duration DEFAULT_RETRY_DELAY =
            Duration.ofMinutes(5);

    private final NotificationDeliveryRepository
            notificationDeliveryRepository;

    private final NotificationDeliveryMapper
            notificationDeliveryMapper;

    @Override
    @Transactional
    public NotificationDeliveryResponse createDelivery(
            NotificationDeliveryCreateRequest request
    ) {
        if (request == null) {
            reject(
                    HttpStatus.BAD_REQUEST,
                    "Notification delivery information is required."
            );
        }

        NotificationDelivery delivery =
                notificationDeliveryMapper.toEntity(request);

        boolean duplicate =
                notificationDeliveryRepository
                        .existsByNotificationEventIdAndChannelAndNormalizedRecipientAddress(
                                delivery.getNotificationEventId(),
                                delivery.getChannel(),
                                delivery.getNormalizedRecipientAddress()
                        );

        if (duplicate) {
            reject(
                    HttpStatus.CONFLICT,
                    "The notification event already has a delivery for this channel and recipient."
            );
        }

        try {
            NotificationDelivery saved =
                    notificationDeliveryRepository.saveAndFlush(
                            delivery
                    );

            log.info(
                    "Notification delivery created. notificationDeliveryId={}, notificationEventId={}, channel={}, recipientAdminUserId={}",
                    saved.getNotificationDeliveryId(),
                    saved.getNotificationEventId(),
                    saved.getChannel(),
                    saved.getRecipientAdminUserId()
            );

            return notificationDeliveryMapper.toResponse(saved);

        } catch (DataIntegrityViolationException exception) {
            throw new PublicRequestRejectedException(
                    HttpStatus.CONFLICT,
                    "The notification delivery conflicts with an existing delivery."
            );
        }
    }

    @Override
    @Transactional(readOnly = true)
    public NotificationDeliveryResponse getDelivery(
            UUID notificationDeliveryId
    ) {
        return notificationDeliveryMapper.toResponse(
                findDelivery(notificationDeliveryId)
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationDeliveryResponse> getDeliveriesForEvent(
            UUID notificationEventId
    ) {
        requireId(
                notificationEventId,
                "Notification event ID is required."
        );

        return notificationDeliveryRepository
                .findByNotificationEventIdOrderByCreatedAtAsc(
                        notificationEventId
                )
                .stream()
                .map(notificationDeliveryMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<NotificationDeliverySummaryResponse> getDeliveries(
            String keyword,
            NotificationChannel channel,
            NotificationDeliveryStatus deliveryStatus,
            UUID notificationEventId,
            UUID customerId,
            UUID recipientAdminUserId,
            Instant createdFrom,
            Instant createdTo,
            Pageable pageable
    ) {
        requirePageable(pageable);

        if (
                createdFrom != null
                        && createdTo != null
                        && createdTo.isBefore(createdFrom)
        ) {
            reject(
                    HttpStatus.BAD_REQUEST,
                    "Delivery creation end time cannot be before its start time."
            );
        }

        return notificationDeliveryRepository
                .searchDeliveries(
                        normalizeOptional(keyword),
                        channel,
                        deliveryStatus,
                        notificationEventId,
                        customerId,
                        recipientAdminUserId,
                        createdFrom,
                        createdTo,
                        pageable
                )
                .map(
                        notificationDeliveryMapper
                                ::toSummaryResponse
                );
    }

    @Override
    @Transactional
    public List<NotificationDelivery> claimAvailableDeliveries(
            String workerId,
            int batchSize,
            Duration leaseDuration
    ) {
        String resolvedWorkerId =
                requireWorkerId(workerId);

        Duration resolvedLease =
                resolvePositiveDuration(
                        leaseDuration,
                        DEFAULT_LEASE_DURATION,
                        "Delivery worker lease duration must be positive."
                );

        List<NotificationDelivery> deliveries =
                notificationDeliveryRepository
                        .claimAvailableDeliveries(
                                Instant.now(),
                                resolveBatchSize(batchSize)
                        );

        deliveries.forEach(
                delivery ->
                        delivery.claim(
                                resolvedWorkerId,
                                resolvedLease
                        )
        );

        if (!deliveries.isEmpty()) {
            notificationDeliveryRepository.saveAll(deliveries);
            notificationDeliveryRepository.flush();
        }

        return deliveries;
    }

    @Override
    @Transactional
    public List<NotificationDelivery> recoverExpiredLeaseDeliveries(
            int batchSize,
            Duration retryDelay
    ) {
        Instant now = Instant.now();

        Duration resolvedDelay =
                resolvePositiveDuration(
                        retryDelay,
                        DEFAULT_RETRY_DELAY,
                        "Delivery retry delay must be positive."
                );

        List<NotificationDelivery> deliveries =
                notificationDeliveryRepository
                        .claimExpiredLeaseDeliveries(
                                now,
                                resolveBatchSize(batchSize)
                        );

        deliveries.forEach(
                delivery ->
                        delivery.recoverExpiredLease(
                                now.plus(resolvedDelay)
                        )
        );

        if (!deliveries.isEmpty()) {
            notificationDeliveryRepository.saveAll(deliveries);
            notificationDeliveryRepository.flush();
        }

        return deliveries;
    }

    @Override
    @Transactional
    public void renewLease(
            UUID notificationDeliveryId,
            String workerId,
            Duration leaseDuration
    ) {
        String resolvedWorkerId =
                requireWorkerId(workerId);

        NotificationDelivery delivery =
                findDeliveryForUpdate(notificationDeliveryId);

        requireLeaseOwner(delivery, resolvedWorkerId);

        delivery.renewLease(
                resolvedWorkerId,
                resolvePositiveDuration(
                        leaseDuration,
                        DEFAULT_LEASE_DURATION,
                        "Delivery worker lease duration must be positive."
                )
        );

        notificationDeliveryRepository.saveAndFlush(delivery);
    }

    @Override
    @Transactional
    public int beginAttempt(
            UUID notificationDeliveryId,
            String workerId
    ) {
        String resolvedWorkerId =
                requireWorkerId(workerId);

        NotificationDelivery delivery =
                findDeliveryForUpdate(notificationDeliveryId);

        requireLeaseOwner(delivery, resolvedWorkerId);

        int attemptNumber = delivery.beginAttempt();

        notificationDeliveryRepository.saveAndFlush(delivery);

        return attemptNumber;
    }

    @Override
    @Transactional
    public void markSent(
            UUID notificationDeliveryId,
            String workerId,
            String providerName,
            String providerMessageId,
            JsonNode providerResponse
    ) {
        String resolvedWorkerId =
                requireWorkerId(workerId);

        NotificationDelivery delivery =
                findDeliveryForUpdate(notificationDeliveryId);

        requireLeaseOwner(delivery, resolvedWorkerId);

        delivery.markSent(
                providerName,
                providerMessageId,
                providerResponse
        );

        notificationDeliveryRepository.saveAndFlush(delivery);
    }

    @Override
    @Transactional
    public void markInAppPublished(
            UUID notificationDeliveryId,
            String workerId
    ) {
        String resolvedWorkerId =
                requireWorkerId(workerId);

        NotificationDelivery delivery =
                findDeliveryForUpdate(notificationDeliveryId);

        requireLeaseOwner(delivery, resolvedWorkerId);

        delivery.markInAppPublished();

        notificationDeliveryRepository.saveAndFlush(delivery);
    }

    @Override
    @Transactional
    public void scheduleRetry(
            UUID notificationDeliveryId,
            String workerId,
            String failureCode,
            String failureMessage,
            Instant retryAt,
            JsonNode providerResponse
    ) {
        String resolvedWorkerId =
                requireWorkerId(workerId);

        NotificationDelivery delivery =
                findDeliveryForUpdate(notificationDeliveryId);

        requireLeaseOwner(delivery, resolvedWorkerId);

        delivery.scheduleRetry(
                failureCode,
                failureMessage,
                retryAt,
                providerResponse
        );

        notificationDeliveryRepository.saveAndFlush(delivery);
    }

    @Override
    @Transactional
    public void markFailed(
            UUID notificationDeliveryId,
            String workerId,
            String failureCode,
            String failureMessage,
            JsonNode providerResponse
    ) {
        String resolvedWorkerId =
                requireWorkerId(workerId);

        NotificationDelivery delivery =
                findDeliveryForUpdate(notificationDeliveryId);

        requireLeaseOwner(delivery, resolvedWorkerId);

        delivery.markFailed(
                failureCode,
                failureMessage,
                providerResponse
        );

        notificationDeliveryRepository.saveAndFlush(delivery);
    }

    @Override
    @Transactional
    public void markDelivered(
            UUID notificationDeliveryId,
            Instant deliveredAt
    ) {
        NotificationDelivery delivery =
                findDeliveryForUpdate(notificationDeliveryId);

        delivery.markDelivered(deliveredAt);

        notificationDeliveryRepository.saveAndFlush(delivery);
    }

    @Override
    @Transactional
    public void markOpened(
            UUID notificationDeliveryId,
            Instant openedAt
    ) {
        NotificationDelivery delivery =
                findDeliveryForUpdate(notificationDeliveryId);

        delivery.markOpened(openedAt);

        notificationDeliveryRepository.saveAndFlush(delivery);
    }

    @Override
    @Transactional
    public void markClicked(
            UUID notificationDeliveryId,
            Instant clickedAt
    ) {
        NotificationDelivery delivery =
                findDeliveryForUpdate(notificationDeliveryId);

        delivery.markClicked(clickedAt);

        notificationDeliveryRepository.saveAndFlush(delivery);
    }

    @Override
    @Transactional
    public void markBounced(
            UUID notificationDeliveryId,
            String failureCode,
            String failureMessage,
            Instant bouncedAt
    ) {
        NotificationDelivery delivery =
                findDeliveryForUpdate(notificationDeliveryId);

        delivery.markBounced(
                failureCode,
                failureMessage,
                bouncedAt
        );

        notificationDeliveryRepository.saveAndFlush(delivery);
    }

    @Override
    @Transactional
    public NotificationDeliveryResponse cancelDelivery(
            UUID notificationDeliveryId,
            String reason
    ) {
        NotificationDelivery delivery =
                findDeliveryForUpdate(notificationDeliveryId);

        delivery.cancel(reason);

        return notificationDeliveryMapper.toResponse(
                notificationDeliveryRepository.saveAndFlush(
                        delivery
                )
        );
    }

    @Override
    @Transactional
    public NotificationDeliveryResponse suppressDelivery(
            UUID notificationDeliveryId,
            String reason
    ) {
        NotificationDelivery delivery =
                findDeliveryForUpdate(notificationDeliveryId);

        delivery.suppress(reason);

        return notificationDeliveryMapper.toResponse(
                notificationDeliveryRepository.saveAndFlush(
                        delivery
                )
        );
    }

    @Override
    @Transactional(readOnly = true)
    public Page<NotificationDeliverySummaryResponse>
    getMyInAppNotifications(
            AdminJwtPrincipal principal,
            boolean unreadOnly,
            Pageable pageable
    ) {
        UUID administratorId =
                requireAdministratorId(principal);

        requirePageable(pageable);

        return notificationDeliveryRepository
                .findAdministratorInAppNotifications(
                        administratorId,
                        unreadOnly,
                        pageable
                )
                .map(
                        notificationDeliveryMapper
                                ::toSummaryResponse
                );
    }

    @Override
    @Transactional(readOnly = true)
    public Page<NotificationDeliverySummaryResponse>
    getMyDismissedNotifications(
            AdminJwtPrincipal principal,
            Pageable pageable
    ) {
        UUID administratorId =
                requireAdministratorId(principal);

        requirePageable(pageable);

        return notificationDeliveryRepository
                .findDismissedInAppNotifications(
                        administratorId,
                        pageable
                )
                .map(
                        notificationDeliveryMapper
                                ::toSummaryResponse
                );
    }

    @Override
    @Transactional(readOnly = true)
    public long countMyUnreadNotifications(
            AdminJwtPrincipal principal
    ) {
        return notificationDeliveryRepository
                .countUnreadInAppNotifications(
                        requireAdministratorId(principal)
                );
    }

    @Override
    @Transactional
    public NotificationDeliveryResponse markMyNotificationRead(
            AdminJwtPrincipal principal,
            UUID notificationDeliveryId
    ) {
        NotificationDelivery delivery =
                findOwnedInAppDelivery(
                        principal,
                        notificationDeliveryId
                );

        delivery.markRead(Instant.now());

        return saveResponse(delivery);
    }

    @Override
    @Transactional
    public NotificationDeliveryResponse markMyNotificationUnread(
            AdminJwtPrincipal principal,
            UUID notificationDeliveryId
    ) {
        NotificationDelivery delivery =
                findOwnedInAppDelivery(
                        principal,
                        notificationDeliveryId
                );

        delivery.markUnread();

        return saveResponse(delivery);
    }

    @Override
    @Transactional
    public NotificationDeliveryResponse dismissMyNotification(
            AdminJwtPrincipal principal,
            UUID notificationDeliveryId
    ) {
        NotificationDelivery delivery =
                findOwnedInAppDelivery(
                        principal,
                        notificationDeliveryId
                );

        delivery.dismiss(Instant.now());

        return saveResponse(delivery);
    }

    @Override
    @Transactional
    public NotificationDeliveryResponse
    restoreMyDismissedNotification(
            AdminJwtPrincipal principal,
            UUID notificationDeliveryId
    ) {
        NotificationDelivery delivery =
                findOwnedInAppDelivery(
                        principal,
                        notificationDeliveryId
                );

        delivery.restoreDismissed();

        return saveResponse(delivery);
    }

    private NotificationDeliveryResponse saveResponse(
            NotificationDelivery delivery
    ) {
        return notificationDeliveryMapper.toResponse(
                notificationDeliveryRepository.saveAndFlush(
                        delivery
                )
        );
    }

    private NotificationDelivery findOwnedInAppDelivery(
            AdminJwtPrincipal principal,
            UUID notificationDeliveryId
    ) {
        UUID administratorId =
                requireAdministratorId(principal);

        NotificationDelivery delivery =
                findDeliveryForUpdate(notificationDeliveryId);

        if (delivery.getChannel() != NotificationChannel.IN_APP) {
            reject(
                    HttpStatus.CONFLICT,
                    "The notification is not an in-app notification."
            );
        }

        if (
                delivery.getRecipientAdminUserId() == null
                        || !delivery
                        .getRecipientAdminUserId()
                        .equals(administratorId)
        ) {
            reject(
                    HttpStatus.FORBIDDEN,
                    "The notification does not belong to the authenticated administrator."
            );
        }

        return delivery;
    }

    private NotificationDelivery findDelivery(
            UUID notificationDeliveryId
    ) {
        requireId(
                notificationDeliveryId,
                "Notification delivery ID is required."
        );

        return notificationDeliveryRepository
                .findById(notificationDeliveryId)
                .orElseThrow(
                        () ->
                                new PublicRequestRejectedException(
                                        HttpStatus.NOT_FOUND,
                                        "Notification delivery was not found."
                                )
                );
    }

    private NotificationDelivery findDeliveryForUpdate(
            UUID notificationDeliveryId
    ) {
        requireId(
                notificationDeliveryId,
                "Notification delivery ID is required."
        );

        return notificationDeliveryRepository
                .findByIdForUpdate(notificationDeliveryId)
                .orElseThrow(
                        () ->
                                new PublicRequestRejectedException(
                                        HttpStatus.NOT_FOUND,
                                        "Notification delivery was not found."
                                )
                );
    }

    private void requireLeaseOwner(
            NotificationDelivery delivery,
            String workerId
    ) {
        if (
                delivery.getDeliveryStatus()
                        != NotificationDeliveryStatus.PROCESSING
        ) {
            reject(
                    HttpStatus.CONFLICT,
                    "Notification delivery is not currently processing."
            );
        }

        if (
                delivery.getLockedBy() == null
                        || !delivery.getLockedBy().equals(workerId)
        ) {
            reject(
                    HttpStatus.CONFLICT,
                    "Notification delivery is owned by another worker."
            );
        }

        if (
                delivery.getLockExpiresAt() == null
                        || !delivery.getLockExpiresAt()
                        .isAfter(Instant.now())
        ) {
            reject(
                    HttpStatus.CONFLICT,
                    "Notification delivery worker lease has expired."
            );
        }
    }

    private UUID requireAdministratorId(
            AdminJwtPrincipal principal
    ) {
        if (
                principal == null
                        || principal.adminUserId() == null
        ) {
            throw AdminAuthenticationException
                    .staleAuthentication();
        }

        return principal.adminUserId();
    }

    private String requireWorkerId(
            String workerId
    ) {
        String normalized =
                normalizeOptional(workerId);

        if (normalized == null) {
            reject(
                    HttpStatus.BAD_REQUEST,
                    "Notification delivery worker ID is required."
            );
        }

        if (normalized.length() > 160) {
            reject(
                    HttpStatus.BAD_REQUEST,
                    "Notification delivery worker ID cannot exceed 160 characters."
            );
        }

        return normalized;
    }

    private int resolveBatchSize(
            int batchSize
    ) {
        if (batchSize <= 0) {
            return DEFAULT_BATCH_SIZE;
        }

        return Math.min(
                batchSize,
                MAXIMUM_BATCH_SIZE
        );
    }

    private Duration resolvePositiveDuration(
            Duration requested,
            Duration defaultValue,
            String errorMessage
    ) {
        if (requested == null) {
            return defaultValue;
        }

        if (
                requested.isZero()
                        || requested.isNegative()
        ) {
            reject(
                    HttpStatus.BAD_REQUEST,
                    errorMessage
            );
        }

        return requested;
    }

    private void requirePageable(
            Pageable pageable
    ) {
        if (pageable == null) {
            throw new IllegalArgumentException(
                    "Notification delivery pagination information is required."
            );
        }
    }

    private void requireId(
            UUID id,
            String message
    ) {
        if (id == null) {
            reject(
                    HttpStatus.BAD_REQUEST,
                    message
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

    private void reject(
            HttpStatus status,
            String message
    ) {
        throw new PublicRequestRejectedException(
                status,
                message
        );
    }
}