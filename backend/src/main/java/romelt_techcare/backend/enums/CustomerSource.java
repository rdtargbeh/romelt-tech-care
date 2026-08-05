package romelt_techcare.backend.enums;

/**
 * ================================================================
 * ROMELT TECHCARE — CUSTOMER SOURCE
 * ================================================================
 *
 * Purpose:
 * Identifies how a customer profile first entered the system.
 *
 * Important:
 * The source describes the customer's initial origin. It should not
 * change every time the customer submits another booking or inquiry.
 * ================================================================
 */
public enum CustomerSource {

    BOOKING,

    CONTACT_INQUIRY,

    ADMIN_CREATED,

    REVIEW,

    REFERRAL,

    IMPORT,

    OTHER
}