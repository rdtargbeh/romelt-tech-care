/**
 * ================================================================
 * ROMELT TECHCARE — LAUNCH SERVICE DATA
 * ================================================================
 *
 * Purpose:
 * Defines the first customer-facing service offerings for the
 * Romelt TechCare launch website.
 *
 * Responsibilities:
 * - Supplies consistent content to the homepage and services page.
 * - Identifies popular launch services.
 * - Provides transparent starting-price guidance.
 *
 * Real-data integration:
 * This is temporary launch data. The backend should eventually own
 * service status, prices, duration, tax treatment, service area,
 * booking availability, required skills, and customer eligibility.
 * ================================================================
 */

import {
  BriefcaseBusiness,
  Headphones,
  Laptop,
  Printer,
  ShieldCheck,
  Wifi,
} from "lucide-react";

import type { ServiceSummary } from "@/types/service";

export const launchServices: ServiceSummary[] = [
  {
    id: "computer-support",
    slug: "computer-support",
    title: "Computer Help & Tune-Ups",
    shortDescription:
      "Friendly support for slow computers, software problems, updates, setup, and everyday technical issues.",
    startingPrice: 99,
    audience: "BOTH",
    icon: Laptop,
    features: [
      "Computer diagnostics",
      "Performance cleanup",
      "Software troubleshooting",
    ],
    isPopular: true,
  },
  {
    id: "wifi-networking",
    slug: "wifi-networking",
    title: "Wi-Fi & Home Networking",
    shortDescription:
      "Improve weak connections, dead zones, router settings, device connectivity, and home-office reliability.",
    startingPrice: 149,
    audience: "BOTH",
    icon: Wifi,
    features: [
      "Wi-Fi troubleshooting",
      "Router and mesh setup",
      "Coverage recommendations",
    ],
    isPopular: true,
  },
  {
    id: "new-device-setup",
    slug: "new-device-setup",
    title: "New Device Setup",
    shortDescription:
      "Get a new computer, monitor, printer, or other device configured correctly without the frustration.",
    startingPrice: 89,
    audience: "BOTH",
    icon: Printer,
    features: [
      "Account and update setup",
      "Printer configuration",
      "Essential application installation",
    ],
  },
  {
    id: "remote-support",
    slug: "remote-support",
    title: "Remote Technology Support",
    shortDescription:
      "Resolve suitable software, email, settings, and application problems without waiting for an on-site visit.",
    startingPrice: 69,
    audience: "BOTH",
    icon: Headphones,
    features: [
      "Secure support session",
      "Plain-language guidance",
      "Session completion summary",
    ],
  },
  {
    id: "small-business-it",
    slug: "small-business-it",
    title: "Small-Business IT Support",
    shortDescription:
      "Dependable workstation, email, printer, networking, and technology support for organizations without internal IT.",
    startingPrice: 149,
    priceLabel: "Starting per visit",
    audience: "BUSINESS",
    icon: BriefcaseBusiness,
    features: [
      "Workstation deployment",
      "Office technology setup",
      "Ongoing support options",
    ],
    isPopular: true,
  },
  {
    id: "security-backup",
    slug: "security-backup",
    title: "Security & Backup Basics",
    shortDescription:
      "Strengthen everyday protection with updates, multifactor authentication, safer account practices, and backup setup.",
    startingPrice: 129,
    audience: "BOTH",
    icon: ShieldCheck,
    features: [
      "Account security review",
      "Backup configuration",
      "Practical security recommendations",
    ],
  },
];

export const featuredServices = launchServices.filter(
  (service) => service.isPopular,
);
