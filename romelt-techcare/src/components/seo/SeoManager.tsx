/**
 * ================================================================
 * ROMELT TECHCARE — SEO MANAGER
 * ================================================================
 *
 * Purpose:
 * Automatically updates browser and search-engine metadata whenever
 * the public route changes.
 *
 * Responsibilities:
 * - Updates the browser page title.
 * - Updates the page description.
 * - Updates canonical URLs.
 * - Updates robots directives.
 * - Updates Open Graph metadata.
 * - Updates social-sharing card metadata.
 * - Uses centralized SEO configuration.
 *
 * Real-data integration:
 * The production domain and social-sharing image must be updated in
 * the SEO configuration before the website is deployed publicly.
 * ================================================================
 */

import { useEffect } from "react";
import { useLocation } from "react-router";

import {
  fallbackSeoConfiguration,
  pageSeoConfigurations,
  siteSeoConfig,
} from "@/config/seo.config";

export function SeoManager() {
  const location = useLocation();

  useEffect(() => {
    const normalizedPath = normalizePath(location.pathname);

    const pageConfiguration =
      pageSeoConfigurations.find(
        (configuration) => configuration.path === normalizedPath,
      ) ?? fallbackSeoConfiguration;

    const canonicalUrl = new URL(
      pageConfiguration.path === "/404"
        ? normalizedPath
        : pageConfiguration.path,
      siteSeoConfig.siteUrl,
    ).toString();

    const socialImageUrl = new URL(
      siteSeoConfig.defaultSocialImage,
      siteSeoConfig.siteUrl,
    ).toString();

    document.title = pageConfiguration.title;

    updateMetaTag("name", "description", pageConfiguration.description);

    updateMetaTag(
      "name",
      "robots",
      pageConfiguration.robots ?? "index, follow",
    );

    updateMetaTag("property", "og:type", "website");

    updateMetaTag("property", "og:site_name", siteSeoConfig.siteName);

    updateMetaTag("property", "og:locale", siteSeoConfig.locale);

    updateMetaTag("property", "og:title", pageConfiguration.title);

    updateMetaTag("property", "og:description", pageConfiguration.description);

    updateMetaTag("property", "og:url", canonicalUrl);

    updateMetaTag("property", "og:image", socialImageUrl);

    updateMetaTag("name", "twitter:card", siteSeoConfig.twitterCard);

    updateMetaTag("name", "twitter:title", pageConfiguration.title);

    updateMetaTag("name", "twitter:description", pageConfiguration.description);

    updateMetaTag("name", "twitter:image", socialImageUrl);

    updateCanonicalLink(canonicalUrl);
  }, [location.pathname]);

  return null;
}

function normalizePath(pathname: string): string {
  if (!pathname || pathname === "/") {
    return "/";
  }

  return pathname.endsWith("/") ? pathname.slice(0, -1) : pathname;
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
