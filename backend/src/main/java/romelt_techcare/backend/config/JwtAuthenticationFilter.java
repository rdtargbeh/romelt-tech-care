package romelt_techcare.backend.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import romelt_techcare.backend.dto.AdminJwtPrincipal;
import romelt_techcare.backend.service.JwtService;

import java.io.IOException;
import java.util.List;

/**
 * ================================================================
 * ROMELT TECHCARE — JWT AUTHENTICATION FILTER
 * ================================================================
 *
 * Purpose:
 * Authenticates administrator API requests that contain a valid JWT
 * bearer token.
 *
 * Responsibilities:
 * - Reads the Authorization request header.
 * - Extracts the JWT bearer token.
 * - Validates and decodes the JWT through JwtService.
 * - Converts AdminJwtPrincipal into a Spring Security
 *   Authentication object.
 * - Stores the authenticated administrator in SecurityContext.
 * - Allows requests without tokens to continue so Spring Security
 *   can apply endpoint authorization rules.
 *
 * Security rules:
 * - Passwords and password hashes are never read by this filter.
 * - Authentication is created only after token verification.
 * - Invalid tokens do not create an authenticated SecurityContext.
 * - Existing authentication is never overwritten.
 * - Administrator roles are exposed as ROLE_<ADMIN_ROLE>.
 *
 * Real-data integration:
 * SecurityConfiguration installs this filter before Spring
 * Security's username/password authentication filter.
 * Protected controllers can access AdminJwtPrincipal through the
 * authenticated principal.
 * ================================================================
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String ROLE_PREFIX = "ROLE_";

    private final JwtService jwtService;

    /**
     * Processes one HTTP request and establishes JWT authentication
     * when a valid bearer token is present.
     *
     * @param request current HTTP request
     * @param response current HTTP response
     * @param filterChain remaining servlet filter chain
     * @throws ServletException when servlet processing fails
     * @throws IOException when request or response processing fails
     */
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String authorizationHeader =
                request.getHeader(HttpHeaders.AUTHORIZATION);

        String accessToken =
                jwtService.extractBearerToken(authorizationHeader);

        if (accessToken == null
                || SecurityContextHolder.getContext()
                .getAuthentication() != null) {

            filterChain.doFilter(request, response);
            return;
        }

        try {
            AdminJwtPrincipal principal =
                    jwtService.validateAndExtractPrincipal(accessToken);

            SimpleGrantedAuthority roleAuthority =
                    new SimpleGrantedAuthority(
                            ROLE_PREFIX + principal.role().name()
                    );

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            principal,
                            null,
                            List.of(roleAuthority)
                    );

            authentication.setDetails(
                    new WebAuthenticationDetailsSource()
                            .buildDetails(request)
            );

            SecurityContextHolder.getContext()
                    .setAuthentication(authentication);

        } catch (RuntimeException exception) {
            /*
             * Do not expose token-validation details to the client.
             *
             * Authentication remains empty. Spring Security will
             * reject the request later when the endpoint requires an
             * authenticated administrator.
             */
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }
}