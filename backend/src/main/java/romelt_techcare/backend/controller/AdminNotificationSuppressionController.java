package romelt_techcare.backend.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import romelt_techcare.backend.dto.AdminJwtPrincipal;
import romelt_techcare.backend.dto.NotificationSuppressionCheckRequest;
import romelt_techcare.backend.dto.NotificationSuppressionCheckResponse;
import romelt_techcare.backend.dto.NotificationSuppressionCreateRequest;
import romelt_techcare.backend.dto.NotificationSuppressionResponse;
import romelt_techcare.backend.dto.NotificationSuppressionSearchRequest;
import romelt_techcare.backend.dto.NotificationSuppressionSummaryResponse;
import romelt_techcare.backend.dto.NotificationSuppressionUpdateRequest;
import romelt_techcare.backend.enums.NotificationCategory;
import romelt_techcare.backend.enums.NotificationChannel;
import romelt_techcare.backend.enums.NotificationSuppressionReason;
import romelt_techcare.backend.service.NotificationSuppressionService;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * ================================================================
 * ROMELT TECHCARE — ADMIN NOTIFICATION SUPPRESSION CONTROLLER
 * ================================================================
 *
 * Purpose:
 * Exposes authenticated administrator endpoints for managing EMAIL
 * and SMS suppressions.
 *
 * Base path:
 * /api/v1/admin/notification-suppressions
 * ================================================================
 */
@RestController
@RequestMapping("/api/v1/admin/notification-suppressions")
@RequiredArgsConstructor
public class AdminNotificationSuppressionController {

    private final NotificationSuppressionService
            notificationSuppressionService;

    @PostMapping
    public ResponseEntity<NotificationSuppressionResponse>
    createSuppression(
            @AuthenticationPrincipal
            AdminJwtPrincipal principal,

            @Valid
            @RequestBody
            NotificationSuppressionCreateRequest request
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        notificationSuppressionService
                                .createSuppression(
                                        principal,
                                        request
                                )
                );
    }

    @GetMapping
    public ResponseEntity<
            Page<NotificationSuppressionSummaryResponse>
            > getSuppressions(
            @RequestParam(required = false)
            String keyword,

            @RequestParam(required = false)
            UUID customerId,

            @RequestParam(required = false)
            NotificationChannel channel,

            @RequestParam(required = false)
            NotificationCategory notificationCategory,

            @RequestParam(required = false)
            NotificationSuppressionReason suppressionReason,

            @RequestParam(required = false)
            Boolean active,

            @RequestParam(required = false)
            Boolean effective,

            Pageable pageable
    ) {
        NotificationSuppressionSearchRequest request =
                new NotificationSuppressionSearchRequest(
                        keyword,
                        customerId,
                        channel,
                        notificationCategory,
                        suppressionReason,
                        active,
                        effective
                );

        return ResponseEntity.ok(
                notificationSuppressionService
                        .getSuppressions(
                                request,
                                pageable
                        )
        );
    }

    @GetMapping("/{notificationSuppressionId}")
    public ResponseEntity<NotificationSuppressionResponse>
    getSuppression(
            @PathVariable
            UUID notificationSuppressionId
    ) {
        return ResponseEntity.ok(
                notificationSuppressionService
                        .getSuppression(
                                notificationSuppressionId
                        )
        );
    }

    @PutMapping("/{notificationSuppressionId}")
    public ResponseEntity<NotificationSuppressionResponse>
    updateSuppression(
            @AuthenticationPrincipal
            AdminJwtPrincipal principal,

            @PathVariable
            UUID notificationSuppressionId,

            @Valid
            @RequestBody
            NotificationSuppressionUpdateRequest request
    ) {
        return ResponseEntity.ok(
                notificationSuppressionService
                        .updateSuppression(
                                principal,
                                notificationSuppressionId,
                                request
                        )
        );
    }

    @PostMapping("/{notificationSuppressionId}/deactivate")
    public ResponseEntity<NotificationSuppressionResponse>
    deactivateSuppression(
            @AuthenticationPrincipal
            AdminJwtPrincipal principal,

            @PathVariable
            UUID notificationSuppressionId
    ) {
        return ResponseEntity.ok(
                notificationSuppressionService
                        .deactivateSuppression(
                                principal,
                                notificationSuppressionId
                        )
        );
    }

    @PostMapping("/{notificationSuppressionId}/reactivate")
    public ResponseEntity<NotificationSuppressionResponse>
    reactivateSuppression(
            @AuthenticationPrincipal
            AdminJwtPrincipal principal,

            @PathVariable
            UUID notificationSuppressionId
    ) {
        return ResponseEntity.ok(
                notificationSuppressionService
                        .reactivateSuppression(
                                principal,
                                notificationSuppressionId
                        )
        );
    }

    @PostMapping("/check")
    public ResponseEntity<NotificationSuppressionCheckResponse>
    checkSuppression(
            @Valid
            @RequestBody
            NotificationSuppressionCheckRequest request
    ) {
        return ResponseEntity.ok(
                notificationSuppressionService
                        .checkSuppression(request)
        );
    }

    @GetMapping("/customer/{customerId}")
    public ResponseEntity<List<NotificationSuppressionResponse>>
    getCustomerSuppressions(
            @PathVariable
            UUID customerId
    ) {
        return ResponseEntity.ok(
                notificationSuppressionService
                        .getCustomerSuppressions(customerId)
        );
    }

    @PostMapping("/recipient-history")
    public ResponseEntity<List<NotificationSuppressionResponse>>
    getRecipientHistory(
            @Valid
            @RequestBody
            NotificationSuppressionCheckRequest request
    ) {
        return ResponseEntity.ok(
                notificationSuppressionService
                        .getRecipientSuppressionHistory(
                                request
                        )
        );
    }

    @PostMapping("/maintenance/deactivate-expired")
    public ResponseEntity<Map<String, Integer>>
    deactivateExpiredSuppressions() {
        int deactivatedCount =
                notificationSuppressionService
                        .deactivateExpiredSuppressions();

        return ResponseEntity.ok(
                Map.of(
                        "deactivatedCount",
                        deactivatedCount
                )
        );
    }
}