/**
 * ================================================================
 * ROMELT TECHCARE — PUBLIC WEBSITE FOOTER
 * ================================================================
 *
 * Purpose:
 * Provides business, navigation, service, contact, and legal
 * information at the bottom of every public website page.
 *
 * Responsibilities:
 * - Reinforces the Romelt TechCare brand and service promise.
 * - Provides primary navigation and service links.
 * - Displays centralized business contact information.
 * - Displays service-area and appointment guidance.
 * - Provides links to legal and accessibility pages.
 * - Maintains accessible keyboard and screen-reader behavior.
 *
 * Real-data integration:
 * Business identity, contact information, operating hours, service
 * areas, and availability are currently loaded from the centralized
 * business configuration. These settings may later come from the
 * Spring Boot backend through a public business-settings endpoint.
 * ================================================================
 */

import { ArrowUpRight, Clock3, Mail, MapPin, Phone } from "lucide-react";
import { Link } from "react-router";

import { BrandLogo } from "@/components/brand/BrandLogo";
import { Container } from "@/components/common/Container";
import {
  businessConfig,
  businessEmailHref,
  businessPhoneHref,
} from "@/config/business.config";
import { primaryNavigation } from "@/data/navigation";

interface FooterServiceLink {
  label: string;
  path: string;
}

const serviceLinks: FooterServiceLink[] = [
  {
    label: "Computer support",
    path: "/services#computer-support",
  },
  {
    label: "Wi-Fi and networking",
    path: "/services#wifi-networking",
  },
  {
    label: "New device setup",
    path: "/services#device-setup",
  },
  {
    label: "Remote support",
    path: "/services#remote-support",
  },
  {
    label: "Small-business IT",
    path: "/services#small-business-it",
  },
];

export function Footer() {
  const currentYear = new Date().getFullYear();

  const appointmentMessage = businessConfig.appointmentOnly
    ? "Services available by scheduled appointment"
    : "Contact us for current service availability";

  return (
    <footer className="bg-navy-950 text-white">
      <Container>
        <div className="grid gap-10 py-14 sm:grid-cols-2 lg:grid-cols-[1.4fr_0.8fr_1fr_1.2fr] lg:gap-12 lg:py-18">
          <div>
            <BrandLogo variant="light" showTagline />

            <p className="mt-5 max-w-md text-sm leading-7 text-slate-300">
              Honest, convenient technology support for Iowa homes, remote
              workers, small businesses, churches, and nonprofit organizations.
            </p>

            <p className="mt-5 font-display text-base font-bold text-brand-200">
              {businessConfig.tagline}
            </p>

            <Link
              to="/book"
              className="focus-ring mt-6 inline-flex items-center gap-2 rounded-lg border border-brand-300/30 bg-brand-400/10 px-4 py-2.5 text-sm font-bold text-brand-100 transition hover:border-brand-300/50 hover:bg-brand-400/20 hover:text-white"
            >
              Book a Service
              <ArrowUpRight aria-hidden="true" className="h-4 w-4" />
            </Link>
          </div>

          <nav aria-labelledby="footer-explore-heading">
            <h2
              id="footer-explore-heading"
              className="font-display text-sm font-extrabold uppercase tracking-[0.16em] text-brand-200"
            >
              Explore
            </h2>

            <ul className="mt-5 space-y-3">
              {primaryNavigation.map((item) => (
                <li key={item.path}>
                  <Link
                    to={item.path}
                    className="focus-ring inline-flex rounded-md text-sm text-slate-300 transition hover:translate-x-0.5 hover:text-white"
                  >
                    {item.label}
                  </Link>
                </li>
              ))}
            </ul>
          </nav>

          <nav aria-labelledby="footer-services-heading">
            <h2
              id="footer-services-heading"
              className="font-display text-sm font-extrabold uppercase tracking-[0.16em] text-brand-200"
            >
              Services
            </h2>

            <ul className="mt-5 space-y-3">
              {serviceLinks.map((service) => (
                <li key={service.path}>
                  <Link
                    to={service.path}
                    className="focus-ring inline-flex rounded-md text-sm text-slate-300 transition hover:translate-x-0.5 hover:text-white"
                  >
                    {service.label}
                  </Link>
                </li>
              ))}
            </ul>
          </nav>

          <section aria-labelledby="footer-contact-heading">
            <h2
              id="footer-contact-heading"
              className="font-display text-sm font-extrabold uppercase tracking-[0.16em] text-brand-200"
            >
              Contact
            </h2>

            <ul className="mt-5 space-y-4 text-sm text-slate-300">
              <li className="flex items-start gap-3">
                <Phone
                  aria-hidden="true"
                  className="mt-0.5 h-4 w-4 shrink-0 text-brand-300"
                />

                {businessPhoneHref ? (
                  <a
                    href={businessPhoneHref}
                    className="focus-ring rounded-md transition hover:text-white"
                  >
                    {businessConfig.phoneDisplay}
                  </a>
                ) : (
                  <span>{businessConfig.phoneDisplay}</span>
                )}
              </li>

              <li className="flex items-start gap-3">
                <Mail
                  aria-hidden="true"
                  className="mt-0.5 h-4 w-4 shrink-0 text-brand-300"
                />

                {businessEmailHref ? (
                  <a
                    href={businessEmailHref}
                    className="focus-ring break-all rounded-md transition hover:text-white"
                  >
                    {businessConfig.email}
                  </a>
                ) : (
                  <span>Business email coming soon</span>
                )}
              </li>

              <li className="flex items-start gap-3">
                <MapPin
                  aria-hidden="true"
                  className="mt-0.5 h-4 w-4 shrink-0 text-brand-300"
                />

                <span>{businessConfig.serviceArea}</span>
              </li>

              <li className="flex items-start gap-3">
                <Clock3
                  aria-hidden="true"
                  className="mt-0.5 h-4 w-4 shrink-0 text-brand-300"
                />

                <span>{appointmentMessage}</span>
              </li>
            </ul>

            <Link
              to="/contact"
              className="focus-ring mt-5 inline-flex items-center gap-1.5 rounded-md text-sm font-bold text-brand-200 transition hover:text-white"
            >
              Contact Romelt TechCare
              <ArrowUpRight aria-hidden="true" className="h-4 w-4" />
            </Link>
          </section>
        </div>

        <div className="flex flex-col gap-4 border-t border-white/10 py-6 text-xs text-slate-400 sm:flex-row sm:items-center sm:justify-between">
          <p>
            © {currentYear} {businessConfig.name}. All rights reserved.
          </p>

          <nav
            aria-label="Legal navigation"
            className="flex flex-wrap gap-x-5 gap-y-2"
          >
            <Link
              to="/privacy"
              className="focus-ring rounded-md transition hover:text-white"
            >
              Privacy
            </Link>

            <Link
              to="/terms"
              className="focus-ring rounded-md transition hover:text-white"
            >
              Terms
            </Link>

            <Link
              to="/accessibility"
              className="focus-ring rounded-md transition hover:text-white"
            >
              Accessibility
            </Link>
          </nav>
        </div>
      </Container>
    </footer>
  );
}
