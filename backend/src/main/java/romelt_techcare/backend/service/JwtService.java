package romelt_techcare.backend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Service;
import romelt_techcare.backend.config.JwtConfigurationProperties;
import romelt_techcare.backend.dto.AdminJwtPrincipal;
import romelt_techcare.backend.dto.AdminJwtToken;
import romelt_techcare.backend.entity.AdminUser;
import romelt_techcare.backend.enums.AdminRole;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * ================================================================
 * ROMELT TECHCARE — JWT SERVICE
 * ================================================================
 *
 * Purpose:
 * Generates, validates, and decodes administrator JWT access tokens
 * using Spring Security's native JWT support.
 *
 * Responsibilities:
 * - Generates signed administrator access tokens.
 * - Includes administrator identity, email, and role claims.
 * - Validates token signature, issuer, audience, and timestamps.
 * - Converts validated JWT claims into AdminJwtPrincipal.
 * - Provides safe token-validation helpers for authentication filters.
 *
 * Security rules:
 * - Tokens are signed with HMAC SHA-256.
 * - Passwords and password hashes are never included in tokens.
 * - Only active, authenticatable administrators may receive tokens.
 * - Every token receives a unique JWT identifier.
 * - Invalid or expired tokens are rejected.
 *
 * Real-data integration:
 * AdminAuthenticationService uses this service after successful
 * password verification. The JWT authentication filter will use it
 * to validate bearer tokens on protected administrator endpoints.
 * ================================================================
 */
@Service
@RequiredArgsConstructor
public class JwtService {

    /**
     * Custom claim containing the administrator email.
     */
    private static final String EMAIL_CLAIM = "email";

    /**
     * Custom claim containing the administrator role.
     */
    private static final String ROLE_CLAIM = "role";

    /**
     * Custom claim identifying the token type.
     */
    private static final String TOKEN_TYPE_CLAIM = "token_type";

    /**
     * Access-token type value.
     */
    private static final String ACCESS_TOKEN_TYPE = "admin_access";

    /**
     * Required bearer-token prefix.
     */
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtEncoder jwtEncoder;
    private final JwtDecoder jwtDecoder;
    private final JwtConfigurationProperties jwtProperties;

    /**
     * Generates a signed administrator access token.
     *
     * @param adminUser authenticated administrator
     * @return generated JWT result
     */
    public AdminJwtToken generateAccessToken(AdminUser adminUser) {
        validateAdministratorForTokenGeneration(adminUser);

        Instant issuedAt = Instant.now();
        Duration lifetime = jwtProperties.getAccessTokenLifetime();
        Instant expiresAt = issuedAt.plus(lifetime);
        String tokenId = UUID.randomUUID().toString();

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .id(tokenId)
                .issuer(jwtProperties.getIssuer())
                .audience(List.of(jwtProperties.getAudience()))
                .subject(adminUser.getAdminUserId().toString())
                .issuedAt(issuedAt)
                .notBefore(issuedAt)
                .expiresAt(expiresAt)
                .claim(EMAIL_CLAIM, normalizeEmail(adminUser.getEmail()))
                .claim(ROLE_CLAIM, adminUser.getRole().name())
                .claim(TOKEN_TYPE_CLAIM, ACCESS_TOKEN_TYPE)
                .build();

        JwsHeader header = JwsHeader
                .with(MacAlgorithm.HS256)
                .type("JWT")
                .build();

        String encodedToken = jwtEncoder
                .encode(
                        JwtEncoderParameters.from(
                                header,
                                claims
                        )
                )
                .getTokenValue();

        long expiresInSeconds = Duration
                .between(issuedAt, expiresAt)
                .getSeconds();

        return new AdminJwtToken(
                encodedToken,
                issuedAt,
                expiresAt,
                expiresInSeconds
        );
    }

    /**
     * Validates a JWT and returns its administrator principal.
     *
     * @param token raw JWT or Bearer authorization value
     * @return validated administrator principal
     */
    public AdminJwtPrincipal validateAndExtractPrincipal(String token) {
        String normalizedToken = normalizeToken(token);

        try {
            Jwt jwt = jwtDecoder.decode(normalizedToken);

            validateRequiredClaims(jwt);

            UUID adminUserId = parseAdministratorId(jwt.getSubject());
            String email = requireClaim(jwt, EMAIL_CLAIM);
            AdminRole role = parseRole(requireClaim(jwt, ROLE_CLAIM));
            String tokenId = requireText(jwt.getId(), "JWT ID");
            Instant issuedAt = requireInstant(
                    jwt.getIssuedAt(),
                    "JWT issue time"
            );
            Instant expiresAt = requireInstant(
                    jwt.getExpiresAt(),
                    "JWT expiration time"
            );

            return new AdminJwtPrincipal(
                    adminUserId,
                    normalizeEmail(email),
                    role,
                    tokenId,
                    issuedAt,
                    expiresAt
            );

        } catch (JwtException exception) {
            throw new IllegalArgumentException(
                    "The administrator access token is invalid or expired.",
                    exception
            );
        }
    }

    /**
     * Returns true when the supplied token is valid.
     *
     * This method intentionally suppresses token details so callers
     * do not expose authentication internals to API clients.
     *
     * @param token raw JWT or Bearer authorization value
     * @return true when valid
     */
    public boolean isTokenValid(String token) {
        try {
            validateAndExtractPrincipal(token);
            return true;
        } catch (RuntimeException exception) {
            return false;
        }
    }

    /**
     * Extracts a raw JWT from an Authorization header.
     *
     * @param authorizationHeader Authorization header
     * @return raw token
     */
    public String extractBearerToken(String authorizationHeader) {
        if (authorizationHeader == null
                || authorizationHeader.isBlank()) {
            return null;
        }

        String normalizedHeader = authorizationHeader.trim();

        if (!normalizedHeader.regionMatches(
                true,
                0,
                BEARER_PREFIX,
                0,
                BEARER_PREFIX.length()
        )) {
            return null;
        }

        String token = normalizedHeader
                .substring(BEARER_PREFIX.length())
                .trim();

        return token.isBlank() ? null : token;
    }

    /**
     * Validates claims that are specific to Romelt TechCare.
     */
    private void validateRequiredClaims(Jwt jwt) {
        String issuer = requireClaim(jwt, "iss");

        if (!jwtProperties.getIssuer().equals(issuer)) {
            throw new IllegalArgumentException(
                    "JWT issuer is invalid."
            );
        }

        List<String> audience = jwt.getAudience();

        if (audience == null
                || !audience.contains(jwtProperties.getAudience())) {
            throw new IllegalArgumentException(
                    "JWT audience is invalid."
            );
        }

        String tokenType = requireClaim(
                jwt,
                TOKEN_TYPE_CLAIM
        );

        if (!ACCESS_TOKEN_TYPE.equals(tokenType)) {
            throw new IllegalArgumentException(
                    "JWT token type is invalid."
            );
        }

        Instant issuedAt = requireInstant(
                jwt.getIssuedAt(),
                "JWT issue time"
        );

        Instant expiresAt = requireInstant(
                jwt.getExpiresAt(),
                "JWT expiration time"
        );

        Instant now = Instant.now();

        if (issuedAt.isAfter(now.plusSeconds(60))) {
            throw new IllegalArgumentException(
                    "JWT issue time is invalid."
            );
        }

        if (!expiresAt.isAfter(now)) {
            throw new IllegalArgumentException(
                    "JWT access token has expired."
            );
        }

        if (!expiresAt.isAfter(issuedAt)) {
            throw new IllegalArgumentException(
                    "JWT timestamps are invalid."
            );
        }

        requireText(jwt.getSubject(), "JWT subject");
        requireText(jwt.getId(), "JWT ID");
        requireClaim(jwt, EMAIL_CLAIM);
        requireClaim(jwt, ROLE_CLAIM);
    }

    /**
     * Validates the administrator before issuing a token.
     */
    private void validateAdministratorForTokenGeneration(
            AdminUser adminUser
    ) {
        if (adminUser == null) {
            throw new IllegalArgumentException(
                    "Administrator must not be null."
            );
        }

        if (adminUser.getAdminUserId() == null) {
            throw new IllegalArgumentException(
                    "Administrator ID must not be null."
            );
        }

        if (adminUser.getEmail() == null
                || adminUser.getEmail().isBlank()) {
            throw new IllegalArgumentException(
                    "Administrator email must not be blank."
            );
        }

        if (adminUser.getRole() == null) {
            throw new IllegalArgumentException(
                    "Administrator role must not be null."
            );
        }

        if (!adminUser.canAuthenticate()) {
            throw new IllegalStateException(
                    "Administrator account is not permitted to authenticate."
            );
        }

        Duration lifetime = jwtProperties.getAccessTokenLifetime();

        if (lifetime == null
                || lifetime.isZero()
                || lifetime.isNegative()) {
            throw new IllegalStateException(
                    "JWT access-token lifetime must be greater than zero."
            );
        }
    }

    /**
     * Retrieves a required string claim.
     */
    private String requireClaim(
            Jwt jwt,
            String claimName
    ) {
        String value = jwt.getClaimAsString(claimName);

        return requireText(
                value,
                "JWT claim '" + claimName + "'"
        );
    }

    /**
     * Converts a JWT subject into an administrator UUID.
     */
    private UUID parseAdministratorId(String subject) {
        String normalizedSubject = requireText(
                subject,
                "JWT subject"
        );

        try {
            return UUID.fromString(normalizedSubject);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(
                    "JWT administrator ID is invalid.",
                    exception
            );
        }
    }

    /**
     * Converts the JWT role claim into AdminRole.
     */
    private AdminRole parseRole(String roleValue) {
        try {
            return AdminRole.valueOf(
                    roleValue.trim().toUpperCase(Locale.ROOT)
            );
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(
                    "JWT administrator role is invalid.",
                    exception
            );
        }
    }

    /**
     * Removes an optional Bearer prefix and validates the token text.
     */
    private String normalizeToken(String token) {
        String normalizedToken = requireText(
                token,
                "JWT access token"
        );

        if (normalizedToken.regionMatches(
                true,
                0,
                BEARER_PREFIX,
                0,
                BEARER_PREFIX.length()
        )) {
            normalizedToken = normalizedToken
                    .substring(BEARER_PREFIX.length())
                    .trim();
        }

        return requireText(
                normalizedToken,
                "JWT access token"
        );
    }

    /**
     * Normalizes administrator emails.
     */
    private String normalizeEmail(String email) {
        return requireText(
                email,
                "Administrator email"
        ).toLowerCase(Locale.ROOT);
    }

    /**
     * Validates required text.
     */
    private String requireText(
            String value,
            String fieldName
    ) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    fieldName + " must not be blank."
            );
        }

        return value.trim();
    }

    /**
     * Validates a required timestamp.
     */
    private Instant requireInstant(
            Instant value,
            String fieldName
    ) {
        if (value == null) {
            throw new IllegalArgumentException(
                    fieldName + " must not be null."
            );
        }

        return value;
    }
}