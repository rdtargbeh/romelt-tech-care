package romelt_techcare.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import romelt_techcare.backend.enums.CustomerReviewDisplayPreference;
import romelt_techcare.backend.enums.CustomerReviewSource;

import java.util.UUID;

/**
 * Administrator request for adding an offline or external review.
 */
public record CustomerReviewAdminCreateRequest(

        UUID bookingRequestId,

        UUID contactInquiryId,

        UUID serviceId,

        @Size(max = 180)
        String reviewerDisplayName,

        @NotNull
        CustomerReviewDisplayPreference reviewerDisplayPreference,

        @Email
        @Size(max = 254)
        String reviewerEmail,

        @Size(max = 40)
        String reviewerPhone,

        @Size(max = 255)
        String reviewTitle,

        @Size(max = 10000)
        String reviewText,

        @NotNull
        @Min(1)
        @Max(5)
        Short rating,

        @NotNull
        CustomerReviewSource reviewSource,

        @Size(max = 1500)
        String externalSourceUrl,

        UUID customerPhotoMediaId,

        Boolean isVerifiedCustomer,

        Boolean customerConsentConfirmed,

        @Size(max = 50)
        String customerConsentVersion
) {
}