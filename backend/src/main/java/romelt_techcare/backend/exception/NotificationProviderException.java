package romelt_techcare.backend.exception;

/**
 * ================================================================
 * ROMELT TECHCARE — NOTIFICATION PROVIDER EXCEPTION
 * ================================================================
 *
 * Purpose:
 * Represents an unexpected failure while communicating with an
 * external email or SMS provider.
 *
 * Retryable:
 * Indicates whether the notification worker may safely schedule
 * another attempt.
 * ================================================================
 */
public class NotificationProviderException
        extends RuntimeException {

    private final String providerName;

    private final String failureCode;

    private final boolean retryable;

    public NotificationProviderException(
            String providerName,
            String failureCode,
            String message,
            boolean retryable
    ) {
        super(message);

        this.providerName = providerName;
        this.failureCode = failureCode;
        this.retryable = retryable;
    }

    public NotificationProviderException(
            String providerName,
            String failureCode,
            String message,
            boolean retryable,
            Throwable cause
    ) {
        super(message, cause);

        this.providerName = providerName;
        this.failureCode = failureCode;
        this.retryable = retryable;
    }

    public String getProviderName() {
        return providerName;
    }

    public String getFailureCode() {
        return failureCode;
    }

    public boolean isRetryable() {
        return retryable;
    }
}