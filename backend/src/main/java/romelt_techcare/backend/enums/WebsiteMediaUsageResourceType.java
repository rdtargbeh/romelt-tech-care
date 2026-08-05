package romelt_techcare.backend.enums;

/**
 * ================================================================
 * ROMELT TECHCARE — WEBSITE MEDIA USAGE RESOURCE TYPE
 * ================================================================
 *
 * Purpose:
 * Identifies the type of website or CMS resource that currently uses
 * a WebsiteMediaAsset.
 *
 * Responsibilities:
 * - Supports media-reference tracking.
 * - Prevents deletion of media that is still in use.
 * - Helps administrators understand where an asset appears.
 * - Supports future media replacement and cleanup workflows.
 *
 * Important:
 * A usage record identifies a reference to media metadata. It does
 * not store or duplicate the binary file.
 * ================================================================
 */
public enum WebsiteMediaUsageResourceType {

    /**
     * Business identity, logo, favicon, or shared branding profile.
     */
    BUSINESS_PROFILE,

    /**
     * A version of a fixed public website page.
     */
    WEBSITE_PAGE_VERSION,

    /**
     * A version of a publicly managed service.
     */
    SERVICE_VERSION,

    /**
     * A version of a publicly managed pricing plan.
     */
    PRICING_PLAN_VERSION,

    /**
     * Customer review or testimonial photograph.
     */
    CUSTOMER_REVIEW,

    /**
     * Social-sharing or search-engine preview image.
     */
    SOCIAL_IMAGE,

    /**
     * Navigation-related icon or image.
     */
    NAVIGATION,

    /**
     * Any approved resource type not yet represented explicitly.
     */
    OTHER
}