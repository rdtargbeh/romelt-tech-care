package romelt_techcare.backend.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import romelt_techcare.backend.api.ApiErrorResponse;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * ================================================================
 * ROMELT TECHCARE — REST AUTHENTICATION ENTRY POINT
 * ================================================================
 *
 * Purpose:
 * Returns the standard API error structure when Spring Security
 * rejects an unauthenticated request before it reaches a controller.
 *
 * Responsibilities:
 * - Returns HTTP 401.
 * - Uses application/json.
 * - Prevents redirects and HTML login responses.
 * - Does not expose JWT validation details.
 * ================================================================
 */
@Component
@RequiredArgsConstructor
public class RestAuthenticationEntryPoint
        implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException exception
    ) throws IOException {
        ApiErrorResponse body =
                new ApiErrorResponse(
                        false,
                        HttpStatus.UNAUTHORIZED.value(),
                        HttpStatus.UNAUTHORIZED
                                .getReasonPhrase(),
                        "Authentication is required to access this resource.",
                        request.getRequestURI(),
                        Instant.now(),
                        List.of(),
                        Map.of()
                );

        response.setStatus(
                HttpStatus.UNAUTHORIZED.value()
        );

        response.setCharacterEncoding("UTF-8");

        response.setContentType(
                MediaType.APPLICATION_JSON_VALUE
        );

        objectMapper.writeValue(
                response.getOutputStream(),
                body
        );
    }
}