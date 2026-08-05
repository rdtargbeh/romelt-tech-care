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
import romelt_techcare.backend.dto.NotificationTemplateCreateRequest;
import romelt_techcare.backend.dto.NotificationTemplateResponse;
import romelt_techcare.backend.dto.NotificationTemplateSearchRequest;
import romelt_techcare.backend.dto.NotificationTemplateSummaryResponse;
import romelt_techcare.backend.dto.NotificationTemplateUpdateRequest;
import romelt_techcare.backend.enums.NotificationChannel;
import romelt_techcare.backend.service.NotificationTemplateService;

import java.util.List;
import java.util.UUID;

/**
 * ================================================================
 * ROMELT TECHCARE — ADMIN NOTIFICATION TEMPLATE CONTROLLER
 * ================================================================
 *
 * Purpose:
 * Exposes authenticated administrator endpoints for managing
 * versioned EMAIL, SMS, and IN_APP notification templates.
 *
 * Responsibilities:
 * - Creates new notification-template versions.
 * - Returns paginated and filtered template lists.
 * - Returns one complete template version.
 * - Returns the active template for a key, channel, and locale.
 * - Returns all active channel and locale variants for a template key.
 * - Returns all versions for a key, channel, and locale.
 * - Updates editable template content.
 * - Activates one template version.
 * - Deactivates one template version.
 *
 * Template identity:
 *
 * templateKey + channel + locale + templateVersion
 *
 * Active-template rule:
 * Only one active version may exist for the same:
 *
 * templateKey + channel + locale
 *
 * Supported channels:
 * - EMAIL
 * - SMS
 * - IN_APP
 *
 * Security:
 * - Every write endpoint requires an authenticated administrator.
 * - Template content may contain internal operational wording.
 * - No public notification-template management endpoint should be
 *   created.
 *
 * Base path:
 * /api/v1/admin/notification-templates
 * ================================================================
 */
@RestController
@RequestMapping("/api/v1/admin/notification-templates")
@RequiredArgsConstructor
public class AdminNotificationTemplateController {

    private final NotificationTemplateService
            notificationTemplateService;

    /**
     * Creates a new notification-template version.
     *
     * When templateVersion is omitted, the service automatically
     * assigns the next available version.
     *
     * Endpoint:
     * POST /api/v1/admin/notification-templates
     */
    @PostMapping
    public ResponseEntity<NotificationTemplateResponse> createTemplate(
            @AuthenticationPrincipal
            AdminJwtPrincipal principal,

            @Valid
            @RequestBody
            NotificationTemplateCreateRequest request
    ) {
        NotificationTemplateResponse response =
                notificationTemplateService.createTemplate(
                        principal,
                        request
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    /**
     * Returns a paginated and filtered notification-template list.
     *
     * Supported query parameters:
     * - keyword
     * - templateKey
     * - channel
     * - locale
     * - templateVersion
     * - active
     *
     * Endpoint:
     * GET /api/v1/admin/notification-templates
     */
    @GetMapping
    public ResponseEntity<Page<NotificationTemplateSummaryResponse>>
    getTemplates(
            @RequestParam(required = false)
            String keyword,

            @RequestParam(required = false)
            String templateKey,

            @RequestParam(required = false)
            NotificationChannel channel,

            @RequestParam(required = false)
            String locale,

            @RequestParam(required = false)
            Integer templateVersion,

            @RequestParam(required = false)
            Boolean active,

            Pageable pageable
    ) {
        NotificationTemplateSearchRequest searchRequest =
                new NotificationTemplateSearchRequest(
                        keyword,
                        templateKey,
                        channel,
                        locale,
                        templateVersion,
                        active
                );

        Page<NotificationTemplateSummaryResponse> response =
                notificationTemplateService.getTemplates(
                        searchRequest,
                        pageable
                );

        return ResponseEntity.ok(response);
    }

    /**
     * Returns one complete notification-template version.
     *
     * Endpoint:
     * GET /api/v1/admin/notification-templates/{notificationTemplateId}
     */
    @GetMapping("/{notificationTemplateId}")
    public ResponseEntity<NotificationTemplateResponse> getTemplate(
            @PathVariable
            UUID notificationTemplateId
    ) {
        NotificationTemplateResponse response =
                notificationTemplateService.getTemplate(
                        notificationTemplateId
                );

        return ResponseEntity.ok(response);
    }

    /**
     * Returns the currently active template for one logical template
     * key, channel, and locale.
     *
     * Example:
     *
     * GET /api/v1/admin/notification-templates/active
     *     ?templateKey=booking-request-received
     *     &channel=IN_APP
     *     &locale=en-US
     *
     * Endpoint:
     * GET /api/v1/admin/notification-templates/active
     */
    @GetMapping("/active")
    public ResponseEntity<NotificationTemplateResponse>
    getActiveTemplate(
            @RequestParam
            String templateKey,

            @RequestParam
            NotificationChannel channel,

            @RequestParam(
                    required = false,
                    defaultValue = "en-US"
            )
            String locale
    ) {
        NotificationTemplateResponse response =
                notificationTemplateService.getActiveTemplate(
                        templateKey,
                        channel,
                        locale
                );

        return ResponseEntity.ok(response);
    }

    /**
     * Returns all active channel and locale variants for one logical
     * template key.
     *
     * Example results may include:
     * - EMAIL / en-US
     * - SMS / en-US
     * - IN_APP / en-US
     *
     * Endpoint:
     * GET
     * /api/v1/admin/notification-templates/by-key/{templateKey}/active
     */
    @GetMapping("/by-key/{templateKey}/active")
    public ResponseEntity<List<NotificationTemplateResponse>>
    getActiveTemplatesByKey(
            @PathVariable
            String templateKey
    ) {
        List<NotificationTemplateResponse> response =
                notificationTemplateService
                        .getActiveTemplatesByKey(
                                templateKey
                        );

        return ResponseEntity.ok(response);
    }

    /**
     * Returns every version for one template key, channel, and locale,
     * ordered from newest to oldest.
     *
     * Endpoint:
     * GET
     * /api/v1/admin/notification-templates/by-key/{templateKey}/versions
     */
    @GetMapping("/by-key/{templateKey}/versions")
    public ResponseEntity<List<NotificationTemplateResponse>>
    getTemplateVersions(
            @PathVariable
            String templateKey,

            @RequestParam
            NotificationChannel channel,

            @RequestParam(
                    required = false,
                    defaultValue = "en-US"
            )
            String locale
    ) {
        List<NotificationTemplateResponse> response =
                notificationTemplateService
                        .getTemplateVersions(
                                templateKey,
                                channel,
                                locale
                        );

        return ResponseEntity.ok(response);
    }

    /**
     * Updates editable content and optional activation state for one
     * existing template version.
     *
     * Immutable fields:
     * - templateKey
     * - channel
     * - locale
     * - templateVersion
     *
     * Endpoint:
     * PUT /api/v1/admin/notification-templates/{notificationTemplateId}
     */
    @PutMapping("/{notificationTemplateId}")
    public ResponseEntity<NotificationTemplateResponse> updateTemplate(
            @AuthenticationPrincipal
            AdminJwtPrincipal principal,

            @PathVariable
            UUID notificationTemplateId,

            @Valid
            @RequestBody
            NotificationTemplateUpdateRequest request
    ) {
        NotificationTemplateResponse response =
                notificationTemplateService.updateTemplate(
                        principal,
                        notificationTemplateId,
                        request
                );

        return ResponseEntity.ok(response);
    }

    /**
     * Activates one template version.
     *
     * Any other active version with the same template key, channel,
     * and locale is deactivated automatically.
     *
     * Endpoint:
     * POST
     * /api/v1/admin/notification-templates/{notificationTemplateId}/activate
     */
    @PostMapping("/{notificationTemplateId}/activate")
    public ResponseEntity<NotificationTemplateResponse> activateTemplate(
            @AuthenticationPrincipal
            AdminJwtPrincipal principal,

            @PathVariable
            UUID notificationTemplateId
    ) {
        NotificationTemplateResponse response =
                notificationTemplateService.activateTemplate(
                        principal,
                        notificationTemplateId
                );

        return ResponseEntity.ok(response);
    }

    /**
     * Deactivates one notification-template version.
     *
     * Deactivating a template does not automatically activate another
     * version.
     *
     * Endpoint:
     * POST
     * /api/v1/admin/notification-templates/{notificationTemplateId}/deactivate
     */
    @PostMapping("/{notificationTemplateId}/deactivate")
    public ResponseEntity<NotificationTemplateResponse>
    deactivateTemplate(
            @AuthenticationPrincipal
            AdminJwtPrincipal principal,

            @PathVariable
            UUID notificationTemplateId
    ) {
        NotificationTemplateResponse response =
                notificationTemplateService.deactivateTemplate(
                        principal,
                        notificationTemplateId
                );

        return ResponseEntity.ok(response);
    }
}