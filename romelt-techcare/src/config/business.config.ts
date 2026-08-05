/**
 * ================================================================
 * ROMELT TECHCARE — BUSINESS CONFIGURATION
 * ================================================================
 *
 * Purpose:
 * Provides one centralized source for public business information.
 *
 * Responsibilities:
 * - Stores business identity and public contact information.
 * - Stores service-area and operating-hour information.
 * - Prevents duplicated business details across website pages.
 * - Provides reusable telephone and email link values.
 *
 * Real-data integration:
 * The current phone number and email address are temporary personal
 * contact details. They should be replaced when dedicated Romelt
 * TechCare business contact information becomes available.
 *
 * These values may later be loaded from the Spring Boot backend
 * through a public business-settings endpoint.
 * ================================================================
 */

export interface BusinessHours {
  day: string;
  hours: string;
}

export interface BusinessConfiguration {
  name: string;
  legalName: string;
  tagline: string;
  description: string;

  phone: string;
  phoneDisplay: string;
  email: string;

  city: string;
  state: string;
  country: string;
  serviceArea: string;

  appointmentOnly: boolean;
  remoteSupportAvailable: boolean;
  onsiteSupportAvailable: boolean;

  businessHours: BusinessHours[];

  socialLinks: {
    facebook: string | null;
    linkedin: string | null;
    instagram: string | null;
  };
}

export const businessConfig: BusinessConfiguration = {
  name: "Romelt TechCare",
  legalName: "Romelt TechCare",
  tagline: "Hassle-Free Technology. Honest Service.",

  description:
    "Dependable computer, networking, Wi-Fi, device setup, remote support, and small business technology services.",

  /**
   * Temporary personal phone number.
   *
   * Replace this with the dedicated Romelt TechCare business phone
   * number when one becomes available.
   *
   * The machine-readable value uses the international US format so
   * telephone links work correctly on mobile devices.
   */
  phone: "+15154231007",
  phoneDisplay: "(515) 423-1007",

  /**
   * Temporary personal email address.
   *
   * Replace this with the dedicated Romelt TechCare business email
   * address when one becomes available.
   */
  email: "rdtargbeh2015@gmail.com",

  city: "Des Moines",
  state: "Iowa",
  country: "United States",
  serviceArea: "Des Moines and surrounding Iowa communities",

  appointmentOnly: true,
  remoteSupportAvailable: true,
  onsiteSupportAvailable: true,

  businessHours: [
    {
      day: "Monday – Friday",
      hours: "Evening appointments",
    },
    {
      day: "Saturday",
      hours: "By appointment",
    },
    {
      day: "Sunday",
      hours: "Limited availability",
    },
  ],

  socialLinks: {
    facebook: null,
    linkedin: null,
    instagram: null,
  },
};

export const businessPhoneHref = businessConfig.phone
  ? `tel:${businessConfig.phone}`
  : null;

export const businessEmailHref = businessConfig.email
  ? `mailto:${businessConfig.email}`
  : null;

export const businessLocationText = [
  businessConfig.city,
  businessConfig.state,
].join(", ");
