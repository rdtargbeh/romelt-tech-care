package romelt_techcare.backend.enums;

/**
 * ================================================================
 * ROMELT TECHCARE — CUSTOMER STATUS
 * ================================================================
 *
 * Purpose:
 * Tracks the operational state of a reusable customer profile.
 *
 * Values:
 * - ACTIVE: Available for normal customer operations.
 * - INACTIVE: Retained but not currently active.
 * - BLOCKED: Prevented from normal service or communication workflows.
 * - ARCHIVED: Retained for historical purposes.
 * - MERGED: Duplicate profile merged into another customer.
 * - DELETED: Soft-deleted customer profile.
 * ================================================================
 */
public enum CustomerStatus {

    ACTIVE,

    INACTIVE,

    BLOCKED,

    ARCHIVED,

    MERGED,

    DELETED
}