package romelt_techcare.backend.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import romelt_techcare.backend.dto.NotificationDeliveryAttemptCreateRequest;
import romelt_techcare.backend.dto.NotificationDeliveryAttemptResponse;
import romelt_techcare.backend.dto.NotificationDeliveryAttemptSummaryResponse;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * ================================================================
 * ROMELT TECHCARE — NOTIFICATION DELIVERY ATTEMPT SERVICE
 * ================================================================
 *
 * Purpose:
 * Manages immutable provider-attempt history.
 * ================================================================
 */
public interface NotificationDeliveryAttemptService {

    NotificationDeliveryAttemptResponse create(
            NotificationDeliveryAttemptCreateRequest request
    );

    NotificationDeliveryAttemptResponse get(
            UUID notificationDeliveryAttemptId
    );

    NotificationDeliveryAttemptResponse getLatestAttempt(
            UUID notificationDeliveryId
    );

    List<NotificationDeliveryAttemptResponse> getAttempts(
            UUID notificationDeliveryId
    );

    Page<NotificationDeliveryAttemptSummaryResponse> search(

            UUID notificationDeliveryId,

            String providerName,

            Boolean success,

            Boolean retryable,

            Instant startedAfter,

            Instant startedBefore,

            Pageable pageable
    );
}