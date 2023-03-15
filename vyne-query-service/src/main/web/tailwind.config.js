module.exports = {
  mode: 'jit',
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
