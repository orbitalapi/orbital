import {Logo} from '@/components/Logo'
import {Footer} from '@/components/home/Footer'
import NextLink from 'next/link'
import Head from 'next/head'
import {Header, NavItems, NavPopover} from '@/components/Header'
import {useTheme} from '@/components/ThemeToggle'

import wormholeCitrus from '@/img/wormhole-citrus-transparent.png';
import wormholeAqua from '@/img/wormhole-aqua-transparent.png';
import {ReactComponent as DataPatternLight} from '@/img/data-pattern.svg';
import PublishYourApi, {publishYourApiCodeSnippets} from "@/components/home/PublishYourApi";
import QueryExamples, {queryExampleCodeSnippets} from "@/components/home/QueryExamples";


function HeroSection() {
  return (
    <header className="relative">
      <div className="px-4 sm:px-6 md:px-8 dark:bg-midnight-blue">
        <Header allowThemeToggle={false}/>
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
                  className="bg-citrus hover:bg-citrus-300 focus:outline-none focus:ring-2 focus:ring-slate-400 focus:ring-offset-2 focus:ring-offset-slate-50 text-midnight-blue font-semibold h-12 px-6 rounded-lg w-10 flex items-center justify-center sm:w-auto dark:bg-citrus dark:highlight-white/20 dark:hover:bg-citrus-300">
                  Get started
                </a>
              </NextLink>
            </div>

          </div>
          <img src={wormholeAqua.src} className="h-[500px] rotate-[130deg] absolute top-0 right-0 dark:hidden block"/>
          <img src={wormholeCitrus.src} className="h-[500px] rotate-[130deg] absolute top-0 right-0 hidden dark:block"/>
          <DataPatternLight
            className="right-0 absolute top-[340px] w-[900px] fill-black dark:fill-sky-100"></DataPatternLight>


        </div>
      </div>
    </header>
  )
}


export default function Home({publishYourApiHighlightedSnippets, queryExampleCodeHighlightedSnippets}) {
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
      {/*Used to have: mb-20 space-y-20  sm:mb-32 sm:space-y-32 md:mb-40 md:space-y-40 */}
      <div
        className="overflow-hidden dark:bg-midnight-blue">
        <HeroSection/>
      </div>
      <div className="pt-20 mb-20 space-y-20 overflow-hidden sm:pt-32 sm:mb-32 md:pt-40 md:mb-40">
        <QueryExamples highlightedSnippets={queryExampleCodeHighlightedSnippets}/>
        <PublishYourApi highlightedSnippets={publishYourApiHighlightedSnippets}/>
        {/*<HowVyneWorks/>*/}

        {/*<DebugTools/>*/}
        {/*<DataPipelines/>*/}
        {/*<FeatureArticles tag="blog" title="Latest from the blog" subtitle="From our brains to your eyeballs."/>*/}
      </div>
      <Footer/>
    </>
  )
}


export function getStaticProps() {
  let {highlightCodeSnippets} = require('@/components/Guides/Snippets')

  return {
    props: {
      publishYourApiHighlightedSnippets: highlightCodeSnippets(publishYourApiCodeSnippets),
      queryExampleCodeHighlightedSnippets: highlightCodeSnippets(queryExampleCodeSnippets)
    },
  }
}
