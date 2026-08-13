package romelt_techcare.backend.service.implement;

import com.twilio.exception.ApiException;
import com.twilio.http.TwilioRestClient;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import romelt_techcare.backend.dto.SmsSendResult;
import romelt_techcare.backend.service.SmsService;

import java.net.URI;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * ================================================================
 * ROMELT TECHCARE — TWILIO SMS SERVICE
 * ================================================================
 *
 * Purpose:
 * Sends transactional SMS notifications through a Twilio Messaging
 * Service.
 *
 * Responsibilities:
 * - Validates the configured Twilio credentials.
 * - Validates recipient numbers in E.164 format.
 * - Sends messages through a Twilio Messaging Service.
 * - Applies an optional delivery-status callback URL.
 * - Returns the Twilio message SID and initial provider status.
 * - Prevents message content and full telephone numbers from being
 *   written to application logs.
 *
 * Supported use cases:
 * - Booking received.
 * - Booking status changed.
 * - Contact inquiry received.
 * - Contact inquiry status changed when SMS is appropriate.
 *
 * Important:
 * Twilio initially returns states such as queued, accepted, or sent.
 * Final delivery states may be reported asynchronously through the
 * configured status callback.
 * ================================================================
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TwilioSmsService
        implements SmsService {

    private static final String PROVIDER_NAME =
            "TWILIO";

    private static final int MAXIMUM_SMS_LENGTH =
            1600;

    private static final Pattern E164_PATTERN =
            Pattern.compile("^\\+[1-9]\\d{7,14}$");

    private final TwilioRestClient twilioRestClient;

    @Value("${notification.sms.enabled:false}")
    private boolean enabled;

    @Value("${notification.sms.account-sid:}")
    private String accountSid;

    @Value("${notification.sms.api-key-sid:}")
    private String apiKeySid;

    @Value("${notification.sms.api-key-secret:}")
    private String apiKeySecret;

    @Value("${notification.sms.messaging-service-sid:}")
    private String messagingServiceSid;

    @Value("${notification.sms.status-callback-url:}")
    private String statusCallbackUrl;

    @Override
    public SmsSendResult sendSms(
            String recipientPhone,
            String message
    ) {
        if (!enabled) {
            return SmsSendResult.failure(
                    PROVIDER_NAME,
                    "SMS_DISABLED",
                    "SMS delivery is disabled."
            );
        }

        validateConfiguration();

        String resolvedRecipientPhone =
                normalizePhoneNumber(
                        recipientPhone
                );

        String resolvedMessage =
                requireText(
                        message,
                        "SMS message is required."
                );

        if (
                resolvedMessage.length()
                        > MAXIMUM_SMS_LENGTH
        ) {
            throw new IllegalArgumentException(
                    "SMS message cannot exceed 1,600 characters."
            );
        }

        try {
            var messageCreator =
                    Message.creator(
                            new PhoneNumber(
                                    resolvedRecipientPhone
                            ),
                            requireText(
                                    messagingServiceSid,
                                    "Twilio Messaging Service SID is required."
                            ),
                            resolvedMessage
                    );

            URI callbackUri =
                    resolveStatusCallbackUri();

            if (callbackUri != null) {
                messageCreator.setStatusCallback(
                        callbackUri
                );
            }

            Message twilioMessage =
                    messageCreator.create(
                            twilioRestClient
                    );

            String messageSid =
                    twilioMessage == null
                            ? null
                            : normalizeOptional(
                            twilioMessage.getSid()
                    );

            String providerStatus =
                    twilioMessage == null
                            || twilioMessage.getStatus() == null
                            ? null
                            : twilioMessage
                            .getStatus()
                            .toString()
                            .toUpperCase(Locale.ROOT);

            if (messageSid == null) {
                return SmsSendResult.failure(
                        PROVIDER_NAME,
                        "TWILIO_EMPTY_RESPONSE",
                        "Twilio did not return a message identifier."
                );
            }

            log.info(
                    "SMS accepted by Twilio. recipientSuffix={}, messageSid={}, providerStatus={}",
                    maskPhoneNumber(
                            resolvedRecipientPhone
                    ),
                    messageSid,
                    providerStatus
            );

            return SmsSendResult.success(
                    PROVIDER_NAME,
                    messageSid,
                    providerStatus
            );

        } catch (ApiException exception) {
            String failureCode =
                    exception.getCode() == null
                            ? "TWILIO_API_ERROR"
                            : "TWILIO_"
                            + exception.getCode();

            log.error(
                    "Twilio SMS submission failed. recipientSuffix={}, failureCode={}",
                    maskPhoneNumber(
                            resolvedRecipientPhone
                    ),
                    failureCode
            );

            return SmsSendResult.failure(
                    PROVIDER_NAME,
                    failureCode,
                    safeFailureMessage(
                            exception.getMessage(),
                            "Twilio rejected the SMS request."
                    )
            );

        } catch (IllegalArgumentException exception) {
            throw exception;

        } catch (Exception exception) {
            log.error(
                    "Unexpected SMS delivery failure. recipientSuffix={}, errorType={}",
                    maskPhoneNumber(
                            resolvedRecipientPhone
                    ),
                    exception
                            .getClass()
                            .getSimpleName()
            );

            return SmsSendResult.failure(
                    PROVIDER_NAME,
                    "SMS_SEND_ERROR",
                    safeFailureMessage(
                            exception.getMessage(),
                            "An unexpected SMS delivery error occurred."
                    )
            );
        }
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    private void validateConfiguration() {
        requireText(
                accountSid,
                "Twilio Account SID is required when SMS is enabled."
        );

        requireText(
                apiKeySid,
                "Twilio API Key SID is required when SMS is enabled."
        );

        requireText(
                apiKeySecret,
                "Twilio API Key Secret is required when SMS is enabled."
        );

        requireText(
                messagingServiceSid,
                "Twilio Messaging Service SID is required when SMS is enabled."
        );
    }

    private String normalizePhoneNumber(
            String value
    ) {
        String normalized =
                requireText(
                        value,
                        "SMS recipient telephone number is required."
                )
                        .replace(" ", "")
                        .replace("-", "")
                        .replace("(", "")
                        .replace(")", "");

        if (!E164_PATTERN.matcher(normalized).matches()) {
            throw new IllegalArgumentException(
                    "SMS recipient telephone number must use E.164 format, such as +15155550123."
            );
        }

        return normalized;
    }

    private URI resolveStatusCallbackUri() {
        String normalizedUrl =
                normalizeOptional(
                        statusCallbackUrl
                );

        if (normalizedUrl == null) {
            return null;
        }

        try {
            URI uri = URI.create(normalizedUrl);

            if (
                    uri.getScheme() == null
                            || uri.getHost() == null
            ) {
                throw new IllegalArgumentException(
                        "Twilio status callback URL must be an absolute URL."
                );
            }

            if (
                    !"https".equalsIgnoreCase(
                            uri.getScheme()
                    )
                            && !"http".equalsIgnoreCase(
                            uri.getScheme()
                    )
            ) {
                throw new IllegalArgumentException(
                        "Twilio status callback URL must use HTTP or HTTPS."
                );
            }

            return uri;

        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(
                    "Twilio status callback URL is invalid.",
                    exception
            );
        }
    }

    private String maskPhoneNumber(
            String phoneNumber
    ) {
        String normalized =
                normalizeOptional(phoneNumber);

        if (normalized == null) {
            return "unknown";
        }

        if (normalized.length() <= 4) {
            return "****";
        }

        return "****"
                + normalized.substring(
                normalized.length() - 4
        );
    }

    private String safeFailureMessage(
            String value,
            String fallback
    ) {
        String normalized =
                normalizeOptional(value);

        if (normalized == null) {
            return fallback;
        }

        return normalized.length() <= 1000
                ? normalized
                : normalized.substring(
                0,
                1000
        );
    }

    private String requireText(
            String value,
            String message
    ) {
        String normalized =
                normalizeOptional(value);

        if (normalized == null) {
            throw new IllegalArgumentException(
                    message
            );
        }

        return normalized;
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