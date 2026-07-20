/**
 * ================================================================
 * ROMELT TECHCARE — STRUCTURED DATA
 * ================================================================
 *
 * Purpose:
 * Adds JSON-LD structured data to the website so search engines can
 * better understand Romelt TechCare's business identity, services,
 * contact information, service area, and website.
 *
 * Responsibilities:
 * - Creates LocalBusiness structured data.
 * - Uses centralized business and SEO configuration.
 * - Adds the structured-data script to the document head.
 * - Removes the script when the component is unmounted.
 * - Avoids duplicate structured-data scripts.
 *
 * Real-data integration:
 * The final production domain, business address, opening hours,
 * pricing range, social profiles, and business registration details
 * should be updated before public launch.
 * ================================================================
 */

import { useEffect } from "react";

import { businessConfig } from "@/config/business.config";
import { siteSeoConfig } from "@/config/seo.config";

const STRUCTURED_DATA_SCRIPT_ID = "romelt-techcare-structured-data";

interface StructuredDataOrganization {
  "@type": "Organization";
  name: string;
  url: string;
}

interface StructuredDataPostalAddress {
  "@type": "PostalAddress";
  addressLocality: string;
  addressRegion: string;
  addressCountry: string;
}

interface StructuredDataOpeningHours {
  "@type": "OpeningHoursSpecification";
  dayOfWeek: string[];
  opens: string;
  closes: string;
}

interface StructuredDataService {
  "@type": "Service";
  name: string;
  description: string;
  provider: StructuredDataOrganization;
  areaServed: string;
}

interface LocalBusinessStructuredData {
  "@context": "https://schema.org";
  "@type": "LocalBusiness";
  "@id": string;
  name: string;
  legalName: string;
  description: string;
  url: string;
  image: string;
  logo: string;
  telephone: string;
  email: string;
  priceRange: string;
  address: StructuredDataPostalAddress;
  areaServed: string;
  serviceType: string[];
  hasOfferCatalog: {
    "@type": "OfferCatalog";
    name: string;
    itemListElement: Array<{
      "@type": "Offer";
      itemOffered: StructuredDataService;
    }>;
  };
  openingHoursSpecification?: StructuredDataOpeningHours[];
  sameAs?: string[];
}

const structuredServices: StructuredDataService[] = [
  {
    "@type": "Service",
    name: "Computer Support",
    description:
      "Computer troubleshooting, software support, performance assistance, and general technology help.",
    provider: {
      "@type": "Organization",
      name: businessConfig.name,
      url: siteSeoConfig.siteUrl,
    },
    areaServed: businessConfig.serviceArea,
  },
  {
    "@type": "Service",
    name: "Wi-Fi and Networking",
    description:
      "Home and small-business Wi-Fi setup, network troubleshooting, router support, and connectivity assistance.",
    provider: {
      "@type": "Organization",
      name: businessConfig.name,
      url: siteSeoConfig.siteUrl,
    },
    areaServed: businessConfig.serviceArea,
  },
  {
    "@type": "Service",
    name: "Device Setup",
    description:
      "Setup and configuration support for computers, printers, phones, tablets, and other technology devices.",
    provider: {
      "@type": "Organization",
      name: businessConfig.name,
      url: siteSeoConfig.siteUrl,
    },
    areaServed: businessConfig.serviceArea,
  },
  {
    "@type": "Service",
    name: "Remote Technology Support",
    description:
      "Remote troubleshooting and technology assistance for eligible computer, software, and device issues.",
    provider: {
      "@type": "Organization",
      name: businessConfig.name,
      url: siteSeoConfig.siteUrl,
    },
    areaServed: businessConfig.serviceArea,
  },
  {
    "@type": "Service",
    name: "Small-Business IT Support",
    description:
      "Practical IT support for small businesses, churches, nonprofit organizations, and home offices.",
    provider: {
      "@type": "Organization",
      name: businessConfig.name,
      url: siteSeoConfig.siteUrl,
    },
    areaServed: businessConfig.serviceArea,
  },
];

export function StructuredData() {
  useEffect(() => {
    const siteUrl = removeTrailingSlash(siteSeoConfig.siteUrl);

    const socialLinks = Object.values(businessConfig.socialLinks).filter(
      (link): link is string => Boolean(link),
    );

    const structuredData: LocalBusinessStructuredData = {
      "@context": "https://schema.org",
      "@type": "LocalBusiness",
      "@id": `${siteUrl}/#localbusiness`,

      name: businessConfig.name,
      legalName: businessConfig.legalName,
      description: businessConfig.description,

      url: siteUrl,
      image: `${siteUrl}${siteSeoConfig.defaultSocialImage}`,
      logo: `${siteUrl}/favicon.svg`,

      telephone: businessConfig.phone,
      email: businessConfig.email,

      /**
       * This is a general indication rather than a guaranteed price.
       *
       * Update later when the final pricing structure is established.
       */
      priceRange: "$$",

      address: {
        "@type": "PostalAddress",
        addressLocality: businessConfig.city,
        addressRegion: businessConfig.state,
        addressCountry: businessConfig.country,
      },

      areaServed: businessConfig.serviceArea,

      serviceType: [
        "Computer support",
        "Wi-Fi and networking",
        "Device setup",
        "Remote technology support",
        "Printer and scanner support",
        "Small-business IT support",
      ],

      hasOfferCatalog: {
        "@type": "OfferCatalog",
        name: `${businessConfig.name} Services`,
        itemListElement: structuredServices.map((service) => ({
          "@type": "Offer",
          itemOffered: service,
        })),
      },

      ...(socialLinks.length > 0
        ? {
            sameAs: socialLinks,
          }
        : {}),
    };

    let scriptElement = document.getElementById(
      STRUCTURED_DATA_SCRIPT_ID,
    ) as HTMLScriptElement | null;

    if (!scriptElement) {
      scriptElement = document.createElement("script");
      scriptElement.id = STRUCTURED_DATA_SCRIPT_ID;
      scriptElement.type = "application/ld+json";

      document.head.appendChild(scriptElement);
    }

    scriptElement.textContent = JSON.stringify(structuredData, null, 2);

    return () => {
      const existingScript = document.getElementById(STRUCTURED_DATA_SCRIPT_ID);

      existingScript?.remove();
    };
  }, []);

  return null;
}

function removeTrailingSlash(value: string): string {
  return value.endsWith("/") ? value.slice(0, -1) : value;
}
