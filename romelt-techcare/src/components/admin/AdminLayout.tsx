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
 * - Highlights the current route.
 * - Displays authenticated administrator identity.
 * - Displays persistent administrator in-app notifications.
 * - Displays the unread notification count.
 * - Allows notifications to be marked read/unread.
 * - Allows notifications to be dismissed.
 * - Opens the backend-provided portal route for related resources.
 * - Refreshes notification state periodically while the administrator
 *   portal is open.
 * - Provides password-change and logout actions.
 * - Renders nested administrator pages through React Router Outlet.
 *
 * Active modules:
 * - Dashboard
 * - Administrator Users
 * - Customers
 * - Customer Reviews
 * - Bookings
 * - Contact Inquiries
 * - Services
 * - Settings
 *
 * Notification architecture:
 *
 * PostgreSQL notifications table
 *          ↓
 * GET /api/v1/admin/notifications
 *          ↓
 * Administrator notification bell
 *
 * REST remains the persistent source of truth.
 *
 * WebSocket support may later push new notifications immediately,
 * while REST continues to provide persistence and recovery after page
 * reload, logout, reconnect, or temporary socket failure.
 *
 * Real-data integration:
 *
 * Authentication:
 * AdminAuthContext / useAdminAuth
 *
 * Customers:
 * /admin/customers
 * /admin/customers/new
 * /admin/customers/{customerId}
 * /admin/customers/reviews
 * /admin/customers/reviews/new
 * /admin/customers/reviews/{customerReviewId}
 *
 * Notifications:
 * GET   /api/v1/admin/notifications
 * GET   /api/v1/admin/notifications/unread-count
 * PATCH /api/v1/admin/notifications/{id}/read
 * PATCH /api/v1/admin/notifications/{id}/unread
 * PATCH /api/v1/admin/notifications/{id}/dismiss
 * ================================================================
 */

import {
  Bell,
  BellRing,
  CalendarDays,
  Check,
  ChevronDown,
  CircleUserRound,
  Gauge,
  KeyRound,
  LoaderCircle,
  LogOut,
  MailOpen,
  Menu,
  MessageSquareQuote,
  MessageSquareText,
  Settings,
  ShieldCheck,
  Trash2,
  UserRound,
  UsersRound,
  Wrench,
  X,
} from "lucide-react";

import { useCallback, useEffect, useRef, useState } from "react";

import {
  Link,
  NavLink,
  Outlet,
  useLocation,
  useNavigate,
} from "react-router-dom";

import { useAdminAuth } from "@/hooks/useAdminAuth";

import {
  dismissAdminNotification,
  getAdminNotifications,
  getAdminNotificationUnreadCount,
  markAdminNotificationRead,
  markAdminNotificationUnread,
} from "@/services/admin-notification.service";

import type { AdminInAppNotification } from "@/types/admin-notification.types";

// =====================================================================
// CONFIGURATION
// =====================================================================

const NOTIFICATION_PREVIEW_SIZE = 10;

/**
 * REST polling remains intentionally moderate.
 *
 * WebSocket support can later provide immediate notification delivery.
 * Until then, refreshing once per minute keeps the administrator
 * notification center reasonably current.
 */
const NOTIFICATION_REFRESH_INTERVAL_MS = 60_000;

// =====================================================================
// NAVIGATION TYPES
// =====================================================================

interface AdminNavigationItem {
  label: string;

  to: string;

  icon: typeof Gauge;

  end?: boolean;

  requiredRoles?: string[];
}

// =====================================================================
// NAVIGATION
// =====================================================================

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
    label: "Customers",
    to: "/admin/customers",
    icon: UserRound,
    end: true,
  },
  {
    label: "Customer reviews",
    to: "/admin/customers/reviews",
    icon: MessageSquareQuote,
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

// =====================================================================
// LAYOUT
// =====================================================================

export default function AdminLayout() {
  const location = useLocation();

  const navigate = useNavigate();

  const { administrator, logout } = useAdminAuth();

  // ===================================================================
  // NAVIGATION / PROFILE STATE
  // ===================================================================

  const [mobileNavigationOpen, setMobileNavigationOpen] = useState(false);

  const [profileMenuOpen, setProfileMenuOpen] = useState(false);

  // ===================================================================
  // NOTIFICATION STATE
  // ===================================================================

  const [notificationMenuOpen, setNotificationMenuOpen] = useState(false);

  const [notifications, setNotifications] = useState<AdminInAppNotification[]>(
    [],
  );

  const [unreadCount, setUnreadCount] = useState(0);

  const [notificationsLoading, setNotificationsLoading] = useState(false);

  const [notificationActionId, setNotificationActionId] = useState<
    string | null
  >(null);

  const [notificationError, setNotificationError] = useState<string | null>(
    null,
  );

  // ===================================================================
  // REFERENCES
  // ===================================================================

  const profileMenuRef = useRef<HTMLDivElement | null>(null);

  const notificationMenuRef = useRef<HTMLDivElement | null>(null);

  const mountedRef = useRef(true);

  // ===================================================================
  // LOAD NOTIFICATIONS
  // ===================================================================

  const refreshNotifications = useCallback(async (showLoading = false) => {
    if (showLoading) {
      setNotificationsLoading(true);
    }

    try {
      const [page, countResponse] = await Promise.all([
        getAdminNotifications({
          page: 0,
          size: NOTIFICATION_PREVIEW_SIZE,
          unreadOnly: false,
        }),

        getAdminNotificationUnreadCount(),
      ]);

      if (!mountedRef.current) {
        return;
      }

      setNotifications(page.content ?? []);

      setUnreadCount(Math.max(0, countResponse.unreadCount ?? 0));

      setNotificationError(null);
    } catch (error) {
      if (!mountedRef.current) {
        return;
      }

      setNotificationError(
        error instanceof Error
          ? error.message
          : "Notifications could not be loaded.",
      );
    } finally {
      if (mountedRef.current && showLoading) {
        setNotificationsLoading(false);
      }
    }
  }, []);

  // ===================================================================
  // MOUNT / INITIAL NOTIFICATION LOAD
  // ===================================================================

  useEffect(() => {
    mountedRef.current = true;

    void refreshNotifications(true);

    const intervalId = window.setInterval(() => {
      void refreshNotifications(false);
    }, NOTIFICATION_REFRESH_INTERVAL_MS);

    return () => {
      mountedRef.current = false;

      window.clearInterval(intervalId);
    };
  }, [refreshNotifications]);

  // ===================================================================
  // ROUTE CHANGE
  // ===================================================================

  useEffect(() => {
    setMobileNavigationOpen(false);

    setProfileMenuOpen(false);

    setNotificationMenuOpen(false);
  }, [location.pathname]);

  // ===================================================================
  // OUTSIDE CLICK / ESCAPE
  // ===================================================================

  useEffect(() => {
    function handlePointerDown(event: PointerEvent) {
      const target = event.target as Node;

      if (profileMenuRef.current && !profileMenuRef.current.contains(target)) {
        setProfileMenuOpen(false);
      }

      if (
        notificationMenuRef.current &&
        !notificationMenuRef.current.contains(target)
      ) {
        setNotificationMenuOpen(false);
      }
    }

    function handleEscape(event: KeyboardEvent) {
      if (event.key !== "Escape") {
        return;
      }

      setMobileNavigationOpen(false);

      setProfileMenuOpen(false);

      setNotificationMenuOpen(false);
    }

    document.addEventListener("pointerdown", handlePointerDown);

    document.addEventListener("keydown", handleEscape);

    return () => {
      document.removeEventListener("pointerdown", handlePointerDown);

      document.removeEventListener("keydown", handleEscape);
    };
  }, []);

  // ===================================================================
  // MOBILE BODY LOCK
  // ===================================================================

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

  // ===================================================================
  // LOGOUT
  // ===================================================================

  function handleLogout() {
    logout();

    navigate("/admin/login", {
      replace: true,
    });
  }

  // ===================================================================
  // OPEN NOTIFICATION PANEL
  // ===================================================================

  async function handleNotificationMenuToggle() {
    const opening = !notificationMenuOpen;

    setNotificationMenuOpen(opening);

    setProfileMenuOpen(false);

    if (opening) {
      await refreshNotifications(true);
    }
  }

  // ===================================================================
  // OPEN NOTIFICATION
  // ===================================================================

  async function handleNotificationOpen(notification: AdminInAppNotification) {
    if (notificationActionId) {
      return;
    }

    setNotificationActionId(notification.notificationId);

    setNotificationError(null);

    try {
      if (!notification.read) {
        const updated = await markAdminNotificationRead(
          notification.notificationId,
        );

        replaceNotification(updated);

        setUnreadCount((current) => Math.max(0, current - 1));
      }

      setNotificationMenuOpen(false);

      const portalPath = normalizePortalPath(notification.portalPath);

      if (portalPath) {
        navigate(portalPath);
      }
    } catch (error) {
      setNotificationError(
        error instanceof Error
          ? error.message
          : "Notification could not be opened.",
      );
    } finally {
      setNotificationActionId(null);
    }
  }

  // ===================================================================
  // READ / UNREAD
  // ===================================================================

  async function handleToggleRead(notification: AdminInAppNotification) {
    if (notificationActionId) {
      return;
    }

    setNotificationActionId(notification.notificationId);

    setNotificationError(null);

    try {
      const updated = notification.read
        ? await markAdminNotificationUnread(notification.notificationId)
        : await markAdminNotificationRead(notification.notificationId);

      replaceNotification(updated);

      setUnreadCount((current) => {
        if (notification.read) {
          return current + 1;
        }

        return Math.max(0, current - 1);
      });
    } catch (error) {
      setNotificationError(
        error instanceof Error
          ? error.message
          : "Notification read state could not be updated.",
      );
    } finally {
      setNotificationActionId(null);
    }
  }

  // ===================================================================
  // DISMISS
  // ===================================================================

  async function handleDismissNotification(
    notification: AdminInAppNotification,
  ) {
    if (notificationActionId) {
      return;
    }

    setNotificationActionId(notification.notificationId);

    setNotificationError(null);

    try {
      await dismissAdminNotification(notification.notificationId);

      setNotifications((current) =>
        current.filter(
          (item) => item.notificationId !== notification.notificationId,
        ),
      );

      if (!notification.read) {
        setUnreadCount((current) => Math.max(0, current - 1));
      }
    } catch (error) {
      setNotificationError(
        error instanceof Error
          ? error.message
          : "Notification could not be dismissed.",
      );
    } finally {
      setNotificationActionId(null);
    }
  }

  // ===================================================================
  // LOCAL NOTIFICATION REPLACEMENT
  // ===================================================================

  function replaceNotification(updated: AdminInAppNotification) {
    setNotifications((current) =>
      current.map((notification) =>
        notification.notificationId === updated.notificationId
          ? updated
          : notification,
      ),
    );
  }

  // ===================================================================
  // ADMINISTRATOR IDENTITY
  // ===================================================================

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

  // ===================================================================
  // RENDER
  // ===================================================================

  return (
    <div className="min-h-screen bg-slate-50">
      {/* =============================================================
       * DESKTOP SIDEBAR
       * ============================================================= */}

      <aside className="fixed inset-y-0 left-0 z-40 hidden w-72 border-r border-slate-800 bg-slate-950 text-white lg:flex lg:flex-col">
        <AdminSidebarContent
          administratorName={administratorName}
          navigationItems={visibleNavigationItems}
        />
      </aside>

      {/* =============================================================
       * MOBILE SIDEBAR
       * ============================================================= */}

      {mobileNavigationOpen ? (
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
      ) : null}

      {/* =============================================================
       * MAIN WORKSPACE
       * ============================================================= */}

      <div className="lg:pl-72">
        <header className="sticky top-0 z-30 border-b border-slate-200 bg-white/95 backdrop-blur">
          <div className="flex min-h-16 items-center justify-between gap-3 px-4 sm:px-6 lg:px-8">
            {/* =======================================================
             * PAGE TITLE
             * ======================================================= */}

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

            {/* =======================================================
             * HEADER ACTIONS
             * ======================================================= */}

            <div className="flex shrink-0 items-center gap-2">
              {/* =====================================================
               * NOTIFICATION BELL
               * ===================================================== */}

              <div ref={notificationMenuRef} className="relative">
                <button
                  type="button"
                  onClick={handleNotificationMenuToggle}
                  className={[
                    "relative flex h-11 w-11 items-center justify-center rounded-xl border transition focus:outline-none focus:ring-2 focus:ring-emerald-600/30",
                    notificationMenuOpen
                      ? "border-emerald-300 bg-emerald-50 text-emerald-800"
                      : "border-slate-200 bg-white text-slate-700 hover:bg-slate-50",
                  ].join(" ")}
                  aria-label={
                    unreadCount > 0
                      ? `${formatCount(unreadCount)} unread notifications`
                      : "Notifications"
                  }
                  aria-expanded={notificationMenuOpen}
                  aria-haspopup="dialog"
                >
                  {unreadCount > 0 ? (
                    <BellRing className="h-5 w-5" aria-hidden="true" />
                  ) : (
                    <Bell className="h-5 w-5" aria-hidden="true" />
                  )}

                  {unreadCount > 0 ? (
                    <span className="absolute -right-1.5 -top-1.5 flex min-h-5 min-w-5 items-center justify-center rounded-full bg-red-600 px-1 text-[10px] font-extrabold leading-none text-white ring-2 ring-white">
                      {formatUnreadBadge(unreadCount)}
                    </span>
                  ) : null}
                </button>

                {/* ===================================================
                 * NOTIFICATION PANEL
                 * =================================================== */}

                {notificationMenuOpen ? (
                  <div
                    className="fixed inset-x-3 top-[4.5rem] z-50 overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-2xl sm:absolute sm:inset-x-auto sm:right-0 sm:top-auto sm:mt-2 sm:w-[26rem]"
                    role="dialog"
                    aria-label="Administrator notifications"
                  >
                    <div className="flex items-center justify-between border-b border-slate-200 px-4 py-4">
                      <div>
                        <h2 className="font-display text-base font-extrabold text-slate-950">
                          Notifications
                        </h2>

                        <p className="mt-0.5 text-xs text-slate-500">
                          {unreadCount > 0
                            ? `${formatCount(unreadCount)} unread`
                            : "You're all caught up"}
                        </p>
                      </div>

                      <button
                        type="button"
                        onClick={() => void refreshNotifications(true)}
                        disabled={notificationsLoading}
                        className="focus-ring rounded-lg px-3 py-2 text-xs font-bold text-emerald-700 transition hover:bg-emerald-50 disabled:opacity-50"
                      >
                        Refresh
                      </button>
                    </div>

                    {notificationError ? (
                      <div className="border-b border-red-200 bg-red-50 px-4 py-3">
                        <p className="text-xs font-semibold leading-5 text-red-800">
                          {notificationError}
                        </p>
                      </div>
                    ) : null}

                    <div className="max-h-[min(65vh,34rem)] overflow-y-auto">
                      {notificationsLoading && notifications.length === 0 ? (
                        <div
                          className="flex min-h-40 items-center justify-center gap-3 px-5 py-8 text-sm text-slate-500"
                          role="status"
                        >
                          <LoaderCircle
                            className="h-5 w-5 animate-spin"
                            aria-hidden="true"
                          />
                          Loading notifications...
                        </div>
                      ) : notifications.length === 0 ? (
                        <EmptyNotificationState />
                      ) : (
                        <div className="divide-y divide-slate-100">
                          {notifications.map((notification) => (
                            <NotificationItem
                              key={notification.notificationId}
                              notification={notification}
                              busy={
                                notificationActionId ===
                                notification.notificationId
                              }
                              onOpen={() =>
                                void handleNotificationOpen(notification)
                              }
                              onToggleRead={() =>
                                void handleToggleRead(notification)
                              }
                              onDismiss={() =>
                                void handleDismissNotification(notification)
                              }
                            />
                          ))}
                        </div>
                      )}
                    </div>

                    <div className="border-t border-slate-200 bg-slate-50 px-4 py-3">
                      <p className="text-center text-xs leading-5 text-slate-500">
                        Notifications are saved to your administrator account
                        and remain available after reload or sign-in.
                      </p>
                    </div>
                  </div>
                ) : null}
              </div>

              {/* =====================================================
               * PROFILE MENU
               * ===================================================== */}

              <div ref={profileMenuRef} className="relative">
                <button
                  type="button"
                  onClick={() => {
                    setProfileMenuOpen((current) => !current);

                    setNotificationMenuOpen(false);
                  }}
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

                {profileMenuOpen ? (
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
                      {administrator?.role === "SUPER_ADMIN" ? (
                        <Link
                          to="/admin/users"
                          className="flex min-h-11 items-center gap-3 rounded-xl px-3 py-2 text-sm font-semibold text-slate-700 transition hover:bg-slate-100 hover:text-slate-950 focus:outline-none focus:ring-2 focus:ring-emerald-600/30"
                          role="menuitem"
                        >
                          <UsersRound className="h-5 w-5" aria-hidden="true" />
                          Administrator users
                        </Link>
                      ) : null}

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
                ) : null}
              </div>
            </div>
          </div>
        </header>

        {/* =============================================================
         * PAGE CONTENT
         * ============================================================= */}

        <main className="px-4 py-6 sm:px-6 lg:px-8 lg:py-8">
          <Outlet />
        </main>
      </div>
    </div>
  );
}

// =====================================================================
// NOTIFICATION ITEM
// =====================================================================

interface NotificationItemProps {
  notification: AdminInAppNotification;

  busy: boolean;

  onOpen: () => void;

  onToggleRead: () => void;

  onDismiss: () => void;
}

function NotificationItem({
  notification,
  busy,
  onOpen,
  onToggleRead,
  onDismiss,
}: NotificationItemProps) {
  const ResourceIcon = resolveNotificationIcon(notification.resourceType);

  return (
    <article
      className={[
        "relative px-4 py-4 transition",
        notification.read ? "bg-white" : "bg-emerald-50/60",
      ].join(" ")}
    >
      <div className="flex items-start gap-3">
        <div
          className={[
            "mt-0.5 flex h-10 w-10 shrink-0 items-center justify-center rounded-xl",
            notification.read
              ? "bg-slate-100 text-slate-600"
              : "bg-emerald-100 text-emerald-700",
          ].join(" ")}
        >
          <ResourceIcon className="h-5 w-5" aria-hidden="true" />
        </div>

        <button
          type="button"
          onClick={onOpen}
          disabled={busy}
          className="focus-ring min-w-0 flex-1 rounded-lg text-left disabled:cursor-wait"
        >
          <div className="flex items-start gap-2">
            <p
              className={[
                "min-w-0 flex-1 text-sm text-slate-900",
                notification.read ? "font-semibold" : "font-extrabold",
              ].join(" ")}
            >
              {notification.title}
            </p>

            {!notification.read ? (
              <span
                className="mt-1 h-2.5 w-2.5 shrink-0 rounded-full bg-emerald-600"
                aria-label="Unread"
              />
            ) : null}
          </div>

          <p className="mt-1 line-clamp-3 text-xs leading-5 text-slate-600">
            {notification.message}
          </p>

          <p className="mt-2 text-[11px] font-semibold text-slate-400">
            {formatRelativeTime(notification.createdAt)}
          </p>
        </button>
      </div>

      <div className="mt-3 flex items-center justify-end gap-1">
        <button
          type="button"
          onClick={onToggleRead}
          disabled={busy}
          className="focus-ring inline-flex min-h-8 items-center gap-1.5 rounded-lg px-2.5 py-1 text-xs font-bold text-slate-600 transition hover:bg-slate-100 hover:text-slate-900 disabled:opacity-50"
          title={notification.read ? "Mark unread" : "Mark read"}
        >
          {busy ? (
            <LoaderCircle
              className="h-3.5 w-3.5 animate-spin"
              aria-hidden="true"
            />
          ) : notification.read ? (
            <Bell className="h-3.5 w-3.5" aria-hidden="true" />
          ) : (
            <MailOpen className="h-3.5 w-3.5" aria-hidden="true" />
          )}

          {notification.read ? "Unread" : "Read"}
        </button>

        <button
          type="button"
          onClick={onDismiss}
          disabled={busy}
          className="focus-ring inline-flex min-h-8 items-center gap-1.5 rounded-lg px-2.5 py-1 text-xs font-bold text-slate-500 transition hover:bg-red-50 hover:text-red-700 disabled:opacity-50"
          title="Dismiss notification"
        >
          <Trash2 className="h-3.5 w-3.5" aria-hidden="true" />
          Dismiss
        </button>
      </div>
    </article>
  );
}

// =====================================================================
// EMPTY NOTIFICATION STATE
// =====================================================================

function EmptyNotificationState() {
  return (
    <div className="px-6 py-10 text-center">
      <div className="mx-auto flex h-12 w-12 items-center justify-center rounded-2xl bg-emerald-50 text-emerald-700">
        <Check className="h-6 w-6" aria-hidden="true" />
      </div>

      <p className="mt-4 font-bold text-slate-900">No notifications</p>

      <p className="mx-auto mt-1 max-w-xs text-xs leading-5 text-slate-500">
        New bookings, contact inquiries, and other operational updates will
        appear here.
      </p>
    </div>
  );
}

// =====================================================================
// SIDEBAR
// =====================================================================

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
      {!hideBrand ? (
        <div className="border-b border-slate-800 px-6 py-5">
          <AdminBrand />
        </div>
      ) : null}

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

// =====================================================================
// BRAND
// =====================================================================

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

// =====================================================================
// NAVIGATION LINK
// =====================================================================

interface AdminNavigationLinkProps {
  item: AdminNavigationItem;
}

function AdminNavigationLink({ item }: AdminNavigationLinkProps) {
  const Icon = item.icon;

  const location = useLocation();

  const customerWorkspaceActive =
    item.to === "/admin/customers" &&
    location.pathname.startsWith("/admin/customers/") &&
    !location.pathname.startsWith("/admin/customers/reviews");

  return (
    <NavLink
      to={item.to}
      end={item.end}
      className={({ isActive }) => {
        const active = isActive || customerWorkspaceActive;

        return [
          "group flex min-h-12 items-center gap-3 rounded-xl px-4 py-3 text-sm font-semibold transition focus:outline-none focus:ring-2 focus:ring-white/50",
          active
            ? "bg-white text-slate-950 shadow-sm"
            : "text-slate-300 hover:bg-white/10 hover:text-white",
        ].join(" ");
      }}
    >
      {({ isActive }) => {
        const active = isActive || customerWorkspaceActive;

        return (
          <>
            <Icon
              className={[
                "h-5 w-5 shrink-0",
                active
                  ? "text-slate-950"
                  : "text-slate-400 group-hover:text-white",
              ].join(" ")}
              aria-hidden="true"
            />

            <span className={active ? "text-slate-950" : "text-inherit"}>
              {item.label}
            </span>
          </>
        );
      }}
    </NavLink>
  );
}

// =====================================================================
// NOTIFICATION RESOURCE ICON
// =====================================================================

function resolveNotificationIcon(resourceType: string): typeof Bell {
  switch (resourceType) {
    case "BOOKING_REQUEST":
      return CalendarDays;

    case "CONTACT_INQUIRY":
      return MessageSquareText;

    case "CUSTOMER":
      return UserRound;

    case "CUSTOMER_REVIEW":
    case "REVIEW_INVITATION":
      return MessageSquareQuote;

    case "ADMIN_USER":
      return UsersRound;

    case "SERVICE":
      return Wrench;

    default:
      return Bell;
  }
}

// =====================================================================
// PORTAL PATH
// =====================================================================

function normalizePortalPath(value: string | null | undefined): string | null {
  if (!value) {
    return null;
  }

  const normalized = value.trim();

  if (!normalized || !normalized.startsWith("/admin")) {
    return null;
  }

  return normalized;
}

// =====================================================================
// PAGE TITLE
// =====================================================================

function resolvePageTitle(pathname: string): string {
  if (pathname === "/admin") {
    return "Dashboard";
  }

  // ===================================================================
  // ADMIN USERS
  // ===================================================================

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

  // ===================================================================
  // CUSTOMERS
  // ===================================================================

  if (pathname === "/admin/customers") {
    return "Customers";
  }

  if (pathname === "/admin/customers/new") {
    return "Create customer";
  }

  if (pathname === "/admin/customers/reviews") {
    return "Customer reviews";
  }

  if (pathname === "/admin/customers/reviews/new") {
    return "Record customer review";
  }

  if (pathname.startsWith("/admin/customers/reviews/")) {
    return "Customer review details";
  }

  if (pathname.startsWith("/admin/customers/")) {
    return "Customer details";
  }

  // ===================================================================
  // BOOKINGS
  // ===================================================================

  if (pathname === "/admin/bookings/new") {
    return "Create booking";
  }

  if (pathname.startsWith("/admin/bookings/")) {
    return "Booking details";
  }

  if (pathname.startsWith("/admin/bookings")) {
    return "Bookings";
  }

  // ===================================================================
  // CONTACT INQUIRIES
  // ===================================================================

  if (pathname.startsWith("/admin/contact-inquiries/")) {
    return "Contact inquiry details";
  }

  if (pathname.startsWith("/admin/contact-inquiries")) {
    return "Contact inquiries";
  }

  // ===================================================================
  // SERVICES
  // ===================================================================

  if (pathname.startsWith("/admin/services")) {
    return "Services";
  }

  // ===================================================================
  // SETTINGS
  // ===================================================================

  if (pathname.startsWith("/admin/settings")) {
    return "Settings";
  }

  return "Administration";
}

// =====================================================================
// ADMINISTRATOR DISPLAY
// =====================================================================

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

// =====================================================================
// NOTIFICATION DISPLAY HELPERS
// =====================================================================

function formatUnreadBadge(count: number): string {
  if (count > 99) {
    return "99+";
  }

  return String(count);
}

function formatCount(count: number): string {
  return count.toLocaleString("en-US");
}

function formatRelativeTime(value: string): string {
  const createdAt = new Date(value);

  if (Number.isNaN(createdAt.getTime())) {
    return value;
  }

  const now = Date.now();

  const differenceMs = Math.max(0, now - createdAt.getTime());

  const minute = 60_000;

  const hour = 60 * minute;

  const day = 24 * hour;

  if (differenceMs < minute) {
    return "Just now";
  }

  if (differenceMs < hour) {
    const minutes = Math.floor(differenceMs / minute);

    return `${minutes} ${minutes === 1 ? "minute" : "minutes"} ago`;
  }

  if (differenceMs < day) {
    const hours = Math.floor(differenceMs / hour);

    return `${hours} ${hours === 1 ? "hour" : "hours"} ago`;
  }

  if (differenceMs < 7 * day) {
    const days = Math.floor(differenceMs / day);

    return `${days} ${days === 1 ? "day" : "days"} ago`;
  }

  return new Intl.DateTimeFormat("en-US", {
    dateStyle: "medium",
  }).format(createdAt);
}
