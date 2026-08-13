package romelt_techcare.backend.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import romelt_techcare.backend.entity.BookingRequest;

import java.util.UUID;

/**
 * ================================================================
 * ROMELT TECHCARE — CUSTOMER REVIEW ELIGIBLE BOOKING REPOSITORY
 * ================================================================
 *
 * Purpose:
 * Provides the customer-review workflow with completed bookings that
 * are currently eligible to receive one customer review.
 *
 * Customer-first review workflow:
 *
 * Customer
 *      -> eligible completed BookingRequest(s)
 *          -> WebsiteService
 *              -> CustomerReview
 *
 * The administrator first selects a Customer.
 *
 * This repository then returns only eligible completed bookings that
 * belong to that specific customer.
 *
 * Core eligibility rules:
 *
 * 1. booking.customerId must equal the selected customerId;
 * 2. booking status must be COMPLETED;
 * 3. completedAt must be populated;
 * 4. reviewEligible must be true;
 * 5. serviceId must be populated;
 * 6. no CustomerReview may already exist for the booking.
 *
 * Important ownership rule:
 *
 * customerId is used here as a search/filter constraint.
 *
 * CustomerReview does NOT need a second independently editable
 * customer relationship because BookingRequest remains the
 * authoritative ownership record:
 *
 * CustomerReview
 *      -> BookingRequest
 *          -> customerId
 *
 * This avoids inconsistent data such as a review pointing to one
 * customer while its booking belongs to another.
 *
 * Search:
 * After the customer has been selected, keyword may further narrow
 * that customer's bookings by:
 * - booking reference number;
 * - customer booking snapshot name;
 * - booking email;
 * - booking phone;
 * - service type.
 *
 * PostgreSQL:
 * The nullable keyword parameter is explicitly cast to String when
 * used with lower()/concat() to avoid PostgreSQL resolving a null
 * parameter as bytea.
 *
 * Real-data integration:
 *
 * GET /api/v1/admin/customer-reviews/eligible-bookings
 *     ?customerId={customerId}
 *     &keyword={optionalKeyword}
 * ================================================================
 */
@Repository
public interface CustomerReviewEligibleBookingRepository
        extends JpaRepository<BookingRequest, UUID> {

    /**
     * Returns review-eligible completed bookings belonging only to
     * the selected customer.
     *
     * customerId is required.
     *
     * keyword is optional.
     */
    @Query("""
            select booking
            from BookingRequest booking

            where booking.customerId = :customerId

              and booking.status =
                  romelt_techcare.backend.enums.BookingRequestStatus.COMPLETED

              and booking.completedAt is not null

              and booking.reviewEligible = true

              and booking.serviceId is not null

              and not exists (
                    select review.customerReviewId
                    from CustomerReview review
                    where review.bookingRequest.bookingRequestId =
                          booking.bookingRequestId
                  )

              and (
                    cast(:keyword as String) is null

                    or lower(
                        coalesce(
                            booking.referenceNumber,
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
                            booking.fullName,
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
                            booking.email,
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
                            booking.phone,
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
                            cast(
                                booking.serviceType
                                as String
                            ),
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

            order by booking.completedAt desc
            """)
    Page<BookingRequest> findEligibleBookingsByCustomer(
            @Param("customerId")
            UUID customerId,

            @Param("keyword")
            String keyword,

            Pageable pageable
    );
}