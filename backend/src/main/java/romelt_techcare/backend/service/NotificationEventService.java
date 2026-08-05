package romelt_techcare.backend.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import romelt_techcare.backend.dto.NotificationEventCreateRequest;
import romelt_techcare.backend.dto.NotificationEventResponse;
import romelt_techcare.backend.dto.NotificationEventSummaryResponse;
import romelt_techcare.backend.entity.NotificationEvent;
import romelt_techcare.backend.enums.NotificationEventStatus;
import romelt_techcare.backend.enums.NotificationEventType;
import romelt_techcare.backend.enums.NotificationResourceType;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * ================================================================
 * ROMELT TECHCARE — NOTIFICATION EVENT SERVICE
 * ================================================================
 *
 * Purpose:
 * Defines transactional-outbox operations used by business services,
 * notification workers, and authenticated administrator interfaces.
 *
 * Responsibilities:
 * - Creates notification events inside business transactions.
 * - Prevents duplicate events through idempotency keys.
 * - Retrieves notification-event details and lists.
 * - Claims available events for background processing.
 * - Recovers abandoned worker leases.
 * - Renews active worker leases.
 * - Marks events successfully processed.
 * - Schedules retryable processing failures.
 * - Marks events permanently failed.
 * - Cancels events that no longer require processing.
 *
 * Business-service usage:
 * Booking, contact, customer, review, and review-invitation services
 * call createEvent() inside their existing database transaction.
 *
 * Worker usage:
 * A separate worker calls claimAvailableEvents(), processes each
 * committed event, and then calls:
 * - markProcessed();
 * - scheduleRetry(); or
 * - markFailed().
 *
 * Important:
 * This service manages notification outbox events only.
 *
 * It does not:
 * - render notification templates;
 * - send email or SMS;
 * - create provider webhook records;
 * - directly process delivery provider callbacks.
 * ================================================================
 */
public interface NotificationEventService {

    /**
     * Creates a transactional notification-outbox event.
     *
     * When a non-null idempotency key already exists, the existing
     * event is returned instead of creating a duplicate.
     *
     * @param request internal notification-event command
     * @return persisted notification event
     */
    NotificationEventResponse createEvent(
            NotificationEventCreateRequest request
    );

    /**
     * Returns one complete notification event.
     *
     * @param notificationEventId notification-event identifier
     * @return complete event response
     */
    NotificationEventResponse getEvent(
            UUID notificationEventId
    );

    /**
     * Returns an administrator-searchable event list.
     */
    Page<NotificationEventSummaryResponse> getEvents(
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
    );

    /**
     * Returns events associated with one business resource.
     */
    List<NotificationEventResponse> getEventsForResource(
            NotificationResourceType resourceType,
            UUID resourceId
    );

    /**
     * Claims available notification events for one worker.
     *
     * The returned entities remain managed within the claiming
     * transaction and are changed to PROCESSING before the transaction
     * commits.
     *
     * @param workerId unique worker-instance identifier
     * @param batchSize maximum events to claim
     * @param leaseDuration worker lease duration
     * @return claimed processing events
     */
    List<NotificationEvent> claimAvailableEvents(
            String workerId,
            int batchSize,
            Duration leaseDuration
    );

    /**
     * Recovers abandoned PROCESSING events whose worker leases have
     * expired.
     *
     * Recovered events are placed into RETRY_PENDING or FAILED when
     * maximum attempts have been reached.
     *
     * @param batchSize maximum expired events to recover
     * @param retryDelay delay before another processing attempt
     * @return recovered events
     */
    List<NotificationEvent> recoverExpiredLeaseEvents(
            int batchSize,
            Duration retryDelay
    );

    /**
     * Renews the active lease owned by a worker.
     */
    void renewLease(
            UUID notificationEventId,
            String workerId,
            Duration leaseDuration
    );

    /**
     * Marks an event processed after its notification-delivery records
     * have been created successfully.
     */
    void markProcessed(
            UUID notificationEventId,
            String workerId
    );

    /**
     * Schedules another processing attempt after a temporary failure.
     */
    void scheduleRetry(
            UUID notificationEventId,
            String workerId,
            String failureCode,
            String failureReason,
            Instant retryAt
    );

    /**
     * Marks an event permanently failed.
     */
    void markFailed(
            UUID notificationEventId,
            String workerId,
            String failureCode,
            String failureReason
    );

    /**
     * Cancels a non-terminal notification event.
     */
    NotificationEventResponse cancelEvent(
            UUID notificationEventId,
            String cancellationReason
    );
}