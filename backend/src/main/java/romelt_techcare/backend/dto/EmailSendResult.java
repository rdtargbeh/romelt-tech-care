package romelt_techcare.backend.dto;

import java.time.Instant;

/**
 * ================================================================
 * ROMELT TECHCARE — EMAIL SEND RESULT
 * ================================================================
 *
 * Purpose:
 * Represents the immediate result of sending one customer email.
 *
 * A successful result means the SMTP provider accepted the message.
 * It does not guarantee that the recipient opened the email.
 * ================================================================
 */
public record EmailSendResult(

        boolean successful,

        String providerName,

        String providerMessageId,

        String failureCode,

        String failureMessage,

        Instant attemptedAt
) {

    public static EmailSendResult success(
            String providerName,
            String providerMessageId
    ) {
        return new EmailSendResult(
                true,
                providerName,
                providerMessageId,
                null,
                null,
                Instant.now()
        );
    }

    public static EmailSendResult failure(
            String providerName,
            String failureCode,
            String failureMessage
    ) {
        return new EmailSendResult(
                false,
                providerName,
                null,
                failureCode,
                failureMessage,
                Instant.now()
        );
    }
}