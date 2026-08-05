package romelt_techcare.backend.mapper;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;
import romelt_techcare.backend.dto.NotificationDeliveryCreateRequest;
import romelt_techcare.backend.dto.NotificationDeliveryResponse;
import romelt_techcare.backend.dto.NotificationDeliverySummaryResponse;
import romelt_techcare.backend.entity.NotificationDelivery;
import romelt_techcare.backend.enums.NotificationChannel;
import romelt_techcare.backend.enums.NotificationDeliveryStatus;

import java.util.Locale;
import java.util.UUID;

/**
 * ================================================================
 * ROMELT TECHCARE — NOTIFICATION DELIVERY MAPPER
 * ================================================================
 *
 * Purpose:
 * Converts internal notification-delivery requests into persistent
 * delivery entities and maps delivery entities into administrator
 * responses.
 *
 * Responsibilities:
 * - Maps EMAIL, SMS, and IN_APP delivery requests.
 * - Generates normalized recipient addresses.
 * - Applies channel-specific recipient and content rules.
 * - Removes provider data from IN_APP deliveries.
 * - Applies delivery defaults.
 * - Maps complete and compact delivery responses.
 *
 * Channel rules:
 *
 * EMAIL:
 * - recipientAddress contains the email address.
 * - subject is required.
 * - renderedBodyText or renderedBodyHtml is required.
 *
 * SMS:
 * - recipientAddress contains the telephone number.
 * - renderedBodyText is required.
 * - subject and renderedBodyHtml are not used.
 *
 * IN_APP:
 * - recipientAdminUserId is required.
 * - recipientAddress stores the administrator UUID as a snapshot.
 * - subject is the portal notification title.
 * - renderedBodyText is the portal notification message.
 * - renderedBodyHtml and provider fields are not used.
 *
 * Security:
 * Provider responses and rendered content may contain personal or
 * operational information and must not be exposed through public APIs.
 * ================================================================
 */
@Component
public class NotificationDeliveryMapper {

    /**
     * Converts an internal delivery request into a new persistent
     * delivery entity.
     */
    public NotificationDelivery toEntity(
            NotificationDeliveryCreateRequest request
    ) {
        requireRequest(request);
        validateRequest(request);

        NotificationChannel channel =
                request.channel();

        String recipientAddress =
                resolveRecipientAddress(request);

        String normalizedRecipientAddress =
                normalizeRecipientAddress(
                        recipientAddress,
                        channel,
                        request.recipientAdminUserId()
                );

        String subject =
                normalizeOptional(request.subject());

        String renderedBodyText =
                normalizeOptional(request.renderedBodyText());

        String renderedBodyHtml =
                normalizeOptional(request.renderedBodyHtml());

        String senderAddress =
                normalizeOptional(request.senderAddress());

        String senderName =
                normalizeOptional(request.senderName());

        String providerName =
                normalizeOptional(request.providerName());

        String providerMessageId =
                normalizeOptional(request.providerMessageId());

        JsonNode providerResponseJson =
                copyJson(request.providerResponseJson());

        if (channel == NotificationChannel.SMS) {
            subject = null;
            renderedBodyHtml = null;
        }

        if (channel == NotificationChannel.IN_APP) {
            renderedBodyHtml = null;
            senderAddress = null;
            providerName = null;
            providerMessageId = null;
            providerResponseJson = null;
        }

        return NotificationDelivery.builder()
                .notificationEventId(
                        request.notificationEventId()
                )
                .customerId(
                        request.customerId()
                )
                .recipientAdminUserId(
                        request.recipientAdminUserId()
                )
                .channel(channel)
                .recipientAddress(recipientAddress)
                .normalizedRecipientAddress(
                        normalizedRecipientAddress
                )
                .recipientName(
                        normalizeOptional(request.recipientName())
                )
                .senderAddress(senderAddress)
                .senderName(senderName)
                .subject(subject)
                .renderedBodyText(renderedBodyText)
                .renderedBodyHtml(renderedBodyHtml)
                .deliveryStatus(
                        NotificationDeliveryStatus.PENDING
                )
                .providerName(providerName)
                .providerMessageId(providerMessageId)
                .providerResponseJson(providerResponseJson)
                .attemptCount(0)
                .maximumAttempts(
                        request.maximumAttempts() == null
                                ? 5
                                : request.maximumAttempts()
                )
                .build();
    }

    /**
     * Maps one complete administrator delivery response.
     */
    public NotificationDeliveryResponse toResponse(
            NotificationDelivery delivery
    ) {
        requireDelivery(delivery);

        return new NotificationDeliveryResponse(
                delivery.getNotificationDeliveryId(),
                delivery.getNotificationEventId(),
                delivery.getCustomerId(),
                delivery.getRecipientAdminUserId(),
                delivery.getChannel(),
                delivery.getRecipientAddress(),
                delivery.getRecipientName(),
                delivery.getSenderAddress(),
                delivery.getSenderName(),
                delivery.getSubject(),
                delivery.getRenderedBodyText(),
                delivery.getRenderedBodyHtml(),
                delivery.getDeliveryStatus(),
                delivery.getProviderName(),
                delivery.getProviderMessageId(),
                copyJson(delivery.getProviderResponseJson()),
                delivery.getAttemptCount(),
                delivery.getMaximumAttempts(),
                delivery.getNextAttemptAt(),
                delivery.getFirstAttemptedAt(),
                delivery.getLastAttemptedAt(),
                delivery.getProcessingStartedAt(),
                delivery.getSentAt(),
                delivery.getDeliveredAt(),
                delivery.getOpenedAt(),
                delivery.getClickedAt(),
                delivery.getBouncedAt(),
                delivery.getFailedAt(),
                delivery.getCancelledAt(),
                delivery.getSuppressedAt(),
                delivery.getReadAt(),
                delivery.getDismissedAt(),
                delivery.getFailureCode(),
                delivery.getFailureMessage(),
                delivery.getSuppressionReason(),
                delivery.getCreatedAt(),
                delivery.getUpdatedAt(),
                delivery.getRowVersion()
        );
    }

    /**
     * Maps one compact administrator delivery response.
     */
    public NotificationDeliverySummaryResponse toSummaryResponse(
            NotificationDelivery delivery
    ) {
        requireDelivery(delivery);

        return new NotificationDeliverySummaryResponse(
                delivery.getNotificationDeliveryId(),
                delivery.getNotificationEventId(),
                delivery.getCustomerId(),
                delivery.getRecipientAdminUserId(),
                delivery.getChannel(),
                delivery.getRecipientName(),
                delivery.getRecipientAddress(),
                delivery.getSubject(),
                delivery.getDeliveryStatus(),
                delivery.getAttemptCount(),
                delivery.getMaximumAttempts(),
                delivery.getNextAttemptAt(),
                delivery.getSentAt(),
                delivery.getDeliveredAt(),
                delivery.getReadAt(),
                delivery.getDismissedAt(),
                delivery.getCreatedAt()
        );
    }

    /**
     * Normalizes a recipient address according to the selected
     * notification channel.
     */
    public String normalizeRecipientAddress(
            String recipientAddress,
            NotificationChannel channel,
            UUID recipientAdminUserId
    ) {
        if (channel == null) {
            throw new IllegalArgumentException(
                    "Notification delivery channel is required."
            );
        }

        return switch (channel) {
            case EMAIL ->
                    normalizeEmail(recipientAddress);

            case SMS ->
                    normalizePhone(recipientAddress);

            case IN_APP ->
                    normalizeAdministratorRecipient(
                            recipientAdminUserId
                    );
        };
    }

    /**
     * Produces the stored readable recipient-address snapshot.
     */
    public String resolveRecipientAddress(
            NotificationDeliveryCreateRequest request
    ) {
        requireRequest(request);

        if (request.channel() == null) {
            throw new IllegalArgumentException(
                    "Notification delivery channel is required."
            );
        }

        if (request.channel() == NotificationChannel.IN_APP) {
            if (request.recipientAdminUserId() == null) {
                throw new IllegalArgumentException(
                        "In-app notification administrator ID is required."
                );
            }

            return request.recipientAdminUserId().toString();
        }

        return normalizeRequired(
                request.recipientAddress(),
                "Notification recipient address is required."
        );
    }

    /**
     * Validates one internal delivery request.
     */
    private void validateRequest(
            NotificationDeliveryCreateRequest request
    ) {
        if (request.notificationEventId() == null) {
            throw new IllegalArgumentException(
                    "Notification event ID is required."
            );
        }

        if (request.channel() == null) {
            throw new IllegalArgumentException(
                    "Notification delivery channel is required."
            );
        }

        if (!request.hasValidContent()) {
            throw new IllegalArgumentException(
                    resolveContentValidationMessage(
                            request.channel()
                    )
            );
        }

        if (
                request.maximumAttempts() != null
                        && request.maximumAttempts() < 1
        ) {
            throw new IllegalArgumentException(
                    "Maximum notification delivery attempts must be greater than zero."
            );
        }

        switch (request.channel()) {
            case EMAIL -> validateEmailRequest(request);
            case SMS -> validateSmsRequest(request);
            case IN_APP -> validateInAppRequest(request);
        }
    }

    private void validateEmailRequest(
            NotificationDeliveryCreateRequest request
    ) {
        String email =
                normalizeRequired(
                        request.recipientAddress(),
                        "Email notification recipient address is required."
                );

        if (
                !email.contains("@")
                        || email.startsWith("@")
                        || email.endsWith("@")
        ) {
            throw new IllegalArgumentException(
                    "Email notification recipient address is invalid."
            );
        }
    }

    private void validateSmsRequest(
            NotificationDeliveryCreateRequest request
    ) {
        String phone =
                normalizeRequired(
                        request.recipientAddress(),
                        "SMS notification recipient telephone number is required."
                );

        String digitsOnly =
                phone.replaceAll("\\D", "");

        if (
                digitsOnly.length() < 7
                        || digitsOnly.length() > 30
        ) {
            throw new IllegalArgumentException(
                    "SMS notification recipient telephone number is invalid."
            );
        }

        if (hasText(request.subject())) {
            throw new IllegalArgumentException(
                    "SMS notification deliveries cannot contain a subject."
            );
        }

        if (hasText(request.renderedBodyHtml())) {
            throw new IllegalArgumentException(
                    "SMS notification deliveries cannot contain an HTML body."
            );
        }
    }

    private void validateInAppRequest(
            NotificationDeliveryCreateRequest request
    ) {
        if (request.recipientAdminUserId() == null) {
            throw new IllegalArgumentException(
                    "In-app notifications require an administrator recipient ID."
            );
        }

        if (hasText(request.renderedBodyHtml())) {
            throw new IllegalArgumentException(
                    "In-app notification deliveries cannot contain an HTML body."
            );
        }

        if (hasText(request.providerName())) {
            throw new IllegalArgumentException(
                    "In-app notification deliveries cannot contain a provider name."
            );
        }

        if (hasText(request.providerMessageId())) {
            throw new IllegalArgumentException(
                    "In-app notification deliveries cannot contain a provider message ID."
            );
        }

        if (request.providerResponseJson() != null) {
            throw new IllegalArgumentException(
                    "In-app notification deliveries cannot contain a provider response."
            );
        }
    }

    private String resolveContentValidationMessage(
            NotificationChannel channel
    ) {
        return switch (channel) {
            case EMAIL ->
                    "Email notification deliveries require a subject and a text or HTML body.";

            case SMS ->
                    "SMS notification deliveries require a text body.";

            case IN_APP ->
                    "In-app notification deliveries require an administrator recipient, title, and message.";
        };
    }

    private String normalizeEmail(
            String value
    ) {
        return normalizeRequired(
                value,
                "Email notification recipient address is required."
        ).toLowerCase(Locale.ROOT);
    }

    private String normalizePhone(
            String value
    ) {
        String normalized =
                normalizeRequired(
                        value,
                        "SMS notification recipient telephone number is required."
                );

        String digitsOnly =
                normalized.replaceAll("\\D", "");

        if (digitsOnly.isBlank()) {
            throw new IllegalArgumentException(
                    "SMS notification recipient telephone number is invalid."
            );
        }

        return digitsOnly;
    }

    private String normalizeAdministratorRecipient(
            UUID recipientAdminUserId
    ) {
        if (recipientAdminUserId == null) {
            throw new IllegalArgumentException(
                    "In-app notification administrator ID is required."
            );
        }

        return recipientAdminUserId
                .toString()
                .toLowerCase(Locale.ROOT);
    }

    private JsonNode copyJson(
            JsonNode source
    ) {
        return source == null
                ? null
                : source.deepCopy();
    }

    private String normalizeRequired(
            String value,
            String message
    ) {
        String normalized =
                normalizeOptional(value);

        if (normalized == null) {
            throw new IllegalArgumentException(message);
        }

        return normalized;
    }

    private String normalizeOptional(
            String value
    ) {
        if (value == null) {
            return null;
        }

        String normalized =
                value.trim();

        return normalized.isEmpty()
                ? null
                : normalized;
    }

    private boolean hasText(
            String value
    ) {
        return value != null
                && !value.trim().isEmpty();
    }

    private void requireRequest(
            NotificationDeliveryCreateRequest request
    ) {
        if (request == null) {
            throw new IllegalArgumentException(
                    "Notification delivery request is required."
            );
        }
    }

    private void requireDelivery(
            NotificationDelivery delivery
    ) {
        if (delivery == null) {
            throw new IllegalArgumentException(
                    "Notification delivery is required."
            );
        }
    }
}