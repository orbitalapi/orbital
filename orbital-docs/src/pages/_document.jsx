import {Head, Html, Main, NextScript} from 'next/document'

const FAVICON_VERSION = 3

function v(href) {
   return `${href}?v=${FAVICON_VERSION}`
}

const themeScript = `
  let isDarkMode = window.matchMedia('(prefers-color-scheme: dark)')

  function updateTheme(theme) {
    theme = theme ?? window.localStorage.theme ?? 'system'

    if (theme === 'dark' || (theme === 'system' && isDarkMode.matches)) {
      document.documentElement.classList.add('dark')
    } else if (theme === 'light' || (theme === 'system' && !isDarkMode.matches)) {
      document.documentElement.classList.remove('dark')
    }

    return theme
  }

  function updateThemeWithoutTransitions(theme) {
    updateTheme(theme)
    document.documentElement.classList.add('[&_*]:!transition-none')
    window.setTimeout(() => {
      document.documentElement.classList.remove('[&_*]:!transition-none')
    }, 0)
  }

  document.documentElement.setAttribute('data-theme', updateTheme())

  new MutationObserver(([{ oldValue }]) => {
    let newValue = document.documentElement.getAttribute('data-theme')
    if (newValue !== oldValue) {
      try {
        window.localStorage.setItem('theme', newValue)
      } catch {}
      updateThemeWithoutTransitions(newValue)
    }
  }).observe(document.documentElement, { attributeFilter: ['data-theme'], attributeOldValue: true })

  isDarkMode.addEventListener('change', () => updateThemeWithoutTransitions())
`

export default function Document() {
  return (
    <Html className="antialiased [font-feature-settings:'ss01']" lang="en">
       <Head>
          <script dangerouslySetInnerHTML={{__html: themeScript}}/>
          <link rel="apple-touch-icon" sizes="180x180" href={v('/favicons/apple-touch-icon.png')}/>
          <link rel="icon" type="image/png" sizes="32x32" href={v('/favicons/favicon-32x32.png')}/>
          <link rel="icon" type="image/png" sizes="16x16" href={v('/favicons/favicon-16x16.png')}/>
          <link rel="manifest" href={v('/favicons/site.webmanifest')}/>
          <link rel="mask-icon" href={v('/favicons/safari-pinned-tab.svg')} color="#38bdf8"/>
          <link rel="shortcut icon" href={v('/favicons/favicon.ico')}/>
          <meta name="apple-mobile-web-app-title" content="Orbital"/>
          <meta name="application-name" content="Orbital"/>
          <meta name="msapplication-TileColor" content="#38bdf8"/>
          <meta name="msapplication-config" content={v('/favicons/browserconfig.xml')}/>

       </Head>
       <body className="bg-white dark:bg-slate-900">
       <Main/>
       <NextScript/>
       </body>
    </Html>
  )
}
