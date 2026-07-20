/**
 * ================================================================
 * ROMELT TECHCARE — ABOUT PAGE
 * ================================================================
 *
 * Purpose:
 * Introduces the business, its mission, values, and service approach.
 *
 * Responsibilities:
 * - Builds trust with prospective customers.
 * - Explains why Romelt TechCare was created.
 * - Communicates the company's customer-service values.
 *
 * Real-data integration:
 * Founder biography, certifications, company milestones, team
 * profiles, testimonials, and community partnerships can be added
 * from the backend later.
 * ================================================================
 */

import {
  BadgeCheck,
  HeartHandshake,
  Lightbulb,
  ShieldCheck,
  Users,
} from "lucide-react";
import { Link } from "react-router";

const values = [
  {
    title: "Honest Service",
    description:
      "Customers receive clear explanations and recommendations based on their actual needs.",
    icon: BadgeCheck,
  },
  {
    title: "Practical Solutions",
    description:
      "We focus on solving the problem without adding unnecessary complexity or expense.",
    icon: Lightbulb,
  },
  {
    title: "Respectful Support",
    description:
      "Every customer deserves patience, professionalism, and technology guidance they can understand.",
    icon: HeartHandshake,
  },
  {
    title: "Dependable Care",
    description:
      "We aim to become a trusted long-term technology partner for homes and small organizations.",
    icon: ShieldCheck,
  },
];

export function AboutPage() {
  return (
    <>
      <section className="border-b border-slate-200 bg-white">
        <div className="mx-auto grid max-w-7xl gap-12 px-4 py-16 sm:px-6 sm:py-20 lg:grid-cols-2 lg:items-center lg:px-8">
          <div>
            <p className="text-sm font-extrabold uppercase tracking-[0.2em] text-blue-700">
              About Romelt TechCare
            </p>

            <h1 className="mt-4 text-4xl font-black tracking-tight text-slate-950 sm:text-5xl">
              Technology support built around trust, clarity, and care
            </h1>

            <p className="mt-6 text-lg leading-8 text-slate-600">
              Romelt TechCare was created to make dependable technology support
              more accessible to individuals, families, professionals, small
              businesses, churches, and nonprofit organizations.
            </p>

            <p className="mt-5 leading-8 text-slate-600">
              Too many people feel frustrated, confused, or uncertain when
              technology stops working. Our goal is to remove that stress by
              explaining problems clearly, offering honest recommendations, and
              providing practical support.
            </p>

            <Link
              to="/book"
              className="mt-8 inline-flex min-h-12 items-center justify-center rounded-xl bg-blue-700 px-6 py-3 font-bold text-white transition hover:bg-blue-800"
            >
              Work With Romelt TechCare
            </Link>
          </div>

          <div className="rounded-3xl bg-slate-950 p-6 text-white shadow-xl sm:p-8">
            <div className="flex size-14 items-center justify-center rounded-2xl bg-blue-600">
              <Users className="size-7" aria-hidden="true" />
            </div>

            <h2 className="mt-8 text-3xl font-black">Our mission</h2>

            <p className="mt-5 text-lg leading-8 text-slate-300">
              To provide reliable, understandable, and honest technology care
              that helps customers work, communicate, learn, and live with
              greater confidence.
            </p>

            <div className="mt-8 border-t border-slate-800 pt-8">
              <p className="text-sm font-bold uppercase tracking-[0.2em] text-blue-400">
                Our promise
              </p>

              <p className="mt-3 text-xl font-extrabold">
                Hassle-Free Technology. Honest Service.
              </p>
              <p className="mt-4 text-sm font-bold">
                ... because you love it, we care for it.
              </p>
            </div>
          </div>
        </div>
      </section>

      <section className="bg-slate-50 py-16 sm:py-20">
        <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
          <div className="max-w-3xl">
            <p className="text-sm font-extrabold uppercase tracking-[0.2em] text-blue-700">
              Our Values
            </p>

            <h2 className="mt-4 text-3xl font-black tracking-tight text-slate-950 sm:text-4xl">
              The principles behind every service
            </h2>
          </div>

          <div className="mt-10 grid gap-6 md:grid-cols-2">
            {values.map((value) => {
              const Icon = value.icon;

              return (
                <article
                  key={value.title}
                  className="rounded-2xl border border-slate-200 bg-white p-6"
                >
                  <div className="flex size-12 items-center justify-center rounded-xl bg-blue-50 text-blue-700">
                    <Icon className="size-6" aria-hidden="true" />
                  </div>

                  <h3 className="mt-5 text-xl font-extrabold text-slate-950">
                    {value.title}
                  </h3>

                  <p className="mt-3 leading-7 text-slate-600">
                    {value.description}
                  </p>
                </article>
              );
            })}
          </div>
        </div>
      </section>
    </>
  );
}
