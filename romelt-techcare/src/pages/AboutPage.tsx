/**
 * ================================================================
 * ROMELT TECHCARE — ABOUT PAGE
 * ================================================================
 *
 * Purpose:
 * Introduces Romelt TechCare, its mission, values, and customer-
 * focused service approach.
 *
 * Responsibilities:
 * - Builds trust with prospective customers.
 * - Explains why Romelt TechCare was created.
 * - Communicates the company mission and service promise.
 * - Presents the values that guide every customer interaction.
 * - Directs visitors toward booking a service.
 *
 * Brand palette:
 * - Primary Blue: #1976D2
 * - Deep Navy: #0B2545
 * - Premium Gold: #D4AF37
 * - Crimson Accent: #C62828
 *
 * Real-data integration:
 * Founder biography, certifications, company milestones, team
 * profiles, testimonials, and community partnerships can later be
 * loaded from the Spring Boot backend.
 * ================================================================
 */

import {
  ArrowRight,
  BadgeCheck,
  CheckCircle2,
  HeartHandshake,
  Lightbulb,
  ShieldCheck,
  Sparkles,
  Users,
} from "lucide-react";
import { Link } from "react-router";

const values = [
  {
    title: "Honest Service",
    description:
      "Customers receive clear explanations and recommendations based on their actual needs.",
    icon: BadgeCheck,
    cardClass: "border-[#B9D8F7] bg-gradient-to-br from-white to-[#EAF4FD]",
    iconClass: "bg-[#EAF4FD] text-[#1976D2]",
  },
  {
    title: "Practical Solutions",
    description:
      "We focus on solving the problem without adding unnecessary complexity or expense.",
    icon: Lightbulb,
    cardClass: "border-[#EADBA3] bg-gradient-to-br from-white to-[#FFF8E1]",
    iconClass: "bg-[#FFF3C4] text-[#A67C00]",
  },
  {
    title: "Respectful Support",
    description:
      "Every customer deserves patience, professionalism, and technology guidance they can understand.",
    icon: HeartHandshake,
    cardClass: "border-[#F3C7C7] bg-gradient-to-br from-white to-[#FDECEC]",
    iconClass: "bg-[#FDECEC] text-[#C62828]",
  },
  {
    title: "Dependable Care",
    description:
      "We aim to become a trusted long-term technology partner for homes and small organizations.",
    icon: ShieldCheck,
    cardClass: "border-[#C9D9EA] bg-gradient-to-br from-white to-[#EEF5FB]",
    iconClass: "bg-[#E4EFF8] text-[#0B2545]",
  },
];

const trustPoints = [
  "Clear, understandable communication",
  "Recommendations based on real needs",
  "Respect for your time and budget",
  "Dependable remote and on-site support",
];

export function AboutPage() {
  return (
    <>
      {/* =========================================================
          ABOUT HERO
          ========================================================= */}
      <section className="relative isolate overflow-hidden bg-[#F8FAFC]">
        <div
          aria-hidden="true"
          className="absolute left-0 top-0 -z-10 h-full w-2 bg-gradient-to-b from-[#1976D2] via-[#D4AF37] to-[#C62828]"
        />

        <div
          aria-hidden="true"
          className="absolute -left-32 -top-32 -z-10 h-96 w-96 rounded-full bg-[#1976D2]/12 blur-3xl"
        />

        <div
          aria-hidden="true"
          className="absolute -bottom-36 right-0 -z-10 h-96 w-96 rounded-full bg-[#D4AF37]/12 blur-3xl"
        />

        <div className="mx-auto grid max-w-7xl items-center gap-12 px-4 py-16 sm:px-6 sm:py-20 lg:grid-cols-[1fr_0.95fr] lg:px-8 lg:py-24">
          <div className="max-w-3xl">
            <div className="inline-flex items-center gap-2 rounded-full border border-[#B9D8F7] bg-[#EAF4FD] px-4 py-2 text-sm font-extrabold uppercase tracking-[0.16em] text-[#1976D2]">
              <Sparkles className="size-4" aria-hidden="true" />
              About Romelt TechCare
            </div>

            <h1 className="mt-6 text-4xl font-black leading-[1.08] tracking-tight text-[#0B2545] sm:text-5xl lg:text-6xl">
              Technology support built around{" "}
              <span className="relative inline-block text-[#1976D2]">
                trust, clarity, and care
                <span
                  aria-hidden="true"
                  className="absolute -bottom-2 left-0 h-1.5 w-full rounded-full bg-[#D4AF37]"
                />
              </span>
            </h1>

            <p className="mt-7 text-lg leading-8 text-slate-600 sm:text-xl">
              Romelt TechCare was created to make dependable technology support
              more accessible to individuals, families, professionals, small
              businesses, churches, and nonprofit organizations.
            </p>

            <p className="mt-5 max-w-2xl leading-8 text-slate-600">
              Too many people feel frustrated, confused, or uncertain when
              technology stops working. Our goal is to remove that stress by
              explaining problems clearly, offering honest recommendations, and
              providing practical support.
            </p>

            <div className="mt-8 flex flex-col gap-3 sm:flex-row">
              <Link
                to="/book"
                className="inline-flex min-h-14 items-center justify-center gap-2 rounded-xl bg-[#1976D2] px-7 py-3.5 font-extrabold !text-white shadow-xl shadow-[#1976D2]/20 transition duration-300 hover:-translate-y-0.5 hover:bg-[#1565C0] hover:!text-white"
              >
                Work With Romelt TechCare
                <ArrowRight className="size-5" aria-hidden="true" />
              </Link>

              <Link
                to="/contact"
                className="inline-flex min-h-14 items-center justify-center rounded-xl border border-[#0B2545]/20 bg-white px-7 py-3.5 font-extrabold !text-[#0B2545] shadow-sm transition duration-300 hover:-translate-y-0.5 hover:border-[#1976D2] hover:bg-[#EAF4FD] hover:!text-[#0B2545]"
              >
                Contact Us
              </Link>
            </div>
          </div>

          {/* Mission card */}
          <div className="relative">
            <div
              aria-hidden="true"
              className="absolute -right-5 -top-5 h-full w-full rounded-[2rem] border-2 border-[#D4AF37]"
            />

            <div className="relative overflow-hidden rounded-[2rem] bg-gradient-to-br from-[#1976D2] via-[#155FA9] to-[#0B2545] p-7 text-white shadow-2xl shadow-[#0B2545]/25 sm:p-9">
              <div
                aria-hidden="true"
                className="absolute -right-20 -top-20 h-64 w-64 rounded-full border-[45px] border-white/5"
              />

              <div
                aria-hidden="true"
                className="absolute -bottom-28 -left-20 h-72 w-72 rounded-full bg-[#D4AF37]/10 blur-3xl"
              />

              <div className="relative">
                <div className="flex size-16 items-center justify-center rounded-2xl border border-white/15 bg-white/10 text-[#D4AF37] shadow-lg backdrop-blur-sm">
                  <Users className="size-8" aria-hidden="true" />
                </div>

                <p className="mt-8 text-sm font-extrabold uppercase tracking-[0.2em] text-[#D4AF37]">
                  Our Mission
                </p>

                <h2 className="mt-3 text-3xl font-black tracking-tight">
                  Helping people use technology with greater confidence
                </h2>

                <p className="mt-5 text-lg leading-8 text-[#EAF4FD]">
                  To provide reliable, understandable, and honest technology
                  care that helps customers work, communicate, learn, and live
                  with greater confidence.
                </p>

                <div className="mt-8 border-t border-white/15 pt-7">
                  <p className="text-sm font-extrabold uppercase tracking-[0.2em] text-[#D4AF37]">
                    Our Promise
                  </p>

                  <p className="mt-3 text-xl font-extrabold text-white">
                    Hassle-Free Technology. Honest Service.
                  </p>

                  <p className="mt-4 text-sm font-bold text-[#EAF4FD]">
                    Because you love it, we care for it.
                  </p>
                </div>
              </div>
            </div>

            <div className="absolute -bottom-6 -left-5 hidden rounded-2xl border border-slate-200 bg-white p-4 shadow-xl sm:flex sm:items-center sm:gap-3">
              <div className="flex size-11 items-center justify-center rounded-xl bg-[#FDECEC] text-[#C62828]">
                <HeartHandshake className="size-5" aria-hidden="true" />
              </div>

              <div>
                <p className="text-xs font-bold uppercase tracking-wide text-slate-500">
                  Customer First
                </p>

                <p className="font-extrabold text-[#0B2545]">
                  Technology support with care
                </p>
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* =========================================================
          WHY WE EXIST
          ========================================================= */}
      <section className="bg-white py-16 sm:py-20 lg:py-24">
        <div className="mx-auto grid max-w-7xl gap-12 px-4 sm:px-6 lg:grid-cols-[0.9fr_1.1fr] lg:items-center lg:px-8">
          <div>
            <p className="text-sm font-extrabold uppercase tracking-[0.2em] text-[#1976D2]">
              Why We Exist
            </p>

            <h2 className="mt-4 text-3xl font-black tracking-tight text-[#0B2545] sm:text-4xl lg:text-5xl">
              Technology should support your life—not become another source of
              stress
            </h2>

            <p className="mt-5 text-lg leading-8 text-slate-600">
              Romelt TechCare exists to close the gap between complex technology
              problems and the clear, patient support people need.
            </p>

            <p className="mt-4 leading-8 text-slate-600">
              Whether a customer needs help with one device, a home network, or
              ongoing support for a small organization, the same principles
              apply: listen carefully, explain clearly, and recommend only what
              is genuinely useful.
            </p>
          </div>

          <div className="grid gap-4 sm:grid-cols-2">
            {trustPoints.map((point, index) => (
              <div
                key={point}
                className="flex items-start gap-4 rounded-2xl border border-slate-200 bg-[#F8FAFC] p-5 transition duration-300 hover:-translate-y-1 hover:border-[#1976D2]/30 hover:bg-white hover:shadow-lg"
              >
                <div
                  className={[
                    "flex size-11 shrink-0 items-center justify-center rounded-xl",
                    index === 0
                      ? "bg-[#EAF4FD] text-[#1976D2]"
                      : index === 1
                        ? "bg-[#FFF8E1] text-[#A67C00]"
                        : index === 2
                          ? "bg-[#FDECEC] text-[#C62828]"
                          : "bg-[#E4EFF8] text-[#0B2545]",
                  ].join(" ")}
                >
                  <CheckCircle2 className="size-5" aria-hidden="true" />
                </div>

                <p className="pt-2 font-extrabold leading-6 text-[#0B2545]">
                  {point}
                </p>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* =========================================================
          VALUES
          ========================================================= */}
      <section className="relative overflow-hidden bg-[#F8FAFC] py-16 sm:py-20 lg:py-24">
        <div
          aria-hidden="true"
          className="absolute -right-40 top-16 h-96 w-96 rounded-full bg-[#1976D2]/8 blur-3xl"
        />

        <div
          aria-hidden="true"
          className="absolute -left-40 bottom-0 h-96 w-96 rounded-full bg-[#D4AF37]/10 blur-3xl"
        />

        <div className="relative mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
          <div className="max-w-3xl">
            <p className="text-sm font-extrabold uppercase tracking-[0.2em] text-[#1976D2]">
              Our Values
            </p>

            <h2 className="mt-4 text-3xl font-black tracking-tight text-[#0B2545] sm:text-4xl lg:text-5xl">
              The principles behind every service
            </h2>

            <p className="mt-5 text-lg leading-8 text-slate-600">
              These values shape how we communicate, make recommendations, and
              support every customer.
            </p>
          </div>

          <div className="mt-12 grid gap-6 md:grid-cols-2">
            {values.map((value, index) => {
              const Icon = value.icon;

              return (
                <article
                  key={value.title}
                  className={`group relative overflow-hidden rounded-3xl border p-7 shadow-sm transition duration-300 hover:-translate-y-2 hover:shadow-2xl hover:shadow-[#0B2545]/10 ${value.cardClass}`}
                >
                  <div
                    aria-hidden="true"
                    className="absolute right-0 top-0 h-28 w-28 rounded-bl-[5rem] bg-white/45 transition duration-300 group-hover:scale-125"
                  />

                  <div className="relative">
                    <div className="flex items-center justify-between">
                      <div
                        className={`flex size-14 items-center justify-center rounded-2xl transition duration-300 group-hover:scale-105 ${value.iconClass}`}
                      >
                        <Icon className="size-7" aria-hidden="true" />
                      </div>

                      <span className="text-sm font-black text-slate-300">
                        {String(index + 1).padStart(2, "0")}
                      </span>
                    </div>

                    <h3 className="mt-6 text-2xl font-extrabold text-[#0B2545]">
                      {value.title}
                    </h3>

                    <p className="mt-4 leading-7 text-slate-600">
                      {value.description}
                    </p>
                  </div>
                </article>
              );
            })}
          </div>
        </div>
      </section>

      {/* =========================================================
          ABOUT CTA
          ========================================================= */}
      <section className="relative isolate overflow-hidden bg-gradient-to-br from-[#D4AF37] via-[#1771cc] to-[#D4AF37] py-16 text-white sm:py-20">
        <div
          aria-hidden="true"
          className="absolute inset-0 -z-10 bg-gradient-to-r from-[#0B2545]/15 via-transparent to-[#0B2545]/25"
        />

        <div
          aria-hidden="true"
          className="absolute -left-24 -top-24 -z-10 h-80 w-80 rounded-full bg-white/10 blur-3xl"
        />

        <div
          aria-hidden="true"
          className="absolute -bottom-32 right-0 -z-10 h-96 w-96 rounded-full bg-[#0B2545]/20 blur-3xl"
        />

        <div className="mx-auto max-w-5xl px-4 text-center sm:px-6 lg:px-8">
          <div className="mx-auto flex size-16 items-center justify-center rounded-2xl border border-white/20 bg-white/10 shadow-xl backdrop-blur-sm">
            <HeartHandshake className="size-8 text-white" aria-hidden="true" />
          </div>

          <h2 className="mt-7 text-3xl font-black tracking-tight sm:text-4xl lg:text-5xl">
            Technology help should feel clear, respectful, and dependable
          </h2>

          <p className="mx-auto mt-5 max-w-2xl text-lg leading-8 text-white/90">
            Tell us what is causing frustration or slowing you down. We will
            listen, explain your options, and help you determine the most
            practical next step.
          </p>

          <div className="mt-9 flex flex-col justify-center gap-3 sm:flex-row">
            <Link
              to="/book"
              className="inline-flex min-h-14 items-center justify-center gap-2 rounded-xl bg-[#1976D2] px-7 py-3.5 font-extrabold !text-white shadow-xl transition hover:-translate-y-0.5 hover:bg-[#1565C0] hover:!text-white"
            >
              Book a Service
              <ArrowRight className="size-5" aria-hidden="true" />
            </Link>

            <Link
              to="/contact"
              className="inline-flex min-h-14 items-center justify-center rounded-xl border border-[#0B2545] bg-[#0B2545] px-7 py-3.5 font-extrabold !text-white shadow-xl transition hover:-translate-y-0.5 hover:bg-[#12365F] hover:!text-white"
            >
              Contact Romelt TechCare
            </Link>
          </div>
        </div>
      </section>
    </>
  );
}
