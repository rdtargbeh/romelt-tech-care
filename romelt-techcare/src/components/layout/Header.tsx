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
 * Real-data integration:
 * Navigation permissions, service alerts, customer login state,
 * and contact details can later be loaded from the backend.
 * ================================================================
 */

import { Menu, ShieldCheck, X } from "lucide-react";
import { useState } from "react";
import { Link, NavLink } from "react-router";

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
    <header className="sticky top-0 z-50 border-b border-slate-200 bg-white/95 backdrop-blur">
      <div className="mx-auto flex min-h-20 max-w-7xl items-center justify-between px-4 sm:px-6 lg:px-8">
        <Link
          to="/"
          className="flex items-center gap-3"
          onClick={closeMobileMenu}
        >
          <span className="flex size-11 items-center justify-center rounded-2xl bg-blue-700 text-white shadow-sm">
            <ShieldCheck className="size-6" aria-hidden="true" />
          </span>

          <span>
            <span className="block text-lg font-extrabold tracking-tight text-slate-950">
              Romelt TechCare
            </span>

            <span className="hidden text-xs font-medium text-slate-500 sm:block">
              Hassle-Free Technology. Honest Service.
            </span>
          </span>
        </Link>

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
                  "rounded-lg px-4 py-2 text-sm font-semibold transition",
                  isActive
                    ? "bg-blue-50 text-blue-700"
                    : "text-slate-600 hover:bg-slate-100 hover:text-slate-950",
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
            className="inline-flex min-h-11 items-center justify-center rounded-xl bg-blue-700 px-5 py-2.5 text-sm font-bold text-white shadow-sm transition hover:bg-blue-800 focus:outline-none focus:ring-2 focus:ring-blue-700 focus:ring-offset-2"
          >
            Book a Service
          </Link>
        </div>

        <button
          type="button"
          className="inline-flex size-11 items-center justify-center rounded-xl border border-slate-200 bg-white text-slate-700 transition hover:bg-slate-100 lg:hidden"
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
        <div className="border-t border-slate-200 bg-white px-4 py-4 lg:hidden">
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
                      ? "bg-blue-50 text-blue-700"
                      : "text-slate-700 hover:bg-slate-100",
                  ].join(" ")
                }
              >
                {item.label}
              </NavLink>
            ))}

            <Link
              to="/book"
              onClick={closeMobileMenu}
              className="mt-2 inline-flex min-h-12 items-center justify-center rounded-xl bg-blue-700 px-5 py-3 font-bold text-white transition hover:bg-blue-800"
            >
              Book a Service
            </Link>
          </nav>
        </div>
      ) : null}
    </header>
  );
}
