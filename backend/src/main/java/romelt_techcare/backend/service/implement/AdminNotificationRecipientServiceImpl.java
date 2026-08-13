package romelt_techcare.backend.service.implement;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import romelt_techcare.backend.entity.AdminUser;
import romelt_techcare.backend.repository.AdminUserRepository;
import romelt_techcare.backend.service.AdminNotificationRecipientService;

import java.util.List;

/**
 * ================================================================
 * ROMELT TECHCARE — ADMIN NOTIFICATION RECIPIENT IMPLEMENTATION
 * ================================================================
 *
 * Purpose:
 * Resolves active administrator accounts that should receive
 * operational in-app notifications.
 *
 * Current behavior:
 * - Loads administrator records.
 * - Excludes inactive administrators.
 * - Excludes administrators without persistent IDs.
 * - Returns a safe display-name snapshot.
 * ================================================================
 */
@Service
@RequiredArgsConstructor
public class AdminNotificationRecipientServiceImpl
        implements AdminNotificationRecipientService {

    private final AdminUserRepository adminUserRepository;

    @Override
    @Transactional(readOnly = true)
    public List<AdminNotificationRecipient> getActiveRecipients() {
        return adminUserRepository
                .findAll()
                .stream()
                .filter(AdminUser::isActive)
                .filter(
                        adminUser ->
                                adminUser.getAdminUserId() != null
                )
                .map(
                        adminUser ->
                                new AdminNotificationRecipient(
                                        adminUser.getAdminUserId(),
                                        createDisplayName(adminUser),
                                        normalizeOptional(
                                                adminUser.getEmail()
                                        )
                                )
                )
                .toList();
    }

    private String createDisplayName(
            AdminUser adminUser
    ) {
        String firstName =
                normalizeOptional(
                        adminUser.getFirstName()
                );

        String lastName =
                normalizeOptional(
                        adminUser.getLastName()
                );

        String displayName =
                String.join(
                                " ",
                                firstName == null
                                        ? ""
                                        : firstName,
                                lastName == null
                                        ? ""
                                        : lastName
                        )
                        .trim();

        if (!displayName.isEmpty()) {
            return displayName;
        }

        String email =
                normalizeOptional(
                        adminUser.getEmail()
                );

        return email == null
                ? "Administrator"
                : email;
    }

    private String normalizeOptional(
            String value
    ) {
        if (value == null) {
            return null;
        }

        String normalized = value.trim();

        return normalized.isEmpty()
                ? null
                : normalized;
    }
}