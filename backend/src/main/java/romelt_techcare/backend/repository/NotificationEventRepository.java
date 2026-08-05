package romelt_techcare.backend.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import romelt_techcare.backend.entity.NotificationEvent;
import romelt_techcare.backend.enums.NotificationEventStatus;
import romelt_techcare.backend.enums.NotificationEventType;
import romelt_techcare.backend.enums.NotificationResourceType;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * ================================================================
 * ROMELT TECHCARE — NOTIFICATION EVENT REPOSITORY
 * ================================================================
 *
 * Purpose:
 * Provides persistence, transactional-outbox claiming, retry
 * retrieval, lease recovery, idempotency checks, and administrator
 * search access for NotificationEvent records.
 *
 * Responsibilities:
 * - Persists notification outbox events.
 * - Prevents duplicate logical events through idempotency keys.
 * - Retrieves events associated with business resources.
 * - Retrieves customer and administrator notification histories.
 * - Claims available events with PostgreSQL FOR UPDATE SKIP LOCKED.
 * - Supports multiple notification workers safely.
 * - Finds abandoned PROCESSING events whose leases have expired.
 * - Supports retryable event processing.
 * - Provides administrator filtering and reporting queries.
 *
 * Worker behavior:
 * claimAvailableEvents() must be called inside a transaction.
 *
 * PostgreSQL FOR UPDATE SKIP LOCKED ensures that multiple application
 * instances do not claim the same notification event concurrently.
 *
 * The service must call NotificationEvent.claim() for every returned
 * event before committing the claim transaction.
 *
 * Important:
 * Business services create notification events. Notification workers
 * process them after the originating business transaction commits.
 * ================================================================
 */
@Repository
public interface NotificationEventRepository
        extends JpaRepository<NotificationEvent, UUID> {

    /**
     * Finds an event by its idempotency key.
     */
    Optional<NotificationEvent> findByIdempotencyKey(
            String idempotencyKey
    );

    /**
     * Checks whether an event already exists for the supplied
     * idempotency key.
     */
    boolean existsByIdempotencyKey(
            String idempotencyKey
    );

    /**
     * Locks one notification event for an explicit state-changing
     * operation.
     *
     * This is useful for:
     * - cancellation;
     * - retry;
     * - manual reprocessing;
     * - lease renewal;
     * - provider callback reconciliation.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select event
            from NotificationEvent event
            where event.notificationEventId = :notificationEventId
            """)
    Optional<NotificationEvent> findByIdForUpdate(
            @Param("notificationEventId")
            UUID notificationEventId
    );

    /**
     * Claims available notification events using PostgreSQL row
     * locking.
     *
     * Eligible records:
     * - PENDING events whose available_at has arrived;
     * - RETRY_PENDING events whose next_attempt_at has arrived.
     *
     * Priority order:
     * - URGENT
     * - HIGH
     * - NORMAL
     * - LOW
     *
     * Events with the same priority are processed oldest first.
     *
     * This query must run inside a transaction. Returned records remain
     * locked until that transaction commits or rolls back.
     */
    @Query(
            value = """
                    SELECT event.*
                    FROM notification_events event
                    WHERE (
                        (
                            event.event_status = 'PENDING'
                            AND event.available_at <= :now
                        )
                        OR
                        (
                            event.event_status = 'RETRY_PENDING'
                            AND event.next_attempt_at IS NOT NULL
                            AND event.next_attempt_at <= :now
                        )
                    )
                    AND event.attempt_count < event.maximum_attempts
                    AND (
                        event.scheduled_for IS NULL
                        OR event.scheduled_for <= :now
                    )
                    ORDER BY
                        CASE event.priority
                            WHEN 'URGENT' THEN 1
                            WHEN 'HIGH' THEN 2
                            WHEN 'NORMAL' THEN 3
                            WHEN 'LOW' THEN 4
                            ELSE 5
                        END,
                        COALESCE(
                            event.next_attempt_at,
                            event.available_at
                        ) ASC,
                        event.created_at ASC
                    LIMIT :batchSize
                    FOR UPDATE SKIP LOCKED
                    """,
            nativeQuery = true
    )
    List<NotificationEvent> claimAvailableEvents(
            @Param("now")
            Instant now,

            @Param("batchSize")
            int batchSize
    );

    /**
     * Claims abandoned PROCESSING events whose worker leases have
     * expired.
     *
     * The recovery service must call recoverExpiredLease() on each
     * returned entity before committing the transaction.
     */
    @Query(
            value = """
                    SELECT event.*
                    FROM notification_events event
                    WHERE event.event_status = 'PROCESSING'
                      AND event.lock_expires_at IS NOT NULL
                      AND event.lock_expires_at <= :now
                    ORDER BY
                        event.lock_expires_at ASC,
                        event.processing_started_at ASC,
                        event.created_at ASC
                    LIMIT :batchSize
                    FOR UPDATE SKIP LOCKED
                    """,
            nativeQuery = true
    )
    List<NotificationEvent> claimExpiredLeaseEvents(
            @Param("now")
            Instant now,

            @Param("batchSize")
            int batchSize
    );

    /**
     * Returns all events related to one business resource.
     */
    List<NotificationEvent>
    findByResourceTypeAndResourceIdOrderByCreatedAtDesc(
            NotificationResourceType resourceType,
            UUID resourceId
    );

    /**
     * Returns all events associated with one customer.
     */
    Page<NotificationEvent>
    findByCustomerIdOrderByCreatedAtDesc(
            UUID customerId,
            Pageable pageable
    );

    /**
     * Returns all events addressed to one administrator.
     */
    Page<NotificationEvent>
    findByRecipientAdminUserIdOrderByCreatedAtDesc(
            UUID recipientAdminUserId,
            Pageable pageable
    );

    /**
     * Returns all events sharing a correlation key.
     */
    List<NotificationEvent>
    findByCorrelationKeyOrderByCreatedAtAsc(
            String correlationKey
    );

    /**
     * Returns all events of one event type for a resource.
     */
    List<NotificationEvent>
    findByEventTypeAndResourceTypeAndResourceIdOrderByCreatedAtDesc(
            NotificationEventType eventType,
            NotificationResourceType resourceType,
            UUID resourceId
    );

    /**
     * Returns an administrator-searchable notification event list.
     *
     * Filters:
     * - event status;
     * - event type;
     * - resource type;
     * - resource ID;
     * - customer ID;
     * - recipient administrator ID;
     * - recipient email;
     * - correlation key;
     * - idempotency key;
     * - template key;
     * - creation range.
     */
    @Query("""
            select event
            from NotificationEvent event
            where (
                    :eventStatus is null
                    or event.eventStatus = :eventStatus
              )
              and (
                    :eventType is null
                    or event.eventType = :eventType
              )
              and (
                    :resourceType is null
                    or event.resourceType = :resourceType
              )
              and (
                    :resourceId is null
                    or event.resourceId = :resourceId
              )
              and (
                    :customerId is null
                    or event.customerId = :customerId
              )
              and (
                    :recipientAdminUserId is null
                    or event.recipientAdminUserId =
                        :recipientAdminUserId
              )
              and (
                    :createdFrom is null
                    or event.createdAt >= :createdFrom
              )
              and (
                    :createdTo is null
                    or event.createdAt <= :createdTo
              )
              and (
                    :keyword is null
                    or lower(
                        coalesce(event.recipientName, '')
                    ) like lower(
                        concat('%', :keyword, '%')
                    )
                    or lower(
                        coalesce(
                            event.normalizedRecipientEmail,
                            ''
                        )
                    ) like lower(
                        concat('%', :keyword, '%')
                    )
                    or lower(
                        coalesce(event.correlationKey, '')
                    ) like lower(
                        concat('%', :keyword, '%')
                    )
                    or lower(
                        coalesce(event.idempotencyKey, '')
                    ) like lower(
                        concat('%', :keyword, '%')
                    )
                    or lower(event.templateKey) like lower(
                        concat('%', :keyword, '%')
                    )
              )
            """)
    Page<NotificationEvent> searchNotificationEvents(
            @Param("keyword")
            String keyword,

            @Param("eventStatus")
            NotificationEventStatus eventStatus,

            @Param("eventType")
            NotificationEventType eventType,

            @Param("resourceType")
            NotificationResourceType resourceType,

            @Param("resourceId")
            UUID resourceId,

            @Param("customerId")
            UUID customerId,

            @Param("recipientAdminUserId")
            UUID recipientAdminUserId,

            @Param("createdFrom")
            Instant createdFrom,

            @Param("createdTo")
            Instant createdTo,

            Pageable pageable
    );

    /**
     * Counts events currently waiting for their first processing
     * attempt.
     */
    long countByEventStatus(
            NotificationEventStatus eventStatus
    );

    /**
     * Counts events of a specific type and status.
     */
    long countByEventTypeAndEventStatus(
            NotificationEventType eventType,
            NotificationEventStatus eventStatus
    );

    /**
     * Finds terminal notification events older than the supplied
     * retention cutoff.
     *
     * This supports later archival, redaction, or cleanup jobs.
     */
    @Query("""
            select event
            from NotificationEvent event
            where event.eventStatus in (
                    romelt_techcare.backend.enums.NotificationEventStatus.PROCESSED,
                    romelt_techcare.backend.enums.NotificationEventStatus.FAILED,
                    romelt_techcare.backend.enums.NotificationEventStatus.CANCELLED
              )
              and event.updatedAt < :retentionCutoff
            order by event.updatedAt asc
            """)
    Page<NotificationEvent> findTerminalEventsBefore(
            @Param("retentionCutoff")
            Instant retentionCutoff,

            Pageable pageable
    );

    /**
     * Clears an expired worker lease without deleting the event.
     *
     * Normal processing should prefer loading the entity and calling
     * recoverExpiredLease() so retry counts and failure details are
     * recorded correctly.
     *
     * This method exists only for administrative repair operations.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update NotificationEvent event
            set event.lockedAt = null,
                event.lockedBy = null,
                event.lockExpiresAt = null,
                event.updatedAt = :updatedAt
            where event.notificationEventId = :notificationEventId
              and event.eventStatus =
                  romelt_techcare.backend.enums.NotificationEventStatus.PROCESSING
              and event.lockExpiresAt is not null
              and event.lockExpiresAt <= :updatedAt
            """)
    int clearExpiredLease(
            @Param("notificationEventId")
            UUID notificationEventId,

            @Param("updatedAt")
            Instant updatedAt
    );
}