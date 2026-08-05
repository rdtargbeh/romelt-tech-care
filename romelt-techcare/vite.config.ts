/**
 * ================================================================
 * ROMELT TECHCARE — VITE CONFIGURATION
 * ================================================================
 *
 * Purpose:
 * Configures the Vite build system for the Romelt TechCare website.
 *
 * Responsibilities:
 * - Enables React support.
 * - Enables Tailwind CSS v4 through the official Vite plugin.
 * - Provides the "@" alias for imports from the src directory.
 * - Configures the local development server.
 *
 * Future integration:
 * Backend API proxy configuration can be added here when booking,
 * customer, ticketing, payment, and authentication APIs are created.
 * ================================================================
 */

import path from "node:path";

import tailwindcss from "@tailwindcss/vite";
import react from "@vitejs/plugin-react";
import { defineConfig } from "vite";

export default defineConfig({
  plugins: [react(), tailwindcss()],

  resolve: {
    alias: {
      "@": path.resolve(__dirname, "./src"),
    },
  },

  server: {
    port: 5173,
    open: true,
  },

  preview: {
    port: 4173,
  },
});
