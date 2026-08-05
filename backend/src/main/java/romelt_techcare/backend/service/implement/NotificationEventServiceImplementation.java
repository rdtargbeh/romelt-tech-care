package romelt_techcare.backend.service.implement;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import romelt_techcare.backend.dto.NotificationEventCreateRequest;
import romelt_techcare.backend.dto.NotificationEventResponse;
import romelt_techcare.backend.dto.NotificationEventSummaryResponse;
import romelt_techcare.backend.entity.NotificationEvent;
import romelt_techcare.backend.enums.NotificationEventStatus;
import romelt_techcare.backend.enums.NotificationEventType;
import romelt_techcare.backend.enums.NotificationResourceType;
import romelt_techcare.backend.exception.PublicRequestRejectedException;
import romelt_techcare.backend.mapper.NotificationEventMapper;
import romelt_techcare.backend.repository.NotificationEventRepository;
import romelt_techcare.backend.service.NotificationEventService;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * ================================================================
 * ROMELT TECHCARE — NOTIFICATION EVENT SERVICE IMPLEMENTATION
 * ================================================================
 *
 * Purpose:
 * Implements the production transactional notification-outbox
 * workflow.
 *
 * Responsibilities:
 * - Creates committed notification events.
 * - Prevents duplicate logical events using idempotency keys.
 * - Retrieves notification events for administrator interfaces.
 * - Claims available events safely for background workers.
 * - Applies expiring worker leases.
 * - Recovers abandoned processing events.
 * - Renews active leases.
 * - Marks events processed.
 * - Schedules temporary failures for retry.
 * - Marks permanent failures.
 * - Cancels pending or processing events.
 *
 * Transactional-outbox behavior:
 * Business services call createEvent() inside the same transaction as
 * the related booking, contact, review, customer, or other operation.
 *
 * If the originating business transaction rolls back, the
 * NotificationEvent insert also rolls back.
 *
 * Worker behavior:
 * - claimAvailableEvents() uses PostgreSQL FOR UPDATE SKIP LOCKED.
 * - Every claimed event is changed to PROCESSING before commit.
 * - The worker must later call markProcessed(), scheduleRetry(), or
 *   markFailed().
 *
 * Idempotency behavior:
 * - A non-null idempotency key identifies one logical event.
 * - When the key already exists, the existing event is returned.
 * - A database uniqueness violation caused by concurrent creation is
 *   resolved by loading the event that won the race.
 *
 * Important:
 * This service does not render templates or contact an email/SMS
 * provider. Those responsibilities belong to the delivery worker and
 * provider services.
 * ================================================================
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationEventServiceImplementation  implements NotificationEventService {

    private static final int DEFAULT_BATCH_SIZE = 20;
    private static final int MAXIMUM_BATCH_SIZE = 100;

    private static final Duration DEFAULT_LEASE_DURATION =
            Duration.ofMinutes(5);

    private static final Duration DEFAULT_RETRY_DELAY =
            Duration.ofMinutes(5);

    private final NotificationEventRepository
            notificationEventRepository;

    private final NotificationEventMapper
            notificationEventMapper;

    /**
     * Creates one notification event.
     *
     * The method is intentionally transactional so business services
     * may call it inside their own transaction and receive the same
     * commit-or-rollback behavior.
     */
    @Override
    @Transactional
    public NotificationEventResponse createEvent(
            NotificationEventCreateRequest request
    ) {
        requireCreateRequest(request);

        String idempotencyKey =
                normalizeOptional(
                        request.idempotencyKey()
                );

        if (idempotencyKey != null) {
            NotificationEvent existingEvent =
                    notificationEventRepository
                            .findByIdempotencyKey(
                                    idempotencyKey
                            )
                            .orElse(null);

            if (existingEvent != null) {
                log.debug(
                        "Notification event already exists for idempotency key. notificationEventId={}, idempotencyKey={}",
                        existingEvent.getNotificationEventId(),
                        idempotencyKey
                );

                return notificationEventMapper
                        .toResponse(existingEvent);
            }
        }

        NotificationEvent notificationEvent =
                notificationEventMapper.toEntity(
                        request
                );

        try {
            NotificationEvent savedEvent =
                    notificationEventRepository
                            .saveAndFlush(
                                    notificationEvent
                            );

            log.info(
                    "Notification event created. notificationEventId={}, eventType={}, resourceType={}, resourceId={}, recipientType={}, eventStatus={}, idempotencyKey={}",
                    savedEvent.getNotificationEventId(),
                    savedEvent.getEventType(),
                    savedEvent.getResourceType(),
                    savedEvent.getResourceId(),
                    savedEvent.getRecipientType(),
                    savedEvent.getEventStatus(),
                    savedEvent.getIdempotencyKey()
            );

            return notificationEventMapper
                    .toResponse(savedEvent);

        } catch (DataIntegrityViolationException exception) {
            /*
             * Two transactions may attempt to insert the same
             * idempotency key at nearly the same time.
             *
             * The unique constraint determines the winner. The losing
             * transaction retrieves and returns the existing event.
             */
            if (idempotencyKey != null) {
                NotificationEvent existingEvent =
                        notificationEventRepository
                                .findByIdempotencyKey(
                                        idempotencyKey
                                )
                                .orElse(null);

                if (existingEvent != null) {
                    log.debug(
                            "Concurrent notification event creation resolved through idempotency. notificationEventId={}, idempotencyKey={}",
                            existingEvent.getNotificationEventId(),
                            idempotencyKey
                    );

                    return notificationEventMapper
                            .toResponse(existingEvent);
                }
            }

            throw exception;
        }
    }

    /**
     * Returns one complete event.
     */
    @Override
    @Transactional(readOnly = true)
    public NotificationEventResponse getEvent(
            UUID notificationEventId
    ) {
        NotificationEvent notificationEvent =
                findEvent(notificationEventId);

        return notificationEventMapper
                .toResponse(notificationEvent);
    }

    /**
     * Returns an administrator-searchable event list.
     */
    @Override
    @Transactional(readOnly = true)
    public Page<NotificationEventSummaryResponse> getEvents(
            String keyword,
            NotificationEventStatus eventStatus,
            NotificationEventType eventType,
            NotificationResourceType resourceType,
            UUID resourceId,
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
                    "Notification creation end time cannot be before the start time."
            );
        }

        return notificationEventRepository
                .searchNotificationEvents(
                        normalizeOptional(keyword),
                        eventStatus,
                        eventType,
                        resourceType,
                        resourceId,
                        customerId,
                        recipientAdminUserId,
                        createdFrom,
                        createdTo,
                        pageable
                )
                .map(
                        notificationEventMapper
                                ::toSummaryResponse
                );
    }

    /**
     * Returns all events associated with one business resource.
     */
    @Override
    @Transactional(readOnly = true)
    public List<NotificationEventResponse> getEventsForResource(
            NotificationResourceType resourceType,
            UUID resourceId
    ) {
        if (resourceType == null) {
            reject(
                    HttpStatus.BAD_REQUEST,
                    "Notification resource type is required."
            );
        }

        if (resourceId == null) {
            reject(
                    HttpStatus.BAD_REQUEST,
                    "Notification resource ID is required."
            );
        }

        return notificationEventRepository
                .findByResourceTypeAndResourceIdOrderByCreatedAtDesc(
                        resourceType,
                        resourceId
                )
                .stream()
                .map(notificationEventMapper::toResponse)
                .toList();
    }

    /**
     * Claims available events and applies worker leases.
     *
     * PostgreSQL row locks remain active until this method's
     * transaction commits.
     */
    @Override
    @Transactional
    public List<NotificationEvent> claimAvailableEvents(
            String workerId,
            int batchSize,
            Duration leaseDuration
    ) {
        String normalizedWorkerId =
                requireWorkerId(workerId);

        int resolvedBatchSize =
                resolveBatchSize(batchSize);

        Duration resolvedLeaseDuration =
                resolveLeaseDuration(
                        leaseDuration
                );

        Instant now = Instant.now();

        List<NotificationEvent> availableEvents =
                notificationEventRepository
                        .claimAvailableEvents(
                                now,
                                resolvedBatchSize
                        );

        for (NotificationEvent event : availableEvents) {
            event.claim(
                    normalizedWorkerId,
                    resolvedLeaseDuration
            );
        }

        if (!availableEvents.isEmpty()) {
            notificationEventRepository
                    .saveAll(availableEvents);

            notificationEventRepository.flush();

            log.debug(
                    "Notification events claimed. workerId={}, eventCount={}, leaseDurationSeconds={}",
                    normalizedWorkerId,
                    availableEvents.size(),
                    resolvedLeaseDuration.toSeconds()
            );
        }

        return availableEvents;
    }

    /**
     * Recovers abandoned PROCESSING events after their leases expire.
     */
    @Override
    @Transactional
    public List<NotificationEvent> recoverExpiredLeaseEvents(
            int batchSize,
            Duration retryDelay
    ) {
        int resolvedBatchSize =
                resolveBatchSize(batchSize);

        Duration resolvedRetryDelay =
                resolveRetryDelay(retryDelay);

        Instant now = Instant.now();

        List<NotificationEvent> expiredEvents =
                notificationEventRepository
                        .claimExpiredLeaseEvents(
                                now,
                                resolvedBatchSize
                        );

        for (NotificationEvent event : expiredEvents) {
            event.recoverExpiredLease(
                    now.plus(resolvedRetryDelay),
                    "Notification processing worker lease expired."
            );
        }

        if (!expiredEvents.isEmpty()) {
            notificationEventRepository
                    .saveAll(expiredEvents);

            notificationEventRepository.flush();

            log.warn(
                    "Expired notification worker leases recovered. eventCount={}, retryDelaySeconds={}",
                    expiredEvents.size(),
                    resolvedRetryDelay.toSeconds()
            );
        }

        return expiredEvents;
    }

    /**
     * Renews an active worker lease.
     */
    @Override
    @Transactional
    public void renewLease(
            UUID notificationEventId,
            String workerId,
            Duration leaseDuration
    ) {
        String normalizedWorkerId =
                requireWorkerId(workerId);

        Duration resolvedLeaseDuration =
                resolveLeaseDuration(
                        leaseDuration
                );

        NotificationEvent event =
                findEventForUpdate(
                        notificationEventId
                );

        requireLeaseOwner(
                event,
                normalizedWorkerId
        );

        event.renewLease(
                normalizedWorkerId,
                resolvedLeaseDuration
        );

        notificationEventRepository
                .saveAndFlush(event);

        log.debug(
                "Notification worker lease renewed. notificationEventId={}, workerId={}, leaseDurationSeconds={}",
                event.getNotificationEventId(),
                normalizedWorkerId,
                resolvedLeaseDuration.toSeconds()
        );
    }

    /**
     * Marks an event processed.
     */
    @Override
    @Transactional
    public void markProcessed(
            UUID notificationEventId,
            String workerId
    ) {
        String normalizedWorkerId =
                requireWorkerId(workerId);

        NotificationEvent event =
                findEventForUpdate(
                        notificationEventId
                );

        requireLeaseOwner(
                event,
                normalizedWorkerId
        );

        event.markProcessed();

        notificationEventRepository
                .saveAndFlush(event);

        log.info(
                "Notification event processed. notificationEventId={}, eventType={}, resourceType={}, resourceId={}",
                event.getNotificationEventId(),
                event.getEventType(),
                event.getResourceType(),
                event.getResourceId()
        );
    }

    /**
     * Schedules another processing attempt.
     */
    @Override
    @Transactional
    public void scheduleRetry(
            UUID notificationEventId,
            String workerId,
            String failureCode,
            String failureReason,
            Instant retryAt
    ) {
        String normalizedWorkerId =
                requireWorkerId(workerId);

        requireFailureReason(failureReason);

        if (retryAt == null) {
            reject(
                    HttpStatus.BAD_REQUEST,
                    "Notification retry time is required."
            );
        }

        NotificationEvent event =
                findEventForUpdate(
                        notificationEventId
                );

        requireLeaseOwner(
                event,
                normalizedWorkerId
        );

        event.scheduleRetry(
                normalizeOptional(failureCode),
                failureReason,
                retryAt
        );

        notificationEventRepository
                .saveAndFlush(event);

        log.warn(
                "Notification event processing failed. notificationEventId={}, eventStatus={}, attemptCount={}, maximumAttempts={}, nextAttemptAt={}, failureCode={}",
                event.getNotificationEventId(),
                event.getEventStatus(),
                event.getAttemptCount(),
                event.getMaximumAttempts(),
                event.getNextAttemptAt(),
                event.getFailureCode()
        );
    }

    /**
     * Marks an event permanently failed.
     */
    @Override
    @Transactional
    public void markFailed(
            UUID notificationEventId,
            String workerId,
            String failureCode,
            String failureReason
    ) {
        String normalizedWorkerId =
                requireWorkerId(workerId);

        requireFailureReason(failureReason);

        NotificationEvent event =
                findEventForUpdate(
                        notificationEventId
                );

        requireLeaseOwner(
                event,
                normalizedWorkerId
        );

        event.markFailed(
                normalizeOptional(failureCode),
                failureReason
        );

        notificationEventRepository
                .saveAndFlush(event);

        log.error(
                "Notification event permanently failed. notificationEventId={}, eventType={}, attemptCount={}, maximumAttempts={}, failureCode={}, failureReason={}",
                event.getNotificationEventId(),
                event.getEventType(),
                event.getAttemptCount(),
                event.getMaximumAttempts(),
                event.getFailureCode(),
                safeFailureLogValue(
                        event.getFailureReason()
                )
        );
    }

    /**
     * Cancels a pending or processing notification event.
     */
    @Override
    @Transactional
    public NotificationEventResponse cancelEvent(
            UUID notificationEventId,
            String cancellationReason
    ) {
        NotificationEvent event =
                findEventForUpdate(
                        notificationEventId
                );

        if (event.isTerminal()) {
            reject(
                    HttpStatus.CONFLICT,
                    "A terminal notification event cannot be cancelled."
            );
        }

        event.cancel(
                normalizeOptional(cancellationReason)
        );

        NotificationEvent savedEvent =
                notificationEventRepository
                        .saveAndFlush(event);

        log.info(
                "Notification event cancelled. notificationEventId={}, eventType={}, resourceType={}, resourceId={}",
                savedEvent.getNotificationEventId(),
                savedEvent.getEventType(),
                savedEvent.getResourceType(),
                savedEvent.getResourceId()
        );

        return notificationEventMapper
                .toResponse(savedEvent);
    }

    /**
     * Loads one event without an explicit database lock.
     */
    private NotificationEvent findEvent(
            UUID notificationEventId
    ) {
        requireNotificationEventId(
                notificationEventId
        );

        return notificationEventRepository
                .findById(notificationEventId)
                .orElseThrow(
                        () ->
                                new PublicRequestRejectedException(
                                        HttpStatus.NOT_FOUND,
                                        "Notification event was not found."
                                )
                );
    }

    /**
     * Loads one event with a pessimistic write lock.
     */
    private NotificationEvent findEventForUpdate(
            UUID notificationEventId
    ) {
        requireNotificationEventId(
                notificationEventId
        );

        return notificationEventRepository
                .findByIdForUpdate(
                        notificationEventId
                )
                .orElseThrow(
                        () ->
                                new PublicRequestRejectedException(
                                        HttpStatus.NOT_FOUND,
                                        "Notification event was not found."
                                )
                );
    }

    /**
     * Ensures only the worker that owns the active lease can complete
     * or fail processing.
     */
    private void requireLeaseOwner(
            NotificationEvent event,
            String workerId
    ) {
        if (
                event.getEventStatus()
                        != NotificationEventStatus.PROCESSING
        ) {
            reject(
                    HttpStatus.CONFLICT,
                    "Notification event is not currently processing."
            );
        }

        String lockedBy =
                normalizeOptional(
                        event.getLockedBy()
                );

        if (
                lockedBy == null
                        || !lockedBy.equals(workerId)
        ) {
            reject(
                    HttpStatus.CONFLICT,
                    "Notification event is owned by another worker."
            );
        }

        Instant lockExpiresAt =
                event.getLockExpiresAt();

        if (
                lockExpiresAt == null
                        || !lockExpiresAt.isAfter(
                        Instant.now()
                )
        ) {
            reject(
                    HttpStatus.CONFLICT,
                    "Notification worker lease has expired."
            );
        }
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

    private Duration resolveLeaseDuration(
            Duration leaseDuration
    ) {
        if (leaseDuration == null) {
            return DEFAULT_LEASE_DURATION;
        }

        if (
                leaseDuration.isZero()
                        || leaseDuration.isNegative()
        ) {
            reject(
                    HttpStatus.BAD_REQUEST,
                    "Notification worker lease duration must be positive."
            );
        }

        return leaseDuration;
    }

    private Duration resolveRetryDelay(
            Duration retryDelay
    ) {
        if (retryDelay == null) {
            return DEFAULT_RETRY_DELAY;
        }

        if (
                retryDelay.isZero()
                        || retryDelay.isNegative()
        ) {
            reject(
                    HttpStatus.BAD_REQUEST,
                    "Notification retry delay must be positive."
            );
        }

        return retryDelay;
    }

    private String requireWorkerId(
            String workerId
    ) {
        String normalizedWorkerId =
                normalizeOptional(workerId);

        if (normalizedWorkerId == null) {
            reject(
                    HttpStatus.BAD_REQUEST,
                    "Notification worker ID is required."
            );
        }

        if (normalizedWorkerId.length() > 160) {
            reject(
                    HttpStatus.BAD_REQUEST,
                    "Notification worker ID cannot exceed 160 characters."
            );
        }

        return normalizedWorkerId;
    }

    private void requireFailureReason(
            String failureReason
    ) {
        if (
                normalizeOptional(failureReason) == null
        ) {
            reject(
                    HttpStatus.BAD_REQUEST,
                    "Notification failure reason is required."
            );
        }
    }

    private void requireNotificationEventId(
            UUID notificationEventId
    ) {
        if (notificationEventId == null) {
            reject(
                    HttpStatus.BAD_REQUEST,
                    "Notification event ID is required."
            );
        }
    }

    private void requireCreateRequest(
            NotificationEventCreateRequest request
    ) {
        if (request == null) {
            reject(
                    HttpStatus.BAD_REQUEST,
                    "Notification event information is required."
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

    /**
     * Prevents long provider errors or sensitive data from being
     * copied directly into application logs.
     */
    private String safeFailureLogValue(
            String failureReason
    ) {
        String normalizedReason =
                normalizeOptional(failureReason);

        if (normalizedReason == null) {
            return null;
        }

        int maximumLength = 300;

        if (normalizedReason.length() <= maximumLength) {
            return normalizedReason;
        }

        return normalizedReason.substring(
                0,
                maximumLength
        ) + "...";
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

    private String normalizeOptional(
            String value
    ) {
        if (value == null) {
            return null;
        }

        String normalizedValue =
                value.trim();

        return normalizedValue.isEmpty()
                ? null
                : normalizedValue;
    }
}