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
import romelt_techcare.backend.dto.NotificationSuppressionCheckRequest;
import romelt_techcare.backend.dto.NotificationSuppressionCheckResponse;
import romelt_techcare.backend.dto.NotificationSuppressionCreateRequest;
import romelt_techcare.backend.dto.NotificationSuppressionResponse;
import romelt_techcare.backend.dto.NotificationSuppressionSearchRequest;
import romelt_techcare.backend.dto.NotificationSuppressionSummaryResponse;
import romelt_techcare.backend.dto.NotificationSuppressionUpdateRequest;
import romelt_techcare.backend.entity.AdminUser;
import romelt_techcare.backend.entity.NotificationSuppression;
import romelt_techcare.backend.enums.NotificationChannel;
import romelt_techcare.backend.exception.AdminAuthenticationException;
import romelt_techcare.backend.exception.PublicRequestRejectedException;
import romelt_techcare.backend.mapper.NotificationSuppressionMapper;
import romelt_techcare.backend.repository.AdminUserRepository;
import romelt_techcare.backend.repository.NotificationSuppressionRepository;
import romelt_techcare.backend.service.NotificationSuppressionService;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * ================================================================
 * ROMELT TECHCARE — NOTIFICATION SUPPRESSION SERVICE IMPLEMENTATION
 * ================================================================
 *
 * Purpose:
 * Implements production EMAIL and SMS suppression behavior.
 *
 * Matching:
 * A delivery is suppressed when an effective record exists for:
 *
 * channel + normalized recipient + exact category
 *
 * or:
 *
 * channel + normalized recipient + null category
 *
 * Null category means all notification categories.
 * ================================================================
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationSuppressionServiceImplementation
        implements NotificationSuppressionService {

    private final NotificationSuppressionRepository
            notificationSuppressionRepository;

    private final NotificationSuppressionMapper
            notificationSuppressionMapper;

    private final AdminUserRepository
            adminUserRepository;

    @Override
    @Transactional
    public NotificationSuppressionResponse createSuppression(
            AdminJwtPrincipal principal,
            NotificationSuppressionCreateRequest request
    ) {
        requireCreateRequest(request);

        AdminUser administrator =
                findAuthenticatedAdministrator(principal);

        String normalizedRecipient =
                notificationSuppressionMapper
                        .normalizeRecipientAddress(
                                request.recipientAddress(),
                                request.channel()
                        );

        NotificationSuppression existing =
                notificationSuppressionRepository
                        .findExactActiveSuppression(
                                request.channel(),
                                normalizedRecipient,
                                request.notificationCategory()
                        )
                        .orElse(null);

        if (existing != null) {
            reject(
                    HttpStatus.CONFLICT,
                    "An active suppression already exists for this channel, recipient, and category."
            );
        }

        NotificationSuppression suppression =
                notificationSuppressionMapper.toEntity(
                        request,
                        administrator.getAdminUserId()
                );

        try {
            NotificationSuppression saved =
                    notificationSuppressionRepository
                            .saveAndFlush(suppression);

            log.info(
                    "Notification suppression created. suppressionId={}, channel={}, category={}, reason={}, customerId={}, createdByAdminUserId={}",
                    saved.getNotificationSuppressionId(),
                    saved.getChannel(),
                    saved.getNotificationCategory(),
                    saved.getSuppressionReason(),
                    saved.getCustomerId(),
                    administrator.getAdminUserId()
            );

            return notificationSuppressionMapper
                    .toResponse(saved);

        } catch (DataIntegrityViolationException exception) {
            throw new PublicRequestRejectedException(
                    HttpStatus.CONFLICT,
                    "An active suppression already exists for this channel, recipient, and category."
            );
        }
    }

    @Override
    @Transactional(readOnly = true)
    public NotificationSuppressionResponse getSuppression(
            UUID notificationSuppressionId
    ) {
        return notificationSuppressionMapper.toResponse(
                findSuppression(notificationSuppressionId)
        );
    }

    @Override
    @Transactional(readOnly = true)
    public Page<NotificationSuppressionSummaryResponse>
    getSuppressions(
            NotificationSuppressionSearchRequest searchRequest,
            Pageable pageable
    ) {
        requirePageable(pageable);

        NotificationSuppressionSearchRequest resolved =
                searchRequest == null
                        ? new NotificationSuppressionSearchRequest(
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null
                )
                        : searchRequest;

        if (resolved.channel() == NotificationChannel.IN_APP) {
            reject(
                    HttpStatus.BAD_REQUEST,
                    "In-app notifications do not support recipient-address suppressions."
            );
        }

        return notificationSuppressionRepository
                .searchSuppressions(
                        normalizeOptional(resolved.keyword()),
                        resolved.customerId(),
                        resolved.channel(),
                        resolved.notificationCategory(),
                        resolved.suppressionReason(),
                        resolved.active(),
                        resolved.effective(),
                        Instant.now(),
                        pageable
                )
                .map(
                        notificationSuppressionMapper
                                ::toSummaryResponse
                );
    }

    @Override
    @Transactional
    public NotificationSuppressionResponse updateSuppression(
            AdminJwtPrincipal principal,
            UUID notificationSuppressionId,
            NotificationSuppressionUpdateRequest request
    ) {
        findAuthenticatedAdministrator(principal);
        requireUpdateRequest(request);

        NotificationSuppression suppression =
                findSuppressionForUpdate(
                        notificationSuppressionId
                );

        notificationSuppressionMapper.updateEntity(
                suppression,
                request
        );

        NotificationSuppression saved =
                notificationSuppressionRepository
                        .saveAndFlush(suppression);

        log.info(
                "Notification suppression updated. suppressionId={}, reason={}, expiresAt={}, active={}",
                saved.getNotificationSuppressionId(),
                saved.getSuppressionReason(),
                saved.getExpiresAt(),
                saved.isActive()
        );

        return notificationSuppressionMapper.toResponse(
                saved
        );
    }

    @Override
    @Transactional
    public NotificationSuppressionResponse deactivateSuppression(
            AdminJwtPrincipal principal,
            UUID notificationSuppressionId
    ) {
        AdminUser administrator =
                findAuthenticatedAdministrator(principal);

        NotificationSuppression suppression =
                findSuppressionForUpdate(
                        notificationSuppressionId
                );

        suppression.deactivate(
                administrator.getAdminUserId()
        );

        NotificationSuppression saved =
                notificationSuppressionRepository
                        .saveAndFlush(suppression);

        log.info(
                "Notification suppression deactivated. suppressionId={}, deactivatedByAdminUserId={}",
                saved.getNotificationSuppressionId(),
                administrator.getAdminUserId()
        );

        return notificationSuppressionMapper.toResponse(
                saved
        );
    }

    @Override
    @Transactional
    public NotificationSuppressionResponse reactivateSuppression(
            AdminJwtPrincipal principal,
            UUID notificationSuppressionId
    ) {
        findAuthenticatedAdministrator(principal);

        NotificationSuppression suppression =
                findSuppressionForUpdate(
                        notificationSuppressionId
                );

        if (suppression.isActive()) {
            return notificationSuppressionMapper.toResponse(
                    suppression
            );
        }

        NotificationSuppression competing =
                notificationSuppressionRepository
                        .findExactActiveSuppression(
                                suppression.getChannel(),
                                suppression
                                        .getNormalizedRecipientAddress(),
                                suppression
                                        .getNotificationCategory()
                        )
                        .orElse(null);

        if (
                competing != null
                        && !competing
                        .getNotificationSuppressionId()
                        .equals(
                                suppression
                                        .getNotificationSuppressionId()
                        )
        ) {
            reject(
                    HttpStatus.CONFLICT,
                    "Another active suppression already exists for this channel, recipient, and category."
            );
        }

        suppression.reactivate();

        try {
            NotificationSuppression saved =
                    notificationSuppressionRepository
                            .saveAndFlush(suppression);

            log.info(
                    "Notification suppression reactivated. suppressionId={}",
                    saved.getNotificationSuppressionId()
            );

            return notificationSuppressionMapper
                    .toResponse(saved);

        } catch (DataIntegrityViolationException exception) {
            throw new PublicRequestRejectedException(
                    HttpStatus.CONFLICT,
                    "Another active suppression already exists for this channel, recipient, and category."
            );
        }
    }

    @Override
    @Transactional(readOnly = true)
    public NotificationSuppressionCheckResponse checkSuppression(
            NotificationSuppressionCheckRequest request
    ) {
        requireCheckRequest(request);

        if (request.channel() == NotificationChannel.IN_APP) {
            return NotificationSuppressionCheckResponse
                    .notSuppressed(
                            request.channel(),
                            request.notificationCategory()
                    );
        }

        String normalizedRecipient =
                notificationSuppressionMapper
                        .normalizeRecipientAddress(
                                request.recipientAddress(),
                                request.channel()
                        );

        NotificationSuppression matched =
                notificationSuppressionRepository
                        .findEffectiveSuppressions(
                                request.channel(),
                                normalizedRecipient,
                                request.notificationCategory(),
                                Instant.now()
                        )
                        .stream()
                        .findFirst()
                        .orElse(null);

        if (matched == null) {
            return NotificationSuppressionCheckResponse
                    .notSuppressed(
                            request.channel(),
                            request.notificationCategory()
                    );
        }

        return new NotificationSuppressionCheckResponse(
                true,
                matched.getNotificationSuppressionId(),
                matched.getChannel(),
                request.notificationCategory(),
                matched.getNotificationCategory(),
                matched.getSuppressionReason(),
                matched.getExpiresAt()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationSuppressionResponse>
    getCustomerSuppressions(
            UUID customerId
    ) {
        requireId(
                customerId,
                "Customer ID is required."
        );

        return notificationSuppressionRepository
                .findByCustomerIdOrderByCreatedAtDesc(
                        customerId
                )
                .stream()
                .map(notificationSuppressionMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationSuppressionResponse>
    getRecipientSuppressionHistory(
            NotificationSuppressionCheckRequest request
    ) {
        requireCheckRequest(request);

        if (request.channel() == NotificationChannel.IN_APP) {
            return List.of();
        }

        String normalizedRecipient =
                notificationSuppressionMapper
                        .normalizeRecipientAddress(
                                request.recipientAddress(),
                                request.channel()
                        );

        return notificationSuppressionRepository
                .findByChannelAndNormalizedRecipientAddressOrderByCreatedAtDesc(
                        request.channel(),
                        normalizedRecipient
                )
                .stream()
                .map(notificationSuppressionMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public int deactivateExpiredSuppressions() {
        Instant now = Instant.now();

        int updated =
                notificationSuppressionRepository
                        .deactivateExpiredSuppressions(now);

        if (updated > 0) {
            log.info(
                    "Expired notification suppressions deactivated. count={}",
                    updated
            );
        }

        return updated;
    }

    private NotificationSuppression findSuppression(
            UUID notificationSuppressionId
    ) {
        requireId(
                notificationSuppressionId,
                "Notification suppression ID is required."
        );

        return notificationSuppressionRepository
                .findById(notificationSuppressionId)
                .orElseThrow(
                        () ->
                                new PublicRequestRejectedException(
                                        HttpStatus.NOT_FOUND,
                                        "Notification suppression was not found."
                                )
                );
    }

    private NotificationSuppression findSuppressionForUpdate(
            UUID notificationSuppressionId
    ) {
        requireId(
                notificationSuppressionId,
                "Notification suppression ID is required."
        );

        return notificationSuppressionRepository
                .findByIdForUpdate(
                        notificationSuppressionId
                )
                .orElseThrow(
                        () ->
                                new PublicRequestRejectedException(
                                        HttpStatus.NOT_FOUND,
                                        "Notification suppression was not found."
                                )
                );
    }

    private AdminUser findAuthenticatedAdministrator(
            AdminJwtPrincipal principal
    ) {
        if (
                principal == null
                        || principal.adminUserId() == null
        ) {
            throw AdminAuthenticationException
                    .staleAuthentication();
        }

        AdminUser administrator =
                adminUserRepository
                        .findById(
                                principal.adminUserId()
                        )
                        .orElseThrow(
                                AdminAuthenticationException
                                        ::accountNotFound
                        );

        if (!administrator.isActive()) {
            throw AdminAuthenticationException
                    .inactiveAccount();
        }

        return administrator;
    }

    private void requireCreateRequest(
            NotificationSuppressionCreateRequest request
    ) {
        if (request == null) {
            reject(
                    HttpStatus.BAD_REQUEST,
                    "Notification suppression information is required."
            );
        }
    }

    private void requireUpdateRequest(
            NotificationSuppressionUpdateRequest request
    ) {
        if (request == null) {
            reject(
                    HttpStatus.BAD_REQUEST,
                    "Notification suppression update information is required."
            );
        }

        if (!request.hasChanges()) {
            reject(
                    HttpStatus.BAD_REQUEST,
                    "At least one notification suppression change is required."
            );
        }
    }

    private void requireCheckRequest(
            NotificationSuppressionCheckRequest request
    ) {
        if (request == null) {
            reject(
                    HttpStatus.BAD_REQUEST,
                    "Notification suppression check information is required."
            );
        }

        if (request.channel() == null) {
            reject(
                    HttpStatus.BAD_REQUEST,
                    "Notification channel is required."
            );
        }

        if (
                request.recipientAddress() == null
                        || request.recipientAddress()
                        .trim()
                        .isEmpty()
        ) {
            reject(
                    HttpStatus.BAD_REQUEST,
                    "Notification recipient address is required."
            );
        }
    }

    private void requirePageable(
            Pageable pageable
    ) {
        if (pageable == null) {
            throw new IllegalArgumentException(
                    "Notification suppression pagination information is required."
            );
        }
    }

    private void requireId(
            UUID id,
            String message
    ) {
        if (id == null) {
            reject(
                    HttpStatus.BAD_REQUEST,
                    message
            );
        }
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

    private void reject(
            HttpStatus status,
            String message
    ) {
        throw new PublicRequestRejectedException(
                status,
                message
        );
    }
}