package romelt_techcare.backend.service;

import romelt_techcare.backend.entity.ContactInquiry;
import romelt_techcare.backend.enums.ContactInquiryStatus;

/**
 * ================================================================
 * ROMELT TECHCARE — CONTACT NOTIFICATION SERVICE
 * ================================================================
 *
 * Purpose:
 * Contains notification rules for customer contact inquiries.
 *
 * Responsibilities:
 * - Sends customer acknowledgment after inquiry submission.
 * - Sends customer updates when inquiry status changes.
 * - Sends administrator in-app notifications.
 * - Prevents customer notification when an inquiry is marked SPAM.
 * - Delegates actual EMAIL, SMS, and IN_APP delivery to
 *   NotificationService.
 *
 * Important:
 * This service should be called after the related contact-inquiry
 * transaction commits.
 * ================================================================
 */
public interface ContactNotificationService {

    /**
     * Sends notifications after a contact inquiry is submitted.
     */
    void inquirySubmitted(
            ContactInquiry contactInquiry
    );

    /**
     * Sends notifications after a contact-inquiry status change.
     *
     * customerVisibleMessage may contain an administrator-approved
     * customer-facing response or explanation.
     */
    void inquiryStatusChanged(
            ContactInquiry contactInquiry,
            ContactInquiryStatus previousStatus,
            String customerVisibleMessage
    );
}