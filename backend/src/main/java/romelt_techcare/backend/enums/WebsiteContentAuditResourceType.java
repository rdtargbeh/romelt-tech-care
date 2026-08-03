package romelt_techcare.backend.enums;

/**
 * ================================================================
 * ROMELT TECHCARE — WEBSITE CONTENT AUDIT RESOURCE TYPE
 * ================================================================
 *
 * Purpose:
 * Identifies the application resource affected by an audited action.
 *
 * Database alignment:
 * Values must remain synchronized with
 * ck_website_content_audit_resource.
 * ================================================================
 */
public enum WebsiteContentAuditResourceType {

    BUSINESS_PROFILE,

    BUSINESS_HOUR,

    BUSINESS_HOUR_EXCEPTION,

    WEBSITE_SETTING,

    WEBSITE_PAGE,

    WEBSITE_PAGE_VERSION,

    NAVIGATION_ITEM,

    MEDIA_ASSET,

    SERVICE,

    SERVICE_VERSION,

    PRICING_PLAN,

    PRICING_PLAN_VERSION,

    FAQ,

    FAQ_VERSION,

    CUSTOMER_REVIEW,

    REVIEW_INVITATION,

    SOCIAL_LINK,

    BOOKING_REQUEST,

    CONTACT_INQUIRY,

    ADMIN_USER,

    OTHER
}