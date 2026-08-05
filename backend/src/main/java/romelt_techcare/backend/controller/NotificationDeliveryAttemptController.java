package romelt_techcare.backend.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import romelt_techcare.backend.dto.NotificationDeliveryAttemptResponse;
import romelt_techcare.backend.dto.NotificationDeliveryAttemptSummaryResponse;
import romelt_techcare.backend.service.NotificationDeliveryAttemptService;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * ================================================================
 * ROMELT TECHCARE — NOTIFICATION DELIVERY ATTEMPT CONTROLLER
 * ================================================================
 *
 * Administrator reporting endpoints.
 *
 * Attempts are immutable and therefore expose only GET endpoints.
 * ================================================================
 */
@RestController
@RequestMapping("/api/v1/admin/notification-delivery-attempts")
@RequiredArgsConstructor
public class NotificationDeliveryAttemptController {

    private final NotificationDeliveryAttemptService service;

    @GetMapping("/{attemptId}")
    public ResponseEntity<NotificationDeliveryAttemptResponse>
    get(
            @PathVariable UUID attemptId
    ) {

        return ResponseEntity.ok(
                service.get(attemptId)
        );
    }

    @GetMapping("/delivery/{deliveryId}")
    public ResponseEntity<List<NotificationDeliveryAttemptResponse>>
    getAttempts(
            @PathVariable UUID deliveryId
    ) {

        return ResponseEntity.ok(
                service.getAttempts(deliveryId)
        );
    }

    @GetMapping("/delivery/{deliveryId}/latest")
    public ResponseEntity<NotificationDeliveryAttemptResponse>
    latest(
            @PathVariable UUID deliveryId
    ) {

        return ResponseEntity.ok(
                service.getLatestAttempt(deliveryId)
        );
    }

    @GetMapping
    public ResponseEntity<
            Page<NotificationDeliveryAttemptSummaryResponse>
            > search(

            @RequestParam(required = false)
            UUID notificationDeliveryId,

            @RequestParam(required = false)
            String providerName,

            @RequestParam(required = false)
            Boolean success,

            @RequestParam(required = false)
            Boolean retryable,

            @RequestParam(required = false)
            Instant startedAfter,

            @RequestParam(required = false)
            Instant startedBefore,

            Pageable pageable
    ) {

        return ResponseEntity.ok(
                service.search(

                        notificationDeliveryId,

                        providerName,

                        success,

                        retryable,

                        startedAfter,

                        startedBefore,

                        pageable
                )
        );
    }
}