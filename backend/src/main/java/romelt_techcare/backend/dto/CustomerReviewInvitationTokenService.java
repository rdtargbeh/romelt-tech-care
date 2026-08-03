package romelt_techcare.backend.service;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * ================================================================
 * ROMELT TECHCARE — REVIEW INVITATION TOKEN SERVICE
 * ================================================================
 *
 * Purpose:
 * Generates cryptographically secure review tokens and calculates
 * deterministic SHA-256 hashes for database lookup.
 *
 * Security:
 * - Uses 32 random bytes, providing 256 bits of entropy.
 * - Uses URL-safe Base64 encoding without padding.
 * - Plain tokens must never be logged or persisted.
 * ================================================================
 */
@Component
public class CustomerReviewInvitationTokenService {

    private static final int TOKEN_BYTE_LENGTH = 32;

    private final SecureRandom secureRandom =
            new SecureRandom();

    public String generateToken() {
        byte[] tokenBytes =
                new byte[TOKEN_BYTE_LENGTH];

        secureRandom.nextBytes(tokenBytes);

        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(tokenBytes);
    }

    public String hashToken(
            String plainToken
    ) {
        if (
                plainToken == null
                        || plainToken.isBlank()
        ) {
            throw new IllegalArgumentException(
                    "Review invitation token is required."
            );
        }

        try {
            MessageDigest digest =
                    MessageDigest.getInstance("SHA-256");

            byte[] hash =
                    digest.digest(
                            plainToken.getBytes(
                                    StandardCharsets.UTF_8
                            )
                    );

            return Base64.getUrlEncoder()
                    .withoutPadding()
                    .encodeToString(hash);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(
                    "SHA-256 is not available.",
                    exception
            );
        }
    }
}