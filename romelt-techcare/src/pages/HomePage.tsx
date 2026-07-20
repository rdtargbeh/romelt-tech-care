/**
 * ================================================================
 * ROMELT TECHCARE — HOME PAGE
 * ================================================================
 *
 * Purpose:
 * Introduces Romelt TechCare and converts visitors into customers.
 *
 * Responsibilities:
 * - Communicates the primary business value proposition.
 * - Displays major service categories.
 * - Explains how the service process works.
 * - Builds customer trust.
 * - Directs visitors toward booking or contacting the business.
 *
 * Real-data integration:
 * Services, testimonials, promotions, service areas, availability,
 * business metrics, and pricing can later be loaded from the backend.
 * ================================================================
 */

import {
  ArrowRight,
  BadgeCheck,
  BriefcaseBusiness,
  CheckCircle2,
  Clock3,
  Computer,
  Headphones,
  House,
  Laptop,
  Network,
  ShieldCheck,
  Wifi,
  Wrench,
} from "lucide-react";
import { Link } from "react-router";

const services = [
  {
    title: "Computer Support",
    description:
      "Troubleshooting, setup, optimization, software installation, and everyday computer assistance.",
    icon: Laptop,
  },
  {
    title: "Wi-Fi and Networking",
    description:
      "Home and small business Wi-Fi setup, connectivity troubleshooting, and network improvement.",
    icon: Wifi,
  },
  {
    title: "Device Setup",
    description:
      "Professional setup for computers, printers, monitors, routers, and other connected devices.",
    icon: Computer,
  },
  {
    title: "Remote Support",
    description:
      "Convenient technology help without requiring an on-site appointment whenever remote service is appropriate.",
    icon: Headphones,
  },
  {
    title: "Small Business IT",
    description:
      "Reliable technology support designed for small businesses that do not need a full internal IT department.",
    icon: BriefcaseBusiness,
  },
  {
    title: "Technology Care",
    description:
      "Preventive maintenance, updates, security checks, backups, and ongoing technology guidance.",
    icon: ShieldCheck,
  },
];

const processSteps = [
  {
    number: "01",
    title: "Tell us the problem",
    description:
      "Describe the issue, device, location, and preferred service time.",
  },
  {
    number: "02",
    title: "Receive clear guidance",
    description: "We review the request and explain the recommended next step.",
  },
  {
    number: "03",
    title: "Get dependable support",
    description:
      "Service is completed remotely, on-site, or through an arranged appointment.",
  },
];

export function HomePage() {
  return (
    <>
      <section className="relative overflow-hidden bg-white">
        <div className="absolute inset-x-0 top-0 -z-10 h-96 bg-gradient-to-b from-blue-50 to-transparent" />

        <div className="mx-auto grid max-w-7xl items-center gap-12 px-4 py-16 sm:px-6 sm:py-20 lg:grid-cols-2 lg:px-8 lg:py-28">
          <div>
            <div className="inline-flex items-center gap-2 rounded-full border border-blue-200 bg-blue-50 px-4 py-2 text-sm font-bold text-blue-700">
              <BadgeCheck className="size-4" aria-hidden="true" />
              Honest technology support you can trust
            </div>

            <h1 className="mt-6 max-w-3xl text-4xl font-black tracking-tight text-slate-950 sm:text-5xl lg:text-6xl">
              Technology should make life easier, not more stressful.
            </h1>

            <p className="mt-6 max-w-2xl text-lg leading-8 text-slate-600 sm:text-xl">
              Romelt TechCare provides dependable computer, Wi-Fi, networking,
              device setup, and small business IT support with clear
              communication and honest service.
            </p>

            <div className="mt-8 flex flex-col gap-3 sm:flex-row">
              <Link
                to="/book"
                className="inline-flex min-h-13 items-center justify-center gap-2 rounded-xl bg-blue-700 px-6 py-3.5 font-bold text-white shadow-lg shadow-blue-700/20 transition hover:bg-blue-800"
              >
                Book a Service
                <ArrowRight className="size-5" aria-hidden="true" />
              </Link>

              <Link
                to="/services"
                className="inline-flex min-h-13 items-center justify-center rounded-xl border border-slate-300 bg-white px-6 py-3.5 font-bold text-slate-800 transition hover:bg-slate-100"
              >
                Explore Services
              </Link>
            </div>

            <div className="mt-8 grid gap-3 text-sm font-semibold text-slate-600 sm:grid-cols-3">
              <div className="flex items-center gap-2">
                <CheckCircle2
                  className="size-5 text-green-600"
                  aria-hidden="true"
                />
                Clear explanations
              </div>

              <div className="flex items-center gap-2">
                <CheckCircle2
                  className="size-5 text-green-600"
                  aria-hidden="true"
                />
                Honest recommendations
              </div>

              <div className="flex items-center gap-2">
                <CheckCircle2
                  className="size-5 text-green-600"
                  aria-hidden="true"
                />
                Dependable support
              </div>
            </div>
          </div>

          <div className="relative">
            <div className="rounded-3xl border border-slate-200 bg-slate-950 p-6 shadow-2xl sm:p-8">
              <div className="rounded-2xl bg-gradient-to-br from-blue-600 to-blue-800 p-6 text-white sm:p-8">
                <div className="flex size-14 items-center justify-center rounded-2xl bg-white/15">
                  <Wrench className="size-7" aria-hidden="true" />
                </div>

                <h2 className="mt-8 text-2xl font-extrabold">
                  Help for the technology you depend on every day.
                </h2>

                <p className="mt-4 leading-7 text-blue-100">
                  From slow computers and unreliable Wi-Fi to new device setup
                  and ongoing small business support, Romelt TechCare helps you
                  move forward with confidence.
                </p>
              </div>

              <div className="mt-5 grid gap-4 sm:grid-cols-2">
                <div className="rounded-2xl bg-white p-5">
                  <House className="size-6 text-blue-700" aria-hidden="true" />
                  <h3 className="mt-4 font-extrabold text-slate-950">
                    Home Technology
                  </h3>
                  <p className="mt-2 text-sm leading-6 text-slate-600">
                    Practical support for computers, devices, home offices, and
                    Wi-Fi.
                  </p>
                </div>

                <div className="rounded-2xl bg-white p-5">
                  <BriefcaseBusiness
                    className="size-6 text-blue-700"
                    aria-hidden="true"
                  />
                  <h3 className="mt-4 font-extrabold text-slate-950">
                    Business Technology
                  </h3>
                  <p className="mt-2 text-sm leading-6 text-slate-600">
                    Flexible IT support for small businesses and nonprofit
                    organizations.
                  </p>
                </div>
              </div>
            </div>

            <div className="absolute -bottom-6 -left-4 hidden rounded-2xl border border-slate-200 bg-white p-4 shadow-xl sm:flex sm:items-center sm:gap-3">
              <div className="flex size-11 items-center justify-center rounded-xl bg-green-100 text-green-700">
                <Clock3 className="size-5" aria-hidden="true" />
              </div>

              <div>
                <p className="text-xs font-semibold uppercase tracking-wide text-slate-500">
                  Flexible service
                </p>
                <p className="font-extrabold text-slate-950">
                  Remote and on-site
                </p>
              </div>
            </div>
          </div>
        </div>
      </section>

      <section className="border-y border-slate-200 bg-slate-50 py-16 sm:py-20">
        <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
          <div className="max-w-3xl">
            <p className="text-sm font-extrabold uppercase tracking-[0.2em] text-blue-700">
              Our Services
            </p>

            <h2 className="mt-3 text-3xl font-black tracking-tight text-slate-950 sm:text-4xl">
              Complete technology support for home and business
            </h2>

            <p className="mt-4 text-lg leading-8 text-slate-600">
              Get practical support without confusing explanations, unnecessary
              recommendations, or hidden surprises.
            </p>
          </div>

          <div className="mt-10 grid gap-5 md:grid-cols-2 lg:grid-cols-3">
            {services.map((service) => {
              const Icon = service.icon;

              return (
                <article
                  key={service.title}
                  className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm transition hover:-translate-y-1 hover:shadow-lg"
                >
                  <div className="flex size-12 items-center justify-center rounded-xl bg-blue-50 text-blue-700">
                    <Icon className="size-6" aria-hidden="true" />
                  </div>

                  <h3 className="mt-5 text-xl font-extrabold text-slate-950">
                    {service.title}
                  </h3>

                  <p className="mt-3 leading-7 text-slate-600">
                    {service.description}
                  </p>

                  <Link
                    to="/services"
                    className="mt-5 inline-flex items-center gap-2 font-bold text-blue-700 hover:text-blue-900"
                  >
                    Learn more
                    <ArrowRight className="size-4" aria-hidden="true" />
                  </Link>
                </article>
              );
            })}
          </div>
        </div>
      </section>

      <section className="bg-white py-16 sm:py-20">
        <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
          <div className="grid gap-12 lg:grid-cols-[0.85fr_1.15fr] lg:items-center">
            <div>
              <p className="text-sm font-extrabold uppercase tracking-[0.2em] text-blue-700">
                How It Works
              </p>

              <h2 className="mt-3 text-3xl font-black tracking-tight text-slate-950 sm:text-4xl">
                A simple process from problem to solution
              </h2>

              <p className="mt-5 text-lg leading-8 text-slate-600">
                Technology support should not create more confusion. Our process
                is designed to keep every step clear and manageable.
              </p>

              <div className="mt-7 rounded-2xl border border-blue-100 bg-blue-50 p-5">
                <div className="flex gap-3">
                  <Network
                    className="mt-1 size-6 shrink-0 text-blue-700"
                    aria-hidden="true"
                  />

                  <div>
                    <h3 className="font-extrabold text-slate-950">
                      Support built around your needs
                    </h3>

                    <p className="mt-2 leading-7 text-slate-600">
                      Recommendations are based on the actual problem, your
                      priorities, and your budget.
                    </p>
                  </div>
                </div>
              </div>
            </div>

            <div className="space-y-4">
              {processSteps.map((step) => (
                <article
                  key={step.number}
                  className="flex gap-5 rounded-2xl border border-slate-200 bg-slate-50 p-5 sm:p-6"
                >
                  <div className="flex size-12 shrink-0 items-center justify-center rounded-xl bg-slate-950 text-sm font-black text-white">
                    {step.number}
                  </div>

                  <div>
                    <h3 className="text-lg font-extrabold text-slate-950">
                      {step.title}
                    </h3>

                    <p className="mt-2 leading-7 text-slate-600">
                      {step.description}
                    </p>
                  </div>
                </article>
              ))}
            </div>
          </div>
        </div>
      </section>

      <section className="bg-blue-700 py-16 text-white sm:py-20">
        <div className="mx-auto max-w-5xl px-4 text-center sm:px-6 lg:px-8">
          <ShieldCheck
            className="mx-auto size-12 text-blue-200"
            aria-hidden="true"
          />

          <h2 className="mt-6 text-3xl font-black tracking-tight sm:text-4xl">
            Get dependable help with your technology
          </h2>

          <p className="mx-auto mt-5 max-w-2xl text-lg leading-8 text-blue-100">
            Tell us what is not working, what you need configured, or where your
            technology is slowing you down.
          </p>

          <div className="mt-8 flex flex-col justify-center gap-3 sm:flex-row">
            <Link
              to="/book"
              className="inline-flex min-h-13 items-center justify-center gap-2 rounded-xl bg-white px-6 py-3.5 font-bold text-blue-800 transition hover:bg-blue-50"
            >
              Book a Service
              <ArrowRight className="size-5" aria-hidden="true" />
            </Link>

            <Link
              to="/contact"
              className="inline-flex min-h-13 items-center justify-center rounded-xl border border-blue-400 px-6 py-3.5 font-bold text-white transition hover:bg-blue-800"
            >
              Contact Us
            </Link>
          </div>
        </div>
      </section>
    </>
  );
}
