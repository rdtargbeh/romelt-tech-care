package romelt_techcare.backend.event;

import romelt_techcare.backend.entity.ContactInquiry;
import romelt_techcare.backend.enums.ContactInquiryStatus;

/**
 * ================================================================
 * ROMELT TECHCARE — CONTACT INQUIRY STATUS CHANGED EVENT
 * ================================================================
 *
 * Purpose:
 * Published after an administrator changes a contact inquiry status.
 *
 * customerVisibleMessage:
 * Contains an optional customer-safe response or explanation.
 *
 * It must never contain:
 * - Private administrator notes.
 * - Security information.
 * - Internal spam-analysis details.
 * - Credentials or tokens.
 * ================================================================
 */
public record ContactInquiryStatusChangedEvent(

        ContactInquiry contactInquiry,

        ContactInquiryStatus previousStatus,

        String customerVisibleMessage
) {

    public ContactInquiryStatusChangedEvent {
        if (
                contactInquiry == null
                        || contactInquiry.getContactInquiryId() == null
        ) {
            throw new IllegalArgumentException(
                    "A persisted contact inquiry is required."
            );
        }

        if (previousStatus == null) {
            throw new IllegalArgumentException(
                    "Previous contact inquiry status is required."
            );
        }

        if (contactInquiry.getStatus() == null) {
            throw new IllegalArgumentException(
                    "Current contact inquiry status is required."
            );
        }

        customerVisibleMessage =
                normalizeOptional(customerVisibleMessage);
    }

    private static String normalizeOptional(
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