package romelt_techcare.backend.enums;

/**
 * ================================================================
 * ROMELT TECHCARE — NAVIGATION DESTINATION TYPE
 * ================================================================
 *
 * Purpose:
 * Identifies how a website navigation item resolves its destination.
 *
 * Types:
 * - INTERNAL_ROUTE: React route or associated WebsitePage.
 * - EXTERNAL_URL: Fully qualified external HTTP or HTTPS URL.
 * - ANCHOR: Same-page or route-based fragment link.
 * - ACTION: Frontend-recognized action key or command.
 * ================================================================
 */
public enum WebsiteNavigationDestinationType {

    INTERNAL_ROUTE,

    EXTERNAL_URL,

    ANCHOR,

    ACTION
}