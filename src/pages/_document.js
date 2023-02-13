import clsx from 'clsx'
import NextDocument, {Head, Html, Main, NextScript} from 'next/document'
import {ServerStyleSheet} from "styled-components";

const FAVICON_VERSION = 3

function v(href) {
  return `${href}?v=${FAVICON_VERSION}`
}

export default class Document extends NextDocument {
  static async getInitialProps(ctx) {
    const sheet = new ServerStyleSheet()
    const originalRenderPage = ctx.renderPage

    // Enable styled-components.
    // See here: https://github.com/vercel/next.js/blob/canary/examples/with-styled-components/pages/_document.tsx
    // docs are here: https://styled-components.com/docs/advanced#nextjs

    try {
      ctx.renderPage = () =>
        originalRenderPage({
          enhanceApp: (App) => (props) =>
            sheet.collectStyles(<App {...props} />),
        })

      const initialProps = await NextDocument.getInitialProps(ctx)
      return {
        ...initialProps,
        styles: [initialProps.styles, sheet.getStyleElement()],
      }
    } finally {
      sheet.seal()
    }
  }

  render() {
    return (
      <Html lang="en" className="dark [--scroll-mt:9.875rem] lg:[--scroll-mt:6.3125rem]">
        <Head>
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
          <meta name="theme-color" content="#f8fafc"/>
          {/*         // Can't use leaderLine in webpack unfortunately,
         // as while it works at dev time, in prod builds
         // it fails as the library refers to `window`, which isn't
         // available in ssr.
         //
         // Instead, we're loading from a CDN, and inserting as a global.*/}
          <script src="https://cdn.jsdelivr.net/npm/leader-line@1.0.7/leader-line.min.js"
                  integrity="sha256-iKeFRzcz3iPVPlQcZXB/1wesZwIwnrY41rN7yaFvVB4=" crossOrigin="anonymous"></script>
        </Head>
        <body
          className={clsx('antialiased text-slate-500 dark:text-slate-400', {
            'bg-white dark:bg-midnight-blue': !this.props.dangerousAsPath.startsWith('/examples/'),
          })}
        >
        <Main/>
        <NextScript/>
        <script></script>
        </body>
      </Html>
    )
  }
}
