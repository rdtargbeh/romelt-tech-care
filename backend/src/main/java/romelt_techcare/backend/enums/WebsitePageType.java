package romelt_techcare.backend.enums;

/**
 * ================================================================
 * ROMELT TECHCARE — WEBSITE PAGE TYPE
 * ================================================================
 *
 * Purpose:
 * Identifies the functional category of a fixed React website page.
 *
 * The values must remain aligned with the database constraint on
 * website_pages.page_type.
 * ================================================================
 */
public enum WebsitePageType {

    HOME,
    SERVICES,
    PRICING,
    ABOUT,
    CONTACT,
    BOOKING,
    LEGAL,
    STANDARD,
    SYSTEM
}