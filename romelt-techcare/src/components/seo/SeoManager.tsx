/**
 * ================================================================
 * ROMELT TECHCARE — SEO MANAGER
 * ================================================================
 *
 * Purpose:
 * Automatically updates browser, search-engine, and social-sharing
 * metadata whenever the active application route changes.
 *
 * Responsibilities:
 * - Updates the browser page title.
 * - Updates the page description.
 * - Updates canonical URLs.
 * - Updates robots directives.
 * - Updates Open Graph metadata.
 * - Updates social-sharing card metadata.
 * - Uses centralized SEO configuration for public pages.
 * - Prevents administrator routes from being indexed.
 * - Prevents administrator routes from inheriting stale public-page
 *   metadata after client-side navigation.
 *
 * Real-data integration:
 * The production domain and social-sharing image must be updated in
 * the centralized SEO configuration before public deployment.
 *
 * Security and privacy:
 * Every route beginning with /admin is assigned:
 *
 * noindex, nofollow, noarchive, nosnippet
 *
 * This discourages search engines from indexing or caching secure
 * administrator pages.
 * ================================================================
 */

import { useEffect } from "react";
import { useLocation } from "react-router-dom";

import {
  fallbackSeoConfiguration,
  pageSeoConfigurations,
  siteSeoConfig,
} from "@/config/seo.config";

interface ResolvedSeoMetadata {
  title: string;
  description: string;
  canonicalUrl: string;
  robots: string;
  socialImageUrl: string;
}

const ADMIN_ROUTE_PREFIX = "/admin";

const ADMIN_DEFAULT_TITLE = "Administrator Portal | Romelt TechCare";

const ADMIN_DEFAULT_DESCRIPTION =
  "Secure Romelt TechCare administrator portal.";

const ADMIN_ROBOTS_DIRECTIVE = "noindex, nofollow, noarchive, nosnippet";

export function SeoManager() {
  const location = useLocation();

  useEffect(() => {
    const normalizedPath = normalizePath(location.pathname);

    const metadata = isAdminRoute(normalizedPath)
      ? resolveAdminMetadata(normalizedPath)
      : resolvePublicMetadata(normalizedPath);

    applyDocumentMetadata(metadata);
  }, [location.pathname]);

  return null;
}

function resolvePublicMetadata(normalizedPath: string): ResolvedSeoMetadata {
  const pageConfiguration =
    pageSeoConfigurations.find(
      (configuration) => configuration.path === normalizedPath,
    ) ?? fallbackSeoConfiguration;

  const canonicalPath =
    pageConfiguration.path === "/404" ? normalizedPath : pageConfiguration.path;

  return {
    title: pageConfiguration.title,

    description: pageConfiguration.description,

    canonicalUrl: createAbsoluteUrl(canonicalPath),

    robots: pageConfiguration.robots ?? "index, follow",

    socialImageUrl: createAbsoluteUrl(siteSeoConfig.defaultSocialImage),
  };
}

function resolveAdminMetadata(normalizedPath: string): ResolvedSeoMetadata {
  const title = resolveAdminPageTitle(normalizedPath);

  return {
    title,

    description: ADMIN_DEFAULT_DESCRIPTION,

    canonicalUrl: createAbsoluteUrl(normalizedPath),

    robots: ADMIN_ROBOTS_DIRECTIVE,

    socialImageUrl: createAbsoluteUrl(siteSeoConfig.defaultSocialImage),
  };
}

function resolveAdminPageTitle(normalizedPath: string): string {
  if (normalizedPath === "/admin/login") {
    return "Administrator Sign In | Romelt TechCare";
  }

  if (normalizedPath === "/admin/change-password") {
    return "Change Password | Romelt TechCare";
  }

  if (normalizedPath === "/admin") {
    return "Administrator Dashboard | Romelt TechCare";
  }

  if (normalizedPath.startsWith("/admin/bookings")) {
    return "Booking Management | Romelt TechCare";
  }

  if (normalizedPath.startsWith("/admin/contact-inquiries")) {
    return "Contact Inquiries | Romelt TechCare";
  }

  if (normalizedPath.startsWith("/admin/services")) {
    return "Service Management | Romelt TechCare";
  }

  if (normalizedPath.startsWith("/admin/settings")) {
    return "Administrator Settings | Romelt TechCare";
  }

  return ADMIN_DEFAULT_TITLE;
}

function applyDocumentMetadata(metadata: ResolvedSeoMetadata) {
  document.title = metadata.title;

  updateMetaTag("name", "description", metadata.description);

  updateMetaTag("name", "robots", metadata.robots);

  updateMetaTag("property", "og:type", "website");

  updateMetaTag("property", "og:site_name", siteSeoConfig.siteName);

  updateMetaTag("property", "og:locale", siteSeoConfig.locale);

  updateMetaTag("property", "og:title", metadata.title);

  updateMetaTag("property", "og:description", metadata.description);

  updateMetaTag("property", "og:url", metadata.canonicalUrl);

  updateMetaTag("property", "og:image", metadata.socialImageUrl);

  updateMetaTag("name", "twitter:card", siteSeoConfig.twitterCard);

  updateMetaTag("name", "twitter:title", metadata.title);

  updateMetaTag("name", "twitter:description", metadata.description);

  updateMetaTag("name", "twitter:image", metadata.socialImageUrl);

  updateCanonicalLink(metadata.canonicalUrl);
}

function normalizePath(pathname: string): string {
  if (!pathname || pathname === "/") {
    return "/";
  }

  const normalizedPath = pathname.startsWith("/") ? pathname : `/${pathname}`;

  return normalizedPath.endsWith("/")
    ? normalizedPath.slice(0, -1)
    : normalizedPath;
}

function isAdminRoute(pathname: string): boolean {
  return (
    pathname === ADMIN_ROUTE_PREFIX ||
    pathname.startsWith(`${ADMIN_ROUTE_PREFIX}/`)
  );
}

function createAbsoluteUrl(path: string): string {
  return new URL(path, ensureTrailingSlash(siteSeoConfig.siteUrl)).toString();
}

function ensureTrailingSlash(value: string): string {
  return value.endsWith("/") ? value : `${value}/`;
}

function updateMetaTag(
  attributeName: "name" | "property",
  attributeValue: string,
  content: string,
) {
  let metaElement = document.head.querySelector<HTMLMetaElement>(
    `meta[${attributeName}="${attributeValue}"]`,
  );

  if (!metaElement) {
    metaElement = document.createElement("meta");

    metaElement.setAttribute(attributeName, attributeValue);

    document.head.appendChild(metaElement);
  }

  metaElement.setAttribute("content", content);
}

function updateCanonicalLink(url: string) {
  let canonicalLink = document.head.querySelector<HTMLLinkElement>(
    'link[rel="canonical"]',
  );

  if (!canonicalLink) {
    canonicalLink = document.createElement("link");

    canonicalLink.setAttribute("rel", "canonical");

    document.head.appendChild(canonicalLink);
  }

  canonicalLink.setAttribute("href", url);
}
