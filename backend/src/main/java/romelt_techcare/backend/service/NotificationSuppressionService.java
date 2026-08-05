package romelt_techcare.backend.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import romelt_techcare.backend.dto.AdminJwtPrincipal;
import romelt_techcare.backend.dto.NotificationSuppressionCheckRequest;
import romelt_techcare.backend.dto.NotificationSuppressionCheckResponse;
import romelt_techcare.backend.dto.NotificationSuppressionCreateRequest;
import romelt_techcare.backend.dto.NotificationSuppressionResponse;
import romelt_techcare.backend.dto.NotificationSuppressionSearchRequest;
import romelt_techcare.backend.dto.NotificationSuppressionSummaryResponse;
import romelt_techcare.backend.dto.NotificationSuppressionUpdateRequest;

import java.util.List;
import java.util.UUID;

/**
 * ================================================================
 * ROMELT TECHCARE — NOTIFICATION SUPPRESSION SERVICE
 * ================================================================
 *
 * Purpose:
 * Defines EMAIL and SMS suppression management and delivery-checking
 * operations.
 *
 * Responsibilities:
 * - Creates suppression records.
 * - Prevents duplicate active suppressions.
 * - Checks whether a recipient is currently suppressed.
 * - Returns administrator lists and complete details.
 * - Updates reason and expiration information.
 * - Deactivates and reactivates suppression records.
 * - Returns suppression history for customers and recipients.
 * - Deactivates expired suppressions.
 *
 * IN_APP notifications are not address-suppressed.
 * ================================================================
 */
public interface NotificationSuppressionService {

    NotificationSuppressionResponse createSuppression(
            AdminJwtPrincipal principal,
            NotificationSuppressionCreateRequest request
    );

    NotificationSuppressionResponse getSuppression(
            UUID notificationSuppressionId
    );

    Page<NotificationSuppressionSummaryResponse> getSuppressions(
            NotificationSuppressionSearchRequest searchRequest,
            Pageable pageable
    );

    NotificationSuppressionResponse updateSuppression(
            AdminJwtPrincipal principal,
            UUID notificationSuppressionId,
            NotificationSuppressionUpdateRequest request
    );

    NotificationSuppressionResponse deactivateSuppression(
            AdminJwtPrincipal principal,
            UUID notificationSuppressionId
    );

    NotificationSuppressionResponse reactivateSuppression(
            AdminJwtPrincipal principal,
            UUID notificationSuppressionId
    );

    NotificationSuppressionCheckResponse checkSuppression(
            NotificationSuppressionCheckRequest request
    );

    List<NotificationSuppressionResponse> getCustomerSuppressions(
            UUID customerId
    );

    List<NotificationSuppressionResponse> getRecipientSuppressionHistory(
            NotificationSuppressionCheckRequest request
    );

    int deactivateExpiredSuppressions();
}