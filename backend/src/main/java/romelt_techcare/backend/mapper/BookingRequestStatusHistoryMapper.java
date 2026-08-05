package romelt_techcare.backend.mapper;

import org.springframework.stereotype.Component;
import romelt_techcare.backend.dto.BookingRequestStatusHistoryResponse;
import romelt_techcare.backend.entity.BookingRequestStatusHistory;

/**
 * ================================================================
 * ROMELT TECHCARE — BOOKING STATUS HISTORY MAPPER
 * ================================================================
 *
 * Purpose:
 * Converts immutable BookingRequestStatusHistory entities into
 * administrator response DTOs.
 *
 * Responsibilities:
 * - Maps every persisted history field.
 * - Prevents entities from being returned directly by controllers.
 * - Keeps booking-history response construction centralized.
 * ================================================================
 */
@Component
public class BookingRequestStatusHistoryMapper {

    public BookingRequestStatusHistoryResponse toResponse(
            BookingRequestStatusHistory history
    ) {
        if (history == null) {
            throw new IllegalArgumentException(
                    "Booking status history must not be null."
            );
        }

        return new BookingRequestStatusHistoryResponse(
                history.getBookingStatusHistoryId(),
                history.getBookingRequestId(),
                history.getPreviousStatus(),
                history.getNewStatus(),
                history.getChangeReason(),
                history.getChangedByAdminUserId(),
                history.getChangedByAdminName(),
                history.getNotificationEventId(),
                history.getChangedAt()
        );
    }
}