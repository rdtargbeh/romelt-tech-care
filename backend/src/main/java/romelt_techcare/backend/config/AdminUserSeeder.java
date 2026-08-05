package romelt_techcare.backend.config;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import romelt_techcare.backend.entity.AdminUser;
import romelt_techcare.backend.enums.AdminRole;
import romelt_techcare.backend.enums.AdminStatus;
import romelt_techcare.backend.repository.AdminUserRepository;
import romelt_techcare.backend.service.AdminPasswordPolicyService;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * ================================================================
 * ROMELT TECHCARE — INITIAL ADMINISTRATOR SEEDER
 * ================================================================
 *
 * Purpose:
 * Creates the first Romelt TechCare SUPER_ADMIN account when the
 * configured administrator email does not already exist.
 *
 * Responsibilities:
 * - Reads initial administrator details from environment variables.
 * - Accepts an administrator-selected memorable password.
 * - Uses the centralized password policy.
 * - Encodes the password using BCrypt.
 * - Stores only the encoded password hash.
 * - Creates an ACTIVE SUPER_ADMIN account.
 * - Prevents duplicate administrator creation.
 * - Never logs or stores the plain-text password.
 *
 * Required environment variables:
 * - ROMELT_ADMIN_EMAIL
 * - ROMELT_ADMIN_PASSWORD
 *
 * Optional environment variables:
 * - ROMELT_ADMIN_FIRST_NAME
 * - ROMELT_ADMIN_LAST_NAME
 * - ROMELT_ADMIN_JOB_TITLE
 *
 * Security rules:
 * - ROMELT_ADMIN_PASSWORD is read only during startup.
 * - The plain password is never written to logs or the database.
 * - The database stores only the BCrypt password hash.
 * - Existing administrator accounts are never overwritten.
 *
 * Real-data integration:
 * After the initial account has been created, authentication and
 * administrator-management services use the same account record.
 * ================================================================
 */
@Component
@RequiredArgsConstructor
public class AdminUserSeeder implements ApplicationRunner {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(AdminUserSeeder.class);

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}$",
            Pattern.CASE_INSENSITIVE
    );

    private final AdminUserRepository adminUserRepository;
    private final AdminPasswordPolicyService passwordPolicyService;

    /**
     * Initial administrator email.
     */
    @Value("${ROMELT_ADMIN_EMAIL:}")
    private String configuredEmail;

    /**
     * Initial administrator password.
     *
     * This value is never persisted directly.
     */
    @Value("${ROMELT_ADMIN_PASSWORD:}")
    private String configuredPassword;

    /**
     * Initial administrator first name.
     */
    @Value("${ROMELT_ADMIN_FIRST_NAME:Ronald}")
    private String configuredFirstName;

    /**
     * Initial administrator last name.
     */
    @Value("${ROMELT_ADMIN_LAST_NAME:Targbeh}")
    private String configuredLastName;

    /**
     * Initial administrator job title.
     */
    @Value("${ROMELT_ADMIN_JOB_TITLE:Owner and Platform Administrator}")
    private String configuredJobTitle;

    @Value("${romelt.admin.seed.enabled:false}")
    private boolean seedEnabled;

    /**
     * Runs after Spring initializes the application context.
     *
     * @param args application startup arguments
     */
    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        seedInitialAdministrator();
    }

    /**
     * Creates the initial SUPER_ADMIN when one does not exist.
     */
    private void seedInitialAdministrator() {

        if (!seedEnabled) {
            LOGGER.info("Administrator seeding is disabled.");
            return;
        }

        String email = normalizeEmail(configuredEmail);
        String firstName = defaultIfBlank(
                configuredFirstName,
                "Romelt"
        );
        String lastName = defaultIfBlank(
                configuredLastName,
                "Administrator"
        );
        String jobTitle = defaultIfBlank(
                configuredJobTitle,
                "Platform Administrator"
        );

        /*
         * Passwords must not be trimmed because internal spaces may be
         * intentional. The password policy rejects leading or trailing
         * whitespace separately.
         */
        String password = normalizePassword(configuredPassword);

        if (email == null && password == null) {
            LOGGER.warn(
                    "Initial administrator was not created because "
                            + "ROMELT_ADMIN_EMAIL and "
                            + "ROMELT_ADMIN_PASSWORD are not configured."
            );
            return;
        }

        if (email == null) {
            throw new IllegalStateException(
                    "ROMELT_ADMIN_EMAIL must be configured before the "
                            + "initial administrator can be created."
            );
        }

        if (!EMAIL_PATTERN.matcher(email).matches()) {
            throw new IllegalStateException(
                    "ROMELT_ADMIN_EMAIL must contain a valid email address."
            );
        }

        /*
         * Existing accounts are never modified by the seeder.
         *
         * This check is performed before password validation so normal
         * application restarts do not require the original password
         * environment variable.
         */
        if (adminUserRepository.existsByEmailIgnoreCase(email)) {
            LOGGER.info(
                    "Initial administrator already exists for email {}. "
                            + "The existing profile and password were "
                            + "not changed.",
                    maskEmail(email)
            );
            return;
        }

        if (password == null) {
            throw new IllegalStateException(
                    "ROMELT_ADMIN_PASSWORD must be configured before the "
                            + "initial administrator can be created."
            );
        }

        final String encodedPassword;

        try {
            encodedPassword =
                    passwordPolicyService.encodeNewPassword(
                            password,
                            email,
                            firstName,
                            lastName
                    );
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException(
                    "ROMELT_ADMIN_PASSWORD is invalid: "
                            + exception.getMessage(),
                    exception
            );
        }

        AdminUser administrator = AdminUser.builder()
                .email(email)
                .firstName(firstName)
                .lastName(lastName)
                .jobTitle(jobTitle)
                .role(AdminRole.SUPER_ADMIN)
                .status(AdminStatus.ACTIVE)
                .mustChangePassword(false)
                .failedLoginAttempts(0)
                .lockedUntil(null)
                .lastLoginAt(null)
                .build();

        /*
         * Only the encoded BCrypt value is passed to the entity.
         */
        administrator.changePassword(
                encodedPassword,
                false
        );

        AdminUser savedAdministrator =
                adminUserRepository.save(administrator);

        LOGGER.info(
                "Initial SUPER_ADMIN account created successfully. "
                        + "Administrator ID: {}, email: {}. "
                        + "Only the encoded password hash was stored.",
                savedAdministrator.getAdminUserId(),
                maskEmail(savedAdministrator.getEmail())
        );
    }

    /**
     * Normalizes the configured email address.
     */
    private String normalizeEmail(String value) {
        String normalizedValue = trimToNull(value);

        return normalizedValue == null
                ? null
                : normalizedValue.toLowerCase(Locale.ROOT);
    }

    /**
     * Converts an absent or empty password to null without trimming.
     */
    private String normalizePassword(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }

        return value;
    }

    /**
     * Returns a default when an optional value is blank.
     */
    private String defaultIfBlank(
            String value,
            String defaultValue
    ) {
        String normalizedValue = trimToNull(value);

        return normalizedValue == null
                ? defaultValue
                : normalizedValue;
    }

    /**
     * Trims a value and converts blank strings to null.
     */
    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }

        String normalizedValue = value.trim();

        return normalizedValue.isEmpty()
                ? null
                : normalizedValue;
    }

    /**
     * Masks an email before writing it to application logs.
     */
    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return "configured administrator";
        }

        int separatorIndex = email.indexOf('@');
        String localPart = email.substring(0, separatorIndex);
        String domainPart = email.substring(separatorIndex);

        if (localPart.length() <= 1) {
            return "*" + domainPart;
        }

        return localPart.charAt(0)
                + "*".repeat(localPart.length() - 1)
                + domainPart;
    }
}