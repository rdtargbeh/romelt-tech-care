package romelt_techcare.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import romelt_techcare.backend.entity.BookingRequestStatusHistory;

import java.util.List;
import java.util.UUID;

/**
 * ================================================================
 * ROMELT TECHCARE — BOOKING STATUS HISTORY REPOSITORY
 * ================================================================
 */
@Repository
public interface BookingRequestStatusHistoryRepository
        extends JpaRepository<BookingRequestStatusHistory, UUID> {

    /**
     * Returns all status changes for one booking, newest first.
     */
    List<BookingRequestStatusHistory>
    findByBookingRequestIdOrderByChangedAtDesc(
            UUID bookingRequestId
    );
}