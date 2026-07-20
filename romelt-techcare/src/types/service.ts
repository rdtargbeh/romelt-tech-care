/**
 * ================================================================
 * ROMELT TECHCARE — SERVICE TYPES
 * ================================================================
 *
 * Purpose:
 * Defines shared TypeScript structures for website service content.
 *
 * Responsibilities:
 * - Standardizes service titles, descriptions, pricing, and features.
 * - Supports reusable cards and future service-detail pages.
 *
 * Real-data integration:
 * Services are currently loaded from local data. Later, active
 * services, pricing, duration, availability, and booking rules
 * should come from the Romelt TechCare backend.
 * ================================================================
 */

import type { LucideIcon } from "lucide-react";

export type ServiceAudience = "HOME" | "BUSINESS" | "BOTH";

export interface ServiceSummary {
  id: string;
  slug: string;
  title: string;
  shortDescription: string;
  startingPrice: number | null;
  priceLabel?: string;
  audience: ServiceAudience;
  icon: LucideIcon;
  features: string[];
  isPopular?: boolean;
}
