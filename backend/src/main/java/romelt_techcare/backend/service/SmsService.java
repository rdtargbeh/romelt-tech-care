package romelt_techcare.backend.service;

import romelt_techcare.backend.dto.SmsSendResult;

/**
 * ================================================================
 * ROMELT TECHCARE — SMS SERVICE
 * ================================================================
 *
 * Purpose:
 * Defines the application-level contract for sending transactional
 * SMS notifications.
 *
 * Responsibilities:
 * - Sends booking-related SMS messages.
 * - Sends contact-inquiry SMS messages when appropriate.
 * - Returns the provider submission result.
 * - Keeps business services independent from the Twilio SDK.
 * ================================================================
 */
public interface SmsService {

    SmsSendResult sendSms(
            String recipientPhone,
            String message
    );

    boolean isEnabled();
}