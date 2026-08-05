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
 * - Highlights the recommended monthly support offering.
 * - Directs customers to request a final estimate.
 *
 * Brand palette:
 * - Primary Blue: #1976D2
 * - Deep Navy: #0B2545
 * - Premium Gold: #D4AF37
 * - Crimson Accent: #C62828
 *
 * Real-data integration:
 * Pricing, discounts, taxes, service fees, subscriptions, coupons,
 * and customer-specific estimates will later come from the backend.
 * ================================================================
 */

import { ArrowRight, CheckCircle2, Info, Sparkles } from "lucide-react";
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
    cardClass: "border-[#B9D8F7] bg-gradient-to-br from-white to-[#EAF4FD]",
    iconBackground: "bg-[#EAF4FD]",
    iconColor: "text-[#1976D2]",
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
    cardClass: "border-[#EADBA3] bg-gradient-to-br from-white to-[#FFF8E1]",
    iconBackground: "bg-[#FFF8E1]",
    iconColor: "text-[#B68E13]",
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
    cardClass: "border-[#F3C7C7] bg-gradient-to-br from-white to-[#FDECEC]",
    iconBackground: "bg-[#FDECEC]",
    iconColor: "text-[#C62828]",
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
    recommended: false,
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
    recommended: true,
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
    recommended: false,
  },
];

export function PricingPage() {
  return (
    <>
      {/* =========================================================
          PRICING HERO
          ========================================================= */}
      <section className="relative isolate overflow-hidden border-b border-[#B9D8F7] bg-gradient-to-br from-[#EAF4FD] via-white to-[#FFF8E1]">
        <div
          aria-hidden="true"
          className="absolute -left-28 top-0 -z-10 h-80 w-80 rounded-full bg-[#1976D2]/15 blur-3xl"
        />

        <div
          aria-hidden="true"
          className="absolute -right-28 bottom-0 -z-10 h-80 w-80 rounded-full bg-[#D4AF37]/15 blur-3xl"
        />

        <div className="mx-auto max-w-7xl px-4 py-16 text-center sm:px-6 sm:py-20 lg:px-8 lg:py-24">
          <div className="inline-flex items-center gap-2 rounded-full border border-[#B9D8F7] bg-white/80 px-4 py-2 text-sm font-extrabold uppercase tracking-[0.18em] text-[#1976D2] shadow-sm backdrop-blur-sm">
            <Sparkles className="size-4" aria-hidden="true" />
            Clear Pricing
          </div>

          <h1 className="mx-auto mt-5 max-w-4xl text-4xl font-black tracking-tight text-[#0B2545] sm:text-5xl lg:text-6xl">
            Straightforward technology support without hidden surprises
          </h1>

          <p className="mx-auto mt-6 max-w-3xl text-lg leading-8 text-slate-600">
            Final pricing depends on the problem, required equipment, service
            location, complexity, and time needed to complete the work.
          </p>
        </div>
      </section>

      {/* =========================================================
          ONE-TIME SERVICES
          ========================================================= */}
      <section className="relative overflow-hidden bg-[#F8FAFC] py-16 sm:py-20 lg:py-24">
        <div
          aria-hidden="true"
          className="absolute -right-40 top-20 h-80 w-80 rounded-full bg-[#1976D2]/8 blur-3xl"
        />

        <div className="relative mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
          <div className="mb-10 max-w-3xl">
            <p className="text-sm font-extrabold uppercase tracking-[0.2em] text-[#1976D2]">
              Flexible Service Options
            </p>

            <h2 className="mt-3 text-3xl font-black tracking-tight text-[#0B2545] sm:text-4xl">
              One-time service
            </h2>

            <p className="mt-4 text-lg leading-8 text-slate-600">
              Ideal for individual problems, installations, troubleshooting, and
              scheduled service visits.
            </p>
          </div>

          <div className="grid gap-6 lg:grid-cols-3">
            {oneTimeServices.map((service, index) => (
              <article
                key={service.title}
                className={`group relative flex flex-col overflow-hidden rounded-3xl border p-7 shadow-sm transition duration-300 hover:-translate-y-2 hover:shadow-2xl hover:shadow-[#0B2545]/10 ${service.cardClass}`}
              >
                <div
                  aria-hidden="true"
                  className="absolute right-0 top-0 h-28 w-28 rounded-bl-[5rem] bg-white/50 transition duration-300 group-hover:scale-125"
                />

                <div className="relative flex flex-1 flex-col">
                  <div className="flex items-center justify-between">
                    <div
                      className={`flex size-12 items-center justify-center rounded-2xl ${service.iconBackground} ${service.iconColor}`}
                    >
                      <span className="text-sm font-black">0{index + 1}</span>
                    </div>

                    <span className="rounded-full bg-white/80 px-3 py-1 text-xs font-extrabold uppercase tracking-wide text-[#0B2545] shadow-sm">
                      One-Time
                    </span>
                  </div>

                  <h3 className="mt-6 text-2xl font-extrabold text-[#0B2545]">
                    {service.title}
                  </h3>

                  <div className="mt-5">
                    <span className="text-4xl font-black text-[#1976D2]">
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
                          className="mt-0.5 size-5 shrink-0 text-[#1976D2]"
                          aria-hidden="true"
                        />
                        {feature}
                      </li>
                    ))}
                  </ul>

                  <Link
                    to="/book"
                    className="mt-7 inline-flex min-h-12 items-center justify-center gap-2 rounded-xl bg-[#1976D2] px-5 py-3 font-bold !text-white shadow-lg shadow-[#1976D2]/20 transition hover:bg-[#1565C0] hover:!text-white"
                  >
                    Request Service
                    <ArrowRight className="size-5" aria-hidden="true" />
                  </Link>
                </div>
              </article>
            ))}
          </div>
        </div>
      </section>

      {/* =========================================================
          MONTHLY SUPPORT PLANS
          ========================================================= */}
      <section className="relative overflow-hidden bg-white py-16 sm:py-20 lg:py-24">
        <div
          aria-hidden="true"
          className="absolute -left-40 bottom-0 h-96 w-96 rounded-full bg-[#D4AF37]/10 blur-3xl"
        />

        <div className="relative mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
          <div className="mb-12 max-w-3xl">
            <p className="text-sm font-extrabold uppercase tracking-[0.2em] text-[#1976D2]">
              Ongoing Technology Care
            </p>

            <h2 className="mt-3 text-3xl font-black tracking-tight text-[#0B2545] sm:text-4xl">
              Monthly technology care plans
            </h2>

            <p className="mt-4 text-lg leading-8 text-slate-600">
              Designed for customers who want dependable ongoing support,
              priority scheduling, and fewer technology disruptions.
            </p>
          </div>

          <div className="grid items-stretch gap-7 lg:grid-cols-3">
            {supportPlans.map((plan) => {
              const isRecommended = plan.recommended;

              return (
                <article
                  key={plan.name}
                  className={
                    isRecommended
                      ? "relative flex flex-col overflow-hidden rounded-3xl border-2 border-[#D4AF37] bg-gradient-to-br from-[#1976D2] to-[#0B2545] p-7 text-white shadow-2xl shadow-[#0B2545]/25 lg:-translate-y-4"
                      : "relative flex flex-col rounded-3xl border border-slate-200 bg-[#F8FAFC] p-7 shadow-sm transition duration-300 hover:-translate-y-2 hover:border-[#1976D2]/30 hover:bg-white hover:shadow-xl"
                  }
                >
                  {isRecommended ? (
                    <div className="absolute right-0 top-0 rounded-bl-2xl bg-[#D4AF37] px-5 py-2 text-xs font-black uppercase tracking-[0.16em] text-[#0B2545]">
                      Recommended
                    </div>
                  ) : null}

                  <div className="flex flex-1 flex-col">
                    {isRecommended ? (
                      <div className="mb-5 flex size-12 items-center justify-center rounded-2xl bg-white/10 text-[#D4AF37]">
                        <Sparkles className="size-6" aria-hidden="true" />
                      </div>
                    ) : null}

                    <h3
                      className={
                        isRecommended
                          ? "text-2xl font-extrabold text-white"
                          : "text-2xl font-extrabold text-[#0B2545]"
                      }
                    >
                      {plan.name}
                    </h3>

                    <div className="mt-5">
                      <span
                        className={
                          isRecommended
                            ? "text-5xl font-black text-[#D4AF37]"
                            : "text-5xl font-black text-[#1976D2]"
                        }
                      >
                        {plan.price}
                      </span>

                      <span
                        className={
                          isRecommended
                            ? "ml-2 font-semibold text-[#EAF4FD]"
                            : "ml-2 font-semibold text-slate-500"
                        }
                      >
                        /month
                      </span>
                    </div>

                    <p
                      className={
                        isRecommended
                          ? "mt-5 leading-7 text-[#EAF4FD]"
                          : "mt-5 leading-7 text-slate-600"
                      }
                    >
                      {plan.description}
                    </p>

                    <ul className="mt-7 flex-1 space-y-4">
                      {plan.features.map((feature) => (
                        <li
                          key={feature}
                          className={
                            isRecommended
                              ? "flex items-start gap-3 text-sm font-semibold text-white"
                              : "flex items-start gap-3 text-sm font-medium text-slate-700"
                          }
                        >
                          <CheckCircle2
                            className={
                              isRecommended
                                ? "mt-0.5 size-5 shrink-0 text-[#D4AF37]"
                                : "mt-0.5 size-5 shrink-0 text-[#1976D2]"
                            }
                            aria-hidden="true"
                          />
                          {feature}
                        </li>
                      ))}
                    </ul>

                    <Link
                      to="/contact"
                      className={
                        isRecommended
                          ? "mt-8 inline-flex min-h-12 w-full items-center justify-center gap-2 rounded-xl bg-[#D4AF37] px-5 py-3 font-extrabold !text-[#0B2545] shadow-lg transition hover:bg-[#E0BE4C] hover:!text-[#0B2545]"
                          : "mt-8 inline-flex min-h-12 w-full items-center justify-center gap-2 rounded-xl border border-[#1976D2] bg-white px-5 py-3 font-bold !text-[#1976D2] transition hover:bg-[#EAF4FD] hover:!text-[#0B2545]"
                      }
                    >
                      {isRecommended
                        ? "Choose Recommended Plan"
                        : "Ask About This Plan"}

                      <ArrowRight className="size-5" aria-hidden="true" />
                    </Link>
                  </div>
                </article>
              );
            })}
          </div>

          <div className="mt-12 flex gap-4 rounded-3xl border border-[#B9D8F7] bg-gradient-to-r from-[#EAF4FD] to-[#F8FAFC] p-6 shadow-sm">
            <div className="flex size-11 shrink-0 items-center justify-center rounded-xl bg-[#1976D2] text-white">
              <Info className="size-5" aria-hidden="true" />
            </div>

            <div>
              <h3 className="font-extrabold text-[#0B2545]">
                Preliminary pricing notice
              </h3>

              <p className="mt-2 leading-7 text-slate-700">
                These prices are preliminary starting rates for website
                development. Parts, equipment, extended labor, travel outside
                the normal service area, and specialized work may require an
                additional estimate.
              </p>
            </div>
          </div>
        </div>
      </section>

      {/* =========================================================
          PRICING CTA
          ========================================================= */}
      <section className="relative isolate overflow-hidden bg-gradient-to-br from-[#D4AF37] via-[#C59F2F] to-[#9F7A16] py-16 text-white sm:py-20">
        <div
          aria-hidden="true"
          className="absolute inset-0 -z-10 bg-gradient-to-r from-[#0B2545]/15 via-transparent to-[#0B2545]/25"
        />

        <div
          aria-hidden="true"
          className="absolute -bottom-24 right-0 -z-10 h-80 w-80 rounded-full bg-[#D4AF37]/15 blur-3xl"
        />

        <div className="mx-auto max-w-4xl px-4 text-center sm:px-6 lg:px-8">
          <p className="text-sm font-extrabold uppercase tracking-[0.2em] text-[#0B2545]">
            Need Help Choosing?
          </p>

          <h2 className="mt-4 text-3xl font-black tracking-tight sm:text-4xl">
            Let us help you choose the right support option
          </h2>

          <p className="mx-auto mt-5 max-w-2xl text-lg leading-8 text-[#EAF4FD]">
            Tell us about your devices, technology needs, and support concerns.
            We will recommend the most practical service or plan.
          </p>

          <div className="mt-8 flex flex-col justify-center gap-3 sm:flex-row">
            <Link
              to="/book"
              className="inline-flex min-h-13 items-center justify-center gap-2 rounded-xl bg-[#1976D2] px-6 py-3.5 font-extrabold !text-white transition hover:bg-[#1565C0] hover:!text-white"
            >
              Book a Service
              <ArrowRight className="size-5" aria-hidden="true" />
            </Link>

            <Link
              to="/contact"
              className="inline-flex min-h-13 items-center justify-center rounded-xl border border-[#D4AF37] bg-[#D4AF37] px-6 py-3.5 font-extrabold !text-[#0B2545] transition hover:bg-[#E0BE4C] hover:!text-[#0B2545]"
            >
              Contact Us
            </Link>
          </div>
        </div>
      </section>
    </>
  );
}
