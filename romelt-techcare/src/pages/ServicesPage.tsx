/**
 * ================================================================
 * ROMELT TECHCARE — SERVICES PAGE
 * ================================================================
 *
 * Purpose:
 * Presents the technology services offered by Romelt TechCare.
 *
 * Responsibilities:
 * - Explains major service categories.
 * - Helps customers identify the support they need.
 * - Directs customers to the booking process.
 *
 * Real-data integration:
 * Service names, descriptions, pricing, availability, service areas,
 * and active status will later be loaded from the backend.
 * ================================================================
 */

import {
  ArrowRight,
  BriefcaseBusiness,
  CheckCircle2,
  Computer,
  HardDrive,
  Headphones,
  Laptop,
  Network,
  Printer,
  ShieldCheck,
  Smartphone,
  Wifi,
} from "lucide-react";
import { Link } from "react-router";

const services = [
  {
    title: "Computer Troubleshooting",
    description:
      "Diagnosis and support for slow computers, startup problems, software errors, freezing, crashes, and general performance issues.",
    icon: Laptop,
    features: [
      "Performance troubleshooting",
      "Software error diagnosis",
      "Startup problem support",
      "System optimization",
    ],
  },
  {
    title: "Wi-Fi and Networking",
    description:
      "Setup and troubleshooting for home networks, small business networks, routers, access points, and unreliable internet connections.",
    icon: Wifi,
    features: [
      "Router and Wi-Fi setup",
      "Connectivity troubleshooting",
      "Network performance review",
      "Device connection support",
    ],
  },
  {
    title: "Device Setup",
    description:
      "Professional setup and configuration for computers, printers, monitors, routers, mobile devices, and other connected technology.",
    icon: Computer,
    features: [
      "New computer setup",
      "Printer installation",
      "Monitor and accessory setup",
      "Device account configuration",
    ],
  },
  {
    title: "Remote Technology Support",
    description:
      "Convenient remote assistance for software problems, configuration, guidance, updates, and everyday technology questions.",
    icon: Headphones,
    features: [
      "Remote troubleshooting",
      "Software installation",
      "Email setup",
      "Technology guidance",
    ],
  },
  {
    title: "Small Business IT Support",
    description:
      "Flexible technology support for small businesses, churches, nonprofits, and organizations without a full internal IT department.",
    icon: BriefcaseBusiness,
    features: [
      "Workstation support",
      "Network assistance",
      "Employee device setup",
      "Ongoing IT guidance",
    ],
  },
  {
    title: "Security and Technology Care",
    description:
      "Preventive maintenance and security support designed to reduce avoidable problems and protect important devices and information.",
    icon: ShieldCheck,
    features: [
      "Security review",
      "Software updates",
      "Backup guidance",
      "Preventive maintenance",
    ],
  },
  {
    title: "Printer Support",
    description:
      "Setup and troubleshooting for home and office printers, scanning features, wireless printing, and printer connectivity.",
    icon: Printer,
    features: [
      "Printer installation",
      "Wireless printing setup",
      "Scanner configuration",
      "Connection troubleshooting",
    ],
  },
  {
    title: "Data and Storage Support",
    description:
      "Support for external drives, file organization, backup planning, storage configuration, and moving information between devices.",
    icon: HardDrive,
    features: [
      "External drive setup",
      "File transfer assistance",
      "Storage organization",
      "Backup planning",
    ],
  },
  {
    title: "Mobile Device Assistance",
    description:
      "Help setting up smartphones and tablets, connecting accounts, installing applications, and configuring essential features.",
    icon: Smartphone,
    features: [
      "Account setup",
      "Application installation",
      "Email configuration",
      "Device connection support",
    ],
  },
];

export function ServicesPage() {
  return (
    <>
      <section className="border-b border-slate-200 bg-white">
        <div className="mx-auto max-w-7xl px-4 py-16 sm:px-6 sm:py-20 lg:px-8">
          <div className="max-w-3xl">
            <p className="text-sm font-extrabold uppercase tracking-[0.2em] text-blue-700">
              Technology Services
            </p>

            <h1 className="mt-4 text-4xl font-black tracking-tight text-slate-950 sm:text-5xl">
              Practical technology support for home and business
            </h1>

            <p className="mt-6 text-lg leading-8 text-slate-600">
              Romelt TechCare provides dependable assistance for computers,
              networks, Wi-Fi, printers, devices, security, and everyday
              technology challenges.
            </p>

            <div className="mt-8 flex flex-col gap-3 sm:flex-row">
              <Link
                to="/book"
                className="inline-flex min-h-12 items-center justify-center gap-2 rounded-xl bg-blue-700 px-6 py-3 font-bold text-white transition hover:bg-blue-800"
              >
                Book a Service
                <ArrowRight className="size-5" aria-hidden="true" />
              </Link>

              <Link
                to="/contact"
                className="inline-flex min-h-12 items-center justify-center rounded-xl border border-slate-300 bg-white px-6 py-3 font-bold text-slate-800 transition hover:bg-slate-100"
              >
                Ask a Question
              </Link>
            </div>
          </div>
        </div>
      </section>

      <section className="bg-slate-50 py-16 sm:py-20">
        <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
          <div className="grid gap-6 md:grid-cols-2 xl:grid-cols-3">
            {services.map((service) => {
              const Icon = service.icon;

              return (
                <article
                  key={service.title}
                  className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm"
                >
                  <div className="flex size-12 items-center justify-center rounded-xl bg-blue-50 text-blue-700">
                    <Icon className="size-6" aria-hidden="true" />
                  </div>

                  <h2 className="mt-5 text-xl font-extrabold text-slate-950">
                    {service.title}
                  </h2>

                  <p className="mt-3 leading-7 text-slate-600">
                    {service.description}
                  </p>

                  <ul className="mt-5 space-y-3">
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
                </article>
              );
            })}
          </div>
        </div>
      </section>

      <section className="bg-slate-950 py-16 text-white">
        <div className="mx-auto grid max-w-7xl gap-8 px-4 sm:px-6 lg:grid-cols-2 lg:items-center lg:px-8">
          <div>
            <Network className="size-10 text-blue-400" aria-hidden="true" />

            <h2 className="mt-5 text-3xl font-black">
              Not sure which service you need?
            </h2>

            <p className="mt-4 max-w-2xl leading-8 text-slate-300">
              Describe the problem in your own words. We will review your
              request and help identify the most appropriate next step.
            </p>
          </div>

          <div className="flex flex-col gap-3 sm:flex-row lg:justify-end">
            <Link
              to="/book"
              className="inline-flex min-h-12 items-center justify-center rounded-xl bg-blue-600 px-6 py-3 font-bold text-white transition hover:bg-blue-500"
            >
              Start a Service Request
            </Link>

            <Link
              to="/contact"
              className="inline-flex min-h-12 items-center justify-center rounded-xl border border-slate-700 px-6 py-3 font-bold text-white transition hover:bg-slate-900"
            >
              Contact Romelt TechCare
            </Link>
          </div>
        </div>
      </section>
    </>
  );
}
