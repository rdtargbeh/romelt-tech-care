package romelt_techcare.backend.mapper;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import romelt_techcare.backend.dto.CustomerReviewAdminCreateRequest;
import romelt_techcare.backend.dto.CustomerReviewResponse;
import romelt_techcare.backend.dto.CustomerReviewUpdateRequest;
import romelt_techcare.backend.dto.PublicCustomerReviewResponse;
import romelt_techcare.backend.dto.PublicWebsiteMediaAssetResponse;
import romelt_techcare.backend.entity.AdminUser;
import romelt_techcare.backend.entity.BookingRequest;
import romelt_techcare.backend.entity.CustomerReview;
import romelt_techcare.backend.entity.CustomerReviewInvitation;
import romelt_techcare.backend.entity.WebsiteService;

import java.util.UUID;

/**
 * ================================================================
 * ROMELT TECHCARE — CUSTOMER REVIEW MAPPER
 * ================================================================
 *
 * Purpose:
 * Maps customer-review request DTOs into service-compatible entities
 * and persisted CustomerReview records into administrator and public
 * responses.
 *
 * Core business rule:
 * Every Romelt TechCare customer review represents feedback about an
 * actual completed, review-eligible BookingRequest.
 *
 * Review ownership:
 *
 * Customer
 *      -> BookingRequest
 *          -> WebsiteService
 *              -> CustomerReview
 *
 * ContactInquiry is intentionally not part of review ownership.
 *
 * Administrator-created review mapping:
 * The administrator request supplies only review-specific information:
 * - display preference;
 * - optional custom display name;
 * - review title;
 * - review text;
 * - rating;
 * - review source;
 * - optional external source URL;
 * - consent information.
 *
 * The following values are NOT trusted from the browser:
 * - BookingRequest relationship;
 * - customer identity;
 * - customer email;
 * - customer telephone number;
 * - WebsiteService relationship;
 * - verified-customer status.
 *
 * CustomerReviewService resolves those authoritative values from the
 * completed booking and its linked reusable Customer record.
 *
 * Review source:
 * Review source describes WHERE or HOW the customer communicated the
 * feedback.
 *
 * Examples:
 * - BOOKING_FOLLOW_UP
 * - PHONE
 * - EMAIL
 * - GOOGLE
 * - FACEBOOK
 * - OTHER
 *
 * Review source does not establish customer verification.
 * Verification comes from the completed booking/customer relationship.
 *
 * Security:
 * Public mapping excludes:
 * - customer email;
 * - customer phone;
 * - IP addresses;
 * - user agents;
 * - consent evidence;
 * - moderation notes;
 * - rejection reasons;
 * - internal administrator metadata.
 * ================================================================
 */
@Component
@RequiredArgsConstructor
public class CustomerReviewMapper {

    private final WebsiteMediaAssetMapper websiteMediaAssetMapper;

    // =================================================================
    // ADMIN CREATE REQUEST -> SERVICE-COMPATIBLE ENTITY
    // =================================================================

    /**
     * Maps administrator-provided review content.
     *
     * This deliberately does NOT populate:
     *
     * - bookingRequest
     * - websiteService
     * - reviewerEmail
     * - reviewerPhone
     * - isVerifiedCustomer
     *
     * Those values are authoritative business data and must be resolved
     * by CustomerReviewService from the completed booking and reusable
     * Customer record.
     *
     * bookingRequestId and customerPhotoMediaId are passed separately
     * to the service and therefore are not mapped directly onto this
     * temporary CustomerReview entity.
     */
    public CustomerReview toEntity(
            CustomerReviewAdminCreateRequest request
    ) {
        if (request == null) {
            return null;
        }

        return CustomerReview.builder()
                .reviewerDisplayName(
                        request.reviewerDisplayName()
                )
                .reviewerDisplayPreference(
                        request.reviewerDisplayPreference()
                )
                .reviewTitle(
                        request.reviewTitle()
                )
                .reviewText(
                        request.reviewText()
                )
                .rating(
                        request.rating()
                )
                .reviewSource(
                        request.reviewSource()
                )
                .externalSourceUrl(
                        request.externalSourceUrl()
                )
                .customerConsentConfirmed(
                        Boolean.TRUE.equals(
                                request.customerConsentConfirmed()
                        )
                )
                .customerConsentVersion(
                        request.customerConsentVersion()
                )
                .build();
    }

    // =================================================================
    // ADMIN UPDATE REQUEST -> SERVICE-COMPATIBLE ENTITY
    // =================================================================

    /**
     * Maps administrator-editable review content.
     *
     * Normal review updates may change:
     * - display name;
     * - display preference;
     * - title;
     * - review text;
     * - rating;
     * - review source;
     * - external source URL.
     *
     * Normal review updates may NOT change:
     * - booking;
     * - customer identity;
     * - customer email;
     * - customer phone;
     * - service;
     * - verified-customer status.
     *
     * customerPhotoMediaId is passed separately to the service because
     * it must first be resolved to a WebsiteMediaAsset.
     */
    public CustomerReview toUpdateEntity(
            CustomerReviewUpdateRequest request
    ) {
        if (request == null) {
            return null;
        }

        return CustomerReview.builder()
                .reviewerDisplayName(
                        request.reviewerDisplayName()
                )
                .reviewerDisplayPreference(
                        request.reviewerDisplayPreference()
                )
                .reviewTitle(
                        request.reviewTitle()
                )
                .reviewText(
                        request.reviewText()
                )
                .rating(
                        request.rating()
                )
                .reviewSource(
                        request.reviewSource()
                )
                .externalSourceUrl(
                        request.externalSourceUrl()
                )
                .build();
    }

    // =================================================================
    // ADMIN RESPONSE
    // =================================================================

    /**
     * Maps a persisted CustomerReview into the complete administrator
     * response.
     *
     * The response exposes the authoritative booking/service
     * relationships and private review-management information required
     * by the administrator interface.
     */
    public CustomerReviewResponse toResponse(
            CustomerReview review
    ) {
        if (review == null) {
            return null;
        }

        CustomerReviewInvitation invitation =
                review.getReviewInvitation();

        BookingRequest booking =
                review.getBookingRequest();

        WebsiteService service =
                review.getWebsiteService();

        return new CustomerReviewResponse(
                review.getCustomerReviewId(),

                invitation == null
                        ? null
                        : invitation.getReviewInvitationId(),

                booking == null
                        ? null
                        : booking.getBookingRequestId(),

                booking == null
                        ? null
                        : booking.getReferenceNumber(),

                service == null
                        ? null
                        : service.getServiceId(),

                service == null
                        ? null
                        : service.getServiceCode(),

                service == null
                        ? null
                        : service.getServiceSlug(),

                review.getReviewerDisplayName(),

                resolvePublicDisplayName(
                        review
                ),

                review.getReviewerDisplayPreference(),

                review.getReviewerEmail(),

                review.getReviewerPhone(),

                review.getReviewTitle(),

                review.getReviewText(),

                review.getRating(),

                review.getReviewSource(),

                review.getExternalSourceUrl(),

                publicMedia(
                        review
                ),

                review.getIsVerifiedCustomer(),

                review.getCustomerConsentConfirmed(),

                review.getCustomerConsentConfirmedAt(),

                review.getCustomerConsentVersion(),

                review.getCustomerConsentIpAddress(),

                review.getModerationStatus(),

                review.getModerationNotes(),

                review.getRejectionReason(),

                review.getIsPublic(),

                review.getIsFeatured(),

                review.isPubliclyVisible(),

                review.getAdminResponse(),

                review.getRespondedAt(),

                adminId(
                        review.getRespondedByAdminUser()
                ),

                adminName(
                        review.getRespondedByAdminUser()
                ),

                review.getSubmissionIpAddress(),

                review.getSubmissionUserAgent(),

                review.getSpamScore(),

                review.getIsSpam(),

                review.getSubmittedAt(),

                review.getModeratedAt(),

                review.getPublishedAt(),

                review.getHiddenAt(),

                review.getArchivedAt(),

                adminId(
                        review.getCreatedByAdminUser()
                ),

                adminName(
                        review.getCreatedByAdminUser()
                ),

                adminId(
                        review.getUpdatedByAdminUser()
                ),

                adminName(
                        review.getUpdatedByAdminUser()
                ),

                adminId(
                        review.getModeratedByAdminUser()
                ),

                adminName(
                        review.getModeratedByAdminUser()
                ),

                adminId(
                        review.getPublishedByAdminUser()
                ),

                adminName(
                        review.getPublishedByAdminUser()
                ),

                adminId(
                        review.getHiddenByAdminUser()
                ),

                adminName(
                        review.getHiddenByAdminUser()
                ),

                adminId(
                        review.getArchivedByAdminUser()
                ),

                adminName(
                        review.getArchivedByAdminUser()
                ),

                review.getCreatedAt(),

                review.getUpdatedAt(),

                review.getRowVersion()
        );
    }

    // =================================================================
    // PUBLIC RESPONSE
    // =================================================================

    /**
     * Creates the safe public representation of an approved,
     * published review.
     *
     * Private customer identity/contact information is deliberately
     * excluded.
     */
    public PublicCustomerReviewResponse toPublicResponse(
            CustomerReview review
    ) {
        if (review == null) {
            return null;
        }

        WebsiteService service =
                review.getWebsiteService();

        return new PublicCustomerReviewResponse(
                review.getCustomerReviewId(),

                resolvePublicDisplayName(
                        review
                ),

                review.getReviewTitle(),

                review.getReviewText(),

                review.getRating(),

                review.getReviewSource(),

                review.getExternalSourceUrl(),

                publicMedia(
                        review
                ),

                review.getIsVerifiedCustomer(),

                review.getIsFeatured(),

                service == null
                        ? null
                        : service.getServiceCode(),

                service == null
                        ? null
                        : service.getServiceSlug(),

                review.getAdminResponse(),

                review.getRespondedAt(),

                review.getPublishedAt()
        );
    }

    // =================================================================
    // PUBLIC DISPLAY NAME
    // =================================================================

    /**
     * Resolves the name that may safely be shown publicly according to
     * the customer's selected display preference.
     *
     * reviewerDisplayName stores the internally resolved or
     * customer-authorized name snapshot.
     */
    public String resolvePublicDisplayName(
            CustomerReview review
    ) {
        if (review == null) {
            return null;
        }

        String name =
                normalizeOptional(
                        review.getReviewerDisplayName()
                );

        if (
                review.getReviewerDisplayPreference()
                        == null
        ) {
            return name;
        }

        return switch (
                review.getReviewerDisplayPreference()
                ) {
            case ANONYMOUS ->
                    "Anonymous Customer";

            case FIRST_NAME_ONLY ->
                    firstName(
                            name
                    );

            case FIRST_NAME_LAST_INITIAL ->
                    firstNameLastInitial(
                            name
                    );

            case FULL_NAME,
                 CUSTOM ->
                    name;
        };
    }

    // =================================================================
    // PUBLIC MEDIA
    // =================================================================

    /**
     * Converts the optional customer review photo into its safe public
     * media representation.
     */
    private PublicWebsiteMediaAssetResponse publicMedia(
            CustomerReview review
    ) {
        if (
                review == null
                        || review.getCustomerPhotoMedia() == null
        ) {
            return null;
        }

        return websiteMediaAssetMapper
                .toPublicResponse(
                        review.getCustomerPhotoMedia()
                );
    }

    // =================================================================
    // NAME HELPERS
    // =================================================================

    private String firstName(
            String value
    ) {
        if (value == null) {
            return null;
        }

        String[] parts =
                value
                        .trim()
                        .split("\\s+");

        return parts.length == 0
                ? null
                : parts[0];
    }

    private String firstNameLastInitial(
            String value
    ) {
        if (value == null) {
            return null;
        }

        String[] parts =
                value
                        .trim()
                        .split("\\s+");

        if (parts.length == 0) {
            return null;
        }

        if (parts.length == 1) {
            return parts[0];
        }

        String lastName =
                parts[
                        parts.length - 1
                        ];

        if (lastName.isBlank()) {
            return parts[0];
        }

        return parts[0]
                + " "
                + Character.toUpperCase(
                lastName.charAt(0)
        )
                + ".";
    }

    // =================================================================
    // ADMINISTRATOR HELPERS
    // =================================================================

    private UUID adminId(
            AdminUser administrator
    ) {
        return administrator == null
                ? null
                : administrator.getAdminUserId();
    }

    private String adminName(
            AdminUser administrator
    ) {
        if (administrator == null) {
            return null;
        }

        String firstName =
                normalizeOptional(
                        administrator.getFirstName()
                );

        String lastName =
                normalizeOptional(
                        administrator.getLastName()
                );

        if (
                firstName != null
                        && lastName != null
        ) {
            return firstName
                    + " "
                    + lastName;
        }

        if (firstName != null) {
            return firstName;
        }

        if (lastName != null) {
            return lastName;
        }

        return normalizeOptional(
                administrator.getEmail()
        );
    }

    // =================================================================
    // NORMALIZATION
    // =================================================================

    private String normalizeOptional(
            String value
    ) {
        if (value == null) {
            return null;
        }

        String normalized =
                value.trim();

        return normalized.isEmpty()
                ? null
                : normalized;
    }
}