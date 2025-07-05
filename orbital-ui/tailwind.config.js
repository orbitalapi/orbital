module.exports = {
  mode: 'jit',
  // We need to prefix, as some classes like container
  // were widely used across the codebase prior to tailwind
  prefix: 'tw-',
  content: [
    './src/**/*.{html,ts}',
  ],
  darkMode: 'media',
  theme: {
    extend: {},
  },
  variants: {
    extend: {},
  },
  plugins: [require('@tailwindcss/forms'), require('@tailwindcss/typography')],
}
