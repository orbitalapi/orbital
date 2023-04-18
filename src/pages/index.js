import { Footer } from '@/components/home/Footer';
import Head from 'next/head';
import { Header } from '@/components/Header';
import GetStartedButton from '@/components/GetStartedButton';
import Wormhole from '@/components/Wormhole';
import Image from 'next/future/image';

import { Paragraph } from '@/components/home/common';
import networkDiagram from '@/components/home/img/network-diagram.png';
import { ReactComponent as ArrowCitrus } from '@/img/arrow-citrus.svg';
import { ReactComponent as DataPatternLight } from '@/img/data-pattern.svg';
import PublishYourApi, { publishYourApiCodeSnippets } from '@/components/home/PublishYourApi';
import QueryExamples, { queryExampleCodeSnippets } from '@/components/home/QueryExamples';
import WhereToUseSection from '@/components/home/WhereToUseSection';
import DebugTools from '@/components/home/DebugTools';
import WatchADemoButton from '../components/DemosModal';
import OrbitalLogo from '@/img/wormhole-citrus-transparent.png';
import { FiCheck, FiCopy } from "react-icons/fi";
import { IconContext } from "react-icons";
import { useEffect, useState } from 'react';



function HeroSection() {
  return (
    <>
      <header className='relative'>
        <div className='sm:px-6 md:px-8 dark:bg-midnight-blue'>
          <Header allowThemeToggle={false} />
          <div className='font-brand relative max-w-8xl md:min-h-[70vh] mx-auto sm:pt-16 md:pt-32 relative'>
            <div className={'flex'}>
              <div className='max-w-2xl sm:pb-10 pb-[6rem]'>
                <h1
                  className='font-light text-5xl md:text-6xl leading-[4rem] md:leading-[5rem] tracking-tight dark:text-white'>
                  Automated integration for microservices
                </h1>
                <p className='sm:text-xl sm:leading-8 mt-6 text-3xl text-slate-600 dark:text-slate-400'>
                  Orbital eliminates the integration effort, so you can get back to <span
                    className='text-citrus'>shipping</span>
                </p>
                <div className='sm:mt-10 mt-14 flex justify-left gap-6 text-base md:text-lg flex-wrap'>
                  <GetStartedButton />
                  <WatchADemoButton />
                </div>
              </div>
              <div
                className={'hidden xl:block bg-slate-900/75 z-10 p-8 backdrop-blur-md shadow-2xl rounded-md ml-20 -mt-24 mb-32'}>
                <img src={networkDiagram.src} className={'w-[560px]'} alt="Orbital example integration diagram" />
              </div>

            </div>

            <Wormhole className="lg:opacity-50" />

            <DataPatternLight
              className='sm:hidden md:left-80 lg:left-auto lg:right-0 absolute top-[380px] md:top-[480px] w-[900px] lg:w-[900px] fill-black dark:fill-sky-100 pointer-events-none'></DataPatternLight>

            <ArrowCitrus className='flex justify-center w-full mb-8 stroke-2 md:absolute md:bottom-0'></ArrowCitrus>

          </div>
        </div>
      </header>
    </>
  );
}


export default function Home({
  publishYourApiHighlightedSnippets, queryExampleCodeHighlightedSnippets
}) {
  const [cmdCopied, setCmdCopied] = useState(false);
  const [isNativeClipboard, setNativeClipboard] = useState(false);

  useEffect(() => {
    if (navigator.clipboard) {
      setNativeClipboard(true);
    }
  }, []);

  const dockerCmd = "docker run -p 9022:9022 orbitalhq/orbital"

  const copyToClipboard = () => {
    if (isNativeClipboard) {
      navigator.clipboard.writeText(dockerCmd);
      setCmdCopied(true);
      setTimeout(() => {
        setCmdCopied(false);
      }, 2000);
    }
  };


  return (
    <>
      <Head>
        <meta
          key='twitter:title'
          name='twitter:title'
          content='Orbital - Automated integration for microservices'
        />
        <meta
          key='og:title'
          property='og:title'
          content='Orbital - Automated integration for microservices'
        />
        <title>Orbital - Automated integration for microservices.</title>
      </Head>
      {/*Used to have: mb-20 space-y-20  sm:mb-32 sm:space-y-32 md:mb-40 md:space-y-40 */}
      <div
        className='overflow-hidden dark:bg-midnight-blue'>
        <HeroSection />
      </div>
      <div className='overflow-hidden sm:mb-32 md:mb-40'>
        <div className="w-full py-10">
          <Paragraph>
            <span className="font-bold">Get started in seconds</span> with our Docker image.
          </Paragraph>
          <div className="mx-auto bg-gray-900 w-fit mt-10 p-8 rounded-lg border border-tint-8">
            <IconContext.Provider value={{ size: "1.5em", color: cmdCopied ? '#e0ff4f' : '' }}>
              <p className="font-mono">
                &gt; {dockerCmd}
                {cmdCopied
                  ? (<FiCheck className="inline-block ml-5" />)
                  : (<FiCopy className="inline-block ml-5 cursor-pointer" onClick={copyToClipboard} />)
                }
              </p>
            </IconContext.Provider>
          </div>
        </div>
        
        <WhereToUseSection className="py-20" />

        <QueryExamples highlightedSnippets={queryExampleCodeHighlightedSnippets}
          className='sm:pt-32 md:pt-20 pb-20' />
        <PublishYourApi highlightedSnippets={publishYourApiHighlightedSnippets} />
        {/*<HowVyneWorks/>*/}

        <DebugTools className='sm:pt-32 md:pt-20 pb-20' />
        {/*<DataPipelines/>*/}
        {/*<FeatureArticles tag="blog" title="Latest from the blog" subtitle="From our brains to your eyeballs."/>*/}
      </div>
      <Footer />
    </>
  );
}


export function getStaticProps() {
  let { highlightCodeSnippets } = require('@/components/Guides/Snippets');

  return {
    props: {
      publishYourApiHighlightedSnippets: highlightCodeSnippets(publishYourApiCodeSnippets),
      queryExampleCodeHighlightedSnippets: highlightCodeSnippets(queryExampleCodeSnippets)
    }
  };
}
