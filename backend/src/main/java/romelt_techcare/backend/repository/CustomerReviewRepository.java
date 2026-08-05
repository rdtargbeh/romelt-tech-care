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
import romelt_techcare.backend.entity.CustomerReview;
import romelt_techcare.backend.enums.CustomerReviewModerationStatus;
import romelt_techcare.backend.enums.CustomerReviewSource;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Persistence operations for customer ratings and reviews.
 */
@Repository
public interface CustomerReviewRepository
        extends JpaRepository<CustomerReview, UUID> {

    @EntityGraph(attributePaths = {
            "reviewInvitation",
            "bookingRequest",
            "contactInquiry",
            "websiteService",
            "customerPhotoMedia",
            "respondedByAdminUser",
            "createdByAdminUser",
            "updatedByAdminUser",
            "moderatedByAdminUser",
            "publishedByAdminUser",
            "hiddenByAdminUser",
            "archivedByAdminUser"
    })
    Optional<CustomerReview> findByCustomerReviewId(
            UUID customerReviewId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select review
            from CustomerReview review
            left join fetch review.reviewInvitation invitation
            left join fetch review.bookingRequest booking
            left join fetch review.contactInquiry inquiry
            left join fetch review.websiteService service
            where review.customerReviewId = :customerReviewId
            """)
    Optional<CustomerReview> findByIdForUpdate(
            @Param("customerReviewId")
            UUID customerReviewId
    );

    boolean existsByReviewInvitation_ReviewInvitationId(
            UUID reviewInvitationId
    );

    boolean existsByBookingRequest_BookingRequestId(
            UUID bookingRequestId
    );

    @EntityGraph(attributePaths = {
            "reviewInvitation",
            "bookingRequest",
            "contactInquiry",
            "websiteService",
            "customerPhotoMedia"
    })
    @Query("""
            select review
            from CustomerReview review
            where (
                    :keyword is null
                    or lower(coalesce(review.reviewerDisplayName, ''))
                        like lower(concat('%', :keyword, '%'))
                    or lower(coalesce(review.reviewerEmail, ''))
                        like lower(concat('%', :keyword, '%'))
                    or lower(coalesce(review.reviewTitle, ''))
                        like lower(concat('%', :keyword, '%'))
                    or lower(coalesce(review.reviewText, ''))
                        like lower(concat('%', :keyword, '%'))
                  )
              and (
                    :moderationStatus is null
                    or review.moderationStatus = :moderationStatus
                  )
              and (
                    :reviewSource is null
                    or review.reviewSource = :reviewSource
                  )
              and (
                    :rating is null
                    or review.rating = :rating
                  )
              and (
                    :serviceId is null
                    or review.websiteService.serviceId = :serviceId
                  )
              and (
                    :isVerifiedCustomer is null
                    or review.isVerifiedCustomer = :isVerifiedCustomer
                  )
              and (
                    :isPublic is null
                    or review.isPublic = :isPublic
                  )
              and (
                    :isFeatured is null
                    or review.isFeatured = :isFeatured
                  )
              and (
                    :isSpam is null
                    or review.isSpam = :isSpam
                  )
            order by review.submittedAt desc
            """)
    Page<CustomerReview> searchReviews(
            @Param("keyword")
            String keyword,

            @Param("moderationStatus")
            CustomerReviewModerationStatus moderationStatus,

            @Param("reviewSource")
            CustomerReviewSource reviewSource,

            @Param("rating")
            Short rating,

            @Param("serviceId")
            UUID serviceId,

            @Param("isVerifiedCustomer")
            Boolean isVerifiedCustomer,

            @Param("isPublic")
            Boolean isPublic,

            @Param("isFeatured")
            Boolean isFeatured,

            @Param("isSpam")
            Boolean isSpam,

            Pageable pageable
    );

    @EntityGraph(attributePaths = {
            "websiteService",
            "customerPhotoMedia"
    })
    @Query("""
            select review
            from CustomerReview review
            where review.moderationStatus =
                  romelt_techcare.backend.enums.CustomerReviewModerationStatus.APPROVED
              and review.isPublic = true
              and review.isSpam = false
              and review.customerConsentConfirmed = true
              and review.publishedAt is not null
            order by review.isFeatured desc,
                     review.publishedAt desc
            """)
    Page<CustomerReview> findPublicReviews(
            Pageable pageable
    );

    @EntityGraph(attributePaths = {
            "websiteService",
            "customerPhotoMedia"
    })
    @Query("""
            select review
            from CustomerReview review
            where review.moderationStatus =
                  romelt_techcare.backend.enums.CustomerReviewModerationStatus.APPROVED
              and review.isPublic = true
              and review.isFeatured = true
              and review.isSpam = false
              and review.customerConsentConfirmed = true
              and review.publishedAt is not null
            order by review.publishedAt desc
            """)
    List<CustomerReview> findFeaturedPublicReviews();

    @EntityGraph(attributePaths = {
            "websiteService",
            "customerPhotoMedia"
    })
    @Query("""
            select review
            from CustomerReview review
            where review.websiteService.serviceSlug = :serviceSlug
              and review.moderationStatus =
                  romelt_techcare.backend.enums.CustomerReviewModerationStatus.APPROVED
              and review.isPublic = true
              and review.isSpam = false
              and review.customerConsentConfirmed = true
              and review.publishedAt is not null
            order by review.isFeatured desc,
                     review.publishedAt desc
            """)
    Page<CustomerReview> findPublicReviewsByServiceSlug(
            @Param("serviceSlug")
            String serviceSlug,
            Pageable pageable
    );

    @Query("""
            select count(review)
            from CustomerReview review
            where review.moderationStatus =
                  romelt_techcare.backend.enums.CustomerReviewModerationStatus.APPROVED
              and review.isPublic = true
              and review.isSpam = false
              and review.customerConsentConfirmed = true
            """)
    long countPublicReviews();

    @Query("""
            select coalesce(avg(review.rating), 0)
            from CustomerReview review
            where review.moderationStatus =
                  romelt_techcare.backend.enums.CustomerReviewModerationStatus.APPROVED
              and review.isPublic = true
              and review.isSpam = false
              and review.customerConsentConfirmed = true
            """)
    Double calculateAveragePublicRating();

    @Query("""
            select count(review)
            from CustomerReview review
            where review.moderationStatus =
                  romelt_techcare.backend.enums.CustomerReviewModerationStatus.APPROVED
              and review.isPublic = true
              and review.isSpam = false
              and review.customerConsentConfirmed = true
              and review.rating = :rating
            """)
    long countPublicReviewsByRating(
            @Param("rating")
            short rating
    );
}