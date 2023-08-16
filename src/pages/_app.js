import '../css/fonts.css'
import '../css/main.css'
import 'focus-visible'
import {Fragment, useEffect, useState} from 'react'
import {Header} from '@/components/Header'
import {Description, OgDescription, OgTitle, Title} from '@/components/Meta'
import Router from 'next/router'
import ProgressBar from '@badrap/bar-of-progress'
import Head from 'next/head'
import {ResizeObserver} from '@juggle/resize-observer'
import 'intersection-observer'
import {SearchProvider} from '@/components/Search'
import Script from 'next/script';


import posthog from 'posthog-js'
import {PostHogProvider} from 'posthog-js/react'

// Check that PostHog is client-side (used to handle Next.js SSR)
if (typeof window !== 'undefined') {
  posthog.init(process.env.NEXT_PUBLIC_POSTHOG_KEY, {
    api_host: process.env.NEXT_PUBLIC_POSTHOG_HOST || 'https://app.posthog.com',
    // Enable debug mode in development
    loaded: (posthog) => {
      if (process.env.NODE_ENV === 'development') posthog.debug()
    }
  })
}

if (typeof window !== 'undefined' && !('ResizeObserver' in window)) {
  window.ResizeObserver = ResizeObserver
}

const progress = new ProgressBar({
  size: 2,
  color: '#38bdf8',
  className: 'bar-of-progress',
  delay: 100,
})

// this fixes safari jumping to the bottom of the page
// when closing the search modal using the `esc` key
if (typeof window !== 'undefined') {
  progress.start()
  progress.finish()
}

Router.events.on('routeChangeStart', () => progress.start())
Router.events.on('routeChangeComplete', () => progress.finish())
Router.events.on('routeChangeError', () => progress.finish())

export default function App({Component, pageProps, router}) {
  let [navIsOpen, setNavIsOpen] = useState(false)

  useEffect(() => {
    if (!navIsOpen) return

    function handleRouteChange() {
      setNavIsOpen(false);
      posthog?.capture('$pageview')
    }

    Router.events.on('routeChangeComplete', handleRouteChange)
    return () => {
      Router.events.off('routeChangeComplete', handleRouteChange)
    }
  }, [navIsOpen])

  const Layout = Component.layoutProps?.Layout || Fragment
  const layoutProps = Component.layoutProps?.Layout
    ? {layoutProps: Component.layoutProps, navIsOpen, setNavIsOpen}
    : {}
  const showHeader = router.pathname !== '/'
  const meta = Component.layoutProps?.meta || {}
  const description =
    meta.metaDescription || meta.description || 'Documentation for Orbital - automated integration.'
  let image = meta.ogImage ?? meta.image
  image = image
    ? `https://orbitalhq.com${image.default?.src ?? image.src ?? image}`
    : `https://orbitalhq.com/api/og?path=${router.pathname}`

  if (router.pathname.startsWith('/examples/')) {
    return <Component {...pageProps} />
  }


  return (
    <>
      {/*  // Can't use leaderLine in webpack unfortunately,
         // as while it works at dev time, in prod builds
         // it fails as the library refers to `window`, which isn't
         // available in ssr.
         //
         // Instead, we're loading from a CDN, and inserting as a global.*/}
      <Script
        src="https://cdn.jsdelivr.net/npm/leader-line@1.0.7/leader-line.min.js"
        strategy="beforeInteractive"
        onError={(e) => {
          console.error('Script failed to load', e)
        }}
      />
      <Title>{meta.metaTitle || meta.title}</Title>
      {meta.ogTitle && <OgTitle>{meta.ogTitle}</OgTitle>}
      <Description>{description}</Description>
      {meta.ogDescription && <OgDescription>{meta.ogDescription}</OgDescription>}
      <Head>
        <meta key="twitter:card" name="twitter:card" content="summary_large_image"/>
        <meta key="twitter:site" name="twitter:site" content="@orbitalapi"/>
        <meta key="twitter:image" name="twitter:image" content={image}/>
        <meta key="twitter:creator" name="twitter:creator" content="@orbitalapi"/>
        <meta
          key="og:url"
          property="og:url"
          content={`https://orbitalhq.com${router.pathname}`}
        />
        <meta key="og:type" property="og:type" content="article"/>
        <meta key="og:image" property="og:image" content={image}/>
        <link rel="alternate" type="application/rss+xml" title="RSS 2.0" href="/feeds/feed.xml"/>
        <link rel="alternate" type="application/atom+xml" title="Atom 1.0" href="/feeds/atom.xml"/>
        <link rel="alternate" type="application/json" title="JSON Feed" href="/feeds/feed.json"/>
      </Head>
      <PostHogProvider client={posthog}>
        <SearchProvider>
          {showHeader && (
            <Header
              hasNav={Boolean(Component.layoutProps?.Layout?.nav)}
              navIsOpen={navIsOpen}
              onNavToggle={(isOpen) => setNavIsOpen(isOpen)}
              title={meta.title}
              className="sm:px-6 md:px-8"
            />
          )}
          <Layout {...layoutProps}>
            <Component {...pageProps} />
          </Layout>
        </SearchProvider>
      </PostHogProvider>
    </>
  )
}
