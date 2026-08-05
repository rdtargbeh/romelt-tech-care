package romelt_techcare.backend.service.implement;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import romelt_techcare.backend.dto.AdminJwtPrincipal;
import romelt_techcare.backend.dto.NotificationTemplateCreateRequest;
import romelt_techcare.backend.dto.NotificationTemplateResponse;
import romelt_techcare.backend.dto.NotificationTemplateSearchRequest;
import romelt_techcare.backend.dto.NotificationTemplateSummaryResponse;
import romelt_techcare.backend.dto.NotificationTemplateUpdateRequest;
import romelt_techcare.backend.entity.AdminUser;
import romelt_techcare.backend.entity.NotificationTemplate;
import romelt_techcare.backend.enums.NotificationChannel;
import romelt_techcare.backend.exception.AdminAuthenticationException;
import romelt_techcare.backend.exception.PublicRequestRejectedException;
import romelt_techcare.backend.mapper.NotificationTemplateMapper;
import romelt_techcare.backend.repository.AdminUserRepository;
import romelt_techcare.backend.repository.NotificationTemplateRepository;
import romelt_techcare.backend.service.NotificationTemplateService;

import java.util.List;
import java.util.UUID;

/**
 * ================================================================
 * ROMELT TECHCARE — NOTIFICATION TEMPLATE SERVICE IMPLEMENTATION
 * ================================================================
 *
 * Purpose:
 * Manages versioned EMAIL, SMS, and IN_APP notification templates.
 *
 * Responsibilities:
 * - Creates notification-template versions.
 * - Automatically assigns the next version when none is supplied.
 * - Enforces unique template-version identity.
 * - Enforces one active version per key, channel, and locale.
 * - Returns searchable administrator template lists.
 * - Returns one complete template version.
 * - Resolves active templates for notification rendering.
 * - Updates editable template content.
 * - Activates and deactivates template versions.
 * - Verifies authenticated administrator identity for write actions.
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
 * Channel support:
 * - EMAIL
 * - SMS
 * - IN_APP
 *
 * Versioning:
 * When templateVersion is absent during creation, the service assigns:
 *
 * maximum existing version + 1
 *
 * Historical template versions are retained rather than deleted.
 * ================================================================
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationTemplateServiceImplementation implements NotificationTemplateService {

    private final NotificationTemplateRepository
            notificationTemplateRepository;

    private final NotificationTemplateMapper
            notificationTemplateMapper;

    private final AdminUserRepository
            adminUserRepository;

    /**
     * Creates a new notification-template version.
     */
    @Override
    @Transactional
    public NotificationTemplateResponse createTemplate(
            AdminJwtPrincipal principal,
            NotificationTemplateCreateRequest request
    ) {
        requirePrincipal(principal);
        requireCreateRequest(request);

        AdminUser administrator =
                findAuthenticatedAdministrator(principal);

        validateCreateRequest(request);

        String normalizedTemplateKey =
                notificationTemplateMapper.normalizeTemplateKey(
                        request.templateKey()
                );

        String normalizedLocale =
                notificationTemplateMapper.normalizeLocale(
                        request.locale()
                );

        NotificationChannel channel =
                request.channel();

        Integer templateVersion =
                resolveTemplateVersion(
                        normalizedTemplateKey,
                        channel,
                        normalizedLocale,
                        request.templateVersion()
                );

        validateTemplateVersionIdentityAvailable(
                normalizedTemplateKey,
                channel,
                normalizedLocale,
                templateVersion
        );

        NotificationTemplateCreateRequest resolvedRequest =
                new NotificationTemplateCreateRequest(
                        normalizedTemplateKey,
                        channel,
                        request.templateName(),
                        request.subjectTemplate(),
                        request.bodyTextTemplate(),
                        request.bodyHtmlTemplate(),
                        normalizedLocale,
                        templateVersion,
                        request.requiredVariablesJson(),
                        request.active()
                );

        NotificationTemplate template =
                notificationTemplateMapper.toEntity(
                        resolvedRequest,
                        administrator.getAdminUserId()
                );

        if (template.isActive()) {
            notificationTemplateRepository
                    .deactivateOtherActiveVersions(
                            normalizedTemplateKey,
                            channel,
                            normalizedLocale,
                            null,
                            administrator.getAdminUserId()
                    );
        }

        try {
            NotificationTemplate savedTemplate =
                    notificationTemplateRepository.saveAndFlush(
                            template
                    );

            log.info(
                    "Notification template created. notificationTemplateId={}, templateKey={}, channel={}, locale={}, templateVersion={}, active={}, createdByAdminUserId={}",
                    savedTemplate.getNotificationTemplateId(),
                    savedTemplate.getTemplateKey(),
                    savedTemplate.getChannel(),
                    savedTemplate.getLocale(),
                    savedTemplate.getTemplateVersion(),
                    savedTemplate.isActive(),
                    administrator.getAdminUserId()
            );

            return notificationTemplateMapper.toResponse(
                    savedTemplate
            );

        } catch (DataIntegrityViolationException exception) {
            throw new PublicRequestRejectedException(
                    HttpStatus.CONFLICT,
                    "A notification template already exists with the same key, channel, locale, and version."
            );
        }
    }

    /**
     * Returns a paginated and filtered administrator template list.
     */
    @Override
    @Transactional(readOnly = true)
    public Page<NotificationTemplateSummaryResponse> getTemplates(
            NotificationTemplateSearchRequest searchRequest,
            Pageable pageable
    ) {
        requirePageable(pageable);

        NotificationTemplateSearchRequest resolvedSearch =
                searchRequest == null
                        ? new NotificationTemplateSearchRequest(
                        null,
                        null,
                        null,
                        null,
                        null,
                        null
                )
                        : searchRequest;

        String keyword =
                normalizeOptional(
                        resolvedSearch.keyword()
                );

        String templateKey =
                resolvedSearch.templateKey() == null
                        ? null
                        : notificationTemplateMapper
                        .normalizeTemplateKey(
                                resolvedSearch.templateKey()
                        );

        String locale =
                resolvedSearch.locale() == null
                        ? null
                        : notificationTemplateMapper
                        .normalizeLocale(
                                resolvedSearch.locale()
                        );

        if (
                resolvedSearch.templateVersion() != null
                        && resolvedSearch.templateVersion() < 1
        ) {
            reject(
                    HttpStatus.BAD_REQUEST,
                    "Notification template version must be greater than zero."
            );
        }

        return notificationTemplateRepository
                .searchTemplates(
                        keyword,
                        templateKey,
                        resolvedSearch.channel(),
                        locale,
                        resolvedSearch.templateVersion(),
                        resolvedSearch.active(),
                        pageable
                )
                .map(
                        notificationTemplateMapper
                                ::toSummaryResponse
                );
    }

    /**
     * Returns one complete notification-template version.
     */
    @Override
    @Transactional(readOnly = true)
    public NotificationTemplateResponse getTemplate(
            UUID notificationTemplateId
    ) {
        NotificationTemplate template =
                findTemplate(notificationTemplateId);

        return notificationTemplateMapper.toResponse(
                template
        );
    }

    /**
     * Returns the active template for one key, channel, and locale.
     */
    @Override
    @Transactional(readOnly = true)
    public NotificationTemplateResponse getActiveTemplate(
            String templateKey,
            NotificationChannel channel,
            String locale
    ) {
        if (channel == null) {
            reject(
                    HttpStatus.BAD_REQUEST,
                    "Notification template channel is required."
            );
        }

        String normalizedTemplateKey =
                notificationTemplateMapper.normalizeTemplateKey(
                        templateKey
                );

        String normalizedLocale =
                notificationTemplateMapper.normalizeLocale(
                        locale
                );

        NotificationTemplate template =
                notificationTemplateRepository
                        .findByTemplateKeyAndChannelAndLocaleAndActiveTrue(
                                normalizedTemplateKey,
                                channel,
                                normalizedLocale
                        )
                        .orElseThrow(
                                () ->
                                        new PublicRequestRejectedException(
                                                HttpStatus.NOT_FOUND,
                                                "An active notification template was not found for the requested key, channel, and locale."
                                        )
                        );

        return notificationTemplateMapper.toResponse(
                template
        );
    }

    /**
     * Returns all active channel and locale variants for a template
     * key.
     */
    @Override
    @Transactional(readOnly = true)
    public List<NotificationTemplateResponse> getActiveTemplatesByKey(
            String templateKey
    ) {
        String normalizedTemplateKey =
                notificationTemplateMapper.normalizeTemplateKey(
                        templateKey
                );

        return notificationTemplateRepository
                .findByTemplateKeyAndActiveTrueOrderByChannelAscLocaleAsc(
                        normalizedTemplateKey
                )
                .stream()
                .map(notificationTemplateMapper::toResponse)
                .toList();
    }

    /**
     * Returns all versions for one key, channel, and locale.
     */
    @Override
    @Transactional(readOnly = true)
    public List<NotificationTemplateResponse> getTemplateVersions(
            String templateKey,
            NotificationChannel channel,
            String locale
    ) {
        if (channel == null) {
            reject(
                    HttpStatus.BAD_REQUEST,
                    "Notification template channel is required."
            );
        }

        String normalizedTemplateKey =
                notificationTemplateMapper.normalizeTemplateKey(
                        templateKey
                );

        String normalizedLocale =
                notificationTemplateMapper.normalizeLocale(
                        locale
                );

        return notificationTemplateRepository
                .findByTemplateKeyAndChannelAndLocaleOrderByTemplateVersionDesc(
                        normalizedTemplateKey,
                        channel,
                        normalizedLocale
                )
                .stream()
                .map(notificationTemplateMapper::toResponse)
                .toList();
    }

    /**
     * Updates one existing template version.
     */
    @Override
    @Transactional
    public NotificationTemplateResponse updateTemplate(
            AdminJwtPrincipal principal,
            UUID notificationTemplateId,
            NotificationTemplateUpdateRequest request
    ) {
        requirePrincipal(principal);
        requireUpdateRequest(request);

        AdminUser administrator =
                findAuthenticatedAdministrator(principal);

        NotificationTemplate template =
                findTemplateForUpdate(
                        notificationTemplateId
                );

        if (!request.hasChanges()) {
            reject(
                    HttpStatus.BAD_REQUEST,
                    "At least one notification template change is required."
            );
        }

        if (!request.hasValidRequiredVariables()) {
            reject(
                    HttpStatus.BAD_REQUEST,
                    "Notification template required variables must be a JSON array containing only non-blank strings."
            );
        }

        boolean activating =
                Boolean.TRUE.equals(request.active());

        if (activating) {
            notificationTemplateRepository
                    .deactivateOtherActiveVersions(
                            template.getTemplateKey(),
                            template.getChannel(),
                            template.getLocale(),
                            template.getNotificationTemplateId(),
                            administrator.getAdminUserId()
                    );
        }

        notificationTemplateMapper.updateEntity(
                template,
                request,
                administrator.getAdminUserId()
        );

        try {
            NotificationTemplate savedTemplate =
                    notificationTemplateRepository.saveAndFlush(
                            template
                    );

            log.info(
                    "Notification template updated. notificationTemplateId={}, templateKey={}, channel={}, locale={}, templateVersion={}, active={}, updatedByAdminUserId={}",
                    savedTemplate.getNotificationTemplateId(),
                    savedTemplate.getTemplateKey(),
                    savedTemplate.getChannel(),
                    savedTemplate.getLocale(),
                    savedTemplate.getTemplateVersion(),
                    savedTemplate.isActive(),
                    administrator.getAdminUserId()
            );

            return notificationTemplateMapper.toResponse(
                    savedTemplate
            );

        } catch (DataIntegrityViolationException exception) {
            throw new PublicRequestRejectedException(
                    HttpStatus.CONFLICT,
                    "The notification template update conflicts with another active or versioned template."
            );
        }
    }

    /**
     * Activates one template and deactivates competing active
     * versions.
     */
    @Override
    @Transactional
    public NotificationTemplateResponse activateTemplate(
            AdminJwtPrincipal principal,
            UUID notificationTemplateId
    ) {
        requirePrincipal(principal);

        AdminUser administrator =
                findAuthenticatedAdministrator(principal);

        NotificationTemplate template =
                findTemplateForUpdate(
                        notificationTemplateId
                );

        if (template.isActive()) {
            return notificationTemplateMapper.toResponse(
                    template
            );
        }

        notificationTemplateRepository
                .deactivateOtherActiveVersions(
                        template.getTemplateKey(),
                        template.getChannel(),
                        template.getLocale(),
                        template.getNotificationTemplateId(),
                        administrator.getAdminUserId()
                );

        template.activate(
                administrator.getAdminUserId()
        );

        try {
            NotificationTemplate savedTemplate =
                    notificationTemplateRepository.saveAndFlush(
                            template
                    );

            log.info(
                    "Notification template activated. notificationTemplateId={}, templateKey={}, channel={}, locale={}, templateVersion={}, activatedByAdminUserId={}",
                    savedTemplate.getNotificationTemplateId(),
                    savedTemplate.getTemplateKey(),
                    savedTemplate.getChannel(),
                    savedTemplate.getLocale(),
                    savedTemplate.getTemplateVersion(),
                    administrator.getAdminUserId()
            );

            return notificationTemplateMapper.toResponse(
                    savedTemplate
            );

        } catch (DataIntegrityViolationException exception) {
            throw new PublicRequestRejectedException(
                    HttpStatus.CONFLICT,
                    "Another notification template version is already active for the same key, channel, and locale."
            );
        }
    }

    /**
     * Deactivates one template version.
     */
    @Override
    @Transactional
    public NotificationTemplateResponse deactivateTemplate(
            AdminJwtPrincipal principal,
            UUID notificationTemplateId
    ) {
        requirePrincipal(principal);

        AdminUser administrator =
                findAuthenticatedAdministrator(principal);

        NotificationTemplate template =
                findTemplateForUpdate(
                        notificationTemplateId
                );

        if (!template.isActive()) {
            return notificationTemplateMapper.toResponse(
                    template
            );
        }

        template.deactivate(
                administrator.getAdminUserId()
        );

        NotificationTemplate savedTemplate =
                notificationTemplateRepository.saveAndFlush(
                        template
                );

        log.info(
                "Notification template deactivated. notificationTemplateId={}, templateKey={}, channel={}, locale={}, templateVersion={}, deactivatedByAdminUserId={}",
                savedTemplate.getNotificationTemplateId(),
                savedTemplate.getTemplateKey(),
                savedTemplate.getChannel(),
                savedTemplate.getLocale(),
                savedTemplate.getTemplateVersion(),
                administrator.getAdminUserId()
        );

        return notificationTemplateMapper.toResponse(
                savedTemplate
        );
    }

    /**
     * Resolves an explicit or automatically generated template
     * version.
     */
    private Integer resolveTemplateVersion(
            String templateKey,
            NotificationChannel channel,
            String locale,
            Integer requestedVersion
    ) {
        if (requestedVersion != null) {
            if (requestedVersion < 1) {
                reject(
                        HttpStatus.BAD_REQUEST,
                        "Notification template version must be greater than zero."
                );
            }

            return requestedVersion;
        }

        Integer maximumVersion =
                notificationTemplateRepository
                        .findMaximumTemplateVersion(
                                templateKey,
                                channel,
                                locale
                        );

        if (maximumVersion == null) {
            return 1;
        }

        if (maximumVersion == Integer.MAX_VALUE) {
            throw new IllegalStateException(
                    "Notification template version limit has been reached."
            );
        }

        return maximumVersion + 1;
    }

    /**
     * Ensures one exact template-version identity does not already
     * exist.
     */
    private void validateTemplateVersionIdentityAvailable(
            String templateKey,
            NotificationChannel channel,
            String locale,
            Integer templateVersion
    ) {
        boolean exists =
                notificationTemplateRepository
                        .existsByTemplateKeyAndChannelAndLocaleAndTemplateVersion(
                                templateKey,
                                channel,
                                locale,
                                templateVersion
                        );

        if (exists) {
            reject(
                    HttpStatus.CONFLICT,
                    "A notification template already exists with the same key, channel, locale, and version."
            );
        }
    }

    /**
     * Performs request-level validation before entity mapping.
     */
    private void validateCreateRequest(
            NotificationTemplateCreateRequest request
    ) {
        if (request.channel() == null) {
            reject(
                    HttpStatus.BAD_REQUEST,
                    "Notification template channel is required."
            );
        }

        if (!request.hasValidRequiredVariables()) {
            reject(
                    HttpStatus.BAD_REQUEST,
                    "Notification template required variables must be a JSON array containing only non-blank strings."
            );
        }

        if (!request.hasValidChannelContent()) {
            reject(
                    HttpStatus.BAD_REQUEST,
                    resolveChannelContentError(
                            request.channel()
                    )
            );
        }

        if (!request.hasValidChannelSpecificFields()) {
            reject(
                    HttpStatus.BAD_REQUEST,
                    resolveChannelSpecificFieldError(
                            request.channel()
                    )
            );
        }
    }

    /**
     * Returns a channel-specific validation message.
     */
    private String resolveChannelContentError(
            NotificationChannel channel
    ) {
        return switch (channel) {
            case EMAIL ->
                    "Email notification templates require a subject and a text or HTML body.";

            case SMS ->
                    "SMS notification templates require a text body.";

            case IN_APP ->
                    "In-app notification templates require a title and message.";
        };
    }

    /**
     * Returns a channel-specific incompatible-field message.
     */
    private String resolveChannelSpecificFieldError(
            NotificationChannel channel
    ) {
        return switch (channel) {
            case EMAIL ->
                    "Email notification template content is invalid.";

            case SMS ->
                    "SMS notification templates cannot contain a subject or HTML body.";

            case IN_APP ->
                    "In-app notification templates cannot contain an HTML body.";
        };
    }

    /**
     * Loads one template.
     */
    private NotificationTemplate findTemplate(
            UUID notificationTemplateId
    ) {
        requireTemplateId(notificationTemplateId);

        return notificationTemplateRepository
                .findById(notificationTemplateId)
                .orElseThrow(
                        () ->
                                new PublicRequestRejectedException(
                                        HttpStatus.NOT_FOUND,
                                        "Notification template was not found."
                                )
                );
    }

    /**
     * Loads one template with a pessimistic write lock.
     */
    private NotificationTemplate findTemplateForUpdate(
            UUID notificationTemplateId
    ) {
        requireTemplateId(notificationTemplateId);

        return notificationTemplateRepository
                .findByIdForUpdate(
                        notificationTemplateId
                )
                .orElseThrow(
                        () ->
                                new PublicRequestRejectedException(
                                        HttpStatus.NOT_FOUND,
                                        "Notification template was not found."
                                )
                );
    }

    /**
     * Finds and verifies the authenticated administrator.
     */
    private AdminUser findAuthenticatedAdministrator(
            AdminJwtPrincipal principal
    ) {
        AdminUser administrator =
                adminUserRepository
                        .findById(
                                principal.adminUserId()
                        )
                        .orElseThrow(
                                AdminAuthenticationException
                                        ::accountNotFound
                        );

        if (
                administrator.getAdminUserId() == null
                        || !administrator
                        .getAdminUserId()
                        .equals(
                                principal.adminUserId()
                        )
        ) {
            throw AdminAuthenticationException
                    .staleAuthentication();
        }

        if (
                administrator.getEmail() == null
                        || principal.email() == null
                        || !administrator
                        .getEmail()
                        .trim()
                        .equalsIgnoreCase(
                                principal.email().trim()
                        )
        ) {
            throw AdminAuthenticationException
                    .staleAuthentication();
        }

        if (
                administrator.getRole() == null
                        || principal.role() == null
                        || administrator.getRole()
                        != principal.role()
        ) {
            throw AdminAuthenticationException
                    .staleAuthentication();
        }

        if (!administrator.isActive()) {
            throw AdminAuthenticationException
                    .inactiveAccount();
        }

        return administrator;
    }

    private void requirePrincipal(
            AdminJwtPrincipal principal
    ) {
        if (
                principal == null
                        || principal.adminUserId() == null
                        || normalizeOptional(principal.email()) == null
                        || principal.role() == null
        ) {
            throw AdminAuthenticationException
                    .staleAuthentication();
        }
    }

    private void requireCreateRequest(
            NotificationTemplateCreateRequest request
    ) {
        if (request == null) {
            reject(
                    HttpStatus.BAD_REQUEST,
                    "Notification template information is required."
            );
        }
    }

    private void requireUpdateRequest(
            NotificationTemplateUpdateRequest request
    ) {
        if (request == null) {
            reject(
                    HttpStatus.BAD_REQUEST,
                    "Notification template update information is required."
            );
        }
    }

    private void requireTemplateId(
            UUID notificationTemplateId
    ) {
        if (notificationTemplateId == null) {
            reject(
                    HttpStatus.BAD_REQUEST,
                    "Notification template ID is required."
            );
        }
    }

    private void requirePageable(
            Pageable pageable
    ) {
        if (pageable == null) {
            throw new IllegalArgumentException(
                    "Notification template pagination information is required."
            );
        }
    }

    private void reject(
            HttpStatus status,
            String message
    ) {
        throw new PublicRequestRejectedException(
                status,
                message
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