package romelt_techcare.backend.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import romelt_techcare.backend.api.ApiErrorResponse;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * ================================================================
 * ROMELT TECHCARE — REST ACCESS DENIED HANDLER
 * ================================================================
 *
 * Purpose:
 * Returns the standard API error structure when an authenticated
 * administrator lacks permission for an operation.
 *
 * Responsibilities:
 * - Returns HTTP 403.
 * - Uses application/json.
 * - Prevents default HTML error responses.
 * - Does not disclose internal authorization rules.
 * ================================================================
 */
@Component
@RequiredArgsConstructor
public class RestAccessDeniedHandler
        implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException exception
    ) throws IOException {
        ApiErrorResponse body =
                new ApiErrorResponse(
                        false,
                        HttpStatus.FORBIDDEN.value(),
                        HttpStatus.FORBIDDEN
                                .getReasonPhrase(),
                        "You do not have permission to perform this operation.",
                        request.getRequestURI(),
                        Instant.now(),
                        List.of(),
                        Map.of()
                );

        response.setStatus(
                HttpStatus.FORBIDDEN.value()
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