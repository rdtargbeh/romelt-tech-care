/**
 * ================================================================
 * ROMELT TECHCARE — HOME PAGE
 * ================================================================
 *
 * Purpose:
 * Introduces Romelt TechCare and converts visitors into customers
 * through a vibrant, professional, image-driven public homepage.
 *
 * Responsibilities:
 * - Communicates the primary business value proposition.
 * - Uses the temporary homepage hero image.
 * - Displays major service categories.
 * - Explains how the service process works.
 * - Builds customer trust.
 * - Directs visitors toward booking or contacting the business.
 *
 * Brand palette:
 * - Primary Blue: #1976D2
 * - Deep Navy: #0B2545
 * - Premium Gold: #D4AF37
 * - Crimson Accent: #C62828
 *
 * Temporary hero asset:
 * public/image/image-2.jpg
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
  MapPin,
  Network,
  ShieldCheck,
  Sparkles,
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
      "Home and small-business Wi-Fi setup, connectivity troubleshooting, and network improvement.",
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
    title: "Small-Business IT",
    description:
      "Reliable technology support for businesses that do not need a full internal IT department.",
    icon: BriefcaseBusiness,
  },
  {
    title: "Technology Care",
    description:
      "Preventive maintenance, updates, security checks, backups, and ongoing technology guidance.",
    icon: ShieldCheck,
  },
];

const trustBenefits = [
  "Clear explanations",
  "Honest recommendations",
  "Dependable support",
  "Hassle-free technology help",
];

const processSteps = [
  {
    number: "01",
    title: "Tell us the problem",
    description:
      "Describe the issue, affected device, service location, and preferred appointment time.",
  },
  {
    number: "02",
    title: "Receive clear guidance",
    description:
      "We review your request and explain the most practical next step without confusing technical language.",
  },
  {
    number: "03",
    title: "Get dependable support",
    description:
      "Your service is completed remotely, on-site, or through a scheduled appointment based on your needs.",
  },
];

export function HomePage() {
  return (
    <>
      {/* =========================================================
          HERO SECTION
          ========================================================= */}
      <section className="relative isolate overflow-hidden bg-[#0B2545]">
        <div className="absolute inset-0 -z-30">
          <img
            src="/image/image-5.jpg"
            alt=""
            aria-hidden="true"
            className="h-full w-full object-cover object-center"
          />
        </div>

        <div className="absolute inset-0 -z-20 bg-gradient-to-r from-[#071B33] via-[#0B2545]/95 to-[#0B2545]/45" />

        <div className="absolute inset-0 -z-10 bg-gradient-to-t from-[#0B2545] via-transparent to-[#1976D2]/15" />

        <div
          aria-hidden="true"
          className="absolute -left-24 top-16 -z-10 h-72 w-72 rounded-full bg-[#1976D2]/30 blur-3xl"
        />

        <div
          aria-hidden="true"
          className="absolute -right-20 bottom-0 -z-10 h-80 w-80 rounded-full bg-[#C62828]/20 blur-3xl"
        />

        <div
          aria-hidden="true"
          className="absolute right-[18%] top-12 -z-10 h-48 w-48 rounded-full bg-[#D4AF37]/15 blur-3xl"
        />

        <div className="mx-auto grid min-h-[700px] max-w-7xl items-center gap-12 px-4 py-16 sm:px-6 sm:py-20 lg:grid-cols-[1.05fr_0.95fr] lg:px-8 lg:py-24">
          <div className="max-w-3xl">
            <div className="inline-flex items-center gap-2 rounded-full border border-white/20 bg-white/10 px-4 py-2 text-sm font-bold text-white shadow-lg shadow-black/10 backdrop-blur-md">
              <BadgeCheck
                className="size-4 text-[#D4AF37]"
                aria-hidden="true"
              />
              Honest technology support you can trust
            </div>

            <h1 className="mt-6 text-4xl font-black leading-[1.06] tracking-tight text-white sm:text-5xl lg:text-6xl xl:text-7xl">
              Technology support that makes life{" "}
              <span className="relative inline-block text-[#D4AF37]">
                easier.
                <span
                  aria-hidden="true"
                  className="absolute -bottom-2 left-0 h-1.5 w-full rounded-full bg-[#C62828]"
                />
              </span>
            </h1>

            <p className="mt-7 max-w-2xl text-lg leading-8 text-[#EAF4FD] sm:text-xl">
              Romelt TechCare provides dependable computer, Wi-Fi, networking,
              device setup, and small-business IT support through clear
              communication, honest recommendations, and hassle-free service.
            </p>

            <div className="mt-9 flex flex-col gap-3 sm:flex-row">
              <Link
                to="/book"
                className="inline-flex min-h-14 items-center justify-center gap-2 rounded-xl bg-[#D4AF37] px-7 py-3.5 font-extrabold !text-[#0B2545] shadow-xl shadow-black/20 transition duration-300 hover:-translate-y-0.5 hover:bg-[#E0BE4C] hover:shadow-2xl hover:!text-[#0B2545]"
              >
                Book a Service
                <ArrowRight className="size-5" aria-hidden="true" />
              </Link>

              <Link
                to="/services"
                className="inline-flex min-h-14 items-center justify-center gap-2 rounded-xl border border-white/30 bg-white/10 px-7 py-3.5 font-extrabold !text-white backdrop-blur-md transition duration-300 hover:-translate-y-0.5 hover:border-white/60 hover:bg-white/20 hover:!text-white"
              >
                Explore Services
                <ArrowRight className="size-5" aria-hidden="true" />
              </Link>
            </div>

            <div className="mt-9 grid gap-x-6 gap-y-3 sm:grid-cols-2">
              {trustBenefits.map((benefit) => (
                <div
                  key={benefit}
                  className="flex items-center gap-2.5 text-sm font-semibold text-[#EAF4FD]"
                >
                  <CheckCircle2
                    className="size-5 shrink-0 text-[#D4AF37]"
                    aria-hidden="true"
                  />
                  {benefit}
                </div>
              ))}
            </div>
          </div>

          <div className="relative hidden min-h-[520px] lg:block">
            <div className="absolute inset-x-8 inset-y-4 rounded-[2.5rem] border border-white/20 bg-white/10 shadow-2xl shadow-black/30 backdrop-blur-sm" />

            <div className="absolute right-0 top-14 w-72 rounded-3xl border border-white/20 bg-white/95 p-6 shadow-2xl shadow-black/20">
              <div className="flex items-start gap-4">
                <div className="flex size-12 shrink-0 items-center justify-center rounded-2xl bg-[#EAF4FD] text-[#1976D2]">
                  <Wrench className="size-6" aria-hidden="true" />
                </div>

                <div>
                  <p className="text-xs font-extrabold uppercase tracking-[0.16em] text-[#1976D2]">
                    Practical Support
                  </p>

                  <h2 className="mt-2 text-lg font-black text-[#0B2545]">
                    Help for the technology you use every day
                  </h2>
                </div>
              </div>

              <p className="mt-4 text-sm leading-6 text-slate-600">
                From slow computers to unreliable Wi-Fi, new devices, and
                business technology, we help you move forward confidently.
              </p>
            </div>

            <div className="absolute bottom-16 left-0 w-64 rounded-3xl border border-white/20 bg-[#0B2545]/95 p-6 text-white shadow-2xl shadow-black/30 backdrop-blur-md">
              <div className="flex items-center gap-3">
                <div className="flex size-11 items-center justify-center rounded-xl bg-[#D4AF37] text-[#0B2545]">
                  <Clock3 className="size-5" aria-hidden="true" />
                </div>

                <div>
                  <p className="text-xs font-bold uppercase tracking-[0.14em] text-[#D4AF37]">
                    Flexible Service
                  </p>

                  <p className="mt-1 font-extrabold">
                    Remote and on-site support
                  </p>
                </div>
              </div>
            </div>

            <div className="absolute bottom-4 right-10 flex items-center gap-3 rounded-2xl border border-white/20 bg-white/95 px-5 py-4 shadow-xl">
              <MapPin
                className="size-5 shrink-0 text-[#C62828]"
                aria-hidden="true"
              />

              <div>
                <p className="text-xs font-bold uppercase tracking-wide text-slate-500">
                  Serving
                </p>

                <p className="font-extrabold text-[#0B2545]">
                  Iowa homes and businesses
                </p>
              </div>
            </div>
          </div>
        </div>

        <div
          aria-hidden="true"
          className="absolute bottom-0 left-0 h-1.5 w-full bg-gradient-to-r from-[#1976D2] via-[#D4AF37] to-[#C62828]"
        />
      </section>

      {/* =========================================================
          QUICK VALUE STRIP
          ========================================================= */}
      <section className="relative z-10 border-b border-slate-200 bg-white">
        <div className="mx-auto grid max-w-7xl gap-4 px-4 py-6 sm:grid-cols-2 sm:px-6 lg:grid-cols-4 lg:px-8">
          <div className="flex items-center gap-3 rounded-2xl p-3">
            <div className="flex size-11 shrink-0 items-center justify-center rounded-xl bg-[#EAF4FD] text-[#1976D2]">
              <House className="size-5" aria-hidden="true" />
            </div>

            <div>
              <p className="font-extrabold text-[#0B2545]">Home Support</p>
              <p className="text-sm text-slate-500">Personal technology help</p>
            </div>
          </div>

          <div className="flex items-center gap-3 rounded-2xl p-3">
            <div className="flex size-11 shrink-0 items-center justify-center rounded-xl bg-[#FFF8E1] text-[#B68E13]">
              <BriefcaseBusiness className="size-5" aria-hidden="true" />
            </div>

            <div>
              <p className="font-extrabold text-[#0B2545]">Business IT</p>
              <p className="text-sm text-slate-500">Support that can grow</p>
            </div>
          </div>

          <div className="flex items-center gap-3 rounded-2xl p-3">
            <div className="flex size-11 shrink-0 items-center justify-center rounded-xl bg-[#FDECEC] text-[#C62828]">
              <ShieldCheck className="size-5" aria-hidden="true" />
            </div>

            <div>
              <p className="font-extrabold text-[#0B2545]">Honest Guidance</p>
              <p className="text-sm text-slate-500">No unnecessary solutions</p>
            </div>
          </div>

          <div className="flex items-center gap-3 rounded-2xl p-3">
            <div className="flex size-11 shrink-0 items-center justify-center rounded-xl bg-[#EAF4FD] text-[#1976D2]">
              <Headphones className="size-5" aria-hidden="true" />
            </div>

            <div>
              <p className="font-extrabold text-[#0B2545]">Flexible Service</p>
              <p className="text-sm text-slate-500">Remote or on-site</p>
            </div>
          </div>
        </div>
      </section>

      {/* =========================================================
          SERVICES
          ========================================================= */}
      <section className="relative overflow-hidden bg-[#F8FAFC] py-16 sm:py-20 lg:py-24">
        <div
          aria-hidden="true"
          className="absolute -right-32 top-10 h-80 w-80 rounded-full bg-[#1976D2]/10 blur-3xl"
        />

        <div
          aria-hidden="true"
          className="absolute -left-32 bottom-0 h-72 w-72 rounded-full bg-[#D4AF37]/10 blur-3xl"
        />

        <div className="relative mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
          <div className="flex flex-col gap-6 lg:flex-row lg:items-end lg:justify-between">
            <div className="max-w-3xl">
              <div className="inline-flex items-center gap-2 rounded-full bg-[#EAF4FD] px-4 py-2 text-sm font-extrabold uppercase tracking-[0.16em] text-[#1976D2]">
                <Sparkles className="size-4" aria-hidden="true" />
                Our Services
              </div>

              <h2 className="mt-5 text-3xl font-black tracking-tight text-[#0B2545] sm:text-4xl lg:text-5xl">
                Complete technology support for home and business
              </h2>

              <p className="mt-5 max-w-2xl text-lg leading-8 text-slate-600">
                Get practical help without confusing explanations, unnecessary
                recommendations, or hidden surprises.
              </p>
            </div>

            <Link
              to="/services"
              className="inline-flex w-fit items-center gap-2 font-extrabold !text-[#1976D2] transition hover:gap-3 hover:!text-[#0B2545]"
            >
              View all services
              <ArrowRight className="size-5" aria-hidden="true" />
            </Link>
          </div>

          <div className="mt-12 grid gap-6 md:grid-cols-2 lg:grid-cols-3">
            {services.map((service, index) => {
              const Icon = service.icon;

              return (
                <article
                  key={service.title}
                  className="group relative overflow-hidden rounded-3xl border border-slate-200 bg-white p-7 shadow-sm transition duration-300 hover:-translate-y-2 hover:border-[#1976D2]/30 hover:shadow-2xl hover:shadow-[#0B2545]/10"
                >
                  <div
                    aria-hidden="true"
                    className="absolute right-0 top-0 h-28 w-28 rounded-bl-[5rem] bg-gradient-to-bl from-[#EAF4FD] to-transparent transition duration-300 group-hover:scale-125"
                  />

                  <div className="relative">
                    <div className="flex items-center justify-between">
                      <div className="flex size-14 items-center justify-center rounded-2xl bg-[#EAF4FD] text-[#1976D2] transition duration-300 group-hover:bg-[#1976D2] group-hover:text-white">
                        <Icon className="size-7" aria-hidden="true" />
                      </div>

                      <span className="text-sm font-black text-slate-300">
                        0{index + 1}
                      </span>
                    </div>

                    <h3 className="mt-6 text-xl font-extrabold text-[#0B2545]">
                      {service.title}
                    </h3>

                    <p className="mt-3 leading-7 text-slate-600">
                      {service.description}
                    </p>

                    <Link
                      to="/services"
                      className="mt-6 inline-flex items-center gap-2 font-bold !text-[#1976D2] transition group-hover:gap-3 hover:!text-[#0B2545]"
                    >
                      Learn more
                      <ArrowRight className="size-4" aria-hidden="true" />
                    </Link>
                  </div>
                </article>
              );
            })}
          </div>
        </div>
      </section>

      {/* =========================================================
          HOW IT WORKS
          ========================================================= */}
      <section className="relative overflow-hidden bg-white py-16 sm:py-20 lg:py-24">
        <div className="mx-auto grid max-w-7xl gap-12 px-4 sm:px-6 lg:grid-cols-[0.85fr_1.15fr] lg:items-center lg:px-8">
          <div>
            <p className="text-sm font-extrabold uppercase tracking-[0.2em] text-[#1976D2]">
              How It Works
            </p>

            <h2 className="mt-4 text-3xl font-black tracking-tight text-[#0B2545] sm:text-4xl lg:text-5xl">
              A simple path from technology problem to solution
            </h2>

            <p className="mt-5 text-lg leading-8 text-slate-600">
              Getting technology support should not create more confusion. Our
              process keeps every step clear, manageable, and focused on your
              needs.
            </p>

            <div className="mt-8 overflow-hidden rounded-3xl bg-gradient-to-br from-[#1976D2] to-[#0B2545] p-7 text-white shadow-xl shadow-[#0B2545]/15">
              <div className="flex items-start gap-4">
                <div className="flex size-12 shrink-0 items-center justify-center rounded-2xl bg-white/15">
                  <Network className="size-6" aria-hidden="true" />
                </div>

                <div>
                  <h3 className="text-lg font-extrabold">
                    Support built around your needs
                  </h3>

                  <p className="mt-2 leading-7 text-[#EAF4FD]">
                    Recommendations are based on the actual problem, your
                    priorities, your budget, and the best long-term solution.
                  </p>
                </div>
              </div>

              <div className="mt-6 h-1 w-20 rounded-full bg-[#D4AF37]" />
            </div>
          </div>

          <div className="relative">
            <div
              aria-hidden="true"
              className="absolute bottom-10 left-6 top-10 hidden w-px bg-gradient-to-b from-[#1976D2] via-[#D4AF37] to-[#C62828] sm:block"
            />

            <div className="space-y-5">
              {processSteps.map((step, index) => (
                <article
                  key={step.number}
                  className="group relative flex gap-5 rounded-3xl border border-slate-200 bg-[#F8FAFC] p-5 transition duration-300 hover:border-[#1976D2]/30 hover:bg-white hover:shadow-xl sm:p-6"
                >
                  <div
                    className={[
                      "relative z-10 flex size-13 shrink-0 items-center justify-center rounded-2xl text-sm font-black text-white shadow-lg",
                      index === 0
                        ? "bg-[#1976D2]"
                        : index === 1
                          ? "bg-[#D4AF37] !text-[#0B2545]"
                          : "bg-[#C62828]",
                    ].join(" ")}
                  >
                    {step.number}
                  </div>

                  <div>
                    <h3 className="text-xl font-extrabold text-[#0B2545]">
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

      {/* =========================================================
    FINAL CTA — CUSTOMER SERVICE
    ========================================================= */}
      <section className="relative overflow-hidden bg-[#1976D2] text-white">
        <div className="grid min-h-[560px] lg:grid-cols-2">
          {/* Customer service image */}
          <div className="relative min-h-[360px] overflow-hidden lg:min-h-full">
            <img
              src="/image/contact-1.jpg"
              alt="Customer service representative providing technology support"
              className="absolute inset-0 h-full w-full object-cover object-center"
            />

            <div className="absolute inset-0 bg-gradient-to-t from-[#0B2545]/70 via-transparent to-transparent lg:bg-gradient-to-r lg:from-transparent lg:to-[#1976D2]/25" />

            <div className="absolute bottom-6 left-6 right-6 rounded-2xl border border-white/20 bg-[#0B2545]/85 p-5 shadow-2xl backdrop-blur-md sm:left-8 sm:right-auto sm:max-w-sm">
              <div className="flex items-center gap-4">
                <div className="flex size-12 shrink-0 items-center justify-center rounded-xl bg-[#D4AF37] text-[#0B2545]">
                  <Headphones className="size-6" aria-hidden="true" />
                </div>

                <div>
                  <p className="text-xs font-extrabold uppercase tracking-[0.16em] text-[#D4AF37]">
                    Ready to Help
                  </p>

                  <p className="mt-1 font-extrabold text-white">
                    Friendly, clear, and dependable support
                  </p>
                </div>
              </div>
            </div>
          </div>

          {/* CTA content */}
          <div className="relative isolate flex items-center overflow-hidden px-4 py-16 sm:px-8 sm:py-20 lg:px-14 xl:px-20">
            <div
              aria-hidden="true"
              className="absolute -right-32 -top-32 -z-10 h-80 w-80 rounded-full border-[60px] border-white/5"
            />

            <div
              aria-hidden="true"
              className="absolute -bottom-40 -left-24 -z-10 h-96 w-96 rounded-full bg-[#0B2545]/25 blur-3xl"
            />

            <div className="max-w-xl">
              <div className="flex size-16 items-center justify-center rounded-2xl border border-white/20 bg-white/10 shadow-xl backdrop-blur-sm">
                <ShieldCheck
                  className="size-8 text-[#D4AF37]"
                  aria-hidden="true"
                />
              </div>

              <p className="mt-7 text-sm font-extrabold uppercase tracking-[0.2em] text-[#D4AF37]">
                We Are Here to Help
              </p>

              <h2 className="mt-4 text-3xl font-black tracking-tight sm:text-4xl lg:text-5xl">
                Get dependable help with your technology
              </h2>

              <p className="mt-5 text-lg leading-8 text-[#EAF4FD]">
                Tell us what is not working, what you need configured, or where
                your technology is slowing you down. We will listen, explain
                your options, and help you determine the most practical next
                step.
              </p>

              <div className="mt-7 grid gap-3 text-sm font-semibold text-white sm:grid-cols-2">
                <div className="flex items-center gap-2.5">
                  <CheckCircle2
                    className="size-5 shrink-0 text-[#D4AF37]"
                    aria-hidden="true"
                  />
                  Clear communication
                </div>

                <div className="flex items-center gap-2.5">
                  <CheckCircle2
                    className="size-5 shrink-0 text-[#D4AF37]"
                    aria-hidden="true"
                  />
                  Honest recommendations
                </div>

                <div className="flex items-center gap-2.5">
                  <CheckCircle2
                    className="size-5 shrink-0 text-[#D4AF37]"
                    aria-hidden="true"
                  />
                  Remote support
                </div>

                <div className="flex items-center gap-2.5">
                  <CheckCircle2
                    className="size-5 shrink-0 text-[#D4AF37]"
                    aria-hidden="true"
                  />
                  On-site appointments
                </div>
              </div>

              <div className="mt-9 flex flex-col gap-3 sm:flex-row">
                <Link
                  to="/book"
                  className="inline-flex min-h-14 items-center justify-center gap-2 rounded-xl bg-[#0B2545] px-7 py-3.5 font-extrabold !text-white shadow-xl shadow-[#0B2545]/25 transition duration-300 hover:-translate-y-0.5 hover:bg-[#12365F] hover:!text-white"
                >
                  Book a Service
                  <ArrowRight className="size-5" aria-hidden="true" />
                </Link>

                <Link
                  to="/contact"
                  className="inline-flex min-h-14 items-center justify-center rounded-xl border border-[#D4AF37] bg-[#D4AF37] px-7 py-3.5 font-extrabold !text-[#0B2545] transition duration-300 hover:-translate-y-0.5 hover:bg-[#E0BE4C] hover:!text-[#0B2545]"
                >
                  Contact Us
                </Link>
              </div>
            </div>
          </div>
        </div>
      </section>
    </>
  );
}
