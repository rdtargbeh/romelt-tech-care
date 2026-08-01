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
 * - Presents all services with equal importance.
 * - Directs customers to the booking process.
 *
 * Brand palette:
 * - Primary Blue: #1976D2
 * - Deep Navy: #0B2545
 * - Premium Gold: #D4AF37
 * - Crimson Accent: #C62828
 *
 * Hero image:
 * public/image/image-4.jpg
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
  House,
  Laptop,
  Network,
  Printer,
  ShieldCheck,
  Smartphone,
  Sparkles,
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
    cardClass: "border-[#B9D8F7] bg-gradient-to-br from-white to-[#EAF4FD]",
    iconClass: "bg-[#EAF4FD] text-[#1976D2]",
    checkClass: "text-[#1976D2]",
  },
  {
    title: "Wi-Fi and Networking",
    description:
      "Setup and troubleshooting for home networks, small-business networks, routers, access points, and unreliable internet connections.",
    icon: Wifi,
    features: [
      "Router and Wi-Fi setup",
      "Connectivity troubleshooting",
      "Network performance review",
      "Device connection support",
    ],
    cardClass: "border-[#D5E7FA] bg-gradient-to-br from-white to-[#F4F9FE]",
    iconClass: "bg-[#EAF4FD] text-[#1976D2]",
    checkClass: "text-[#1976D2]",
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
    cardClass: "border-[#EADBA3] bg-gradient-to-br from-white to-[#FFF8E1]",
    iconClass: "bg-[#FFF3C4] text-[#A67C00]",
    checkClass: "text-[#B68E13]",
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
    cardClass: "border-[#D7DDF7] bg-gradient-to-br from-white to-[#F1F4FF]",
    iconClass: "bg-[#E8EDFF] text-[#315BC7]",
    checkClass: "text-[#315BC7]",
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
    cardClass: "border-[#C9D9EA] bg-gradient-to-br from-white to-[#EEF5FB]",
    iconClass: "bg-[#E4EFF8] text-[#0B2545]",
    checkClass: "text-[#1976D2]",
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
    cardClass: "border-[#F3C7C7] bg-gradient-to-br from-white to-[#FDECEC]",
    iconClass: "bg-[#FDECEC] text-[#C62828]",
    checkClass: "text-[#C62828]",
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
    cardClass: "border-[#CBE8E4] bg-gradient-to-br from-white to-[#EEF9F7]",
    iconClass: "bg-[#E3F6F2] text-[#138A78]",
    checkClass: "text-[#138A78]",
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
    cardClass: "border-[#DDD3F0] bg-gradient-to-br from-white to-[#F6F1FC]",
    iconClass: "bg-[#F0E8FA] text-[#7450A6]",
    checkClass: "text-[#7450A6]",
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
    cardClass: "border-[#F1D1C2] bg-gradient-to-br from-white to-[#FFF3ED]",
    iconClass: "bg-[#FFE9DE] text-[#C85E2E]",
    checkClass: "text-[#C85E2E]",
  },
];

export function ServicesPage() {
  return (
    <>
      {/* =========================================================
    SERVICES HERO — SPLIT IMAGE LAYOUT
    ========================================================= */}
      <section className="relative overflow-hidden bg-[#F8FAFC]">
        <div
          aria-hidden="true"
          className="absolute left-0 top-0 h-full w-2 bg-gradient-to-b from-[#1976D2] via-[#D4AF37] to-[#C62828]"
        />

        <div
          aria-hidden="true"
          className="absolute -left-32 -top-32 h-80 w-80 rounded-full bg-[#1976D2]/10 blur-3xl"
        />

        <div
          aria-hidden="true"
          className="absolute bottom-0 right-1/3 h-64 w-64 rounded-full bg-[#D4AF37]/10 blur-3xl"
        />

        <div className="relative mx-auto grid max-w-7xl items-center gap-12 px-4 py-14 sm:px-6 sm:py-18 lg:grid-cols-[0.9fr_1.1fr] lg:px-8 lg:py-20">
          {/* Hero content */}
          <div className="max-w-2xl">
            <div className="inline-flex items-center gap-2 rounded-full border border-[#B9D8F7] bg-[#EAF4FD] px-4 py-2 text-sm font-extrabold uppercase tracking-[0.16em] text-[#1976D2]">
              <Sparkles className="size-4" aria-hidden="true" />
              Technology Services
            </div>

            <h1 className="mt-6 text-4xl font-black leading-[1.08] tracking-tight text-[#0B2545] sm:text-5xl lg:text-6xl">
              The right support for every{" "}
              <span className="relative inline-block text-[#1976D2]">
                technology need
                <span
                  aria-hidden="true"
                  className="absolute -bottom-2 left-0 h-1.5 w-full rounded-full bg-[#D4AF37]"
                />
              </span>
            </h1>

            <p className="mt-7 text-lg leading-8 text-slate-600 sm:text-xl">
              From computer problems and unreliable Wi-Fi to device setup,
              security, and small-business IT, Romelt TechCare provides
              practical support designed around your actual needs.
            </p>

            <div className="mt-8 flex flex-col gap-3 sm:flex-row">
              <Link
                to="/book"
                className="inline-flex min-h-14 items-center justify-center gap-2 rounded-xl bg-[#1976D2] px-7 py-3.5 font-extrabold !text-white shadow-xl shadow-[#1976D2]/20 transition duration-300 hover:-translate-y-0.5 hover:bg-[#1565C0] hover:!text-white"
              >
                Book a Service
                <ArrowRight className="size-5" aria-hidden="true" />
              </Link>

              <Link
                to="/contact"
                className="inline-flex min-h-14 items-center justify-center rounded-xl border border-[#0B2545]/20 bg-white px-7 py-3.5 font-extrabold !text-[#0B2545] shadow-sm transition duration-300 hover:-translate-y-0.5 hover:border-[#1976D2] hover:bg-[#EAF4FD] hover:!text-[#0B2545]"
              >
                Ask a Question
              </Link>
            </div>

            <div className="mt-9 grid gap-4 sm:grid-cols-2">
              <div className="flex items-start gap-3">
                <div className="flex size-10 shrink-0 items-center justify-center rounded-xl bg-[#EAF4FD] text-[#1976D2]">
                  <House className="size-5" aria-hidden="true" />
                </div>

                <div>
                  <p className="font-extrabold text-[#0B2545]">
                    Home Technology
                  </p>
                  <p className="mt-1 text-sm leading-6 text-slate-500">
                    Support for computers, devices, Wi-Fi, and home offices.
                  </p>
                </div>
              </div>

              <div className="flex items-start gap-3">
                <div className="flex size-10 shrink-0 items-center justify-center rounded-xl bg-[#FFF8E1] text-[#B68E13]">
                  <BriefcaseBusiness className="size-5" aria-hidden="true" />
                </div>

                <div>
                  <p className="font-extrabold text-[#0B2545]">
                    Business Technology
                  </p>
                  <p className="mt-1 text-sm leading-6 text-slate-500">
                    Flexible IT assistance for small organizations.
                  </p>
                </div>
              </div>
            </div>
          </div>

          {/* Hero image */}
          <div className="relative">
            <div
              aria-hidden="true"
              className="absolute -right-5 -top-5 h-full w-full rounded-[2rem] border-2 border-[#D4AF37]"
            />

            <div className="relative overflow-hidden rounded-[2rem] bg-[#0B2545] p-2 shadow-2xl shadow-[#0B2545]/20">
              <div className="relative min-h-[420px] overflow-hidden rounded-[1.65rem] sm:min-h-[500px]">
                <img
                  src="/image/image-4.jpg"
                  alt="Technology professional providing technical services"
                  className="absolute inset-0 h-full w-full object-cover object-center transition duration-700 hover:scale-105"
                />

                <div className="absolute inset-0 bg-gradient-to-t from-[#0B2545]/80 via-transparent to-transparent" />

                <div className="absolute bottom-0 left-0 right-0 p-6 sm:p-8">
                  <div className="max-w-md rounded-2xl border border-white/20 bg-[#0B2545]/85 p-5 text-white shadow-xl backdrop-blur-md">
                    <p className="text-xs font-extrabold uppercase tracking-[0.16em] text-[#D4AF37]">
                      Complete Technology Care
                    </p>

                    <p className="mt-2 text-lg font-extrabold">
                      Clear answers. Practical solutions. Dependable support.
                    </p>
                  </div>
                </div>
              </div>
            </div>

            <div className="absolute -bottom-6 -left-5 hidden rounded-2xl border border-slate-200 bg-white p-4 shadow-xl sm:flex sm:items-center sm:gap-3">
              <div className="flex size-11 items-center justify-center rounded-xl bg-[#FDECEC] text-[#C62828]">
                <ShieldCheck className="size-5" aria-hidden="true" />
              </div>

              <div>
                <p className="text-xs font-bold uppercase tracking-wide text-slate-500">
                  Trusted Approach
                </p>
                <p className="font-extrabold text-[#0B2545]">
                  Honest recommendations
                </p>
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* =========================================================
          SERVICE CARDS
          ========================================================= */}
      <section className="relative overflow-hidden bg-[#F8FAFC] py-16 sm:py-20 lg:py-24">
        <div
          aria-hidden="true"
          className="absolute -right-40 top-24 h-96 w-96 rounded-full bg-[#1976D2]/8 blur-3xl"
        />

        <div
          aria-hidden="true"
          className="absolute -left-40 bottom-0 h-96 w-96 rounded-full bg-[#D4AF37]/10 blur-3xl"
        />

        <div className="relative mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
          <div className="mb-12 max-w-3xl">
            <p className="text-sm font-extrabold uppercase tracking-[0.2em] text-[#1976D2]">
              Complete Support Options
            </p>

            <h2 className="mt-3 text-3xl font-black tracking-tight text-[#0B2545] sm:text-4xl">
              Find the right technology service for your needs
            </h2>

            <p className="mt-4 text-lg leading-8 text-slate-600">
              Choose from flexible home, remote, on-site, and small-business
              technology services designed around real customer needs.
            </p>
          </div>

          <div className="grid items-stretch gap-7 md:grid-cols-2 xl:grid-cols-3">
            {services.map((service, index) => {
              const Icon = service.icon;

              return (
                <article
                  key={service.title}
                  className={`group relative flex flex-col overflow-hidden rounded-3xl border p-7 shadow-sm transition duration-300 hover:-translate-y-2 hover:shadow-2xl hover:shadow-[#0B2545]/10 ${service.cardClass}`}
                >
                  <div
                    aria-hidden="true"
                    className="absolute right-0 top-0 h-28 w-28 rounded-bl-[5rem] bg-white/45 transition duration-300 group-hover:scale-125"
                  />

                  <div className="relative flex flex-1 flex-col">
                    <div className="flex items-center justify-between">
                      <div
                        className={`flex size-14 items-center justify-center rounded-2xl transition duration-300 group-hover:scale-105 ${service.iconClass}`}
                      >
                        <Icon className="size-7" aria-hidden="true" />
                      </div>

                      <span className="text-sm font-black text-slate-300">
                        {String(index + 1).padStart(2, "0")}
                      </span>
                    </div>

                    <h3 className="mt-6 text-2xl font-extrabold text-[#0B2545]">
                      {service.title}
                    </h3>

                    <p className="mt-4 leading-7 text-slate-600">
                      {service.description}
                    </p>

                    <ul className="mt-6 flex-1 space-y-3">
                      {service.features.map((feature) => (
                        <li
                          key={feature}
                          className="flex items-start gap-3 text-sm font-medium text-slate-700"
                        >
                          <CheckCircle2
                            className={`mt-0.5 size-5 shrink-0 ${service.checkClass}`}
                            aria-hidden="true"
                          />

                          {feature}
                        </li>
                      ))}
                    </ul>

                    <Link
                      to="/book"
                      className="mt-8 inline-flex min-h-12 items-center justify-center gap-2 rounded-xl bg-[#1976D2] px-5 py-3 font-extrabold !text-white shadow-lg shadow-[#1976D2]/15 transition hover:bg-[#1565C0] hover:!text-white"
                    >
                      Request This Service
                      <ArrowRight className="size-5" aria-hidden="true" />
                    </Link>
                  </div>
                </article>
              );
            })}
          </div>
        </div>
      </section>

      {/* =========================================================
          SERVICE GUIDANCE CTA
          ========================================================= */}
      <section className="relative isolate overflow-hidden bg-gradient-to-br from-[#D4AF37] via-[#C59F2F] to-[#9F7A16] py-16 text-white sm:py-20">
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

        <div className="mx-auto grid max-w-7xl gap-8 px-4 sm:px-6 lg:grid-cols-[1fr_auto] lg:items-center lg:px-8">
          <div className="max-w-3xl">
            <div className="flex size-14 items-center justify-center rounded-2xl border border-white/20 bg-white/10 shadow-lg backdrop-blur-sm">
              <Network className="size-7 text-white" aria-hidden="true" />
            </div>

            <h2 className="mt-6 text-3xl font-black tracking-tight text-white sm:text-4xl">
              Not sure which service you need?
            </h2>

            <p className="mt-4 max-w-2xl text-lg leading-8 text-white/90">
              Describe the problem in your own words. We will review your
              request and help identify the most appropriate and practical next
              step.
            </p>
          </div>

          <div className="flex flex-col gap-3 sm:flex-row lg:justify-end">
            <Link
              to="/book"
              className="inline-flex min-h-13 items-center justify-center gap-2 rounded-xl bg-[#1976D2] px-6 py-3.5 font-extrabold !text-white shadow-xl transition hover:-translate-y-0.5 hover:bg-[#1565C0] hover:!text-white"
            >
              Start a Service Request
              <ArrowRight className="size-5" aria-hidden="true" />
            </Link>

            <Link
              to="/contact"
              className="inline-flex min-h-13 items-center justify-center rounded-xl border border-[#0B2545] bg-[#0B2545] px-6 py-3.5 font-extrabold !text-white shadow-xl transition hover:-translate-y-0.5 hover:bg-[#12365F] hover:!text-white"
            >
              Contact Romelt TechCare
            </Link>
          </div>
        </div>
      </section>
    </>
  );
}
