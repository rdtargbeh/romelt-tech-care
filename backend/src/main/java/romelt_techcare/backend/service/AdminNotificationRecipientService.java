package romelt_techcare.backend.service;

import java.util.List;
import java.util.UUID;

/**
 * ================================================================
 * ROMELT TECHCARE — ADMIN NOTIFICATION RECIPIENT SERVICE
 * ================================================================
 *
 * Purpose:
 * Resolves active administrators who should receive operational
 * in-app notifications.
 *
 * Current routing:
 * - All active administrators receive new booking notifications.
 * - All active administrators receive booking-status notifications.
 *
 * Future routing:
 * This service can later resolve recipients by permission, assignment,
 * or department without changing booking notification logic.
 * ================================================================
 */
public interface AdminNotificationRecipientService {

    List<AdminNotificationRecipient> getActiveRecipients();

    record AdminNotificationRecipient(

            UUID adminUserId,

            String displayName,

            String email
    ) {
    }
}