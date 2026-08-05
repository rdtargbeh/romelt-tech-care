package romelt_techcare.backend.enums;

/**
 * ================================================================
 * ROMELT TECHCARE — NOTIFICATION SUPPRESSION REASON
 * ================================================================
 *
 * Purpose:
 * Identifies why EMAIL or SMS notifications must not be sent to a
 * recipient address.
 * ================================================================
 */
public enum NotificationSuppressionReason {

    CUSTOMER_OPT_OUT,

    HARD_BOUNCE,

    INVALID_ADDRESS,

    SPAM_COMPLAINT,

    ADMIN_BLOCK,

    OTHER
}