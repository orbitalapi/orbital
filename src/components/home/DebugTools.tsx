import * as React from 'react';
import { useState } from 'react';
import Image from 'next/future/image';
import { BigText, Caption, LearnMoreLink, Paragraph } from '@/components/home/common';
import { Widont } from '../Widont';
import { Tabs } from '../Tabs';
import WormholeImg from '@/img/wormhole-citrus-transparent.png'
import QueryLinageImg from './img/query-lineage.png';
import SequenceDiagramImg from './img/sequence-diagram.png';
import CellLineageImg from './img/cell-lineage.png';
import { AnimatePresence, motion } from 'framer-motion';

const paragraphClass = 'text-lg font-semibold text-white mb-8'
const ImgClass = 'relative rounded-l md:rounded-xl shadow-xl ring-1 ring-black ring-opacity-5 m-auto';
const CellLineage = () => {
  return (
    <div className="flex flex-col">
      <Paragraph className={paragraphClass}>See the full end-to-end lineage for every value
        returned. </Paragraph>
      <Paragraph className={paragraphClass}>Unlike traditional
        lineage tools - which are manual and time consuming, Orbital's linage is automatically captured at
        runtime, so you can be confident it's what actually happened.
      </Paragraph>
      <div className="w-full h-auto">
        <Image
          className={ImgClass}
          src={CellLineageImg}
          alt="Orbital cell lineage app screenshot"
        />
      </div>
    </div>
  )
}
const QueryLineage = () => {
  return (
    <>
      <Paragraph className={paragraphClass}>See a high level summary of traffic between data sources to know where
        your data is sourced from.
      </Paragraph>
      <img
        className={ImgClass}
        src={QueryLinageImg.src}
      />
    </>
  )
}

const Traffic = () => {
  return (<>
    <Paragraph className={paragraphClass}>Drill into every call that Orbital makes to see the request and response
    </Paragraph>
    <img
      className={ImgClass}
      src={SequenceDiagramImg.src}
    />
  </>
  )
}

let tabs = {
  'Cell lineage': (selected) => (
    <>
      <path fillRule="evenodd" clipRule="evenodd"
        d="M36 12C38.2091 12 40 10.2091 40 8C40 5.79086 38.2091 4 36 4C33.7909 4 32 5.79086 32 8C32 10.2091 33.7909 12 36 12Z"
        fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
      <path fillRule="evenodd" clipRule="evenodd"
        d="M14 12C16.2091 12 18 10.2091 18 8C18 5.79086 16.2091 4 14 4C11.7909 4 10 5.79086 10 8C10 10.2091 11.7909 12 14 12Z"
        fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
      <path fillRule="evenodd" clipRule="evenodd"
        d="M14 44C16.2091 44 18 42.2091 18 40C18 37.7909 16.2091 36 14 36C11.7909 36 10 37.7909 10 40C10 42.2091 11.7909 44 14 44Z"
        fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
      <path d="M14 12L14 36L14 33C14 25 36 24 36 16V12" stroke="currentColor" strokeWidth="2"
        strokeLinecap="round" strokeLinejoin="round" />
    </>
  ),
  'Query lineage': (selected) => (
    <>
      <rect width="48" height="48" fill="white" fillOpacity="0.01" />
      <path d="M26 24L42 24" stroke="currentColor" strokeWidth="2" strokeLinecap="round"
        strokeLinejoin="round" />
      <path d="M26 38H42" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
      <path d="M26 10H42" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
      <path d="M18 24L6 24C6 24 7.65685 24 10 24M18 38C12 36 16 24 10 24M18 10C12 12 16 24 10 24"
        stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
    </>
  ),
  Traffic: (selected) => (
    <>
      <path d="M14 25.9999L5 34.9999L14 43.9999" stroke="currentColor" strokeWidth="2" strokeLinecap="round"
        strokeLinejoin="round" />
      <path d="M5 35.0083H22.5" stroke="currentColor" strokeWidth="2" strokeLinecap="round"
        strokeLinejoin="round" />
      <path d="M34 18L43 27L34 36" stroke="currentColor" strokeWidth="2" strokeLinecap="round"
        strokeLinejoin="round" />
      <path d="M43 27.0084H25.5" stroke="currentColor" strokeWidth="2" strokeLinecap="round"
        strokeLinejoin="round" />
      <path d="M4.5 24V7.5L43.5 7.5V15" stroke="currentColor" strokeWidth="2" strokeLinecap="round"
        strokeLinejoin="round" />
    </>
  ),


}


function DebugTools({ className = '' }) {
  const [tab, setTab] = useState('Cell lineage')
  return (
    <section id="debug-tools" className={`relative dark:bg-slate-900 ${className}`}>
      <div className="max-w-4xl mx-auto px-4 sm:px-6 md:px-8">
        <div className={'flex flex-col isolate'}>
          <Image src={WormholeImg}
            alt="Orbital wormhole logo"
            className={'absolute blur-3xl opacity-25 right-0 -z-10'} />
          <Caption className="text-citrus text-center">Lineage & Monitoring</Caption>
          <BigText className={'text-center text-white'}>
            <Widont>See exactly what's happening.</Widont>
          </BigText>
          <Paragraph>
            Detailed call traces, request monitoring and lineage gives you end-to-end visibility of query execution, so
            you can see
            exactly what is happening under the hood.
          </Paragraph>
          <LearnMoreLink href="https://docs.vyne.co/querying-with-vyne/data-lineage/" className='mt-8 mb-8 w-full' />
          <div className="mt-10 w-full">
            <Tabs
              tabs={tabs}
              selected={tab}
              onChange={(tab) => setTab(tab)}
              className="text-citrus"
              iconClassName="text-citrus"
            />
          </div>
          <div className='max-w-3xl mx-auto'>
            <AnimatePresence initial={false} exitBeforeEnter>
              <motion.div
                key={tab}
                className=''
                initial={{ opacity: 0 }}
                animate={{ opacity: 1 }}
                exit={{ opacity: 0 }}
              >
                {tab === 'Cell lineage' && <CellLineage />}
                {tab === 'Query lineage' && <QueryLineage />}
                {tab === 'Traffic' && <Traffic />}
              </motion.div>
            </AnimatePresence>
          </div>
        </div>
      </div>
    </section>
  )
}

export default DebugTools
