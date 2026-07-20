package com.romelttechcare.backend.contact.repository;

import com.romelttechcare.backend.contact.entity.ContactInquiry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * ================================================================
 * ROMELT TECHCARE — CONTACT INQUIRY REPOSITORY
 * ================================================================
 *
 * Purpose:
 * Provides persistence operations for public contact inquiries.
 *
 * Responsibilities:
 * - Stores accepted contact inquiries.
 * - Checks generated reference-number uniqueness.
 * - Supports future administrative inquiry management.
 * ================================================================
 */
@Repository
public interface ContactInquiryRepository
        extends JpaRepository<ContactInquiry, UUID> {

    boolean existsByReferenceNumber(
            String referenceNumber
    );
}