package romelt_techcare.backend.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import romelt_techcare.backend.entity.NotificationDelivery;
import romelt_techcare.backend.enums.NotificationChannel;
import romelt_techcare.backend.enums.NotificationDeliveryStatus;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * ================================================================
 * ROMELT TECHCARE — NOTIFICATION DELIVERY REPOSITORY
 * ================================================================
 *
 * Purpose:
 * Provides persistence, worker claiming, retry processing, in-app
 * notification retrieval, and administrator search operations for
 * notification deliveries.
 *
 * Responsibilities:
 * - Finds deliveries by event, customer, administrator, and provider.
 * - Prevents duplicate event/channel/recipient deliveries.
 * - Claims pending and retryable deliveries using PostgreSQL
 *   FOR UPDATE SKIP LOCKED.
 * - Claims abandoned deliveries whose worker lease expired.
 * - Supports administrator delivery monitoring.
 * - Supports in-app notification lists, unread counts, read state,
 *   dismissal, and restoration.
 *
 * Worker requirement:
 * Claiming methods must execute inside an active transaction.
 * ================================================================
 */
@Repository
public interface NotificationDeliveryRepository
        extends JpaRepository<NotificationDelivery, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select delivery
            from NotificationDelivery delivery
            where delivery.notificationDeliveryId = :notificationDeliveryId
            """)
    Optional<NotificationDelivery> findByIdForUpdate(
            @Param("notificationDeliveryId")
            UUID notificationDeliveryId
    );

    boolean existsByNotificationEventIdAndChannelAndNormalizedRecipientAddress(
            UUID notificationEventId,
            NotificationChannel channel,
            String normalizedRecipientAddress
    );

    List<NotificationDelivery>
    findByNotificationEventIdOrderByCreatedAtAsc(
            UUID notificationEventId
    );

    Page<NotificationDelivery>
    findByCustomerIdOrderByCreatedAtDesc(
            UUID customerId,
            Pageable pageable
    );

    Optional<NotificationDelivery>
    findFirstByProviderMessageIdOrderByCreatedAtDesc(
            String providerMessageId
    );

    /**
     * Claims EMAIL, SMS, and IN_APP deliveries ready for processing.
     */
    @Query(
            value = """
                    SELECT delivery.*
                    FROM notification_deliveries delivery
                    WHERE (
                        delivery.delivery_status = 'PENDING'
                        OR (
                            delivery.delivery_status = 'RETRY_PENDING'
                            AND delivery.next_attempt_at IS NOT NULL
                            AND delivery.next_attempt_at <= :now
                        )
                    )
                    AND delivery.attempt_count < delivery.maximum_attempts
                    ORDER BY
                        COALESCE(
                            delivery.next_attempt_at,
                            delivery.created_at
                        ) ASC,
                        delivery.created_at ASC
                    LIMIT :batchSize
                    FOR UPDATE SKIP LOCKED
                    """,
            nativeQuery = true
    )
    List<NotificationDelivery> claimAvailableDeliveries(
            @Param("now")
            Instant now,

            @Param("batchSize")
            int batchSize
    );

    /**
     * Claims PROCESSING deliveries whose worker leases expired.
     */
    @Query(
            value = """
                    SELECT delivery.*
                    FROM notification_deliveries delivery
                    WHERE delivery.delivery_status = 'PROCESSING'
                      AND delivery.lock_expires_at IS NOT NULL
                      AND delivery.lock_expires_at <= :now
                    ORDER BY
                        delivery.lock_expires_at ASC,
                        delivery.processing_started_at ASC,
                        delivery.created_at ASC
                    LIMIT :batchSize
                    FOR UPDATE SKIP LOCKED
                    """,
            nativeQuery = true
    )
    List<NotificationDelivery> claimExpiredLeaseDeliveries(
            @Param("now")
            Instant now,

            @Param("batchSize")
            int batchSize
    );

    /**
     * Returns visible in-app notifications for one administrator.
     */
    @Query("""
            select delivery
            from NotificationDelivery delivery
            where delivery.channel =
                    romelt_techcare.backend.enums.NotificationChannel.IN_APP
              and delivery.recipientAdminUserId = :administratorId
              and delivery.dismissedAt is null
              and delivery.deliveryStatus in (
                    romelt_techcare.backend.enums.NotificationDeliveryStatus.DELIVERED,
                    romelt_techcare.backend.enums.NotificationDeliveryStatus.OPENED
              )
              and (
                    :unreadOnly = false
                    or delivery.readAt is null
              )
            """)
    Page<NotificationDelivery> findAdministratorInAppNotifications(
            @Param("administratorId")
            UUID administratorId,

            @Param("unreadOnly")
            boolean unreadOnly,

            Pageable pageable
    );

    /**
     * Returns dismissed in-app notifications for one administrator.
     */
    @Query("""
            select delivery
            from NotificationDelivery delivery
            where delivery.channel =
                    romelt_techcare.backend.enums.NotificationChannel.IN_APP
              and delivery.recipientAdminUserId = :administratorId
              and delivery.dismissedAt is not null
            """)
    Page<NotificationDelivery> findDismissedInAppNotifications(
            @Param("administratorId")
            UUID administratorId,

            Pageable pageable
    );

    @Query("""
            select count(delivery)
            from NotificationDelivery delivery
            where delivery.channel =
                    romelt_techcare.backend.enums.NotificationChannel.IN_APP
              and delivery.recipientAdminUserId = :administratorId
              and delivery.readAt is null
              and delivery.dismissedAt is null
              and delivery.deliveryStatus =
                    romelt_techcare.backend.enums.NotificationDeliveryStatus.DELIVERED
            """)
    long countUnreadInAppNotifications(
            @Param("administratorId")
            UUID administratorId
    );

    /**
     * Administrator delivery monitoring query.
     */
    @Query("""
            select delivery
            from NotificationDelivery delivery
            where (
                    :channel is null
                    or delivery.channel = :channel
              )
              and (
                    :deliveryStatus is null
                    or delivery.deliveryStatus = :deliveryStatus
              )
              and (
                    :notificationEventId is null
                    or delivery.notificationEventId =
                        :notificationEventId
              )
              and (
                    :customerId is null
                    or delivery.customerId = :customerId
              )
              and (
                    :recipientAdminUserId is null
                    or delivery.recipientAdminUserId =
                        :recipientAdminUserId
              )
              and (
                    :createdFrom is null
                    or delivery.createdAt >= :createdFrom
              )
              and (
                    :createdTo is null
                    or delivery.createdAt <= :createdTo
              )
              and (
                    :keyword is null
                    or lower(
                        coalesce(delivery.recipientName, '')
                    ) like lower(
                        concat('%', :keyword, '%')
                    )
                    or lower(
                        coalesce(delivery.recipientAddress, '')
                    ) like lower(
                        concat('%', :keyword, '%')
                    )
                    or lower(
                        coalesce(delivery.subject, '')
                    ) like lower(
                        concat('%', :keyword, '%')
                    )
                    or lower(
                        coalesce(delivery.providerMessageId, '')
                    ) like lower(
                        concat('%', :keyword, '%')
                    )
              )
            """)
    Page<NotificationDelivery> searchDeliveries(
            @Param("keyword")
            String keyword,

            @Param("channel")
            NotificationChannel channel,

            @Param("deliveryStatus")
            NotificationDeliveryStatus deliveryStatus,

            @Param("notificationEventId")
            UUID notificationEventId,

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

    long countByDeliveryStatus(
            NotificationDeliveryStatus deliveryStatus
    );

    long countByChannelAndDeliveryStatus(
            NotificationChannel channel,
            NotificationDeliveryStatus deliveryStatus
    );
}