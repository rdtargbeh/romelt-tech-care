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
import romelt_techcare.backend.entity.ContactInquiry;
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
 * Maps review requests into service-compatible entities and persisted
 * reviews into administrator and public responses.
 *
 * Security:
 * Public mapping excludes email, phone, IP addresses, user agents,
 * consent evidence, moderation notes, rejection reasons, and internal
 * administrator metadata.
 * ================================================================
 */
@Component
@RequiredArgsConstructor
public class CustomerReviewMapper {

    private final WebsiteMediaAssetMapper websiteMediaAssetMapper;

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
                .reviewerEmail(request.reviewerEmail())
                .reviewerPhone(request.reviewerPhone())
                .reviewTitle(request.reviewTitle())
                .reviewText(request.reviewText())
                .rating(request.rating())
                .reviewSource(request.reviewSource())
                .externalSourceUrl(
                        request.externalSourceUrl()
                )
                .isVerifiedCustomer(
                        Boolean.TRUE.equals(
                                request.isVerifiedCustomer()
                        )
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
                .reviewerEmail(request.reviewerEmail())
                .reviewerPhone(request.reviewerPhone())
                .reviewTitle(request.reviewTitle())
                .reviewText(request.reviewText())
                .rating(request.rating())
                .reviewSource(request.reviewSource())
                .externalSourceUrl(
                        request.externalSourceUrl()
                )
                .build();
    }

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

        ContactInquiry inquiry =
                review.getContactInquiry();

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
                inquiry == null
                        ? null
                        : inquiry.getContactInquiryId(),
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
                resolvePublicDisplayName(review),
                review.getReviewerDisplayPreference(),
                review.getReviewerEmail(),
                review.getReviewerPhone(),
                review.getReviewTitle(),
                review.getReviewText(),
                review.getRating(),
                review.getReviewSource(),
                review.getExternalSourceUrl(),
                publicMedia(review),
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
                adminId(review.getRespondedByAdminUser()),
                adminName(review.getRespondedByAdminUser()),
                review.getSubmissionIpAddress(),
                review.getSubmissionUserAgent(),
                review.getSpamScore(),
                review.getIsSpam(),
                review.getSubmittedAt(),
                review.getModeratedAt(),
                review.getPublishedAt(),
                review.getHiddenAt(),
                review.getArchivedAt(),
                adminId(review.getCreatedByAdminUser()),
                adminName(review.getCreatedByAdminUser()),
                adminId(review.getUpdatedByAdminUser()),
                adminName(review.getUpdatedByAdminUser()),
                adminId(review.getModeratedByAdminUser()),
                adminName(review.getModeratedByAdminUser()),
                adminId(review.getPublishedByAdminUser()),
                adminName(review.getPublishedByAdminUser()),
                adminId(review.getHiddenByAdminUser()),
                adminName(review.getHiddenByAdminUser()),
                adminId(review.getArchivedByAdminUser()),
                adminName(review.getArchivedByAdminUser()),
                review.getCreatedAt(),
                review.getUpdatedAt(),
                review.getRowVersion()
        );
    }

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
                resolvePublicDisplayName(review),
                review.getReviewTitle(),
                review.getReviewText(),
                review.getRating(),
                review.getReviewSource(),
                review.getExternalSourceUrl(),
                publicMedia(review),
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

        return switch (
                review.getReviewerDisplayPreference()
                ) {
            case ANONYMOUS -> "Anonymous Customer";

            case FIRST_NAME_ONLY ->
                    firstName(name);

            case FIRST_NAME_LAST_INITIAL ->
                    firstNameLastInitial(name);

            case FULL_NAME,
                 CUSTOM -> name;
        };
    }

    private PublicWebsiteMediaAssetResponse publicMedia(
            CustomerReview review
    ) {
        if (
                review == null
                        || review.getCustomerPhotoMedia() == null
        ) {
            return null;
        }

        return websiteMediaAssetMapper.toPublicResponse(
                review.getCustomerPhotoMedia()
        );
    }

    private String firstName(
            String value
    ) {
        if (value == null) {
            return null;
        }

        String[] parts =
                value.trim().split("\\s+");

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
                value.trim().split("\\s+");

        if (parts.length == 0) {
            return null;
        }

        if (parts.length == 1) {
            return parts[0];
        }

        return parts[0]
                + " "
                + Character.toUpperCase(
                parts[parts.length - 1].charAt(0)
        )
                + ".";
    }

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

        if (firstName != null && lastName != null) {
            return firstName + " " + lastName;
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

    private String normalizeOptional(
            String value
    ) {
        if (value == null) {
            return null;
        }

        String normalized = value.trim();

        return normalized.isEmpty()
                ? null
                : normalized;
    }
}