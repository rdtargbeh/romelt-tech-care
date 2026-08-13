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
 * ================================================================
 * ROMELT TECHCARE — CUSTOMER REVIEW REPOSITORY
 * ================================================================
 *
 * Purpose:
 * Provides persistence, duplicate protection, administrator search,
 * moderation locking, public review retrieval, and rating statistics
 * for customer reviews.
 *
 * Core service-review rule:
 * Every Romelt TechCare customer review represents feedback about an
 * actual completed, review-eligible BookingRequest and the
 * WebsiteService performed through that booking.
 *
 * Review ownership:
 *
 * Customer
 *      -> BookingRequest
 *          -> WebsiteService
 *              -> CustomerReview
 *
 * ContactInquiry is intentionally not part of customer-review
 * ownership or repository fetching.
 *
 * Responsibilities:
 * - Retrieves complete customer-review records.
 * - Supports pessimistic locking for review updates and moderation.
 * - Prevents duplicate invitation and booking reviews.
 * - Provides administrator review search and filtering.
 * - Retrieves approved and published reviews for the public website.
 * - Retrieves featured public reviews.
 * - Provides public rating statistics using exactly the same
 *   visibility rules as the public review list.
 *
 * One-review-per-booking rule:
 * A completed service booking may have at most one CustomerReview,
 * regardless of how the customer communicated the feedback.
 *
 * Examples:
 * - secure booking follow-up;
 * - telephone;
 * - email;
 * - Google;
 * - Facebook;
 * - another supported source.
 *
 * Review source identifies HOW or WHERE feedback was communicated.
 * The completed BookingRequest establishes that the service was
 * actually received.
 *
 * PostgreSQL / Hibernate note:
 * Nullable String parameters used in lower()/concat() expressions
 * require explicit String typing.
 *
 * Without an explicit cast PostgreSQL may infer an untyped null
 * parameter as bytea and produce:
 *
 * function lower(bytea) does not exist
 *
 * Therefore :keyword is explicitly cast as String both in the null
 * condition and every text-expression condition.
 *
 * Public visibility rule:
 * A review contributes to public lists and rating statistics only
 * when all of the following are true:
 * - moderationStatus = APPROVED;
 * - isPublic = true;
 * - isSpam = false;
 * - customerConsentConfirmed = true;
 * - publishedAt is not null.
 * ================================================================
 */
@Repository
public interface CustomerReviewRepository
        extends JpaRepository<CustomerReview, UUID> {

    // =================================================================
    // GET ONE
    // =================================================================

    /**
     * Retrieves one complete customer review for administrator use.
     */
    @EntityGraph(attributePaths = {
            "reviewInvitation",
            "bookingRequest",
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

    // =================================================================
    // GET ONE FOR UPDATE
    // =================================================================

    /**
     * Retrieves and pessimistically locks one review while an
     * administrator performs a state-changing operation.
     *
     * Booking and service are fetched because they form the
     * authoritative ownership of the review.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select review
            from CustomerReview review

            left join fetch review.reviewInvitation invitation

            join fetch review.bookingRequest booking

            join fetch review.websiteService service

            where review.customerReviewId = :customerReviewId
            """)
    Optional<CustomerReview> findByIdForUpdate(
            @Param("customerReviewId")
            UUID customerReviewId
    );

    // =================================================================
    // DUPLICATE CHECKS
    // =================================================================

    /**
     * Prevents the same secure review invitation from producing more
     * than one CustomerReview.
     */
    boolean existsByReviewInvitation_ReviewInvitationId(
            UUID reviewInvitationId
    );

    /**
     * Enforces one customer review per completed service booking.
     *
     * The review source does not alter this rule.
     */
    boolean existsByBookingRequest_BookingRequestId(
            UUID bookingRequestId
    );

    // =================================================================
    // ADMIN SEARCH
    // =================================================================

    /**
     * Searches administrator-visible customer reviews.
     *
     * Supported filters:
     * - keyword;
     * - moderation status;
     * - review source;
     * - rating;
     * - service;
     * - verified-customer status;
     * - public status;
     * - featured status;
     * - spam status.
     *
     * Keyword searches:
     * - reviewer display name;
     * - reviewer email;
     * - review title;
     * - review text.
     *
     * The nullable keyword parameter is explicitly cast to String to
     * avoid PostgreSQL/Hibernate null-type ambiguity.
     */
    @EntityGraph(attributePaths = {
            "reviewInvitation",
            "bookingRequest",
            "websiteService",
            "customerPhotoMedia"
    })
    @Query("""
            select review
            from CustomerReview review

            where (
                    cast(:keyword as String) is null

                    or lower(
                        coalesce(
                            review.reviewerDisplayName,
                            ''
                        )
                    )
                    like lower(
                        concat(
                            '%',
                            cast(:keyword as String),
                            '%'
                        )
                    )

                    or lower(
                        coalesce(
                            review.reviewerEmail,
                            ''
                        )
                    )
                    like lower(
                        concat(
                            '%',
                            cast(:keyword as String),
                            '%'
                        )
                    )

                    or lower(
                        coalesce(
                            review.reviewTitle,
                            ''
                        )
                    )
                    like lower(
                        concat(
                            '%',
                            cast(:keyword as String),
                            '%'
                        )
                    )

                    or lower(
                        coalesce(
                            review.reviewText,
                            ''
                        )
                    )
                    like lower(
                        concat(
                            '%',
                            cast(:keyword as String),
                            '%'
                        )
                    )
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
                    or review.isVerifiedCustomer =
                       :isVerifiedCustomer
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

    // =================================================================
    // PUBLIC REVIEWS
    // =================================================================

    /**
     * Returns reviews that are actually visible on the public website.
     */
    @EntityGraph(attributePaths = {
            "websiteService",
            "customerPhotoMedia"
    })
    @Query("""
            select review
            from CustomerReview review

            where review.moderationStatus =
                  romelt_techcare.backend.enums
                          .CustomerReviewModerationStatus.APPROVED

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

    // =================================================================
    // FEATURED PUBLIC REVIEWS
    // =================================================================

    /**
     * Returns approved, published customer reviews selected for
     * featured placement.
     */
    @EntityGraph(attributePaths = {
            "websiteService",
            "customerPhotoMedia"
    })
    @Query("""
            select review
            from CustomerReview review

            where review.moderationStatus =
                  romelt_techcare.backend.enums
                          .CustomerReviewModerationStatus.APPROVED

              and review.isPublic = true

              and review.isFeatured = true

              and review.isSpam = false

              and review.customerConsentConfirmed = true

              and review.publishedAt is not null

            order by review.publishedAt desc
            """)
    List<CustomerReview> findFeaturedPublicReviews();

    // =================================================================
    // PUBLIC REVIEWS BY SERVICE
    // =================================================================

    /**
     * Returns public reviews associated with one WebsiteService.
     *
     * WebsiteService is derived from the completed booking by the
     * service layer when the review is created.
     */
    @EntityGraph(attributePaths = {
            "websiteService",
            "customerPhotoMedia"
    })
    @Query("""
            select review
            from CustomerReview review

            where review.websiteService.serviceSlug = :serviceSlug

              and review.moderationStatus =
                  romelt_techcare.backend.enums
                          .CustomerReviewModerationStatus.APPROVED

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

    // =================================================================
    // PUBLIC REVIEW COUNT
    // =================================================================

    /**
     * Counts only reviews that are actually published and publicly
     * visible.
     *
     * This uses the exact same visibility requirements as
     * findPublicReviews().
     */
    @Query("""
            select count(review)
            from CustomerReview review

            where review.moderationStatus =
                  romelt_techcare.backend.enums
                          .CustomerReviewModerationStatus.APPROVED

              and review.isPublic = true

              and review.isSpam = false

              and review.customerConsentConfirmed = true

              and review.publishedAt is not null
            """)
    long countPublicReviews();

    // =================================================================
    // PUBLIC AVERAGE RATING
    // =================================================================

    /**
     * Calculates the average rating from only published public
     * customer reviews.
     */
    @Query("""
            select coalesce(
                avg(review.rating),
                0
            )
            from CustomerReview review

            where review.moderationStatus =
                  romelt_techcare.backend.enums
                          .CustomerReviewModerationStatus.APPROVED

              and review.isPublic = true

              and review.isSpam = false

              and review.customerConsentConfirmed = true

              and review.publishedAt is not null
            """)
    Double calculateAveragePublicRating();

    // =================================================================
    // PUBLIC COUNT BY RATING
    // =================================================================

    /**
     * Counts published public reviews for one rating value.
     *
     * Used for the public 1-star through 5-star distribution.
     */
    @Query("""
            select count(review)
            from CustomerReview review

            where review.moderationStatus =
                  romelt_techcare.backend.enums
                          .CustomerReviewModerationStatus.APPROVED

              and review.isPublic = true

              and review.isSpam = false

              and review.customerConsentConfirmed = true

              and review.publishedAt is not null

              and review.rating = :rating
            """)
    long countPublicReviewsByRating(
            @Param("rating")
            short rating
    );
}