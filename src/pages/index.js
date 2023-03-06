import { Footer } from '@/components/home/Footer';
import NextLink from 'next/link';
import Head from 'next/head';
import { Header } from '@/components/Header';

import wormholeCitrus from '@/img/wormhole-citrus-transparent.png';
import networkDiagram from '@/components/home/img/network-diagram.png';
import { ReactComponent as ArrowCitrus } from '@/img/arrow-citrus.svg';
import { ReactComponent as DataPatternLight } from '@/img/data-pattern.svg';
import PublishYourApi, { publishYourApiCodeSnippets } from '@/components/home/PublishYourApi';
import QueryExamples, { queryExampleCodeSnippets } from '@/components/home/QueryExamples';
import DebugTools from '@/components/home/DebugTools';
import { Fragment, useState } from 'react';
import { Dialog, Transition } from '@headlessui/react';
import clsx from 'clsx';


function HeroSection() {
  const [playerVisible, setPlayerVisible] = useState(false);

  function PlayerModal() {


    const videos = [
      {
        title: 'Introduction',
        duration: '2 mins',
        link: 'https://www.loom.com/embed/611473f78fa54235bc70e5d0dea13534'
      },
      {
        title: 'Handling breaking changes with Orbital',
        duration: '3 mins',
        link: 'https://www.loom.com/embed/ff01b2a3655047c58f60240aaae54445'
      }
    ];

    const [currentVideo, setCurrentVideo] = useState(videos[0]);

    function onVideoClicked(event, video) {
      event.stopPropagation();
      event.nativeEvent.stopImmediatePropagation();
      event.preventDefault();
      setCurrentVideo(video);
    }


    return (<div className='fixed inset-0 z-10 overflow-y-auto'>
      <div className='flex min-h-full items-center justify-center p-4 text-center sm:items-center sm:p-0'>
        <Transition.Child
          as={Fragment}
          enter='ease-out duration-300'
          enterFrom='opacity-0 translate-y-4 sm:translate-y-0 sm:scale-95'
          enterTo='opacity-100 translate-y-0 sm:scale-100'
          leave='ease-in duration-200'
          leaveFrom='opacity-100 translate-y-0 sm:scale-100'
          leaveTo='opacity-0 translate-y-4 sm:translate-y-0 sm:scale-95'
        >
          <Dialog.Panel
            className='relative transform overflow-hidden rounded-lg bg-slate-800 px-4 pt-5 pb-4 text-left shadow-2xl transition-all sm:my-8 sm:w-full sm:max-w-sm sm:p-6 w-[75vw] h-[75vh]'>
            <div className='flex w-full h-full'>
              <iframe className='w-[80%]' src={currentVideo.link}
                      frameBorder='0' webkitallowfullscreen mozallowfullscreen allowFullScreen></iframe>
              <div className='flex-grow px-8 py-12'>
                <h2 className='text-3xl text-slate-50 font-extrabold py-8'>Demos</h2>
                {videos.map(video => {
                  return (<div key={video.link} className='mb-8'>
                    <a onClick={(e) => onVideoClicked(e, video)} href=''>
                      <h3 className={clsx(
                        'text-xl text-slate-50 mb-1',
                        video.link === currentVideo.link ? 'text-citrus font-bold' : 'text-slate-50 hover:text-sky-300'
                      )}>{video.title}</h3>
                    </a>
                    {
                      (video.link === currentVideo.link) && (
                        <div className={'flex items-center text-citrus mb-2 text-sm font-bold'}>
                          <svg xmlns='http://www.w3.org/2000/svg' fill='none' viewBox='0 0 24 24' strokeWidth={1.5}
                               stroke='currentColor' className='w-4 h-4 mr-4'>
                            <path strokeLinecap='round' strokeLinejoin='round'
                                  d='M5.25 5.653c0-.856.917-1.398 1.667-.986l11.54 6.348a1.125 1.125 0 010 1.971l-11.54 6.347a1.125 1.125 0 01-1.667-.985V5.653z' />
                          </svg>
                          <span>Now playing</span>

                        </div>
                      )}

                    <div className='flex items-center'>
                      <svg xmlns='http://www.w3.org/2000/svg' fill='none' viewBox='0 0 24 24' strokeWidth='1.5'
                           stroke='currentColor' className='w-4 h-4 mr-4'>
                        <path strokeLinecap='round' strokeLinejoin='round'
                              d='M12 6v6h4.5m4.5 0a9 9 0 11-18 0 9 9 0 0118 0z' />
                      </svg>
                      <span>{video.duration}</span>
                    </div>


                  </div>);
                })}

              </div>
            </div>

          </Dialog.Panel>
        </Transition.Child>
      </div>
    </div>);
  }


  return (
    <>
      <Transition.Root show={playerVisible} as={Fragment}>
        <Dialog as='div' className='relative z-10' onClose={setPlayerVisible}>
          <Transition.Child
            as={Fragment}
            enter='ease-out duration-300'
            enterFrom='opacity-0'
            enterTo='opacity-100'
            leave='ease-in duration-200'
            leaveFrom='opacity-100'
            leaveTo='opacity-0'
          >
            <div className='fixed inset-0 bg-slate-800 bg-opacity-75 backdrop-blur-sm  transition-opacity' />
          </Transition.Child>
          <PlayerModal></PlayerModal>

        </Dialog>
      </Transition.Root>

      <header className='relative'>
        <div className='sm:px-6 md:px-8 dark:bg-midnight-blue'>
          <Header allowThemeToggle={false} />
          <div className='font-brand relative max-w-8xl mx-auto sm:pt-16 md:pt-32 relative'>
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
                <div className='sm:mt-10 mt-14 flex justify-left space-x-6 text-lg'>
                  <NextLink href='/docs'>
                    <a
                      className='bg-citrus hover:bg-citrus-300 focus:outline-none focus:ring-2 focus:ring-slate-400 focus:ring-offset-2 focus:ring-offset-slate-50 text-midnight-blue font-bold h-12 px-6 rounded-full sm:w-full w-48 flex items-center justify-center dark:bg-citrus dark:highlight-white/20 dark:hover:bg-citrus-300 uppercase tracking-wider'>
                      Get started
                    </a>
                  </NextLink>
                  <button
                    className='bg-midnight-blue hover:bg-slate-300/20 color-white text-white font-bold h-12 px-6 rounded-full border-2 border-white sm:w-full w-60 flex items-center justify-center uppercase tracking-wider'
                    onClick={() => setPlayerVisible(true)}
                  >
                    Watch a demo {'>'}
                  </button>
                </div>
              </div>
              <div
                className={'hidden xl:block bg-slate-900/75 z-10 p-8 backdrop-blur-md shadow-2xl rounded-md ml-20 -mt-24 mb-32'}>
                <img src={networkDiagram.src} className={'w-[560px]'} />
              </div>

            </div>
            <img src={wormholeCitrus.src}
                 className='sm:hidden md:h-[400px] lg:h-[600px] md:opacity-25 lg:opacity-50 rotate-[130deg] absolute top-0 right-0 block' />

            <DataPatternLight
              className='sm:hidden md:left-80 lg:left-auto lg:right-0 absolute top-[320px] lg:top-[450px] w-[900px] lg:w-[1000px] fill-black dark:fill-sky-100 pointer-events-none'></DataPatternLight>

            <ArrowCitrus className='flex justify-center w-full mb-8 stroke-2'></ArrowCitrus>

          </div>
        </div>
      </header>
    </>
  );
}


export default function Home({
                               publishYourApiHighlightedSnippets, queryExampleCodeHighlightedSnippets
                             }) {
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
