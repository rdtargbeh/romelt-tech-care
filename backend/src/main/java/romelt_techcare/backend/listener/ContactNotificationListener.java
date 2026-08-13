package romelt_techcare.backend.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import romelt_techcare.backend.event.ContactInquiryStatusChangedEvent;
import romelt_techcare.backend.event.ContactInquirySubmittedEvent;
import romelt_techcare.backend.service.ContactNotificationService;

/**
 * ================================================================
 * ROMELT TECHCARE — CONTACT NOTIFICATION LISTENER
 * ================================================================
 *
 * Purpose:
 * Processes contact-inquiry notifications after the related database
 * transaction commits.
 *
 * Behavior:
 * - Sends customer acknowledgment after inquiry creation.
 * - Sends customer status updates when permitted.
 * - Creates administrator in-app notifications.
 * - Prevents notification failures from rolling back valid inquiries.
 * ================================================================
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ContactNotificationListener {

    private final ContactNotificationService
            contactNotificationService;

    /**
     * Processes a newly submitted contact inquiry after commit.
     */
    @TransactionalEventListener(
            phase = TransactionPhase.AFTER_COMMIT
    )
    public void handleInquirySubmitted(
            ContactInquirySubmittedEvent event
    ) {
        if (event == null || event.contactInquiry() == null) {
            log.warn(
                    "Contact-inquiry-submitted notification event was empty."
            );

            return;
        }

        try {
            contactNotificationService.inquirySubmitted(
                    event.contactInquiry()
            );

        } catch (Exception exception) {
            log.error(
                    "Contact-inquiry submission notification processing failed. contactInquiryId={}, referenceNumber={}, errorType={}",
                    event.contactInquiry()
                            .getContactInquiryId(),
                    event.contactInquiry()
                            .getReferenceNumber(),
                    exception.getClass().getSimpleName(),
                    exception
            );
        }
    }

    /**
     * Processes a contact-inquiry status change after commit.
     */
    @TransactionalEventListener(
            phase = TransactionPhase.AFTER_COMMIT
    )
    public void handleInquiryStatusChanged(
            ContactInquiryStatusChangedEvent event
    ) {
        if (
                event == null
                        || event.contactInquiry() == null
                        || event.previousStatus() == null
        ) {
            log.warn(
                    "Contact-inquiry-status notification event was incomplete."
            );

            return;
        }

        try {
            contactNotificationService.inquiryStatusChanged(
                    event.contactInquiry(),
                    event.previousStatus(),
                    event.customerVisibleMessage()
            );

        } catch (Exception exception) {
            log.error(
                    "Contact-inquiry status notification processing failed. contactInquiryId={}, referenceNumber={}, previousStatus={}, currentStatus={}, errorType={}",
                    event.contactInquiry()
                            .getContactInquiryId(),
                    event.contactInquiry()
                            .getReferenceNumber(),
                    event.previousStatus(),
                    event.contactInquiry()
                            .getStatus(),
                    exception.getClass().getSimpleName(),
                    exception
            );
        }
    }
}