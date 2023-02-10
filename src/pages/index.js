import {Logo} from '@/components/Logo'
import {Footer} from '@/components/home/Footer'
import NextLink from 'next/link'
import Head from 'next/head'
import {Header, NavItems, NavPopover} from '@/components/Header'
import {useTheme} from '@/components/ThemeToggle'

import wormholeCitrus from '@/img/wormhole-citrus-transparent.png';
import networkDiagram from '@/components/home/img/network-diagram.png';
import {ReactComponent as ArrowCitrus} from '@/img/arrow-citrus.svg';
import {ReactComponent as DataPatternLight} from '@/img/data-pattern.svg';
import PublishYourApi, {publishYourApiCodeSnippets} from "@/components/home/PublishYourApi";
import QueryExamples, {queryExampleCodeSnippets} from "@/components/home/QueryExamples";
import DebugTools from "@/components/home/DebugTools";


function HeroSection() {
  return (
    <header className="relative">
      <div className="px-4 sm:px-6 md:px-8 dark:bg-midnight-blue">
        <Header allowThemeToggle={false}/>
        <div className="font-brand relative max-w-8xl mx-auto pt-20 sm:pt-4 lg:pt-32 relative">
          <div className={'flex'}>  
            <div className="max-w-2xl sm:pb-10 pb-[6rem]">
              <h1 className="font-light text-5xl md:text-6xl leading-[4rem] md:leading-[5rem] tracking-tight dark:text-white">
                Automated integration for microservices
              </h1>
              <p className="sm:text-xl sm:leading-8 mt-6 text-3xl text-slate-600 dark:text-slate-400">
                Orbital eliminates the integration effort, so you can get back to <span className="text-citrus">shipping</span>
              </p>
              <div className="sm:mt-10 mt-14 flex justify-left space-x-6 text-lg">
                <NextLink href="/docs/installation">
                  <a
                    className="bg-citrus hover:bg-citrus-300 focus:outline-none focus:ring-2 focus:ring-slate-400 focus:ring-offset-2 focus:ring-offset-slate-50 text-midnight-blue font-bold h-12 px-6 rounded-full sm:w-full w-48 flex items-center justify-center dark:bg-citrus dark:highlight-white/20 dark:hover:bg-citrus-300 uppercase tracking-wider">
                    Get started
                  </a>
                </NextLink>
              </div>
            </div>
            <div className={'hidden lg:block bg-slate-900/75 z-10 p-8 backdrop-blur-md shadow-2xl rounded-md ml-20 -mt-24 mb-32'}>
              <img src={networkDiagram.src} className={'w-[560px]'}/>
            </div>

          </div>
          <img src={wormholeCitrus.src} className="sm:hidden md:h-[400px] lg:h-[600px] md:opacity-25 lg:opacity-50 rotate-[130deg] absolute top-0 right-0 block"/>

          <DataPatternLight
            className="sm:hidden md:left-80 lg:left-auto lg:right-0 absolute top-[320px] lg:top-[450px] w-[900px] lg:w-[1000px] fill-black dark:fill-sky-100"></DataPatternLight>

          <ArrowCitrus className="flex justify-center w-full mb-8 stroke-2"></ArrowCitrus>

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
      <div className="space-y-20 overflow-hidden sm:pt-32 md:pt-20 sm:mb-32 md:mb-40">
        <QueryExamples highlightedSnippets={queryExampleCodeHighlightedSnippets}/>
        <PublishYourApi highlightedSnippets={publishYourApiHighlightedSnippets}/>
        {/*<HowVyneWorks/>*/}

        <DebugTools/>
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
