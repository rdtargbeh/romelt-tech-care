package romelt_techcare.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import romelt_techcare.backend.entity.ContactInquiry;

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