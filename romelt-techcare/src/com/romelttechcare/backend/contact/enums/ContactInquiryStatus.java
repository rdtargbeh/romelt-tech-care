package com.romelttechcare.backend.contact.enums;

/**
 * ================================================================
 * ROMELT TECHCARE — CONTACT INQUIRY STATUS
 * ================================================================
 *
 * Purpose:
 * Tracks the operational lifecycle of a public customer inquiry.
 *
 * Status flow:
 * NEW → IN_PROGRESS → RESPONDED → CLOSED
 *
 * SPAM may be assigned when the inquiry is determined to be abusive
 * or unrelated.
 * ================================================================
 */
public enum ContactInquiryStatus {
    NEW,
    IN_PROGRESS,
    RESPONDED,
    CLOSED,
    SPAM
}