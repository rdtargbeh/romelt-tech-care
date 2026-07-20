/**
 * ================================================================
 * ROMELT TECHCARE — SEO CONFIGURATION
 * ================================================================
 *
 * Purpose:
 * Provides centralized search-engine, browser, social-sharing, and
 * canonical metadata for all public website routes.
 *
 * Responsibilities:
 * - Uses the environment-based production website URL.
 * - Defines page-specific titles and descriptions.
 * - Defines Open Graph and social-sharing metadata.
 * - Provides the social-preview image path.
 * - Supplies website values used by structured data.
 * - Provides fallback metadata for unknown routes.
 *
 * Real-data integration:
 * VITE_SITE_URL must match the final verified production domain.
 * The social-preview image must be placed in the public directory.
 * ================================================================
 */

import { businessConfig } from "@/config/business.config";
import { environmentConfig } from "@/config/environment.config";

export interface PageSeoConfiguration {
  title: string;
  description: string;
  path: string;
  robots?: string;
}

export const siteSeoConfig = {
  siteName: businessConfig.name,

  siteUrl: environmentConfig.siteUrl,

  defaultTitle: `${businessConfig.name} | ${businessConfig.tagline}`,

  defaultDescription:
    "Honest and convenient computer, Wi-Fi, networking, device setup, remote support, and small-business IT services for selected Iowa communities.",

  /**
   * This file must exist at:
   *
   * public/romelt-techcare-social-preview.jpg
   */
  defaultSocialImage: "/romelt-techcare-social-preview.jpg",

  locale: "en_US",

  twitterCard: "summary_large_image",
} as const;

export const pageSeoConfigurations: PageSeoConfiguration[] = [
  {
    path: "/",
    title: `${businessConfig.name} | Hassle-Free Technology Support`,
    description:
      "Dependable computer support, Wi-Fi and networking, device setup, remote assistance, and small-business IT services for homes and organizations in Iowa.",
  },
  {
    path: "/services",
    title: `Technology Services | ${businessConfig.name}`,
    description:
      "Explore computer troubleshooting, Wi-Fi and networking, device setup, remote support, printer assistance, security care, and small-business IT services.",
  },
  {
    path: "/pricing",
    title: `Technology Support Pricing | ${businessConfig.name}`,
    description:
      "Review starting prices for remote support, on-site technology assistance, device setup, and ongoing IT support plans from Romelt TechCare.",
  },
  {
    path: "/about",
    title: `About Us | ${businessConfig.name}`,
    description:
      "Learn about Romelt TechCare's commitment to honest service, practical technology solutions, dependable support, and respectful customer care.",
  },
  {
    path: "/book",
    title: `Book Technology Support | ${businessConfig.name}`,
    description:
      "Request computer, Wi-Fi, networking, device setup, remote support, or small-business IT assistance from Romelt TechCare.",
  },
  {
    path: "/contact",
    title: `Contact Us | ${businessConfig.name}`,
    description:
      "Contact Romelt TechCare with questions about computer support, networking, device setup, remote assistance, appointments, and technology services.",
  },
  {
    path: "/privacy",
    title: `Privacy Policy | ${businessConfig.name}`,
    description:
      "Read how Romelt TechCare may collect, use, protect, retain, and manage information submitted through its website and service-request forms.",
  },
  {
    path: "/terms",
    title: `Terms of Service | ${businessConfig.name}`,
    description:
      "Review the preliminary terms governing the Romelt TechCare website, appointments, technology services, customer responsibilities, and payments.",
  },
  {
    path: "/accessibility",
    title: `Accessibility | ${businessConfig.name}`,
    description:
      "Learn about Romelt TechCare's commitment to an accessible and usable website for customers using different devices and assistive technologies.",
  },
];

export const fallbackSeoConfiguration: PageSeoConfiguration = {
  path: "/404",
  title: `Page Not Found | ${businessConfig.name}`,
  description:
    "The requested Romelt TechCare page could not be found. Return to the homepage or explore available technology services.",
  robots: "noindex, nofollow",
};
