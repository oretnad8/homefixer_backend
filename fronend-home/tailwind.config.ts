import type { Config } from "tailwindcss";

const config: Config = {
  content: [
    "./pages/**/*.{js,ts,jsx,tsx,mdx}",
    "./components/**/*.{js,ts,jsx,tsx,mdx}",
    "./app/**/*.{js,ts,jsx,tsx,mdx}",
  ],
  theme: {
    extend: {
      colors: {
        // Primarios
        primary: {
          50:  "#eff6ff",
          100: "#dbeafe",
          200: "#bfdbfe",
          300: "#93c5fd",
          400: "#60a5fa",
          500: "#3b82f6",
          600: "#2563eb",
          700: "#1d4ed8",
          800: "#1e40af",
          900: "#1e3a8a",
        },
        // Semánticos
        success: {
          DEFAULT: "#16a34a",
          light: "#dcfce7",
          dark: "#15803d",
        },
        warning: {
          DEFAULT: "#d97706",
          light: "#fef3c7",
          dark: "#b45309",
        },
        danger: {
          DEFAULT: "#dc2626",
          light: "#fee2e2",
          dark: "#b91c1c",
        },
        gold: {
          DEFAULT: "#ca8a04",
          light: "#fef9c3",
          dark: "#a16207",
        },
        // Texto
        ink: "#111827",
        subtle: "#6b7280",
        // Superficies
        surface: {
          DEFAULT: "#ffffff",
          muted: "#f9fafb",
        },
        // Borde
        border: "#e5e7eb",
      },
      backgroundImage: {
        "gradient-brand": "linear-gradient(135deg, #60a5fa 0%, #1d4ed8 100%)",
        "gradient-brand-v": "linear-gradient(180deg, #3b82f6 0%, #1d4ed8 100%)",
      },
      borderRadius: {
        card: "1rem",
        input: "0.5rem",
        pill: "9999px",
      },
      boxShadow: {
        card: "0 1px 3px 0 rgb(0 0 0 / 0.1), 0 1px 2px -1px rgb(0 0 0 / 0.1)",
        "card-md": "0 4px 6px -1px rgb(0 0 0 / 0.1), 0 2px 4px -2px rgb(0 0 0 / 0.1)",
        "card-lg": "0 10px 15px -3px rgb(0 0 0 / 0.1), 0 4px 6px -4px rgb(0 0 0 / 0.1)",
      },
      fontFamily: {
        sans: ["Inter", "system-ui", "sans-serif"],
      },
    },
  },
  plugins: [],
};

export default config;
