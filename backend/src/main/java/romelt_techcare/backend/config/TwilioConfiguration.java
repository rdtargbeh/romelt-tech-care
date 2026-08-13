package romelt_techcare.backend.config;

import com.twilio.http.TwilioRestClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * ================================================================
 * ROMELT TECHCARE — TWILIO CONFIGURATION
 * ================================================================
 *
 * Purpose:
 * Creates the Twilio REST client used by the SMS notification
 * service.
 *
 * Authentication:
 * - Twilio Account SID identifies the parent account.
 * - Twilio API Key SID identifies the API key.
 * - Twilio API Key Secret authenticates requests.
 *
 * Credentials must be supplied through environment variables and
 * must never be committed to source control.
 * ================================================================
 */
@Configuration
public class TwilioConfiguration {

    @Bean
    public TwilioRestClient twilioRestClient(

            @Value("${notification.sms.account-sid:}")
            String accountSid,

            @Value("${notification.sms.api-key-sid:}")
            String apiKeySid,

            @Value("${notification.sms.api-key-secret:}")
            String apiKeySecret
    ) {
        String normalizedAccountSid =
                normalizeOptional(accountSid);

        String normalizedApiKeySid =
                normalizeOptional(apiKeySid);

        String normalizedApiKeySecret =
                normalizeOptional(apiKeySecret);

        /*
         * The application must still start while SMS is disabled and
         * credentials have not yet been configured.
         *
         * Validation is performed by TwilioSmsService immediately
         * before an SMS is submitted.
         */
        return new TwilioRestClient.Builder(
                normalizedApiKeySid == null
                        ? "SK_NOT_CONFIGURED"
                        : normalizedApiKeySid,
                normalizedApiKeySecret == null
                        ? "NOT_CONFIGURED"
                        : normalizedApiKeySecret
        )
                .accountSid(
                        normalizedAccountSid == null
                                ? "AC_NOT_CONFIGURED"
                                : normalizedAccountSid
                )
                .build();
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