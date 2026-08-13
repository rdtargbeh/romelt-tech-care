package romelt_techcare.backend.service.implement;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import romelt_techcare.backend.dto.BookingRequestStatusHistoryResponse;
import romelt_techcare.backend.exception.PublicRequestRejectedException;
import romelt_techcare.backend.mapper.BookingRequestStatusHistoryMapper;
import romelt_techcare.backend.repository.BookingRequestRepository;
import romelt_techcare.backend.repository.BookingRequestStatusHistoryRepository;
import romelt_techcare.backend.service.BookingRequestStatusHistoryService;

import java.util.List;
import java.util.UUID;

/**
 * ================================================================
 * ROMELT TECHCARE — BOOKING STATUS HISTORY SERVICE IMPLEMENTATION
 * ================================================================
 *
 * Purpose:
 * Provides read-only access to persisted booking lifecycle history.
 *
 * Responsibilities:
 * - Validates the booking request identifier.
 * - Confirms that the parent booking exists.
 * - Retrieves status-history records in newest-first order.
 * - Maps entities to administrator-safe response DTOs.
 *
 * Important:
 * BookingRequestServiceImpl remains responsible for creating history
 * entries whenever a booking is created or its status changes.
 *
 * This service does not modify or delete history records.
 * ================================================================
 */
@Service
@RequiredArgsConstructor
public class BookingRequestStatusHistoryServiceImpl implements BookingRequestStatusHistoryService {

    private final BookingRequestRepository bookingRequestRepository;

    private final BookingRequestStatusHistoryRepository bookingRequestStatusHistoryRepository;

    private final BookingRequestStatusHistoryMapper
            bookingRequestStatusHistoryMapper;

    @Override
    @Transactional(readOnly = true)
    public List<BookingRequestStatusHistoryResponse>
    getBookingStatusHistory(
            UUID bookingRequestId
    ) {
        requireBookingRequestId(bookingRequestId);

        if (
                !bookingRequestRepository.existsById(
                        bookingRequestId
                )
        ) {
            throw new PublicRequestRejectedException(
                    HttpStatus.NOT_FOUND,
                    "Booking request was not found."
            );
        }

        return bookingRequestStatusHistoryRepository
                .findByBookingRequestIdOrderByChangedAtDesc(
                        bookingRequestId
                )
                .stream()
                .map(
                        bookingRequestStatusHistoryMapper
                                ::toResponse
                )
                .toList();
    }

    private void requireBookingRequestId(
            UUID bookingRequestId
    ) {
        if (bookingRequestId == null) {
            throw new PublicRequestRejectedException(
                    HttpStatus.BAD_REQUEST,
                    "Booking request ID is required."
            );
        }
    }
}