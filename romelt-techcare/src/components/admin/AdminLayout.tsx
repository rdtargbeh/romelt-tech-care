/**
 * ================================================================
 * ROMELT TECHCARE — ADMINISTRATOR PORTAL LAYOUT
 * ================================================================
 *
 * Purpose:
 * Provides the shared responsive workspace layout for authenticated
 * Romelt TechCare administrators.
 *
 * Responsibilities:
 * - Displays desktop and mobile administrator navigation.
 * - Keeps every registered administrator module clickable.
 * - Highlights the current route with visible text and icons.
 * - Displays the authenticated administrator profile.
 * - Provides password-change and logout actions.
 * - Renders nested administrator pages through React Router Outlet.
 *
 * Active modules:
 * - Dashboard
 * - Administrator Users
 * - Bookings
 * - Contact Inquiries
 * - Services
 * - Settings
 *
 * Real-data integration:
 * Authentication and administrator identity are supplied through
 * AdminAuthContext and the useAdminAuth hook.
 * ================================================================
 */

import {
  CalendarDays,
  ChevronDown,
  CircleUserRound,
  Gauge,
  KeyRound,
  LogOut,
  Menu,
  MessageSquareText,
  Settings,
  ShieldCheck,
  UsersRound,
  Wrench,
  X,
} from "lucide-react";
import { useEffect, useRef, useState } from "react";
import {
  Link,
  NavLink,
  Outlet,
  useLocation,
  useNavigate,
} from "react-router-dom";

import { useAdminAuth } from "@/hooks/useAdminAuth";

interface AdminNavigationItem {
  label: string;
  to: string;
  icon: typeof Gauge;
  end?: boolean;
  requiredRoles?: string[];
}

const ADMIN_NAVIGATION_ITEMS: AdminNavigationItem[] = [
  {
    label: "Dashboard",
    to: "/admin",
    icon: Gauge,
    end: true,
  },
  {
    label: "Administrator users",
    to: "/admin/users",
    icon: UsersRound,
    requiredRoles: ["SUPER_ADMIN"],
  },
  {
    label: "Bookings",
    to: "/admin/bookings",
    icon: CalendarDays,
  },
  {
    label: "Contact inquiries",
    to: "/admin/contact-inquiries",
    icon: MessageSquareText,
  },
  {
    label: "Services",
    to: "/admin/services",
    icon: Wrench,
  },
  {
    label: "Settings",
    to: "/admin/settings",
    icon: Settings,
  },
];

export default function AdminLayout() {
  const location = useLocation();
  const navigate = useNavigate();

  const { administrator, logout } = useAdminAuth();

  const [mobileNavigationOpen, setMobileNavigationOpen] = useState(false);

  const [profileMenuOpen, setProfileMenuOpen] = useState(false);

  const profileMenuRef = useRef<HTMLDivElement | null>(null);

  useEffect(() => {
    setMobileNavigationOpen(false);
    setProfileMenuOpen(false);
  }, [location.pathname]);

  useEffect(() => {
    function handlePointerDown(event: PointerEvent) {
      if (
        profileMenuRef.current &&
        !profileMenuRef.current.contains(event.target as Node)
      ) {
        setProfileMenuOpen(false);
      }
    }

    function handleEscape(event: KeyboardEvent) {
      if (event.key === "Escape") {
        setMobileNavigationOpen(false);
        setProfileMenuOpen(false);
      }
    }

    document.addEventListener("pointerdown", handlePointerDown);
    document.addEventListener("keydown", handleEscape);

    return () => {
      document.removeEventListener("pointerdown", handlePointerDown);

      document.removeEventListener("keydown", handleEscape);
    };
  }, []);

  useEffect(() => {
    if (!mobileNavigationOpen) {
      return;
    }

    const previousOverflow = document.body.style.overflow;

    document.body.style.overflow = "hidden";

    return () => {
      document.body.style.overflow = previousOverflow;
    };
  }, [mobileNavigationOpen]);

  function handleLogout() {
    logout();

    navigate("/admin/login", {
      replace: true,
    });
  }

  const administratorName =
    administrator?.fullName?.trim() ||
    [administrator?.firstName, administrator?.lastName]
      .filter(Boolean)
      .join(" ")
      .trim() ||
    "Administrator";

  const administratorInitials = createInitials(administratorName);

  const visibleNavigationItems = ADMIN_NAVIGATION_ITEMS.filter(
    (item) =>
      !item.requiredRoles ||
      item.requiredRoles.includes(administrator?.role ?? ""),
  );

  return (
    <div className="min-h-screen bg-slate-50">
      {/* =========================================================
       * DESKTOP SIDEBAR
       * ========================================================= */}
      <aside className="fixed inset-y-0 left-0 z-40 hidden w-72 border-r border-slate-800 bg-slate-950 text-white lg:flex lg:flex-col">
        <AdminSidebarContent
          administratorName={administratorName}
          navigationItems={visibleNavigationItems}
        />
      </aside>

      {/* =========================================================
       * MOBILE SIDEBAR
       * ========================================================= */}
      {mobileNavigationOpen && (
        <div
          className="fixed inset-0 z-50 lg:hidden"
          role="dialog"
          aria-modal="true"
          aria-label="Administrator navigation"
        >
          <button
            type="button"
            className="absolute inset-0 bg-slate-950/70"
            onClick={() => setMobileNavigationOpen(false)}
            aria-label="Close navigation"
          />

          <aside className="relative flex h-full w-[min(88vw,19rem)] flex-col bg-slate-950 text-white shadow-2xl">
            <div className="flex items-center justify-between border-b border-slate-800 px-5 py-4">
              <AdminBrand />

              <button
                type="button"
                onClick={() => setMobileNavigationOpen(false)}
                className="flex h-10 w-10 items-center justify-center rounded-xl text-slate-300 transition hover:bg-white/10 hover:text-white focus:outline-none focus:ring-2 focus:ring-white/50"
                aria-label="Close navigation"
              >
                <X className="h-5 w-5" aria-hidden="true" />
              </button>
            </div>

            <AdminSidebarContent
              administratorName={administratorName}
              navigationItems={visibleNavigationItems}
              hideBrand
            />
          </aside>
        </div>
      )}

      {/* =========================================================
       * MAIN WORKSPACE
       * ========================================================= */}
      <div className="lg:pl-72">
        <header className="sticky top-0 z-30 border-b border-slate-200 bg-white/95 backdrop-blur">
          <div className="flex min-h-16 items-center justify-between gap-4 px-4 sm:px-6 lg:px-8">
            <div className="flex min-w-0 items-center gap-3">
              <button
                type="button"
                onClick={() => setMobileNavigationOpen(true)}
                className="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl border border-slate-200 bg-white text-slate-700 transition hover:bg-slate-50 focus:outline-none focus:ring-2 focus:ring-emerald-600/30 lg:hidden"
                aria-label="Open navigation"
              >
                <Menu className="h-5 w-5" aria-hidden="true" />
              </button>

              <div className="min-w-0">
                <p className="truncate text-xs font-bold uppercase tracking-[0.18em] text-emerald-700">
                  Administrator portal
                </p>

                <h1 className="truncate text-lg font-extrabold text-slate-950 sm:text-xl">
                  {resolvePageTitle(location.pathname)}
                </h1>
              </div>
            </div>

            {/* ===================================================
             * PROFILE MENU
             * =================================================== */}
            <div ref={profileMenuRef} className="relative">
              <button
                type="button"
                onClick={() => setProfileMenuOpen((current) => !current)}
                className="flex min-h-11 items-center gap-3 rounded-xl border border-slate-200 bg-white px-2 py-1.5 text-left transition hover:bg-slate-50 focus:outline-none focus:ring-2 focus:ring-emerald-600/30 sm:px-3"
                aria-expanded={profileMenuOpen}
                aria-haspopup="menu"
              >
                <span className="flex h-9 w-9 shrink-0 items-center justify-center rounded-lg bg-slate-950 text-sm font-extrabold text-white">
                  {administratorInitials}
                </span>

                <span className="hidden min-w-0 sm:block">
                  <span className="block max-w-44 truncate text-sm font-bold text-slate-900">
                    {administratorName}
                  </span>

                  <span className="block max-w-44 truncate text-xs text-slate-500">
                    {formatAdminRole(administrator?.role)}
                  </span>
                </span>

                <ChevronDown
                  className={[
                    "hidden h-4 w-4 shrink-0 text-slate-500 transition sm:block",
                    profileMenuOpen ? "rotate-180" : "",
                  ].join(" ")}
                  aria-hidden="true"
                />
              </button>

              {profileMenuOpen && (
                <div
                  className="absolute right-0 mt-2 w-64 overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-xl"
                  role="menu"
                >
                  <div className="border-b border-slate-200 px-4 py-4">
                    <p className="truncate text-sm font-bold text-slate-900">
                      {administratorName}
                    </p>

                    <p className="mt-1 truncate text-xs text-slate-500">
                      {administrator?.email}
                    </p>

                    <span className="mt-3 inline-flex rounded-full bg-emerald-50 px-2.5 py-1 text-xs font-bold text-emerald-800">
                      {formatAdminRole(administrator?.role)}
                    </span>
                  </div>

                  <div className="p-2">
                    {administrator?.role === "SUPER_ADMIN" && (
                      <Link
                        to="/admin/users"
                        className="flex min-h-11 items-center gap-3 rounded-xl px-3 py-2 text-sm font-semibold text-slate-700 transition hover:bg-slate-100 hover:text-slate-950 focus:outline-none focus:ring-2 focus:ring-emerald-600/30"
                        role="menuitem"
                      >
                        <UsersRound className="h-5 w-5" aria-hidden="true" />
                        Administrator users
                      </Link>
                    )}

                    <Link
                      to="/admin/change-password"
                      className="flex min-h-11 items-center gap-3 rounded-xl px-3 py-2 text-sm font-semibold text-slate-700 transition hover:bg-slate-100 hover:text-slate-950 focus:outline-none focus:ring-2 focus:ring-emerald-600/30"
                      role="menuitem"
                    >
                      <KeyRound className="h-5 w-5" aria-hidden="true" />
                      Change password
                    </Link>

                    <button
                      type="button"
                      onClick={handleLogout}
                      className="flex min-h-11 w-full items-center gap-3 rounded-xl px-3 py-2 text-left text-sm font-semibold text-red-700 transition hover:bg-red-50 focus:outline-none focus:ring-2 focus:ring-red-600/30"
                      role="menuitem"
                    >
                      <LogOut className="h-5 w-5" aria-hidden="true" />
                      Sign out
                    </button>
                  </div>
                </div>
              )}
            </div>
          </div>
        </header>

        <main className="px-4 py-6 sm:px-6 lg:px-8 lg:py-8">
          <Outlet />
        </main>
      </div>
    </div>
  );
}

interface AdminSidebarContentProps {
  administratorName: string;
  navigationItems: AdminNavigationItem[];
  hideBrand?: boolean;
}

function AdminSidebarContent({
  administratorName,
  navigationItems,
  hideBrand = false,
}: AdminSidebarContentProps) {
  return (
    <>
      {!hideBrand && (
        <div className="border-b border-slate-800 px-6 py-5">
          <AdminBrand />
        </div>
      )}

      <nav
        className="flex-1 overflow-y-auto px-4 py-5"
        aria-label="Administrator navigation"
      >
        <p className="px-3 text-xs font-bold uppercase tracking-[0.18em] text-slate-500">
          Workspace
        </p>

        <div className="mt-3 space-y-2">
          {navigationItems.map((item) => (
            <AdminNavigationLink key={item.to} item={item} />
          ))}
        </div>
      </nav>

      <div className="border-t border-slate-800 p-4">
        <div className="rounded-2xl bg-white/5 p-4">
          <div className="flex items-center gap-3">
            <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-white/10 text-white">
              <CircleUserRound className="h-5 w-5" aria-hidden="true" />
            </div>

            <div className="min-w-0">
              <p className="truncate text-sm font-bold text-white">
                {administratorName}
              </p>

              <p className="text-xs text-slate-400">Secure session active</p>
            </div>
          </div>
        </div>
      </div>
    </>
  );
}

function AdminBrand() {
  return (
    <Link
      to="/admin"
      className="inline-flex items-center gap-3 rounded-xl focus:outline-none focus:ring-2 focus:ring-white/50"
    >
      <span className="flex h-11 w-11 shrink-0 items-center justify-center rounded-xl bg-white text-slate-950">
        <ShieldCheck className="h-6 w-6" aria-hidden="true" />
      </span>

      <span>
        <span className="block text-lg font-extrabold text-white">
          Romelt TechCare
        </span>

        <span className="block text-xs font-semibold uppercase tracking-[0.14em] text-slate-400">
          Administration
        </span>
      </span>
    </Link>
  );
}

interface AdminNavigationLinkProps {
  item: AdminNavigationItem;
}

function AdminNavigationLink({ item }: AdminNavigationLinkProps) {
  const Icon = item.icon;

  return (
    <NavLink
      to={item.to}
      end={item.end}
      className={({ isActive }) =>
        [
          "group flex min-h-12 items-center gap-3 rounded-xl px-4 py-3 text-sm font-semibold transition focus:outline-none focus:ring-2 focus:ring-white/50",
          isActive
            ? "bg-white text-slate-950 shadow-sm"
            : "text-slate-300 hover:bg-white/10 hover:text-white",
        ].join(" ")
      }
    >
      {({ isActive }) => (
        <>
          <Icon
            className={[
              "h-5 w-5 shrink-0",
              isActive
                ? "text-slate-950"
                : "text-slate-400 group-hover:text-white",
            ].join(" ")}
            aria-hidden="true"
          />

          <span className={isActive ? "text-slate-950" : "text-inherit"}>
            {item.label}
          </span>
        </>
      )}
    </NavLink>
  );
}

function resolvePageTitle(pathname: string): string {
  if (pathname === "/admin") {
    return "Dashboard";
  }

  if (pathname === "/admin/users") {
    return "Administrator users";
  }

  if (pathname === "/admin/users/new") {
    return "Create administrator";
  }

  if (pathname.startsWith("/admin/users/") && pathname.endsWith("/edit")) {
    return "Edit administrator";
  }

  if (pathname.startsWith("/admin/users/")) {
    return "Administrator details";
  }

  if (pathname.startsWith("/admin/bookings")) {
    return "Bookings";
  }

  if (pathname.startsWith("/admin/contact-inquiries")) {
    return "Contact inquiries";
  }

  if (pathname.startsWith("/admin/services")) {
    return "Services";
  }

  if (pathname.startsWith("/admin/settings")) {
    return "Settings";
  }

  return "Administration";
}

function createInitials(name: string): string {
  const words = name.trim().split(/\s+/).filter(Boolean);

  if (words.length === 0) {
    return "AD";
  }

  if (words.length === 1) {
    return words[0].slice(0, 2).toUpperCase();
  }

  return `${words[0][0]}${words.at(-1)?.[0] ?? ""}`.toUpperCase();
}

function formatAdminRole(role: string | undefined): string {
  if (!role) {
    return "Administrator";
  }

  return role.toLowerCase().split("_").map(capitalize).join(" ");
}

function capitalize(value: string): string {
  if (!value) {
    return value;
  }

  return `${value.charAt(0).toUpperCase()}${value.slice(1)}`;
}
