/**
 * ================================================================
 * ROMELT TECHCARE — PUBLIC WEBSITE HEADER
 * ================================================================
 *
 * Purpose:
 * Displays the primary branding and navigation for the website.
 *
 * Responsibilities:
 * - Shows the Romelt TechCare brand.
 * - Provides primary navigation links.
 * - Provides a visible service-booking call to action.
 * - Supports mobile and desktop layouts.
 *
 * Temporary logo asset:
 * public/image/logo-1.png
 *
 * Real-data integration:
 * Navigation permissions, service alerts, customer login state,
 * and contact details can later be loaded from the backend.
 * ================================================================
 */

import { Menu, X } from "lucide-react";
import { useState } from "react";
import { Link, NavLink } from "react-router";

import { BrandLogo } from "@/components/brand/BrandLogo";

const navigationItems = [
  {
    label: "Home",
    path: "/",
  },
  {
    label: "Services",
    path: "/services",
  },
  {
    label: "Pricing",
    path: "/pricing",
  },
  {
    label: "About",
    path: "/about",
  },
  {
    label: "Contact",
    path: "/contact",
  },
];

export function Header() {
  const [isMobileMenuOpen, setIsMobileMenuOpen] = useState(false);

  const closeMobileMenu = () => {
    setIsMobileMenuOpen(false);
  };

  return (
    <header className="sticky top-0 z-50 border-b border-[#1565C0] bg-[#1976D2] backdrop-blur">
      <div className="mx-auto flex min-h-20 max-w-7xl items-center justify-between px-4 sm:px-6 lg:px-8">
        <div onClick={closeMobileMenu}>
          <BrandLogo variant="light" showTagline />
        </div>

        <nav
          className="hidden items-center gap-1 lg:flex"
          aria-label="Primary navigation"
        >
          {navigationItems.map((item) => (
            <NavLink
              key={item.path}
              to={item.path}
              className={({ isActive }) =>
                [
                  "rounded-lg px-4 py-2 text-base font-semibold transition",
                  isActive
                    ? "bg-white !text-[#0B2545]"
                    : "!text-[#F8FAFC] hover:bg-[#D4AF37] hover:!text-[#0B2545]",
                ].join(" ")
              }
            >
              {item.label}
            </NavLink>
          ))}
        </nav>

        <div className="hidden lg:block">
          <Link
            to="/book"
            className="inline-flex min-h-11 items-center justify-center rounded-xl bg-[#D4AF37] px-5 py-2.5 text-sm font-bold !text-[#0B2545] shadow-sm transition hover:bg-[#C9A52F] hover:!text-[#0B2545] focus:outline-none focus:ring-2 focus:ring-white focus:ring-offset-2 focus:ring-offset-[#1976D2]"
          >
            Book a Service
          </Link>
        </div>

        <button
          type="button"
          className="inline-flex size-11 items-center justify-center rounded-xl border border-white/40 bg-white text-[#0B2545] transition hover:bg-[#EAF4FD] lg:hidden"
          aria-label={
            isMobileMenuOpen ? "Close navigation menu" : "Open navigation menu"
          }
          aria-expanded={isMobileMenuOpen}
          onClick={() => setIsMobileMenuOpen((current) => !current)}
        >
          {isMobileMenuOpen ? (
            <X className="size-6" aria-hidden="true" />
          ) : (
            <Menu className="size-6" aria-hidden="true" />
          )}
        </button>
      </div>

      {isMobileMenuOpen ? (
        <div className="border-t border-[#1565C0] bg-white px-4 py-4 lg:hidden">
          <nav
            className="mx-auto flex max-w-7xl flex-col gap-2"
            aria-label="Mobile navigation"
          >
            {navigationItems.map((item) => (
              <NavLink
                key={item.path}
                to={item.path}
                onClick={closeMobileMenu}
                className={({ isActive }) =>
                  [
                    "rounded-xl px-4 py-3 text-base font-semibold transition",
                    isActive
                      ? "bg-[#EAF4FD] !text-[#1976D2]"
                      : "!text-[#0B2545] hover:bg-slate-100",
                  ].join(" ")
                }
              >
                {item.label}
              </NavLink>
            ))}

            <Link
              to="/book"
              onClick={closeMobileMenu}
              className="mt-2 inline-flex min-h-12 items-center justify-center rounded-xl bg-[#1976D2] px-5 py-3 font-bold !text-white transition hover:bg-[#1565C0] hover:!text-white"
            >
              Book a Service
            </Link>
          </nav>
        </div>
      ) : null}
    </header>
  );
}
