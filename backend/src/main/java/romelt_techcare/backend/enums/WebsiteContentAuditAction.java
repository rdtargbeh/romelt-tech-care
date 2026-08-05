package romelt_techcare.backend.enums;

/**
 * ================================================================
 * ROMELT TECHCARE — WEBSITE CONTENT AUDIT ACTION
 * ================================================================
 *
 * Purpose:
 * Defines actions recorded by the website content-management and
 * public-content audit system.
 *
 * Database alignment:
 * Values must remain synchronized with
 * ck_website_content_audit_action.
 * ================================================================
 */
public enum WebsiteContentAuditAction {

    CREATE,

    UPDATE,

    DELETE,

    ARCHIVE,

    RESTORE,

    SAVE_DRAFT,

    PUBLISH,

    UNPUBLISH,

    APPROVE,

    REJECT,

    HIDE,

    FEATURE,

    UNFEATURE,

    UPLOAD,

    LOGIN_PREVIEW,

    MARK_SENT,

    CONSUME,

    REVOKE,

    EXPIRE,

    MARK_SPAM,

    RESPOND,

    REMOVE_RESPONSE
}