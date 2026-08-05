package romelt_techcare.backend.dto;

/**
 * ================================================================
 * ROMELT TECHCARE — WEBSITE CONTENT AUDIT CONTEXT
 * ================================================================
 *
 * Purpose:
 * Carries request-origin information used when recording a CMS audit
 * event.
 *
 * This avoids passing HttpServletRequest into the service layer.
 * ================================================================
 */
public record WebsiteContentAuditContext(

        String ipAddress,

        String userAgent
) {
}