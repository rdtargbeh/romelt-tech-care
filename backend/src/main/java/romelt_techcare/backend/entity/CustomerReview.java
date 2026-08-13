package romelt_techcare.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import romelt_techcare.backend.enums.CustomerReviewDisplayPreference;
import romelt_techcare.backend.enums.CustomerReviewModerationStatus;
import romelt_techcare.backend.enums.CustomerReviewSource;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

/**
 * ================================================================
 * ROMELT TECHCARE — CUSTOMER REVIEW ENTITY
 * ================================================================
 *
 * Purpose:
 * Stores customer ratings, testimonials, moderation information,
 * publication state, consent evidence, and optional administrator
 * responses.
 *
 * Core business rule:
 * Every Romelt TechCare customer review represents feedback about an
 * actual service received through a completed, review-eligible
 * BookingRequest.
 *
 * The completed booking is the authoritative relationship that proves:
 * - the customer received a Romelt TechCare service;
 * - which customer received the service;
 * - which service was performed;
 * - whether the service is eligible for review.
 *
 * Review ownership:
 *
 * Customer
 *      -> BookingRequest
 *          -> WebsiteService
 *              -> CustomerReview
 *
 * ContactInquiry is intentionally not part of customer-review
 * ownership. A contact inquiry does not prove that a service was
 * actually received.
 *
 * Review source describes HOW or WHERE the customer communicated the
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
 * Review source itself does not establish customer verification.
 *
 * Responsibilities:
 * - Supports verified reviews submitted through secure invitation
 *   tokens.
 * - Supports administrator recording of service-based feedback
 *   communicated by phone, email, Google, Facebook, or another
 *   supported source.
 * - Requires every persisted review to remain associated with its
 *   completed booking and performed service.
 * - Preserves consent information required for public publication.
 * - Supports moderation, approval, rejection, spam classification,
 *   hiding, archival, and featured placement.
 * - Protects private customer email and phone information from public
 *   responses.
 * - Supports optimistic locking.
 *
 * Immutable service identity:
 * Once a review has been created, normal review-content editing must
 * not replace:
 * - bookingRequest;
 * - reviewerEmail;
 * - reviewerPhone;
 * - websiteService;
 * - isVerifiedCustomer;
 * - reviewInvitation.
 *
 * Those fields represent authoritative customer/service history.
 *
 * Publication rules:
 * A review may be public only when:
 * - moderationStatus is APPROVED;
 * - customer consent is confirmed;
 * - the review is not spam;
 * - publishedAt is populated.
 *
 * Uniqueness:
 * - One review may be submitted per invitation.
 * - One review may be associated with each booking request.
 * ================================================================
 */
@Entity
@Table(
        name = "customer_reviews",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_customer_review_invitation",
                        columnNames = "review_invitation_id"
                ),
                @UniqueConstraint(
                        name = "uk_customer_review_booking",
                        columnNames = "booking_request_id"
                )
        },
        indexes = {
                @Index(
                        name = "idx_customer_reviews_moderation",
                        columnList = "moderation_status, submitted_at"
                ),
                @Index(
                        name = "idx_customer_reviews_public",
                        columnList = "is_public, is_featured, published_at"
                ),
                @Index(
                        name = "idx_customer_reviews_rating",
                        columnList = "rating, published_at"
                ),
                @Index(
                        name = "idx_customer_reviews_service",
                        columnList = "service_id, moderation_status, published_at"
                ),
                @Index(
                        name = "idx_customer_reviews_email",
                        columnList = "reviewer_email"
                ),
                @Index(
                        name = "idx_customer_reviews_submitted",
                        columnList = "submitted_at"
                )
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerReview {

    // =================================================================
    // IDENTITY
    // =================================================================

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(
            name = "customer_review_id",
            nullable = false,
            updatable = false
    )
    private UUID customerReviewId;

    // =================================================================
    // REVIEW INVITATION
    // =================================================================

    /**
     * Secure invitation used by the customer to submit the review.
     *
     * Null when an administrator records legitimate customer feedback
     * received through another supported source such as phone, email,
     * Google, or Facebook.
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "review_invitation_id",
            unique = true
    )
    private CustomerReviewInvitation reviewInvitation;

    // =================================================================
    // COMPLETED BOOKING
    // =================================================================

    /**
     * Completed service booking associated with this review.
     *
     * REQUIRED.
     *
     * Every customer review must be anchored to the actual completed
     * BookingRequest that proves the customer received the service.
     *
     * The service layer is responsible for validating that the booking:
     * - is COMPLETED;
     * - has completedAt;
     * - is review eligible;
     * - has customerId;
     * - has serviceId.
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "booking_request_id",
            nullable = false,
            unique = true
    )
    private BookingRequest bookingRequest;

    // =================================================================
    // SERVICE
    // =================================================================

    /**
     * Website service actually performed for the customer.
     *
     * REQUIRED.
     *
     * This relationship must be derived by the backend from:
     *
     * bookingRequest.serviceId
     *
     * It must never be independently selected or overridden by a
     * public or administrator browser request.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "service_id",
            nullable = false
    )
    private WebsiteService websiteService;

    // =================================================================
    // REVIEWER DISPLAY IDENTITY
    // =================================================================

    /**
     * Internal customer name snapshot used for public display-name
     * resolution.
     *
     * For standard display preferences this value is resolved from the
     * reusable Customer associated with bookingRequest.customerId.
     *
     * For CUSTOM display preference this may contain the customer's
     * explicitly selected display name.
     *
     * For ANONYMOUS this value may be null.
     */
    @Column(
            name = "reviewer_display_name",
            length = 180
    )
    private String reviewerDisplayName;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(
            name = "reviewer_display_preference",
            nullable = false,
            length = 40
    )
    private CustomerReviewDisplayPreference reviewerDisplayPreference =
            CustomerReviewDisplayPreference.FIRST_NAME_LAST_INITIAL;

    // =================================================================
    // CUSTOMER CONTACT SNAPSHOTS
    // =================================================================

    /**
     * Customer email snapshot stored when the review is created.
     *
     * This value is resolved by the backend from the reusable Customer
     * linked through bookingRequest.customerId.
     *
     * It is not editable through normal review-content updates.
     */
    @Column(
            name = "reviewer_email",
            length = 254
    )
    private String reviewerEmail;

    /**
     * Customer telephone snapshot stored when the review is created.
     *
     * This value is resolved by the backend from the reusable Customer
     * linked through bookingRequest.customerId.
     *
     * It is not editable through normal review-content updates.
     */
    @Column(
            name = "reviewer_phone",
            length = 40
    )
    private String reviewerPhone;

    // =================================================================
    // REVIEW CONTENT
    // =================================================================

    @Column(
            name = "review_title",
            length = 255
    )
    private String reviewTitle;

    /**
     * Customer's review/testimonial content.
     *
     * REQUIRED.
     *
     * Blank strings are normalized to null before entity validation,
     * therefore both null and blank review text are rejected.
     */
    @Column(
            name = "review_text",
            nullable = false,
            columnDefinition = "text"
    )
    private String reviewText;

    @Column(
            name = "rating",
            nullable = false
    )
    private Short rating;

    // =================================================================
    // REVIEW SOURCE
    // =================================================================

    /**
     * Describes WHERE or HOW the customer communicated the feedback.
     *
     * Customer verification comes from the associated completed
     * booking, not from this value.
     */
    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(
            name = "review_source",
            nullable = false,
            length = 40
    )
    private CustomerReviewSource reviewSource =
            CustomerReviewSource.WEBSITE;

    /**
     * Original external review URL when applicable.
     *
     * Examples:
     * - Google review URL
     * - Facebook recommendation URL
     */
    @Column(
            name = "external_source_url",
            length = 1500
    )
    private String externalSourceUrl;

    // =================================================================
    // CUSTOMER PHOTO
    // =================================================================

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_photo_media_id")
    private WebsiteMediaAsset customerPhotoMedia;

    // =================================================================
    // VERIFIED CUSTOMER
    // =================================================================

    /**
     * Indicates that the review is connected to a verified customer
     * service relationship.
     *
     * For the active Romelt TechCare review workflow this value is
     * established by the backend after validating the completed
     * booking.
     *
     * It must never be controllable through public or administrator
     * review DTOs.
     */
    @Builder.Default
    @Column(
            name = "is_verified_customer",
            nullable = false
    )
    private Boolean isVerifiedCustomer = false;

    // =================================================================
    // CUSTOMER CONSENT
    // =================================================================

    @Builder.Default
    @Column(
            name = "customer_consent_confirmed",
            nullable = false
    )
    private Boolean customerConsentConfirmed = false;

    @Column(name = "customer_consent_confirmed_at")
    private Instant customerConsentConfirmedAt;

    @Column(
            name = "customer_consent_version",
            length = 50
    )
    private String customerConsentVersion;

    @Column(
            name = "customer_consent_ip_address",
            length = 64
    )
    private String customerConsentIpAddress;

    // =================================================================
    // MODERATION
    // =================================================================

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(
            name = "moderation_status",
            nullable = false,
            length = 30
    )
    private CustomerReviewModerationStatus moderationStatus =
            CustomerReviewModerationStatus.PENDING;

    @Column(
            name = "moderation_notes",
            columnDefinition = "text"
    )
    private String moderationNotes;

    @Column(
            name = "rejection_reason",
            length = 500
    )
    private String rejectionReason;

    // =================================================================
    // PUBLICATION
    // =================================================================

    @Builder.Default
    @Column(
            name = "is_public",
            nullable = false
    )
    private Boolean isPublic = false;

    @Builder.Default
    @Column(
            name = "is_featured",
            nullable = false
    )
    private Boolean isFeatured = false;

    // =================================================================
    // ADMIN RESPONSE
    // =================================================================

    @Column(
            name = "admin_response",
            columnDefinition = "text"
    )
    private String adminResponse;

    @Column(name = "responded_at")
    private Instant respondedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "responded_by_admin_user_id")
    private AdminUser respondedByAdminUser;

    // =================================================================
    // SUBMISSION AUDIT
    // =================================================================

    @Column(
            name = "submission_ip_address",
            length = 64
    )
    private String submissionIpAddress;

    @Column(
            name = "submission_user_agent",
            length = 500
    )
    private String submissionUserAgent;

    // =================================================================
    // SPAM
    // =================================================================

    @Column(
            name = "spam_score",
            precision = 5,
            scale = 2
    )
    private BigDecimal spamScore;

    @Builder.Default
    @Column(
            name = "is_spam",
            nullable = false
    )
    private Boolean isSpam = false;

    // =================================================================
    // REVIEW LIFECYCLE
    // =================================================================

    @Column(
            name = "submitted_at",
            nullable = false
    )
    private Instant submittedAt;

    @Column(name = "moderated_at")
    private Instant moderatedAt;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "hidden_at")
    private Instant hiddenAt;

    @Column(name = "archived_at")
    private Instant archivedAt;

    // =================================================================
    // ADMINISTRATOR AUDIT RELATIONSHIPS
    // =================================================================

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_admin_user_id")
    private AdminUser createdByAdminUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by_admin_user_id")
    private AdminUser updatedByAdminUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "moderated_by_admin_user_id")
    private AdminUser moderatedByAdminUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "published_by_admin_user_id")
    private AdminUser publishedByAdminUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hidden_by_admin_user_id")
    private AdminUser hiddenByAdminUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "archived_by_admin_user_id")
    private AdminUser archivedByAdminUser;

    // =================================================================
    // ENTITY AUDIT
    // =================================================================

    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private Instant createdAt;

    @Column(
            name = "updated_at",
            nullable = false
    )
    private Instant updatedAt;

    @Version
    @Column(
            name = "row_version",
            nullable = false
    )
    private Long rowVersion;

    // =================================================================
    // ENTITY LIFECYCLE
    // =================================================================

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();

        initializeDefaults();
        normalizeFields();

        if (submittedAt == null) {
            submittedAt = now;
        }

        if (createdAt == null) {
            createdAt = now;
        }

        updatedAt = now;

        validateState();
    }

    @PreUpdate
    protected void onUpdate() {
        initializeDefaults();
        normalizeFields();

        updatedAt = Instant.now();

        validateState();
    }

    // =================================================================
    // EDITABLE REVIEW CONTENT
    // =================================================================

    /**
     * Updates administrator-editable review content.
     *
     * This operation intentionally does NOT accept or modify:
     *
     * - reviewerEmail
     * - reviewerPhone
     * - websiteService
     * - bookingRequest
     * - reviewInvitation
     * - isVerifiedCustomer
     *
     * Those fields represent authoritative customer/service history
     * established through the completed BookingRequest.
     */
    public void updateContent(
            String newDisplayName,
            CustomerReviewDisplayPreference newDisplayPreference,
            String newReviewTitle,
            String newReviewText,
            Short newRating,
            CustomerReviewSource newReviewSource,
            String newExternalSourceUrl,
            WebsiteMediaAsset newCustomerPhotoMedia,
            AdminUser administrator
    ) {
        ensureEditable();

        reviewerDisplayName = newDisplayName;
        reviewerDisplayPreference = newDisplayPreference;

        reviewTitle = newReviewTitle;
        reviewText = newReviewText;

        rating = newRating;

        reviewSource = newReviewSource;
        externalSourceUrl = newExternalSourceUrl;

        customerPhotoMedia = newCustomerPhotoMedia;

        updatedByAdminUser = administrator;
    }

    // =================================================================
    // CONSENT
    // =================================================================

    public void confirmConsent(
            String consentVersion,
            String ipAddress
    ) {
        customerConsentConfirmed = true;
        customerConsentConfirmedAt = Instant.now();

        customerConsentVersion = consentVersion;
        customerConsentIpAddress = ipAddress;
    }

    // =================================================================
    // MODERATION
    // =================================================================

    public void approve(
            String notes,
            AdminUser administrator
    ) {
        ensureNotArchived();

        moderationStatus =
                CustomerReviewModerationStatus.APPROVED;

        moderationNotes = notes;
        rejectionReason = null;

        moderatedAt = Instant.now();
        moderatedByAdminUser = administrator;

        isSpam = false;

        hiddenAt = null;
        hiddenByAdminUser = null;

        archivedAt = null;
        archivedByAdminUser = null;

        updatedByAdminUser = administrator;
    }

    public void reject(
            String reason,
            String notes,
            AdminUser administrator
    ) {
        ensureNotArchived();

        moderationStatus =
                CustomerReviewModerationStatus.REJECTED;

        rejectionReason = reason;
        moderationNotes = notes;

        moderatedAt = Instant.now();
        moderatedByAdminUser = administrator;

        removeFromPublicDisplay();

        updatedByAdminUser = administrator;
    }

    public void markSpam(
            BigDecimal newSpamScore,
            String notes,
            AdminUser administrator
    ) {
        ensureNotArchived();

        moderationStatus =
                CustomerReviewModerationStatus.SPAM;

        isSpam = true;
        spamScore = newSpamScore;
        moderationNotes = notes;

        moderatedAt = Instant.now();
        moderatedByAdminUser = administrator;

        removeFromPublicDisplay();

        updatedByAdminUser = administrator;
    }

    // =================================================================
    // PUBLICATION
    // =================================================================

    public void publish(
            boolean featured,
            AdminUser administrator
    ) {
        if (
                moderationStatus
                        != CustomerReviewModerationStatus.APPROVED
        ) {
            throw new IllegalStateException(
                    "Only an approved review may be published."
            );
        }

        if (
                !Boolean.TRUE.equals(
                        customerConsentConfirmed
                )
        ) {
            throw new IllegalStateException(
                    "Customer consent is required before publication."
            );
        }

        if (
                Boolean.TRUE.equals(
                        isSpam
                )
        ) {
            throw new IllegalStateException(
                    "A spam review cannot be published."
            );
        }

        isPublic = true;
        isFeatured = featured;

        publishedAt = Instant.now();
        publishedByAdminUser = administrator;

        hiddenAt = null;
        hiddenByAdminUser = null;

        updatedByAdminUser = administrator;
    }

    public void unpublish(
            AdminUser administrator
    ) {
        isPublic = false;
        isFeatured = false;

        publishedAt = null;
        publishedByAdminUser = null;

        updatedByAdminUser = administrator;
    }

    public void setFeatured(
            boolean featured,
            AdminUser administrator
    ) {
        if (
                featured
                        && !Boolean.TRUE.equals(
                        isPublic
                )
        ) {
            throw new IllegalStateException(
                    "Only a public review may be featured."
            );
        }

        isFeatured = featured;

        updatedByAdminUser = administrator;
    }

    // =================================================================
    // VISIBILITY / ARCHIVAL
    // =================================================================

    public void hide(
            String notes,
            AdminUser administrator
    ) {
        ensureNotArchived();

        moderationStatus =
                CustomerReviewModerationStatus.HIDDEN;

        moderationNotes = notes;

        hiddenAt = Instant.now();
        hiddenByAdminUser = administrator;

        removeFromPublicDisplay();

        updatedByAdminUser = administrator;
    }

    public void archive(
            AdminUser administrator
    ) {
        moderationStatus =
                CustomerReviewModerationStatus.ARCHIVED;

        archivedAt = Instant.now();
        archivedByAdminUser = administrator;

        removeFromPublicDisplay();

        updatedByAdminUser = administrator;
    }

    // =================================================================
    // ADMIN RESPONSE
    // =================================================================

    public void addAdminResponse(
            String response,
            AdminUser administrator
    ) {
        adminResponse = response;

        respondedAt = Instant.now();
        respondedByAdminUser = administrator;

        updatedByAdminUser = administrator;
    }

    public void removeAdminResponse(
            AdminUser administrator
    ) {
        adminResponse = null;

        respondedAt = null;
        respondedByAdminUser = null;

        updatedByAdminUser = administrator;
    }

    // =================================================================
    // PUBLIC VISIBILITY
    // =================================================================

    public boolean isPubliclyVisible() {
        return moderationStatus
                == CustomerReviewModerationStatus.APPROVED

                && Boolean.TRUE.equals(
                isPublic
        )

                && Boolean.TRUE.equals(
                customerConsentConfirmed
        )

                && !Boolean.TRUE.equals(
                isSpam
        )

                && publishedAt != null;
    }

    private void removeFromPublicDisplay() {
        isPublic = false;
        isFeatured = false;

        publishedAt = null;
        publishedByAdminUser = null;
    }

    // =================================================================
    // STATE GUARDS
    // =================================================================

    private void ensureEditable() {
        if (
                moderationStatus
                        == CustomerReviewModerationStatus.ARCHIVED
        ) {
            throw new IllegalStateException(
                    "An archived review cannot be edited."
            );
        }
    }

    private void ensureNotArchived() {
        if (
                moderationStatus
                        == CustomerReviewModerationStatus.ARCHIVED
        ) {
            throw new IllegalStateException(
                    "An archived review cannot be moderated."
            );
        }
    }

    // =================================================================
    // DEFAULTS
    // =================================================================

    private void initializeDefaults() {
        if (reviewerDisplayPreference == null) {
            reviewerDisplayPreference =
                    CustomerReviewDisplayPreference
                            .FIRST_NAME_LAST_INITIAL;
        }

        if (reviewSource == null) {
            reviewSource =
                    CustomerReviewSource.WEBSITE;
        }

        if (moderationStatus == null) {
            moderationStatus =
                    CustomerReviewModerationStatus.PENDING;
        }

        if (isVerifiedCustomer == null) {
            isVerifiedCustomer = false;
        }

        if (customerConsentConfirmed == null) {
            customerConsentConfirmed = false;
        }

        if (isPublic == null) {
            isPublic = false;
        }

        if (isFeatured == null) {
            isFeatured = false;
        }

        if (isSpam == null) {
            isSpam = false;
        }
    }

    // =================================================================
    // NORMALIZATION
    // =================================================================

    private void normalizeFields() {
        reviewerDisplayName =
                normalizeOptional(
                        reviewerDisplayName
                );

        reviewerEmail =
                normalizeEmail(
                        reviewerEmail
                );

        reviewerPhone =
                normalizeOptional(
                        reviewerPhone
                );

        reviewTitle =
                normalizeOptional(
                        reviewTitle
                );

        reviewText =
                normalizeOptional(
                        reviewText
                );

        externalSourceUrl =
                normalizeOptional(
                        externalSourceUrl
                );

        customerConsentVersion =
                normalizeOptional(
                        customerConsentVersion
                );

        customerConsentIpAddress =
                normalizeOptional(
                        customerConsentIpAddress
                );

        moderationNotes =
                normalizeOptional(
                        moderationNotes
                );

        rejectionReason =
                normalizeOptional(
                        rejectionReason
                );

        adminResponse =
                normalizeOptional(
                        adminResponse
                );

        submissionIpAddress =
                normalizeOptional(
                        submissionIpAddress
                );

        submissionUserAgent =
                normalizeOptional(
                        submissionUserAgent
                );
    }

    // =================================================================
    // ENTITY VALIDATION
    // =================================================================

    private void validateState() {

        // -------------------------------------------------------------
        // REQUIRED COMPLETED-SERVICE RELATIONSHIPS
        // -------------------------------------------------------------

        if (bookingRequest == null) {
            throw new IllegalStateException(
                    "Customer review must be associated with a completed booking."
            );
        }

        if (websiteService == null) {
            throw new IllegalStateException(
                    "Customer review must be associated with the service performed."
            );
        }

        // -------------------------------------------------------------
        // RATING
        // -------------------------------------------------------------

        if (
                rating == null
                        || rating < 1
                        || rating > 5
        ) {
            throw new IllegalStateException(
                    "Rating must be between 1 and 5."
            );
        }

        // -------------------------------------------------------------
        // REVIEW TEXT
        // -------------------------------------------------------------

        /*
         * normalizeFields() converts blank review text to null before
         * this validation executes.
         */
        if (reviewText == null) {
            throw new IllegalStateException(
                    "Review text is required."
            );
        }

        // -------------------------------------------------------------
        // DISPLAY NAME
        // -------------------------------------------------------------

        if (
                reviewerDisplayPreference
                        != CustomerReviewDisplayPreference.ANONYMOUS

                        && reviewerDisplayName == null
        ) {
            throw new IllegalStateException(
                    "Reviewer display name is required unless the review is anonymous."
            );
        }

        // -------------------------------------------------------------
        // CONSENT
        // -------------------------------------------------------------

        if (
                Boolean.TRUE.equals(
                        customerConsentConfirmed
                )
                        && (
                        customerConsentConfirmedAt == null
                                || customerConsentVersion == null
                )
        ) {
            throw new IllegalStateException(
                    "Confirmed consent requires a timestamp and consent version."
            );
        }

        // -------------------------------------------------------------
        // PUBLIC REVIEW STATE
        // -------------------------------------------------------------

        if (
                Boolean.TRUE.equals(
                        isPublic
                )
        ) {
            if (
                    moderationStatus
                            != CustomerReviewModerationStatus.APPROVED
            ) {
                throw new IllegalStateException(
                        "A public review must be approved."
                );
            }

            if (
                    !Boolean.TRUE.equals(
                            customerConsentConfirmed
                    )
            ) {
                throw new IllegalStateException(
                        "A public review requires customer consent."
                );
            }

            if (
                    Boolean.TRUE.equals(
                            isSpam
                    )
            ) {
                throw new IllegalStateException(
                        "A spam review cannot be public."
                );
            }

            if (publishedAt == null) {
                throw new IllegalStateException(
                        "A public review requires a publication timestamp."
                );
            }
        }

        // -------------------------------------------------------------
        // FEATURED STATE
        // -------------------------------------------------------------

        if (
                Boolean.TRUE.equals(
                        isFeatured
                )
                        && !Boolean.TRUE.equals(
                        isPublic
                )
        ) {
            throw new IllegalStateException(
                    "A featured review must be public."
            );
        }

        // -------------------------------------------------------------
        // PUBLICATION TIMESTAMP
        // -------------------------------------------------------------

        if (
                publishedAt != null
                        && (
                        !Boolean.TRUE.equals(
                                isPublic
                        )
                                || moderationStatus
                                != CustomerReviewModerationStatus.APPROVED
                )
        ) {
            throw new IllegalStateException(
                    "A publication timestamp requires a public approved review."
            );
        }

        // -------------------------------------------------------------
        // ADMINISTRATOR RESPONSE
        // -------------------------------------------------------------

        if (
                respondedAt != null
                        && adminResponse == null
        ) {
            throw new IllegalStateException(
                    "A response timestamp requires an administrator response."
            );
        }

        // -------------------------------------------------------------
        // HIDDEN STATE
        // -------------------------------------------------------------

        if (
                moderationStatus
                        == CustomerReviewModerationStatus.HIDDEN
                        && hiddenAt == null
        ) {
            throw new IllegalStateException(
                    "A hidden review requires a hidden timestamp."
            );
        }

        // -------------------------------------------------------------
        // ARCHIVED STATE
        // -------------------------------------------------------------

        if (
                moderationStatus
                        == CustomerReviewModerationStatus.ARCHIVED
                        && archivedAt == null
        ) {
            throw new IllegalStateException(
                    "An archived review requires an archived timestamp."
            );
        }

        // -------------------------------------------------------------
        // SPAM SCORE
        // -------------------------------------------------------------

        if (
                spamScore != null
                        && (
                        spamScore.compareTo(
                                BigDecimal.ZERO
                        ) < 0

                                || spamScore.compareTo(
                                BigDecimal.valueOf(100)
                        ) > 0
                )
        ) {
            throw new IllegalStateException(
                    "Spam score must be between 0 and 100."
            );
        }
    }

    // =================================================================
    // NORMALIZATION HELPERS
    // =================================================================

    private String normalizeEmail(
            String value
    ) {
        String normalized =
                normalizeOptional(
                        value
                );

        return normalized == null
                ? null
                : normalized.toLowerCase(
                Locale.ROOT
        );
    }

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