package romelt_techcare.backend.enums;

/**
 * ================================================================
 * ROMELT TECHCARE — BOOKING SOURCE
 * ================================================================
 *
 * Purpose:
 * Identifies how a customer booking entered the Romelt TechCare
 * system.
 *
 * Values:
 * - WEBSITE: Customer submitted the public website booking form.
 * - PHONE: Customer called Romelt TechCare.
 * - EMAIL: Customer requested service through email.
 * - WALK_IN: Customer requested service in person.
 * - ADMIN_ENTRY: Administrator manually entered the booking without
 *   a more specific source.
 * - OTHER: Booking originated through another supported channel.
 * ================================================================
 */
public enum BookingSource {

    WEBSITE,

    PHONE,

    EMAIL,

    WALK_IN,

    ADMIN_ENTRY,

    OTHER
}