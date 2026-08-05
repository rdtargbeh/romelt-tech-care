package romelt_techcare.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * ================================================================
 * ROMELT TECHCARE — PASSWORD SECURITY CONFIGURATION
 * ================================================================
 *
 * Purpose:
 * Provides the application-wide password encoder used for
 * administrator account passwords.
 *
 * Responsibilities:
 * - Encodes memorable administrator passwords into BCrypt hashes.
 * - Verifies login passwords against stored BCrypt hashes.
 * - Ensures plain-text passwords are never persisted.
 *
 * Password workflow:
 * 1. Administrator enters a memorable password.
 * 2. The service temporarily receives the plain-text password.
 * 3. PasswordEncoder.encode(...) creates a BCrypt hash.
 * 4. Only the resulting hash is saved in password_hash.
 * 5. Login uses PasswordEncoder.matches(...).
 *
 * Real-data integration:
 * This encoder is used by the initial administrator seeder,
 * authentication service, administrator-creation service, password
 * change service, and account-recovery service.
 * ================================================================
 */
@Configuration
public class PasswordConfiguration {

    /**
     * BCrypt work factor.
     *
     * A strength of 12 provides a strong production baseline while
     * remaining practical for normal authentication operations.
     */
    private static final int BCRYPT_STRENGTH = 12;

    /**
     * Creates the application-wide password encoder.
     *
     * BCrypt automatically generates a random salt for each encoded
     * password. Two administrators using the same password will still
     * have different stored hashes.
     *
     * @return BCrypt password encoder
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(BCRYPT_STRENGTH);
    }
}