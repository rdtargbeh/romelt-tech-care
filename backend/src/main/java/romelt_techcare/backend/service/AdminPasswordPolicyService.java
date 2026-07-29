package romelt_techcare.backend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

/**
 * ================================================================
 * ROMELT TECHCARE — ADMINISTRATOR PASSWORD POLICY SERVICE
 * ================================================================
 *
 * Purpose:
 * Centralizes administrator password validation, encoding, and
 * verification.
 *
 * Responsibilities:
 * - Validates new administrator passwords.
 * - Enforces BCrypt-compatible password length limits.
 * - Rejects weak or identity-based passwords.
 * - Encodes valid passwords using the configured PasswordEncoder.
 * - Verifies login passwords against stored password hashes.
 *
 * Password workflow:
 * 1. An administrator enters a memorable password.
 * 2. The plain password is temporarily received by the backend.
 * 3. This service validates the password.
 * 4. PasswordEncoder creates a salted BCrypt hash.
 * 5. Only the encoded hash is stored in password_hash.
 *
 * Security rules:
 * - Plain-text passwords must never be logged.
 * - Plain-text passwords must never be persisted.
 * - Password hashes must never be returned to clients.
 * - Login verification uses PasswordEncoder.matches(...).
 * - Existing passwords are not required to satisfy the newest policy
 *   before they can be checked during login.
 *
 * Real-data integration:
 * Used by:
 * - AdminUserSeeder
 * - AdminAuthenticationService
 * - Administrator creation
 * - Password change
 * - Password reset and account recovery
 * ================================================================
 */
@Service
@RequiredArgsConstructor
public class AdminPasswordPolicyService {

    /**
     * Minimum number of characters required.
     */
    public static final int MINIMUM_PASSWORD_LENGTH = 8;

    /**
     * BCrypt processes at most 72 password bytes.
     *
     * The byte limit is checked using UTF-8 rather than Java character
     * count because non-ASCII characters may use multiple bytes.
     */
    public static final int MAXIMUM_BCRYPT_BYTES = 72;

    private final PasswordEncoder passwordEncoder;

    /**
     * Validates and encodes a new administrator password.
     *
     * @param rawPassword plain-text password supplied by administrator
     * @param email administrator email, when available
     * @param firstName administrator first name, when available
     * @param lastName administrator last name, when available
     * @return encoded BCrypt password hash
     */
    public String encodeNewPassword(
            String rawPassword,
            String email,
            String firstName,
            String lastName
    ) {
        validateNewPassword(
                rawPassword,
                email,
                firstName,
                lastName
        );

        return passwordEncoder.encode(rawPassword);
    }

    /**
     * Validates a new administrator password.
     *
     * @param rawPassword password being selected
     * @param email administrator email, when available
     * @param firstName administrator first name, when available
     * @param lastName administrator last name, when available
     */
    public void validateNewPassword(
            String rawPassword,
            String email,
            String firstName,
            String lastName
    ) {
        if (rawPassword == null || rawPassword.isEmpty()) {
            throw new IllegalArgumentException(
                    "Password is required."
            );
        }

        if (rawPassword.length() < MINIMUM_PASSWORD_LENGTH) {
            throw new IllegalArgumentException(
                    "Password must contain at least "
                            + MINIMUM_PASSWORD_LENGTH
                            + " characters."
            );
        }

        int passwordByteLength =
                rawPassword.getBytes(StandardCharsets.UTF_8).length;

        if (passwordByteLength > MAXIMUM_BCRYPT_BYTES) {
            throw new IllegalArgumentException(
                    "Password must not exceed "
                            + MAXIMUM_BCRYPT_BYTES
                            + " UTF-8 bytes."
            );
        }

        if (containsLeadingOrTrailingWhitespace(rawPassword)) {
            throw new IllegalArgumentException(
                    "Password must not begin or end with whitespace."
            );
        }

        boolean containsUppercase =
                rawPassword.codePoints()
                        .anyMatch(Character::isUpperCase);

        boolean containsLowercase =
                rawPassword.codePoints()
                        .anyMatch(Character::isLowerCase);

        boolean containsDigit =
                rawPassword.codePoints()
                        .anyMatch(Character::isDigit);

        boolean containsSpecialCharacter =
                rawPassword.codePoints()
                        .anyMatch(character ->
                                !Character.isLetterOrDigit(character)
                                        && !Character.isWhitespace(character)
                        );

        if (!containsUppercase
                || !containsLowercase
                || !containsDigit
                || !containsSpecialCharacter) {

            throw new IllegalArgumentException(
                    "Password must contain at least one uppercase "
                            + "letter, one lowercase letter, one number, "
                            + "and one special character."
            );
        }

        rejectCommonPasswords(rawPassword);

        rejectIdentityBasedPassword(
                rawPassword,
                email,
                firstName,
                lastName
        );
    }

    /**
     * Verifies a submitted password against a stored hash.
     *
     * This method intentionally does not apply the current password
     * policy. An older valid password may still be verified so the
     * administrator can log in and change it.
     *
     * @param rawPassword submitted plain-text password
     * @param encodedPassword stored password hash
     * @return true when the password matches
     */
    public boolean matches(
            String rawPassword,
            String encodedPassword
    ) {
        if (rawPassword == null
                || rawPassword.isEmpty()
                || encodedPassword == null
                || encodedPassword.isBlank()) {

            return false;
        }

        return passwordEncoder.matches(
                rawPassword,
                encodedPassword
        );
    }

    /**
     * Determines whether a stored password should be re-encoded.
     *
     * This supports future increases to the BCrypt strength value.
     *
     * @param encodedPassword existing stored password hash
     * @return true when the configured encoder recommends upgrading it
     */
    public boolean needsUpgrade(String encodedPassword) {
        if (encodedPassword == null || encodedPassword.isBlank()) {
            return false;
        }

        return passwordEncoder.upgradeEncoding(encodedPassword);
    }

    /**
     * Upgrades a valid password hash when the encoder configuration
     * has changed.
     *
     * The caller must first verify that the password is correct.
     *
     * @param rawPassword valid plain-text password
     * @return newly encoded password hash
     */
    public String reencodeVerifiedPassword(String rawPassword) {
        if (rawPassword == null || rawPassword.isEmpty()) {
            throw new IllegalArgumentException(
                    "Password is required for hash upgrade."
            );
        }

        return passwordEncoder.encode(rawPassword);
    }

    /**
     * Rejects a small set of passwords that remain weak even when
     * modified to satisfy basic complexity requirements.
     */
    private void rejectCommonPasswords(String rawPassword) {
        String normalizedPassword =
                normalizeForComparison(rawPassword);

        String[] prohibitedValues = {
                "password",
                "password1",
                "password123",
                "admin",
                "administrator",
                "admin123",
                "welcome",
                "welcome1",
                "qwerty",
                "qwerty123",
                "letmein",
                "changeme",
                "romelttechcare"
        };

        for (String prohibitedValue : prohibitedValues) {
            if (normalizedPassword.equals(prohibitedValue)
                    || normalizedPassword.contains(prohibitedValue)) {

                throw new IllegalArgumentException(
                        "Password is too common or predictable. "
                                + "Choose a different password."
                );
            }
        }
    }

    /**
     * Rejects passwords containing easily guessed administrator
     * identity information.
     */
    private void rejectIdentityBasedPassword(
            String rawPassword,
            String email,
            String firstName,
            String lastName
    ) {
        String normalizedPassword =
                normalizeForComparison(rawPassword);

        rejectContainedIdentityValue(
                normalizedPassword,
                extractEmailLocalPart(email),
                "email address"
        );

        rejectContainedIdentityValue(
                normalizedPassword,
                firstName,
                "first name"
        );

        rejectContainedIdentityValue(
                normalizedPassword,
                lastName,
                "last name"
        );
    }

    /**
     * Rejects an identity value when it is sufficiently long to be a
     * meaningful password component.
     */
    private void rejectContainedIdentityValue(
            String normalizedPassword,
            String identityValue,
            String identityLabel
    ) {
        String normalizedIdentityValue =
                normalizeForComparison(identityValue);

        if (normalizedIdentityValue.length() < 4) {
            return;
        }

        if (normalizedPassword.contains(normalizedIdentityValue)) {
            throw new IllegalArgumentException(
                    "Password must not contain the administrator's "
                            + identityLabel + "."
            );
        }
    }

    /**
     * Extracts the portion of an email before the @ symbol.
     */
    private String extractEmailLocalPart(String email) {
        if (email == null || email.isBlank()) {
            return "";
        }

        int separatorIndex = email.indexOf('@');

        if (separatorIndex <= 0) {
            return email;
        }

        return email.substring(0, separatorIndex);
    }

    /**
     * Checks for accidental spaces at the beginning or end.
     */
    private boolean containsLeadingOrTrailingWhitespace(
            String password
    ) {
        if (password.isEmpty()) {
            return false;
        }

        return Character.isWhitespace(password.charAt(0))
                || Character.isWhitespace(
                password.charAt(password.length() - 1)
        );
    }

    /**
     * Produces a lowercase alphanumeric representation for secure
     * comparison checks.
     */
    private String normalizeForComparison(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }

        return value
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]", "");
    }
}