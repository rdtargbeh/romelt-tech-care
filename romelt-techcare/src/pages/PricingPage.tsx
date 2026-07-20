/**
 * ================================================================
 * ROMELT TECHCARE — PRICING PAGE
 * ================================================================
 *
 * Purpose:
 * Presents preliminary service pricing and support plan options.
 *
 * Responsibilities:
 * - Helps customers understand expected service costs.
 * - Explains what each service option includes.
 * - Directs customers to request a final estimate.
 *
 * Real-data integration:
 * Pricing, discounts, taxes, service fees, subscriptions, coupons,
 * and customer-specific estimates will later come from the backend.
 * ================================================================
 */

import { ArrowRight, CheckCircle2, Info } from "lucide-react";
import { Link } from "react-router";

const oneTimeServices = [
  {
    title: "Remote Support",
    price: "$45",
    unit: "starting price",
    description:
      "For software troubleshooting, account setup, updates, configuration, and guided technology assistance.",
    features: [
      "Initial problem review",
      "Remote troubleshooting session",
      "Clear explanation of findings",
      "Recommended next steps",
    ],
  },
  {
    title: "On-Site Support",
    price: "$75",
    unit: "starting price",
    description:
      "For home or business technology issues that require an in-person visit.",
    features: [
      "On-site service visit",
      "Device or network diagnosis",
      "Basic configuration support",
      "Service summary",
    ],
  },
  {
    title: "New Device Setup",
    price: "$85",
    unit: "starting price",
    description:
      "For setting up a new computer, printer, router, monitor, or connected device.",
    features: [
      "Device installation",
      "Basic account configuration",
      "Software updates",
      "Connection testing",
    ],
  },
];

const supportPlans = [
  {
    name: "Essential Care",
    price: "$49",
    description:
      "Basic ongoing technology support for individuals and home offices.",
    features: [
      "One remote support session each month",
      "Basic technology guidance",
      "Device health review",
      "Priority scheduling",
    ],
  },
  {
    name: "Business Care",
    price: "$99",
    description:
      "Flexible support for small businesses and nonprofit organizations.",
    features: [
      "Monthly remote support allowance",
      "Workstation assistance",
      "Network and device guidance",
      "Priority business response",
    ],
  },
  {
    name: "Complete Care",
    price: "$149",
    description:
      "Expanded ongoing support for customers with multiple devices or recurring needs.",
    features: [
      "Extended monthly support",
      "Multiple-device assistance",
      "Preventive maintenance review",
      "Priority remote and on-site scheduling",
    ],
  },
];

export function PricingPage() {
  return (
    <>
      <section className="border-b border-slate-200 bg-white">
        <div className="mx-auto max-w-7xl px-4 py-16 text-center sm:px-6 sm:py-20 lg:px-8">
          <p className="text-sm font-extrabold uppercase tracking-[0.2em] text-blue-700">
            Clear Pricing
          </p>

          <h1 className="mx-auto mt-4 max-w-4xl text-4xl font-black tracking-tight text-slate-950 sm:text-5xl">
            Straightforward technology support without hidden surprises
          </h1>

          <p className="mx-auto mt-6 max-w-3xl text-lg leading-8 text-slate-600">
            Final pricing depends on the problem, required equipment, service
            location, complexity, and time needed to complete the work.
          </p>
        </div>
      </section>

      <section className="bg-slate-50 py-16 sm:py-20">
        <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
          <div className="mb-10">
            <h2 className="text-3xl font-black tracking-tight text-slate-950">
              One-time service
            </h2>

            <p className="mt-3 text-lg text-slate-600">
              Ideal for individual problems, installations, and service visits.
            </p>
          </div>

          <div className="grid gap-6 lg:grid-cols-3">
            {oneTimeServices.map((service) => (
              <article
                key={service.title}
                className="flex flex-col rounded-2xl border border-slate-200 bg-white p-6 shadow-sm"
              >
                <h3 className="text-xl font-extrabold text-slate-950">
                  {service.title}
                </h3>

                <div className="mt-5">
                  <span className="text-4xl font-black text-blue-700">
                    {service.price}
                  </span>

                  <span className="ml-2 text-sm font-semibold text-slate-500">
                    {service.unit}
                  </span>
                </div>

                <p className="mt-5 leading-7 text-slate-600">
                  {service.description}
                </p>

                <ul className="mt-6 flex-1 space-y-3">
                  {service.features.map((feature) => (
                    <li
                      key={feature}
                      className="flex items-start gap-3 text-sm font-medium text-slate-700"
                    >
                      <CheckCircle2
                        className="mt-0.5 size-5 shrink-0 text-green-600"
                        aria-hidden="true"
                      />
                      {feature}
                    </li>
                  ))}
                </ul>

                <Link
                  to="/book"
                  className="mt-7 inline-flex min-h-12 items-center justify-center gap-2 rounded-xl bg-blue-700 px-5 py-3 font-bold text-white transition hover:bg-blue-800"
                >
                  Request Service
                  <ArrowRight className="size-5" aria-hidden="true" />
                </Link>
              </article>
            ))}
          </div>
        </div>
      </section>

      <section className="bg-white py-16 sm:py-20">
        <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
          <div className="mb-10">
            <h2 className="text-3xl font-black tracking-tight text-slate-950">
              Monthly technology care plans
            </h2>

            <p className="mt-3 text-lg text-slate-600">
              Designed for customers who need dependable ongoing support.
            </p>
          </div>

          <div className="grid gap-6 lg:grid-cols-3">
            {supportPlans.map((plan) => (
              <article
                key={plan.name}
                className="rounded-2xl border border-slate-200 bg-slate-50 p-6"
              >
                <h3 className="text-xl font-extrabold text-slate-950">
                  {plan.name}
                </h3>

                <div className="mt-5">
                  <span className="text-4xl font-black text-blue-700">
                    {plan.price}
                  </span>

                  <span className="ml-2 font-semibold text-slate-500">
                    /month
                  </span>
                </div>

                <p className="mt-5 leading-7 text-slate-600">
                  {plan.description}
                </p>

                <ul className="mt-6 space-y-3">
                  {plan.features.map((feature) => (
                    <li
                      key={feature}
                      className="flex items-start gap-3 text-sm font-medium text-slate-700"
                    >
                      <CheckCircle2
                        className="mt-0.5 size-5 shrink-0 text-green-600"
                        aria-hidden="true"
                      />
                      {feature}
                    </li>
                  ))}
                </ul>

                <Link
                  to="/contact"
                  className="mt-7 inline-flex min-h-12 w-full items-center justify-center rounded-xl border border-slate-300 bg-white px-5 py-3 font-bold text-slate-800 transition hover:bg-slate-100"
                >
                  Ask About This Plan
                </Link>
              </article>
            ))}
          </div>

          <div className="mt-10 flex gap-3 rounded-2xl border border-blue-100 bg-blue-50 p-5">
            <Info
              className="mt-0.5 size-6 shrink-0 text-blue-700"
              aria-hidden="true"
            />

            <p className="leading-7 text-slate-700">
              These prices are preliminary starting rates for website
              development. Parts, equipment, extended labor, travel outside the
              normal service area, and specialized work may require an
              additional estimate.
            </p>
          </div>
        </div>
      </section>
    </>
  );
}
