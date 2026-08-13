package romelt_techcare.backend.service;

import romelt_techcare.backend.dto.EmailSendResult;

/**
 * ================================================================
 * ROMELT TECHCARE — EMAIL SERVICE
 * ================================================================
 *
 * Purpose:
 * Defines the application-level contract for sending customer email.
 *
 * Responsibilities:
 * - Sends plain-text and optional HTML email.
 * - Uses the configured Spring Mail SMTP provider.
 * - Returns a simple send result.
 *
 * This interface keeps booking and contact services independent from
 * JavaMailSender and the selected SMTP provider.
 * ================================================================
 */
public interface EmailService {

    EmailSendResult sendEmail(
            String recipientEmail,
            String recipientName,
            String subject,
            String textBody,
            String htmlBody
    );

    boolean isEnabled();
}