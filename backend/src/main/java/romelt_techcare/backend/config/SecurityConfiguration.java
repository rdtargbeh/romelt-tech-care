package romelt_techcare.backend.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.savedrequest.NullRequestCache;

import static org.springframework.security.config.Customizer.withDefaults;

/**
 * ================================================================
 * ROMELT TECHCARE — SECURITY CONFIGURATION
 * ================================================================
 *
 * Purpose:
 * Configures Spring Security for the Romelt TechCare REST API.
 *
 * Responsibilities:
 * - Allows public health, contact, booking, and admin login access.
 * - Protects administrator endpoints with JWT authentication.
 * - Enables the configured CORS policy.
 * - Uses JSON responses for authentication and authorization errors.
 * - Prevents Spring Security from creating HTTP sessions.
 * - Disables form login and HTTP Basic authentication.
 * - Disables CSRF for stateless bearer-token authentication.
 * - Registers the JWT authentication filter.
 * - Enables method-level authorization.
 *
 * Security behavior:
 * - /api/v1/public/** is public.
 * - /api/v1/admin/auth/login is public.
 * - /api/v1/admin/** requires a valid JWT.
 * - Unrecognized routes are denied by default.
 * ================================================================
 */
@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfiguration {

    private final JwtAuthenticationFilter
            jwtAuthenticationFilter;

    private final RestAuthenticationEntryPoint
            restAuthenticationEntryPoint;

    private final RestAccessDeniedHandler
            restAccessDeniedHandler;

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http
    ) throws Exception {
        http
                /*
                 * Apply the CorsConfigurationSource bean defined by
                 * CorsConfiguration.
                 */
                .cors(withDefaults())

                /*
                 * The REST API uses stateless JWT authentication.
                 */
                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                )

                /*
                 * Prevent browser-style saved requests and sessions.
                 */
                .requestCache(requestCache ->
                        requestCache.requestCache(
                                new NullRequestCache()
                        )
                )

                /*
                 * Authentication uses the Authorization header rather
                 * than browser cookies.
                 */
                .csrf(csrf -> csrf.disable())

                /*
                 * Disable browser-oriented authentication mechanisms.
                 */
                .formLogin(formLogin ->
                        formLogin.disable()
                )
                .httpBasic(httpBasic ->
                        httpBasic.disable()
                )
                .logout(logout ->
                        logout.disable()
                )

                /*
                 * Ensure security failures use the standard JSON API
                 * response instead of HTML or redirects.
                 */
                .exceptionHandling(exceptionHandling ->
                        exceptionHandling
                                .authenticationEntryPoint(
                                        restAuthenticationEntryPoint
                                )
                                .accessDeniedHandler(
                                        restAccessDeniedHandler
                                )
                )

                /*
                 * Define public and protected endpoint access.
                 */
                .authorizeHttpRequests(authorize ->
                        authorize
                                .requestMatchers(
                                        HttpMethod.OPTIONS,
                                        "/**"
                                )
                                .permitAll()

                                .requestMatchers(
                                        "/api/v1/public/**",
                                        "/api/v1/admin/auth/login",
                                        "/error"
                                )
                                .permitAll()

                                .requestMatchers(
                                        "/api/v1/admin/**"
                                )
                                .authenticated()

                                .anyRequest()
                                .denyAll()
                )

                /*
                 * Authenticate bearer tokens before Spring's standard
                 * username/password filter.
                 */
                .addFilterBefore(
                        jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }
}