/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{vue,js,ts,jsx,tsx}'],
  darkMode: 'class',
  theme: {
    extend: {
      colors: {
        primary: {
          DEFAULT: '#4f46e5',
          50: '#eef2ff', 100: '#e0e7ff', 200: '#c7d2fe', 300: '#a5b4fc',
          400: '#818cf8', 500: '#6366f1', 600: '#4f46e5', 700: '#4338ca',
          800: '#3730a3', 900: '#312e81', 950: '#1e1b4b',
        },
        accent: { DEFAULT: '#f59e0b', 50: '#fffbeb', 400: '#fbbf24', 500: '#f59e0b', 600: '#d97706' },
        success: { DEFAULT: '#10b981', 50: '#ecfdf5', 500: '#10b981', 600: '#059669' },
        danger: { DEFAULT: '#ef4444', 50: '#fef2f2', 500: '#ef4444', 600: '#dc2626' },
        surface: { DEFAULT: '#ffffff', hover: '#f8fafc', muted: '#f1f5f9' },
      },
      fontFamily: {
        sans: ['Inter', '-apple-system', 'BlinkMacSystemFont', 'Segoe UI', 'PingFang SC', 'Hiragino Sans GB', 'Microsoft YaHei', 'Helvetica Neue', 'Arial', 'sans-serif'],
      },
      borderRadius: {
        '2xl': '1rem',
        '3xl': '1.25rem',
      },
      borderWidth: {
        '1.5': '1.5px',
      },
      boxShadow: {
        'card': '0 1px 3px 0 rgba(0,0,0,0.04), 0 4px 12px -3px rgba(79,70,229,0.06)',
        'card-hover': '0 12px 28px -4px rgba(0,0,0,0.08), 0 4px 12px -2px rgba(0,0,0,0.04)',
        'glow': '0 0 0 4px rgba(79,70,229,0.1)',
      },
      animation: {
        'fade-up': 'fade-in-up 0.4s ease both',
        'fade-scale': 'fade-in-scale 0.3s ease both',
        'shimmer': 'shimmer 1.8s ease-in-out infinite',
      },
    },
  },
  plugins: [],
}
