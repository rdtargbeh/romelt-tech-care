package romelt_techcare.backend.event;

import romelt_techcare.backend.entity.ContactInquiry;

/**
 * ================================================================
 * ROMELT TECHCARE — CONTACT INQUIRY SUBMITTED EVENT
 * ================================================================
 *
 * Purpose:
 * Published after a contact inquiry has been persisted successfully.
 *
 * Processing:
 * ContactNotificationListener receives the event after the inquiry
 * transaction commits.
 * ================================================================
 */
public record ContactInquirySubmittedEvent(

        ContactInquiry contactInquiry
) {

    public ContactInquirySubmittedEvent {
        if (
                contactInquiry == null
                        || contactInquiry.getContactInquiryId() == null
        ) {
            throw new IllegalArgumentException(
                    "A persisted contact inquiry is required."
            );
        }
    }
}