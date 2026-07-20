/**
 * ================================================================
 * ROMELT TECHCARE — ACCESSIBILITY PAGE
 * ================================================================
 *
 * Purpose:
 * Communicates the business commitment to providing an accessible
 * and usable public website.
 *
 * Responsibilities:
 * - Describes current accessibility goals and practices.
 * - Identifies supported navigation and readability features.
 * - Provides a way to report accessibility barriers.
 * - Guides future accessibility testing and improvements.
 *
 * Real-data integration:
 * Accessibility feedback may later be submitted to the backend,
 * converted into support tickets, assigned to staff, and tracked
 * through resolution.
 * ================================================================
 */

import {
  Accessibility,
  Eye,
  Keyboard,
  Mail,
  MessageSquareText,
  Smartphone,
} from "lucide-react";
import { Link } from "react-router";

import { Container } from "@/components/common/Container";
import {
  businessConfig,
  businessEmailHref,
  businessPhoneHref,
} from "@/config/business.config";

const accessibilityFeatures = [
  {
    title: "Keyboard navigation",
    description:
      "Interactive controls are designed to remain reachable and usable without relying only on a mouse.",
    icon: Keyboard,
  },
  {
    title: "Readable structure",
    description:
      "Pages use headings, labels, landmarks, and logical content organization to support easier navigation.",
    icon: Eye,
  },
  {
    title: "Mobile responsiveness",
    description:
      "Layouts are designed for phones, tablets, laptops, and larger displays without requiring a fixed screen size.",
    icon: Smartphone,
  },
  {
    title: "Accessible communication",
    description:
      "Customers may contact Romelt TechCare to request help using the website or an alternate way to obtain information.",
    icon: MessageSquareText,
  },
];

export function AccessibilityPage() {
  return (
    <>
      <section className="border-b border-slate-200 bg-white">
        <Container>
          <div className="max-w-4xl py-16 sm:py-20">
            <div className="flex h-14 w-14 items-center justify-center rounded-2xl bg-brand-50 text-brand-700">
              <Accessibility className="h-7 w-7" aria-hidden="true" />
            </div>

            <p className="mt-6 text-sm font-extrabold uppercase tracking-[0.18em] text-brand-700">
              Accessibility
            </p>

            <h1 className="mt-4 font-display text-4xl font-black tracking-tight text-navy-950 sm:text-5xl">
              Technology support should be accessible to everyone
            </h1>

            <p className="mt-6 max-w-3xl text-lg leading-8 text-slate-600">
              {businessConfig.name} is committed to improving the usability and
              accessibility of its public website for customers with different
              abilities, devices, and ways of navigating online.
            </p>
          </div>
        </Container>
      </section>

      <section className="bg-slate-50 py-16">
        <Container>
          <div className="mx-auto max-w-5xl">
            <div className="grid gap-6 md:grid-cols-2">
              {accessibilityFeatures.map((feature) => {
                const Icon = feature.icon;

                return (
                  <article
                    key={feature.title}
                    className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm"
                  >
                    <div className="flex h-12 w-12 items-center justify-center rounded-xl bg-brand-50 text-brand-700">
                      <Icon className="h-6 w-6" aria-hidden="true" />
                    </div>

                    <h2 className="mt-5 font-display text-xl font-extrabold text-navy-950">
                      {feature.title}
                    </h2>

                    <p className="mt-3 leading-7 text-slate-600">
                      {feature.description}
                    </p>
                  </article>
                );
              })}
            </div>

            <div className="mt-8 space-y-8">
              <AccessibilitySection title="Our accessibility goals">
                <p>
                  The website is being developed with the goal of supporting
                  commonly recognized accessibility practices, including
                  semantic page structure, visible keyboard focus, descriptive
                  labels, sufficient contrast, responsive layouts, and clear
                  error messages.
                </p>
              </AccessibilitySection>

              <AccessibilitySection title="Known limitations">
                <p>
                  The website is still under development. Some future booking,
                  customer-portal, payment, document-upload, or third-party
                  features may require additional accessibility review after
                  they are introduced.
                </p>

                <p>
                  Accessibility testing will continue as new sections and
                  interactive features are added.
                </p>
              </AccessibilitySection>

              <AccessibilitySection title="Request assistance or report a barrier">
                <p>
                  Contact {businessConfig.name} when a page, form, link, or
                  feature is difficult to use. Please describe the page, the
                  problem encountered, and the device or assistive technology
                  being used when possible.
                </p>

                <div className="mt-6 grid gap-4 sm:grid-cols-2">
                  {businessEmailHref ? (
                    <a
                      href={businessEmailHref}
                      className="focus-ring flex items-start gap-3 rounded-xl border border-slate-200 bg-slate-50 p-4 transition hover:border-brand-300 hover:bg-brand-50"
                    >
                      <Mail
                        className="mt-0.5 h-5 w-5 shrink-0 text-brand-700"
                        aria-hidden="true"
                      />

                      <span>
                        <span className="block font-bold text-navy-950">
                          Email
                        </span>

                        <span className="mt-1 block break-all text-sm text-slate-600">
                          {businessConfig.email}
                        </span>
                      </span>
                    </a>
                  ) : null}

                  {businessPhoneHref ? (
                    <a
                      href={businessPhoneHref}
                      className="focus-ring flex items-start gap-3 rounded-xl border border-slate-200 bg-slate-50 p-4 transition hover:border-brand-300 hover:bg-brand-50"
                    >
                      <Accessibility
                        className="mt-0.5 h-5 w-5 shrink-0 text-brand-700"
                        aria-hidden="true"
                      />

                      <span>
                        <span className="block font-bold text-navy-950">
                          Phone
                        </span>

                        <span className="mt-1 block text-sm text-slate-600">
                          {businessConfig.phoneDisplay}
                        </span>
                      </span>
                    </a>
                  ) : null}
                </div>

                <Link
                  to="/contact"
                  className="focus-ring mt-6 inline-flex min-h-11 items-center justify-center rounded-xl bg-brand-700 px-5 py-3 font-bold text-white transition hover:bg-brand-800"
                >
                  Use the Contact Form
                </Link>
              </AccessibilitySection>

              <AccessibilitySection title="Ongoing improvement">
                <p>
                  Accessibility is an ongoing responsibility. Feedback will be
                  reviewed as the website develops, and reasonable improvements
                  will be made when barriers are identified.
                </p>
              </AccessibilitySection>
            </div>
          </div>
        </Container>
      </section>
    </>
  );
}

interface AccessibilitySectionProps {
  title: string;
  children: React.ReactNode;
}

function AccessibilitySection({ title, children }: AccessibilitySectionProps) {
  return (
    <article className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm sm:p-8">
      <h2 className="font-display text-2xl font-extrabold text-navy-950">
        {title}
      </h2>

      <div className="mt-5 space-y-4 leading-8 text-slate-600">{children}</div>
    </article>
  );
}
