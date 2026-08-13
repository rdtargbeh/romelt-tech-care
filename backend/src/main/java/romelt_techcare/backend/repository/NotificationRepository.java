package romelt_techcare.backend.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import romelt_techcare.backend.entity.Notification;
import romelt_techcare.backend.enums.NotificationChannel;
import romelt_techcare.backend.enums.NotificationResourceType;
import romelt_techcare.backend.enums.NotificationStatus;

import java.util.Optional;
import java.util.UUID;

/**
 * ================================================================
 * ROMELT TECHCARE — NOTIFICATION REPOSITORY
 * ================================================================
 *
 * Purpose:
 * Provides persistence and retrieval operations for the simplified
 * notification system.
 *
 * Responsibilities:
 * - Stores EMAIL, SMS, and IN_APP notification records.
 * - Retrieves administrator portal notifications.
 * - Counts unread administrator notifications.
 * - Retrieves customer notification history.
 * - Retrieves notifications related to bookings and contact
 *   inquiries.
 * - Supports administrator notification filtering and pagination.
 *
 * Hibernate compatibility:
 * messageText is stored as a large text field and may be mapped as a
 * CLOB when @Lob is used. Hibernate 6 does not allow LOWER() to be
 * applied directly to CLOB attributes.
 *
 * Therefore, the JPQL keyword search uses regular VARCHAR fields:
 * - title
 * - recipientName
 * - recipientAddress
 * - providerName
 * - providerMessageId
 *
 * Full message-body searching can be added later using a PostgreSQL
 * native query or PostgreSQL full-text search.
 * ================================================================
 */
@Repository
public interface NotificationRepository
        extends JpaRepository<Notification, UUID> {

    /**
     * Finds one notification owned by a specific administrator.
     *
     * This prevents one administrator from reading or changing
     * another administrator's in-app notification.
     */
    Optional<Notification>
    findByNotificationIdAndAdminUserId(
            UUID notificationId,
            UUID adminUserId
    );

    /**
     * Returns active in-app notifications for an administrator.
     *
     * Dismissed notifications are excluded.
     */
    Page<Notification>
    findByAdminUserIdAndChannelAndDismissedAtIsNullOrderByCreatedAtDesc(
            UUID adminUserId,
            NotificationChannel channel,
            Pageable pageable
    );

    /**
     * Returns unread and non-dismissed in-app notifications for an
     * administrator.
     */
    Page<Notification>
    findByAdminUserIdAndChannelAndReadAtIsNullAndDismissedAtIsNullOrderByCreatedAtDesc(
            UUID adminUserId,
            NotificationChannel channel,
            Pageable pageable
    );

    /**
     * Counts unread and non-dismissed in-app notifications for an
     * administrator.
     */
    long countByAdminUserIdAndChannelAndReadAtIsNullAndDismissedAtIsNull(
            UUID adminUserId,
            NotificationChannel channel
    );

    /**
     * Returns notification history for one customer.
     */
    Page<Notification>
    findByCustomerIdOrderByCreatedAtDesc(
            UUID customerId,
            Pageable pageable
    );

    /**
     * Returns notification history for one booking, contact inquiry,
     * or other supported business resource.
     */
    Page<Notification>
    findByResourceTypeAndResourceIdOrderByCreatedAtDesc(
            NotificationResourceType resourceType,
            UUID resourceId,
            Pageable pageable
    );

    /**
     * Finds a provider-managed notification using the provider's
     * message identifier.
     *
     * This is used by SMS and email delivery callbacks.
     */
    Optional<Notification>
    findByProviderMessageId(
            String providerMessageId
    );

    /**
     * Searches notification records for administrator monitoring.
     *
     * Keyword fields:
     * - title
     * - recipient name
     * - recipient address
     * - provider name
     * - provider message ID
     *
     * messageText is intentionally excluded because Hibernate maps
     * @Lob String attributes as CLOB and rejects LOWER(CLOB).
     */
    @Query("""
            select n
            from Notification n
            where (:channel is null or n.channel = :channel)
              and (:status is null or n.notificationStatus = :status)
              and (:resourceType is null or n.resourceType = :resourceType)
              and (:resourceId is null or n.resourceId = :resourceId)
              and (:customerId is null or n.customerId = :customerId)
              and (:adminUserId is null or n.adminUserId = :adminUserId)
              and (
                    :keyword is null
                    or lower(n.title)
                        like lower(concat('%', :keyword, '%'))
                    or lower(coalesce(n.recipientName, ''))
                        like lower(concat('%', :keyword, '%'))
                    or lower(coalesce(n.recipientAddress, ''))
                        like lower(concat('%', :keyword, '%'))
                    or lower(coalesce(n.providerName, ''))
                        like lower(concat('%', :keyword, '%'))
                    or lower(coalesce(n.providerMessageId, ''))
                        like lower(concat('%', :keyword, '%'))
              )
            """)
    Page<Notification> search(

            @Param("channel")
            NotificationChannel channel,

            @Param("status")
            NotificationStatus status,

            @Param("resourceType")
            NotificationResourceType resourceType,

            @Param("resourceId")
            UUID resourceId,

            @Param("customerId")
            UUID customerId,

            @Param("adminUserId")
            UUID adminUserId,

            @Param("keyword")
            String keyword,

            Pageable pageable
    );
}