package romelt_techcare.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import romelt_techcare.backend.entity.BookingRequest;

import java.util.UUID;

/**
 * ================================================================
 * ROMELT TECHCARE — BOOKING REQUEST REPOSITORY
 * ================================================================
 *
 * Purpose:
 * Provides database operations for customer booking requests.
 *
 * Responsibilities:
 * - Persists public booking requests.
 * - Checks customer reference-number uniqueness.
 * - Supports future administrative booking management.
 * ================================================================
 */
@Repository
public interface BookingRequestRepository
        extends JpaRepository<BookingRequest, UUID> {

    boolean existsByReferenceNumber(
            String referenceNumber
    );
}