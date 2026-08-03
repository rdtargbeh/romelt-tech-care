package romelt_techcare.backend.util;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import romelt_techcare.backend.dto.WebsiteContentAuditContext;

/**
 * ================================================================
 * ROMELT TECHCARE — WEBSITE CONTENT AUDIT CONTEXT RESOLVER
 * ================================================================
 *
 * Purpose:
 * Extracts normalized request-origin metadata for CMS audit records.
 *
 * Proxy note:
 * X-Forwarded-For should be trusted only when the application is
 * deployed behind a controlled reverse proxy that overwrites the
 * client-supplied header.
 * ================================================================
 */
@Component
public class WebsiteContentAuditContextResolver {

    public WebsiteContentAuditContext resolve(
            HttpServletRequest request
    ) {
        if (request == null) {
            return new WebsiteContentAuditContext(
                    null,
                    null
            );
        }

        return new WebsiteContentAuditContext(
                resolveClientIp(request),
                truncate(
                        request.getHeader("User-Agent"),
                        500
                )
        );
    }

    private String resolveClientIp(
            HttpServletRequest request
    ) {
        String forwardedFor =
                normalizeOptional(
                        request.getHeader("X-Forwarded-For")
                );

        if (forwardedFor != null) {
            String firstAddress =
                    forwardedFor
                            .split(",")[0]
                            .trim();

            return truncate(firstAddress, 64);
        }

        return truncate(
                request.getRemoteAddr(),
                64
        );
    }

    private String truncate(
            String value,
            int maximumLength
    ) {
        String normalized =
                normalizeOptional(value);

        if (
                normalized == null
                        || normalized.length() <= maximumLength
        ) {
            return normalized;
        }

        return normalized.substring(
                0,
                maximumLength
        );
    }

    private String normalizeOptional(
            String value
    ) {
        if (value == null) {
            return null;
        }

        String normalized = value.trim();

        return normalized.isEmpty()
                ? null
                : normalized;
    }
}