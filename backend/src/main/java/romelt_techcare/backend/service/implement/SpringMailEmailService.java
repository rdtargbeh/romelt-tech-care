package romelt_techcare.backend.service.implement;


import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import romelt_techcare.backend.dto.EmailSendResult;
import romelt_techcare.backend.service.EmailService;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * ================================================================
 * ROMELT TECHCARE — SPRING MAIL EMAIL SERVICE
 * ================================================================
 *
 * Purpose:
 * Sends transactional customer email using Spring JavaMailSender.
 *
 * Responsibilities:
 * - Sends booking confirmations and status updates.
 * - Sends contact-inquiry confirmations and responses.
 * - Supports plain-text and HTML email.
 * - Uses sender information from application configuration.
 * - Returns a safe success or failure result.
 *
 * Security:
 * - SMTP credentials are supplied through environment variables.
 * - Email body content and credentials are not written to logs.
 * ================================================================
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SpringMailEmailService
        implements EmailService {

    private static final String PROVIDER_NAME = "SPRING_MAIL_SMTP";

    private final JavaMailSender javaMailSender;

    @Value("${notification.email.enabled:false}")
    private boolean enabled;

    @Value("${notification.email.from-address}")
    private String fromAddress;

    @Value("${notification.email.from-name:Romelt TechCare}")
    private String fromName;

    @Value("${notification.email.reply-to-address:}")
    private String replyToAddress;


    @Override
    public EmailSendResult sendEmail(
            String recipientEmail,
            String recipientName,
            String subject,
            String textBody,
            String htmlBody
    ) {
        if (!enabled) {
            return EmailSendResult.failure(
                    PROVIDER_NAME,
                    "EMAIL_DISABLED",
                    "Email delivery is disabled."
            );
        }

        String resolvedRecipientEmail =
                requireText(
                        recipientEmail,
                        "Recipient email is required."
                );

        String resolvedSubject =
                requireText(
                        subject,
                        "Email subject is required."
                );

        String resolvedTextBody =
                normalizeOptional(textBody);

        String resolvedHtmlBody =
                normalizeOptional(htmlBody);

        if (
                resolvedTextBody == null
                        && resolvedHtmlBody == null
        ) {
            throw new IllegalArgumentException(
                    "Email text or HTML content is required."
            );
        }

        String localMessageId =
                UUID.randomUUID().toString();

        try {
            MimeMessage message =
                    javaMailSender.createMimeMessage();

            MimeMessageHelper helper =
                    new MimeMessageHelper(
                            message,
                            true,
                            StandardCharsets.UTF_8.name()
                    );

            helper.setFrom(
                    new InternetAddress(
                            requireText(
                                    fromAddress,
                                    "Configured sender email is required."
                            ),
                            normalizeOptional(fromName) == null
                                    ? "Romelt TechCare"
                                    : fromName
                    )
            );

            helper.setTo(
                    createRecipientAddress(
                            resolvedRecipientEmail,
                            recipientName
                    )
            );

            helper.setSubject(resolvedSubject);

            applyReplyTo(helper);

            applyContent(
                    helper,
                    resolvedTextBody,
                    resolvedHtmlBody
            );

            message.setHeader(
                    "X-Romelt-TechCare-Message-ID",
                    localMessageId
            );

            javaMailSender.send(message);

            String providerMessageId =
                    resolveProviderMessageId(
                            message,
                            localMessageId
                    );

            log.info(
                    "Email accepted by SMTP provider. recipientDomain={}, providerMessageId={}",
                    extractEmailDomain(
                            resolvedRecipientEmail
                    ),
                    providerMessageId
            );

            return EmailSendResult.success(
                    PROVIDER_NAME,
                    providerMessageId
            );

        } catch (MailException exception) {
            log.error(
                    "SMTP email delivery failed. recipientDomain={}, errorType={}",
                    extractEmailDomain(
                            resolvedRecipientEmail
                    ),
                    exception
                            .getClass()
                            .getSimpleName()
            );

            return EmailSendResult.failure(
                    PROVIDER_NAME,
                    "SMTP_SEND_FAILED",
                    safeFailureMessage(
                            exception.getMessage(),
                            "The SMTP provider rejected the email."
                    )
            );

        } catch (Exception exception) {
            log.error(
                    "Unexpected email delivery failure. recipientDomain={}, errorType={}",
                    extractEmailDomain(
                            resolvedRecipientEmail
                    ),
                    exception
                            .getClass()
                            .getSimpleName()
            );

            return EmailSendResult.failure(
                    PROVIDER_NAME,
                    "EMAIL_SEND_ERROR",
                    safeFailureMessage(
                            exception.getMessage(),
                            "An unexpected email delivery error occurred."
                    )
            );
        }
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    private InternetAddress createRecipientAddress(
            String recipientEmail,
            String recipientName
    ) throws Exception {
        String resolvedRecipientName =
                normalizeOptional(recipientName);

        if (resolvedRecipientName == null) {
            return new InternetAddress(
                    recipientEmail
            );
        }

        return new InternetAddress(
                recipientEmail,
                resolvedRecipientName
        );
    }

    private void applyReplyTo(
            MimeMessageHelper helper
    ) throws Exception {
        String resolvedReplyTo =
                normalizeOptional(replyToAddress);

        if (resolvedReplyTo != null) {
            helper.setReplyTo(
                    resolvedReplyTo
            );
        }
    }

    private void applyContent(
            MimeMessageHelper helper,
            String textBody,
            String htmlBody
    ) throws Exception {
        if (
                textBody != null
                        && htmlBody != null
        ) {
            helper.setText(
                    textBody,
                    htmlBody
            );

            return;
        }

        if (htmlBody != null) {
            helper.setText(
                    htmlBody,
                    true
            );

            return;
        }

        helper.setText(
                textBody,
                false
        );
    }

    private String resolveProviderMessageId(
            MimeMessage message,
            String fallbackId
    ) {
        try {
            String[] messageIds =
                    message.getHeader(
                            "Message-ID"
                    );

            if (
                    messageIds != null
                            && messageIds.length > 0
                            && normalizeOptional(
                            messageIds[0]
                    ) != null
            ) {
                return messageIds[0];
            }
        } catch (Exception ignored) {
            // Use the application-generated identifier.
        }

        return fallbackId;
    }

    private String extractEmailDomain(
            String email
    ) {
        String normalized =
                normalizeOptional(email);

        if (normalized == null) {
            return "unknown";
        }

        int separatorIndex =
                normalized.lastIndexOf('@');

        if (
                separatorIndex < 0
                        || separatorIndex
                        == normalized.length() - 1
        ) {
            return "invalid";
        }

        return normalized
                .substring(separatorIndex + 1)
                .toLowerCase();
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

        String normalized =
                value.trim();

        return normalized.isEmpty()
                ? null
                : normalized;
    }
}