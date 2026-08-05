package romelt_techcare.backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import romelt_techcare.backend.enums.BookingRequestStatus;

import java.time.Instant;
import java.util.UUID;

/**
 * ================================================================
 * ROMELT TECHCARE — BOOKING STATUS HISTORY ENTITY
 * ================================================================
 *
 * Purpose:
 * Preserves every booking lifecycle transition.
 *
 * The BookingRequest entity stores the current status. This entity
 * stores the historical sequence of status changes.
 * ================================================================
 */
@Entity
@Table(
        name = "booking_request_status_history",
        indexes = {
                @Index(
                        name = "idx_booking_status_history_booking",
                        columnList = "booking_request_id, changed_at"
                ),
                @Index(
                        name = "idx_booking_status_history_new_status",
                        columnList = "new_status, changed_at"
                ),
                @Index(
                        name = "idx_booking_status_history_admin",
                        columnList = "changed_by_admin_user_id, changed_at"
                )
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingRequestStatusHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "booking_status_history_id", nullable = false, updatable = false)
    private UUID bookingStatusHistoryId;

    @Column(name = "booking_request_id", nullable = false, updatable = false)
    private UUID bookingRequestId;

    @Enumerated(EnumType.STRING)
    @Column(name = "previous_status", length = 30, updatable = false)
    private BookingRequestStatus previousStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "new_status", nullable = false, length = 30, updatable = false)
    private BookingRequestStatus newStatus;

    @Column(name = "change_reason", length = 500, updatable = false)
    private String changeReason;

    @Column(name = "changed_by_admin_user_id", updatable = false)
    private UUID changedByAdminUserId;

    @Column(name = "changed_by_admin_name", length = 160, updatable = false)
    private String changedByAdminName;

    @Column(name = "notification_event_id", updatable = false)
    private UUID notificationEventId;

    @Column(name = "changed_at", nullable = false, updatable = false)
    private Instant changedAt;

    @PrePersist
    protected void onCreate() {
        if (changedAt == null) {
            changedAt = Instant.now();
        }
    }
}