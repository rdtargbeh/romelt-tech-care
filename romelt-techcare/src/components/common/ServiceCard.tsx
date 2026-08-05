/**
 * ================================================================
 * ROMELT TECHCARE — SERVICE CARD
 * ================================================================
 *
 * Purpose:
 * Displays a concise, reusable preview of a customer service.
 *
 * Responsibilities:
 * - Shows service icon, title, description, features, and price.
 * - Links customers toward the booking experience.
 * - Supports both launch data and future backend-provided services.
 *
 * Real-data integration:
 * The service object is currently sourced from local launch data.
 * It should later come from the service catalog API.
 * ================================================================
 */

import { ArrowRight, Check } from "lucide-react";
import { Link } from "react-router";

import type { ServiceSummary } from "@/types/service";

interface ServiceCardProps {
  service: ServiceSummary;
}

function formatPrice(price: number | null): string {
  if (price === null) {
    return "Custom estimate";
  }

  return `From $${price}`;
}

export function ServiceCard({ service }: ServiceCardProps) {
  const Icon = service.icon;

  return (
    <article className="site-card group flex h-full flex-col p-6 transition duration-300 hover:-translate-y-1 hover:border-brand-200 hover:shadow-soft sm:p-7">
      <div className="mb-6 flex items-start justify-between gap-4">
        <div className="flex h-13 w-13 items-center justify-center rounded-2xl bg-brand-50 text-brand-700 ring-1 ring-brand-100">
          <Icon aria-hidden="true" className="h-6 w-6" strokeWidth={1.8} />
        </div>

        {service.isPopular ? (
          <span className="rounded-full bg-warm-100 px-3 py-1 text-xs font-bold text-warm-800">
            Popular
          </span>
        ) : null}
      </div>

      <h3 className="font-display text-xl font-bold tracking-tight text-navy-950">
        {service.title}
      </h3>

      <p className="mt-3 text-sm leading-6 text-slate-600">
        {service.shortDescription}
      </p>

      <ul className="mt-5 space-y-3">
        {service.features.map((feature) => (
          <li
            key={feature}
            className="flex items-start gap-3 text-sm text-slate-700"
          >
            <span className="mt-0.5 flex h-5 w-5 shrink-0 items-center justify-center rounded-full bg-brand-50 text-brand-700">
              <Check aria-hidden="true" className="h-3.5 w-3.5" />
            </span>

            <span>{feature}</span>
          </li>
        ))}
      </ul>

      <div className="mt-auto pt-7">
        <p className="mb-4 text-sm font-extrabold text-navy-900">
          {formatPrice(service.startingPrice)}
        </p>

        <Link
          to={`/book-service?service=${encodeURIComponent(service.slug)}`}
          className="focus-ring inline-flex min-h-11 items-center gap-2 rounded-xl font-bold text-brand-700 transition hover:text-brand-900"
        >
          Request this service
          <ArrowRight
            aria-hidden="true"
            className="h-4 w-4 transition-transform group-hover:translate-x-1"
          />
        </Link>
      </div>
    </article>
  );
}
