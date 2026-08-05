package romelt_techcare.backend.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import romelt_techcare.backend.entity.CustomerReviewInvitation;
import romelt_techcare.backend.enums.CustomerReviewInvitationStatus;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Persistence operations for customer-review invitations.
 */
@Repository
public interface CustomerReviewInvitationRepository
        extends JpaRepository<CustomerReviewInvitation, UUID> {

    @EntityGraph(attributePaths = {
            "bookingRequest",
            "createdByAdminUser",
            "revokedByAdminUser"
    })
    Optional<CustomerReviewInvitation>
    findByReviewInvitationId(
            UUID reviewInvitationId
    );

    @EntityGraph(attributePaths = {
            "bookingRequest",
            "createdByAdminUser",
            "revokedByAdminUser"
    })
    Optional<CustomerReviewInvitation>
    findByTokenHash(
            String tokenHash
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select invitation
            from CustomerReviewInvitation invitation
            join fetch invitation.bookingRequest booking
            where invitation.reviewInvitationId =
                  :reviewInvitationId
            """)
    Optional<CustomerReviewInvitation> findByIdForUpdate(
            @Param("reviewInvitationId")
            UUID reviewInvitationId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select invitation
            from CustomerReviewInvitation invitation
            join fetch invitation.bookingRequest booking
            where invitation.tokenHash = :tokenHash
            """)
    Optional<CustomerReviewInvitation> findByTokenHashForUpdate(
            @Param("tokenHash")
            String tokenHash
    );

    boolean existsByTokenHash(
            String tokenHash
    );

    boolean existsByBookingRequest_BookingRequestIdAndInvitationStatusIn(
            UUID bookingRequestId,
            List<CustomerReviewInvitationStatus> statuses
    );

    @EntityGraph(attributePaths = {
            "bookingRequest",
            "createdByAdminUser",
            "revokedByAdminUser"
    })
    Page<CustomerReviewInvitation>
    findAllByBookingRequest_BookingRequestIdOrderByCreatedAtDesc(
            UUID bookingRequestId,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {
            "bookingRequest",
            "createdByAdminUser",
            "revokedByAdminUser"
    })
    @Query("""
            select invitation
            from CustomerReviewInvitation invitation
            where (
                    :status is null
                    or invitation.invitationStatus = :status
                  )
              and (
                    :keyword is null
                    or lower(invitation.customerEmail)
                        like lower(concat('%', :keyword, '%'))
                    or lower(invitation.bookingRequest.referenceNumber)
                        like lower(concat('%', :keyword, '%'))
                    or lower(invitation.bookingRequest.fullName)
                        like lower(concat('%', :keyword, '%'))
                  )
            order by invitation.createdAt desc
            """)
    Page<CustomerReviewInvitation> searchInvitations(
            @Param("keyword")
            String keyword,

            @Param("status")
            CustomerReviewInvitationStatus status,

            Pageable pageable
    );

    @Query("""
            select invitation.reviewInvitationId
            from CustomerReviewInvitation invitation
            where invitation.invitationStatus in :statuses
              and invitation.expiresAt <= :currentTime
            """)
    List<UUID> findExpiredInvitationIds(
            @Param("statuses")
            List<CustomerReviewInvitationStatus> statuses,

            @Param("currentTime")
            Instant currentTime
    );
}