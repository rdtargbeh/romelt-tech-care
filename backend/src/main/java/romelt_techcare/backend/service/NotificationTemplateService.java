package romelt_techcare.backend.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import romelt_techcare.backend.dto.AdminJwtPrincipal;
import romelt_techcare.backend.dto.NotificationTemplateCreateRequest;
import romelt_techcare.backend.dto.NotificationTemplateResponse;
import romelt_techcare.backend.dto.NotificationTemplateSearchRequest;
import romelt_techcare.backend.dto.NotificationTemplateSummaryResponse;
import romelt_techcare.backend.dto.NotificationTemplateUpdateRequest;
import romelt_techcare.backend.enums.NotificationChannel;

import java.util.List;
import java.util.UUID;

/**
 * ================================================================
 * ROMELT TECHCARE — NOTIFICATION TEMPLATE SERVICE
 * ================================================================
 *
 * Purpose:
 * Defines administrator and internal operations for managing
 * versioned EMAIL, SMS, and IN_APP notification templates.
 *
 * Responsibilities:
 * - Creates new notification-template versions.
 * - Supports explicit or automatically resolved version numbers.
 * - Enforces one active template per key, channel, and locale.
 * - Returns paginated administrator template lists.
 * - Returns one complete template version.
 * - Returns the currently active template for internal rendering.
 * - Returns all versions of one logical template.
 * - Updates editable template content.
 * - Activates and deactivates template versions.
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
 * Channel rules:
 *
 * EMAIL:
 * - Subject is required.
 * - Text or HTML body is required.
 *
 * SMS:
 * - Text body is required.
 * - Subject and HTML body are not used.
 *
 * IN_APP:
 * - Subject is the portal notification title.
 * - Text body is the portal notification message.
 * - HTML body is not used.
 *
 * Security:
 * All administrator-facing operations require an authenticated
 * administrator principal.
 *
 * Internal rendering operations return template responses rather than
 * exposing persistent entities outside the service layer.
 * ================================================================
 */
public interface NotificationTemplateService {

    /**
     * Creates a new notification-template version.
     *
     * When templateVersion is null, the service automatically assigns
     * the next version for the same template key, channel, and locale.
     *
     * When the new template is active, any currently active version
     * for the same key, channel, and locale is deactivated before the
     * new version is persisted.
     *
     * @param principal authenticated administrator
     * @param request template creation information
     * @return complete persisted template response
     */
    NotificationTemplateResponse createTemplate(
            AdminJwtPrincipal principal,
            NotificationTemplateCreateRequest request
    );

    /**
     * Returns a paginated administrator-searchable template list.
     *
     * @param searchRequest optional template filters
     * @param pageable pagination and sorting information
     * @return paginated compact template responses
     */
    Page<NotificationTemplateSummaryResponse> getTemplates(
            NotificationTemplateSearchRequest searchRequest,
            Pageable pageable
    );

    /**
     * Returns one complete notification-template version.
     *
     * @param notificationTemplateId template identifier
     * @return complete template response
     */
    NotificationTemplateResponse getTemplate(
            UUID notificationTemplateId
    );

    /**
     * Returns the currently active template for a logical template key,
     * channel, and locale.
     *
     * This operation is used by notification rendering and delivery
     * services.
     *
     * @param templateKey logical template key
     * @param channel EMAIL, SMS, or IN_APP
     * @param locale requested locale
     * @return active template response
     */
    NotificationTemplateResponse getActiveTemplate(
            String templateKey,
            NotificationChannel channel,
            String locale
    );

    /**
     * Returns all active channel and locale variants for one logical
     * template key.
     *
     * Example:
     * - booking-request-received / EMAIL / en-US
     * - booking-request-received / SMS / en-US
     * - booking-request-received / IN_APP / en-US
     *
     * @param templateKey logical template key
     * @return active template variants
     */
    List<NotificationTemplateResponse> getActiveTemplatesByKey(
            String templateKey
    );

    /**
     * Returns every version for one logical template identity, ordered
     * from newest version to oldest.
     *
     * @param templateKey logical template key
     * @param channel EMAIL, SMS, or IN_APP
     * @param locale requested locale
     * @return all matching template versions
     */
    List<NotificationTemplateResponse> getTemplateVersions(
            String templateKey,
            NotificationChannel channel,
            String locale
    );

    /**
     * Updates editable content and optional activation state for one
     * existing template version.
     *
     * Immutable identity fields are not changed:
     * - template key;
     * - channel;
     * - locale;
     * - template version.
     *
     * @param principal authenticated administrator
     * @param notificationTemplateId template identifier
     * @param request requested template changes
     * @return updated complete template response
     */
    NotificationTemplateResponse updateTemplate(
            AdminJwtPrincipal principal,
            UUID notificationTemplateId,
            NotificationTemplateUpdateRequest request
    );

    /**
     * Activates one template version and deactivates any other active
     * version with the same key, channel, and locale.
     *
     * @param principal authenticated administrator
     * @param notificationTemplateId template identifier
     * @return activated template response
     */
    NotificationTemplateResponse activateTemplate(
            AdminJwtPrincipal principal,
            UUID notificationTemplateId
    );

    /**
     * Deactivates one template version.
     *
     * Deactivation does not automatically activate another version.
     *
     * @param principal authenticated administrator
     * @param notificationTemplateId template identifier
     * @return deactivated template response
     */
    NotificationTemplateResponse deactivateTemplate(
            AdminJwtPrincipal principal,
            UUID notificationTemplateId
    );
}