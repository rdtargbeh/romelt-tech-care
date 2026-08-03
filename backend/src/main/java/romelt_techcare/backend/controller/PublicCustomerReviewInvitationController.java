package romelt_techcare.backend.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import romelt_techcare.backend.api.ApiResponse;
import romelt_techcare.backend.dto.PublicCustomerReviewInvitationResponse;
import romelt_techcare.backend.mapper.CustomerReviewInvitationMapper;
import romelt_techcare.backend.service.CustomerReviewInvitationService;

/**
 * Validates review-invitation tokens for the public review page.
 *
 * Security:
 * Public token responses must never be cached.
 */
@RestController
@RequestMapping("/api/v1/public/customer-review-invitations")
@RequiredArgsConstructor
public class PublicCustomerReviewInvitationController {

    private final CustomerReviewInvitationService
            invitationService;

    private final CustomerReviewInvitationMapper
            invitationMapper;

    @GetMapping("/validate")
    public ResponseEntity<
            ApiResponse<PublicCustomerReviewInvitationResponse>
            > validateInvitation(
            @RequestParam
            String token,

            HttpServletRequest httpRequest
    ) {
        PublicCustomerReviewInvitationResponse response =
                invitationMapper.toPublicResponse(
                        invitationService
                                .validatePublicToken(token)
                );

        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(
                        ApiResponse.success(
                                "Review invitation validated successfully.",
                                response,
                                httpRequest.getRequestURI()
                        )
                );
    }
}