package romelt_techcare.backend.dto;

import java.time.Instant;

/**
 * ================================================================
 * ROMELT TECHCARE — SMS SEND RESULT
 * ================================================================
 *
 * Purpose:
 * Represents the immediate result of submitting one SMS message to
 * Twilio.
 *
 * A successful result means Twilio accepted the message request.
 * Final delivery may later be confirmed through Twilio's status
 * callback.
 * ================================================================
 */
public record SmsSendResult(

        boolean successful,

        String providerName,

        String providerMessageId,

        String providerStatus,

        String failureCode,

        String failureMessage,

        Instant attemptedAt
) {

    public static SmsSendResult success(
            String providerName,
            String providerMessageId,
            String providerStatus
    ) {
        return new SmsSendResult(
                true,
                providerName,
                providerMessageId,
                providerStatus,
                null,
                null,
                Instant.now()
        );
    }

    public static SmsSendResult failure(
            String providerName,
            String failureCode,
            String failureMessage
    ) {
        return new SmsSendResult(
                false,
                providerName,
                null,
                null,
                failureCode,
                failureMessage,
                Instant.now()
        );
    }
}