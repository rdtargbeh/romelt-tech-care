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
import romelt_techcare.backend.entity.NotificationSuppression;
import romelt_techcare.backend.enums.NotificationCategory;
import romelt_techcare.backend.enums.NotificationChannel;
import romelt_techcare.backend.enums.NotificationSuppressionReason;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * ================================================================
 * ROMELT TECHCARE — NOTIFICATION SUPPRESSION REPOSITORY
 * ================================================================
 *
 * Purpose:
 * Provides persistence, suppression matching, administrator search,
 * expiration handling, and lifecycle operations for EMAIL and SMS
 * notification suppressions.
 *
 * Active matching:
 * A suppression is effective when:
 * - isActive is true; and
 * - expiresAt is null or later than the current time; and
 * - notificationCategory is null or matches the requested category.
 *
 * Uniqueness:
 * PostgreSQL enforces one active suppression for the same:
 *
 * channel + normalizedRecipientAddress + category
 *
 * A null category represents suppression across all categories.
 * ================================================================
 */
@Repository
public interface NotificationSuppressionRepository
        extends JpaRepository<NotificationSuppression, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select suppression
            from NotificationSuppression suppression
            where suppression.notificationSuppressionId =
                :notificationSuppressionId
            """)
    Optional<NotificationSuppression> findByIdForUpdate(
            @Param("notificationSuppressionId")
            UUID notificationSuppressionId
    );

    /**
     * Finds an effective suppression for a proposed delivery.
     *
     * Category priority:
     * 1. Exact category match
     * 2. Null category, which suppresses all categories
     */
    @Query("""
            select suppression
            from NotificationSuppression suppression
            where suppression.channel = :channel
              and suppression.normalizedRecipientAddress =
                    :normalizedRecipientAddress
              and suppression.active = true
              and (
                    suppression.expiresAt is null
                    or suppression.expiresAt > :now
              )
              and (
                    suppression.notificationCategory is null
                    or suppression.notificationCategory =
                        :notificationCategory
              )
            order by
                case
                    when suppression.notificationCategory =
                            :notificationCategory
                    then 0
                    else 1
                end,
                suppression.createdAt desc
            """)
    List<NotificationSuppression> findEffectiveSuppressions(
            @Param("channel")
            NotificationChannel channel,

            @Param("normalizedRecipientAddress")
            String normalizedRecipientAddress,

            @Param("notificationCategory")
            NotificationCategory notificationCategory,

            @Param("now")
            Instant now
    );

    /**
     * Finds an active suppression using an exact category match,
     * including null category values.
     */
    @Query("""
            select suppression
            from NotificationSuppression suppression
            where suppression.channel = :channel
              and suppression.normalizedRecipientAddress =
                    :normalizedRecipientAddress
              and (
                    (
                        :notificationCategory is null
                        and suppression.notificationCategory is null
                    )
                    or suppression.notificationCategory =
                        :notificationCategory
              )
              and suppression.active = true
            """)
    Optional<NotificationSuppression> findExactActiveSuppression(
            @Param("channel")
            NotificationChannel channel,

            @Param("normalizedRecipientAddress")
            String normalizedRecipientAddress,

            @Param("notificationCategory")
            NotificationCategory notificationCategory
    );

    List<NotificationSuppression>
    findByCustomerIdOrderByCreatedAtDesc(
            UUID customerId
    );

    List<NotificationSuppression>
    findByChannelAndNormalizedRecipientAddressOrderByCreatedAtDesc(
            NotificationChannel channel,
            String normalizedRecipientAddress
    );

    long countByActiveTrue();

    long countBySuppressionReasonAndActiveTrue(
            NotificationSuppressionReason suppressionReason
    );

    /**
     * Administrator search query.
     */
    @Query("""
            select suppression
            from NotificationSuppression suppression
            where (
                    :customerId is null
                    or suppression.customerId = :customerId
              )
              and (
                    :channel is null
                    or suppression.channel = :channel
              )
              and (
                    :notificationCategory is null
                    or suppression.notificationCategory =
                        :notificationCategory
              )
              and (
                    :suppressionReason is null
                    or suppression.suppressionReason =
                        :suppressionReason
              )
              and (
                    :active is null
                    or suppression.active = :active
              )
              and (
                    :effective is null
                    or (
                        :effective = true
                        and suppression.active = true
                        and (
                            suppression.expiresAt is null
                            or suppression.expiresAt > :now
                        )
                    )
                    or (
                        :effective = false
                        and (
                            suppression.active = false
                            or (
                                suppression.expiresAt is not null
                                and suppression.expiresAt <= :now
                            )
                        )
                    )
              )
              and (
                    :keyword is null
                    or lower(
                        suppression.recipientAddress
                    ) like lower(
                        concat('%', :keyword, '%')
                    )
                    or lower(
                        suppression.normalizedRecipientAddress
                    ) like lower(
                        concat('%', :keyword, '%')
                    )
                    or lower(
                        coalesce(suppression.reasonDetails, '')
                    ) like lower(
                        concat('%', :keyword, '%')
                    )
              )
            """)
    Page<NotificationSuppression> searchSuppressions(
            @Param("keyword")
            String keyword,

            @Param("customerId")
            UUID customerId,

            @Param("channel")
            NotificationChannel channel,

            @Param("notificationCategory")
            NotificationCategory notificationCategory,

            @Param("suppressionReason")
            NotificationSuppressionReason suppressionReason,

            @Param("active")
            Boolean active,

            @Param("effective")
            Boolean effective,

            @Param("now")
            Instant now,

            Pageable pageable
    );

    /**
     * Finds active suppressions whose expiration has arrived.
     */
    @Query("""
            select suppression
            from NotificationSuppression suppression
            where suppression.active = true
              and suppression.expiresAt is not null
              and suppression.expiresAt <= :now
            order by suppression.expiresAt asc
            """)
    Page<NotificationSuppression> findExpiredActiveSuppressions(
            @Param("now")
            Instant now,
            Pageable pageable
    );

    /**
     * Deactivates expired suppressions in bulk.
     */
    @Modifying(
            clearAutomatically = true,
            flushAutomatically = true
    )
    @Query("""
            update NotificationSuppression suppression
            set suppression.active = false,
                suppression.deactivatedAt = :now,
                suppression.updatedAt = :now
            where suppression.active = true
              and suppression.expiresAt is not null
              and suppression.expiresAt <= :now
            """)
    int deactivateExpiredSuppressions(
            @Param("now")
            Instant now
    );
}