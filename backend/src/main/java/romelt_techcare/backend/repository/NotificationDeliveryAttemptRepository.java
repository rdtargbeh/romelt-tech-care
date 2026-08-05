package romelt_techcare.backend.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import romelt_techcare.backend.entity.NotificationDeliveryAttempt;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * ================================================================
 * ROMELT TECHCARE — NOTIFICATION DELIVERY ATTEMPT REPOSITORY
 * ================================================================
 *
 * Purpose:
 * Provides persistence and reporting for immutable provider delivery
 * attempts.
 *
 * Responsibilities:
 * - Retrieve all attempts for one delivery.
 * - Retrieve latest attempt.
 * - Search attempts for operational reporting.
 * - Lookup provider message IDs.
 * ================================================================
 */
@Repository
public interface NotificationDeliveryAttemptRepository
        extends JpaRepository<NotificationDeliveryAttempt, UUID> {

    List<NotificationDeliveryAttempt>
    findByNotificationDeliveryIdOrderByAttemptNumberDesc(
            UUID notificationDeliveryId
    );

    Optional<NotificationDeliveryAttempt>
    findFirstByNotificationDeliveryIdOrderByAttemptNumberDesc(
            UUID notificationDeliveryId
    );

    Optional<NotificationDeliveryAttempt>
    findByNotificationDeliveryIdAndAttemptNumber(
            UUID notificationDeliveryId,
            Integer attemptNumber
    );

    Optional<NotificationDeliveryAttempt>
    findFirstByProviderMessageId(
            String providerMessageId
    );

    long countBySuccess(
            boolean success
    );

    long countByRetryable(
            boolean retryable
    );

    @Query("""
            select attempt
            from NotificationDeliveryAttempt attempt
            where
                (
                    :notificationDeliveryId is null
                    or attempt.notificationDeliveryId =
                       :notificationDeliveryId
                )
            and (
                    :providerName is null
                    or lower(attempt.providerName)
                        like lower(
                            concat('%',:providerName,'%')
                        )
                )
            and (
                    :success is null
                    or attempt.success = :success
                )
            and (
                    :retryable is null
                    or attempt.retryable = :retryable
                )
            and (
                    :startedAfter is null
                    or attempt.startedAt >= :startedAfter
                )
            and (
                    :startedBefore is null
                    or attempt.startedAt <= :startedBefore
                )
            """)
    Page<NotificationDeliveryAttempt> search(

            @Param("notificationDeliveryId")
            UUID notificationDeliveryId,

            @Param("providerName")
            String providerName,

            @Param("success")
            Boolean success,

            @Param("retryable")
            Boolean retryable,

            @Param("startedAfter")
            Instant startedAfter,

            @Param("startedBefore")
            Instant startedBefore,

            Pageable pageable
    );
}