package romelt_techcare.backend.config;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.nimbusds.jose.proc.SecurityContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

/**
 * ================================================================
 * ROMELT TECHCARE — JWT SECURITY CONFIGURATION
 * ================================================================
 *
 * Purpose:
 * Configures Spring Security's native JWT encoder and decoder
 * using a shared HMAC signing key.
 *
 * Responsibilities:
 * - Creates the JwtEncoder bean.
 * - Creates the JwtDecoder bean.
 * - Uses the application JWT secret.
 * - Supports stateless administrator authentication.
 *
 * Security Rules:
 * - Secret must be at least 32 UTF-8 bytes.
 * - Secret must never be logged.
 * * Real-data integration:
 * JwtService uses these beans to issue and validate access tokens.
 * ================================================================
 */
@Configuration
public class JwtSecurityConfiguration {

    /**
     * Creates the HMAC signing key.
     */
    @Bean
    public SecretKey jwtSecretKey(
            JwtConfigurationProperties properties
    ) {

        byte[] secret =
                properties.getSecret()
                        .getBytes(StandardCharsets.UTF_8);

        if (secret.length < 32) {
            throw new IllegalStateException(
                    "JWT secret must contain at least 32 UTF-8 bytes."
            );
        }

        return new SecretKeySpec(
                secret,
                "HmacSHA256"
        );
    }

    /**
     * Spring Security JWT encoder.
     */
    @Bean
    public JwtEncoder jwtEncoder(
            SecretKey secretKey
    ) {

        return new NimbusJwtEncoder(
                new ImmutableSecret<SecurityContext>(secretKey)
        );
    }

    /**
     * Spring Security JWT decoder.
     */
    @Bean
    public JwtDecoder jwtDecoder(
            SecretKey secretKey
    ) {

        return NimbusJwtDecoder
                .withSecretKey(secretKey)
                .build();
    }

}