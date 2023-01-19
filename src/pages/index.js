import {Logo} from '@/components/Logo'
import {Footer} from '@/components/home/Footer'
import NextLink from 'next/link'
import Head from 'next/head'
import {NavItems, NavPopover} from '@/components/Header'
import {ThemeToggle, useTheme} from '@/components/ThemeToggle'

import wormholeCitrus from '@/img/wormhole-citrus-transparent.png';
import wormholeAqua from '@/img/wormhole-aqua-transparent.png';
import {ReactComponent as DataPatternLight} from '@/img/data-pattern.svg';


function Header() {
  const [theme, setTheme] = useTheme();
  const wormholeImg = theme === 'dark' ? wormholeCitrus.src : wormholeAqua.src
  return (
    <header className="relative">
      <div className="px-4 sm:px-6 md:px-8 dark:bg-midnight-blue">
        <div
          className="relative pt-6 lg:pt-8 flex items-center justify-between text-slate-700 font-semibold text-sm leading-6 dark:text-slate-200 z-10">
          <Logo className="w-auto h-5"/>
          <div className="flex items-center">
            <NavPopover className="-my-1 ml-2 -mr-1" display="md:hidden"/>
            <div className="hidden md:flex items-center">
              <nav>
                <ul className="flex items-center space-x-8">
                  <NavItems/>
                </ul>
              </nav>
              <div className="flex items-center border-l border-slate-200 ml-6 pl-6 dark:border-slate-800">
                <ThemeToggle/>
                <a
                  href="https://github.com/orbitalapi/orbital"
                  className="ml-6 block text-slate-400 hover:text-slate-500 dark:hover:text-slate-300"
                >
                  <span className="sr-only">Orbital on GitHub</span>
                  <svg
                    viewBox="0 0 16 16"
                    className="w-5 h-5"
                    fill="currentColor"
                    aria-hidden="true"
                  >
                    <path
                      d="M8 0C3.58 0 0 3.58 0 8c0 3.54 2.29 6.53 5.47 7.59.4.07.55-.17.55-.38 0-.19-.01-.82-.01-1.49-2.01.37-2.53-.49-2.69-.94-.09-.23-.48-.94-.82-1.13-.28-.15-.68-.52-.01-.53.63-.01 1.08.58 1.23.82.72 1.21 1.87.87 2.33.66.07-.52.28-.87.51-1.07-1.78-.2-3.64-.89-3.64-3.95 0-.87.31-1.59.82-2.15-.08-.2-.36-1.02.08-2.12 0 0 .67-.21 2.2.82.64-.18 1.32-.27 2-.27.68 0 1.36.09 2 .27 1.53-1.04 2.2-.82 2.2-.82.44 1.1.16 1.92.08 2.12.51.56.82 1.27.82 2.15 0 3.07-1.87 3.75-3.65 3.95.29.25.54.73.54 1.48 0 1.07-.01 1.93-.01 2.2 0 .21.15.46.55.38A8.013 8.013 0 0016 8c0-4.42-3.58-8-8-8z"/>
                  </svg>
                </a>
              </div>
            </div>
          </div>
        </div>
        <div className="font-brand relative max-w-8xl mx-auto pt-20 sm:pt-24 lg:pt-32 relative">
          <div className="max-w-2xl pb-[15rem]">
            <h1 className="text-slate-900 text-4xl sm:text-5xl lg:text-6xl tracking-tight dark:text-white">
              Automated integration for microservices
            </h1>
            <p className="mt-6 text-3xl text-slate-600 dark:text-slate-400">
              Orbital eliminates the integration effort, so you can get back to shipping
            </p>
            <div className="mt-6 sm:mt-10 flex justify-left space-x-6 text-sm">
              <NextLink href="/docs/installation">
                <a
                  className="bg-citrus hover:bg-slate-700 focus:outline-none focus:ring-2 focus:ring-slate-400 focus:ring-offset-2 focus:ring-offset-slate-50 text-midnight-blue font-semibold h-12 px-6 rounded-lg w-10 flex items-center justify-center sm:w-auto dark:bg-citrus dark:highlight-white/20 dark:hover:bg-sky-400">
                  Get started
                </a>
              </NextLink>
            </div>

          </div>
          <img src={wormholeImg} className="h-[500px] rotate-[130deg] absolute top-0 right-0"/>
          <DataPatternLight
            className="right-0 absolute top-[340px] w-[900px] fill-black dark:fill-sky-100"></DataPatternLight>


        </div>
      </div>
    </header>
  )
}


export default function Home() {
  return (
    <>
      <Head>
        <meta
          key="twitter:title"
          name="twitter:title"
          content="Orbital - Automated integration for microservices"
        />
        <meta
          key="og:title"
          property="og:title"
          content="Orbital - Automated integration for microservices"
        />
        <title>Orbital - Automated integration for microservices.</title>
      </Head>
      <div
        className="mb-20 space-y-20 overflow-hidden sm:mb-32 sm:space-y-32 md:mb-40 md:space-y-40 dark:bg-midnight-blue">
        <Header/>
        <section className="text-center px-8 pb-[5rem]">
          <h2 className="font-brand text-slate-900 text-4xl tracking-tight sm:text-5xl dark:text-white">
            Connect APIs, Databases, Queues and more, on-the-fly
          </h2>
          <p className="mt-6 max-w-3xl mx-auto text-2xl">
            Orbital uses metadata in your OpenAPI specs & schemas to integrate data sources on demand.
          </p>
          <p className="mt-4 max-w-3xl mx-auto text-2xl">
            As things change, Orbital adapts.
          </p>
        </section>
      </div>
      <div className="pt-20 mb-20 space-y-20 overflow-hidden sm:pt-32 sm:mb-32 md:pt-40 md:mb-40">
        {/*<HowVyneWorks/>*/}
        {/*<QueryExamples/>*/}
        {/*<DebugTools/>*/}
        {/*<DataPipelines/>*/}
        {/*<FeatureArticles tag="blog" title="Latest from the blog" subtitle="From our brains to your eyeballs."/>*/}
      </div>
      <Footer/>
    </>
  )
}

Home.layoutProps = {
  meta: {
    // ogImage: socialCardLarge.src,
  },
}
