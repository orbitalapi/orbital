import * as React from 'react';
import { useEffect, useState } from 'react';
import { BigText, Caption, Paragraph } from '@/components/home/common';
import { Tabs } from '../Tabs';
import { GridLockup } from '../GridLockup';
import { AnimatePresence } from 'framer-motion';
import { Widont } from '../Widont';
import ExplainPlan, { ExplainPlanProps } from './ExplainPlan';
import Prism from 'prismjs';
import { Button } from '@/components/Button';
import { Snippet } from '@/components/Steps';
import { CodeSnippet, CodeSnippetMap, HighlightedCodeSnippetMap } from '@/components/Guides/CodeSnippet';
import IntegrationDiagram, { IntegrationDiagramProps } from '@/components/home/IntegrationDiagram';

const paragraphClass = 'text-lg font-semibold text-sky-400 mb-8'

export type Tabs = 'Fetch' | 'Combine' | 'Stream' | 'Expressions';

const tabData = {
  'Fetch': {
    query: `find { Customer[] }
as {
 id: CustomerId
 firstName: CustomerFirstName
 lastName: CustomerLastName
 name: concat(CustomerFirstName, ' ', CustomerLastName)
}
      `,
    paragraph: () => (
      <Paragraph className={paragraphClass}>Orbital automatically generates integration code for calling your APIs
      </Paragraph>
    ),
    integrationDiagrams: [{
      title: ['Customer', 'CardNumber'],
      nodes: [
        { title: 'Orbital', type: 'orbital', linkText: 'Fetch' },
        { title: 'Customer\nAPI', type: 'api' },
      ]
    }] as IntegrationDiagramProps[],
    lines: [
      {
        from: () => Array.from(document.getElementsByClassName('token keyword')).filter((t: HTMLElement) => t.innerText === 'find')[0],
        to: () => document.getElementById('Fetch-Plan-0').querySelector('img'),
        offset: 'find { Customer[] }'.length
      }
    ],
  },
  'Combine': {
    query: `find { Customer[] }
as {
   id: CustomerId
   firstName: CustomerFirstName
   lastName: CustomerLastName
   cardNumber: CardNumber
   balance: AccountBalance
}
      `,
    paragraph: () => (
      <Paragraph className={paragraphClass}>Query for data from multiple sources, Orbital automatically links data
        sources, as required
      </Paragraph>
    ),
    integrationDiagrams: [
      {
        title: ['Customer', 'CardNumber'],
        nodes: [
          { title: 'Customer\nAPI', type: 'api', linkText: 'CustomerId' },
          { title: 'Cards\nDatabase', type: 'database' },
        ]
      },
      {
        title: ['Customer', 'AccountBalance'],
        nodes: [
          { title: 'Customer\nAPI', type: 'api', linkText: 'CustomerId' },
          { title: 'Cards\nDatabase', type: 'database', linkText: 'CardNbr' },
          { title: 'Balances\nAPI', type: 'api' }
        ]
      }
    ] as IntegrationDiagramProps[],
    lines: [
      {
        from: () => Array.from(document.getElementsByClassName('token class-name')).filter((t: HTMLElement) => t.innerText === ': CardNumber')[0],
        to: () => document.getElementById('Combine-Plan-0').querySelector('img'),
        offset: ': CardNumber'.length
      },
      {
        from: () => Array.from(document.getElementsByClassName('token class-name')).filter((t: HTMLElement) => t.innerText === ': AccountBalance')[0],
        to: () => document.getElementById('Combine-Plan-1').querySelector('img'),
        offset: ': AccountBalance'.length
      }
    ],
  },
  Stream: {
    query: `stream { PurchaseEvents }
as {
   id: CustomerId
   name: CustomerName
   storeLocation: StoreLocation
   txnId: TransactionId
   value: TransactionValue
   remainingBalance: AccountBalance
}
      `,
    paragraph: () => (
      <Paragraph className={paragraphClass}>Work with streaming data, joined across databases and APIs
      </Paragraph>
    ),
    integrationDiagrams: [
      {
        title: ['Subscribe for PurchaseEvents'],
        nodes: [
          { title: 'Orbital', type: 'api', linkText: 'Subscribe' },
          { title: 'Purchases', type: 'kafka' },
        ]
      }, {
        title: ['PurchaseEvent', 'AccountBalance'],
        nodes: [
          { title: 'Purchases', type: 'kafka', linkText: 'CustomerId' },
          { title: 'Cards\nDatabase', type: 'database', linkText: 'CardNumber' },
          { title: 'Balances\nAPI', type: 'api' },
        ]
      },
    ] as IntegrationDiagramProps[],
    lines: [
      {
        from: () => Array.from(document.getElementsByClassName('token keyword')).filter((t: HTMLElement) => t.innerText === 'stream')[0],
        to: () => document.getElementById('Stream-Plan-0').querySelector('img'),
        offset: 'stream { PurchaseEvents }'.length
      },
      {
        from: () => Array.from(document.getElementsByClassName('token class-name')).filter((t: HTMLElement) => t.innerText === ': AccountBalance')[0],
        to: () => document.getElementById('Stream-Plan-1').querySelector('img'),
        offset: ': AccountBalance'.length
      }
    ],
  },
  Expressions: {
    query: `// Create simple definitions to share,
// Orbital will find the data for you.
type Profit = PurchasePrice - CostOfSale
type CostOfSale = (PurchasePrice * AgentCommission) + UnitPrice
// Use definitions in your queries
stream { PurchaseEvents }
as {
   id: CustomerId
   name: CustomerName
   profit: Profit
}`,
    paragraph: () => (
      <Paragraph className={paragraphClass}>Define your logic once, let Orbital do the working out.
      </Paragraph>
    ),
    integrationDiagrams: [{
      title: ['Customer', 'CardNumber'],
      nodes: [
        { title: `Customer API`, type: 'api', linkText: 'CustomerId' },
        { title: 'Cards Database', type: 'database', linkText: 'CardNbr' },
        { title: 'Balances API', type: 'kafka' }
      ]
    }] as IntegrationDiagramProps[],
    // lines: [
    //     {
    //         from: () => Array.from(document.getElementsByClassName('token class-name')).filter((t:HTMLElement) => t.innerText === 'Profit')[1],
    //         to: () => document.getElementById('Expressions-Plan-0').querySelector('img'),
    //         offset: 'Profit'.length
    //     }
    // ]
  },
}

export const queryExampleCodeSnippets: CodeSnippetMap = Object.fromEntries(Object.entries(tabData).map(([key, value]) => {
  return [key, {
    // Using graphQL for query snippets until we improve
    // the grammar spec to cover TaxiQL
    // NOte: Changing this also need to update the element selection logic in IntegrationDiagram.
    // Just look for the text span in chrome-dev-tools, and find the css class applied to the span.  Super simple
    lang: 'taxi',
    name: 'query.taxi',
    code: value.query
  } as CodeSnippet]
}))
// Icons taken from:
// https://iconpark.oceanengine.com/official
let tabs = {
  'Fetch': (selected) => (
    <>
      <rect width="48" height="48" fill="white" fillOpacity="0.01"/>
      <path d="M10 8L4 14L10 20" stroke="currentColor" strokeWidth="2" strokeLinecap="round"
            strokeLinejoin="round"/>
      <path d="M38 28L44 34L38 40" stroke="currentColor" strokeWidth="2" strokeLinecap="round"
            strokeLinejoin="round"/>
      <path d="M4 14H44" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"/>
      <path d="M4 34H44" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"/>
    </>
  ),
  Combine: (selected) => (
    <>
      <svg width="38" height="38" viewBox="0 0 48 48" fill="none" xmlns="http://www.w3.org/2000/svg">
        <rect x="16" y="16" width="27" height="27" rx="2" stroke="currentColor" strokeWidth="2"
              strokeLinecap="round" strokeLinejoin="round"/>
        <rect x="5" y="5" width="27" height="27" rx="2" stroke="currentColor" strokeWidth="2"
              strokeLinecap="round" strokeLinejoin="round"/>
        <path d="M27 16L16 27" stroke="currentColor" strokeWidth="2" strokeLinecap="round"
              strokeLinejoin="round"/>
        <path d="M32 21L21 32" stroke="currentColor" strokeWidth="2" strokeLinecap="round"
              strokeLinejoin="round"/>
      </svg>
    </>
  ),
  Stream: (selected) => (
    <>
      <path
        d="M38.1421 38.1421C45.9526 30.3316 45.9526 17.6684 38.1421 9.85786C30.3316 2.04738 17.6684 2.04738 9.85786 9.85786C2.04738 17.6684 2.04738 30.3316 9.85786 38.1421M32.4853 32.4853C37.1716 27.799 37.1716 20.201 32.4853 15.5147C27.799 10.8284 20.201 10.8284 15.5147 15.5147C10.8284 20.201 10.8284 27.799 15.5147 32.4853"
        stroke="currentColor" strokeWidth="3" strokeLinecap="round" strokeLinejoin="round"/>
      <path
        d="M28 24C28 26.2091 26.2091 28 24 28C21.7909 28 20 26.2091 20 24C20 21.7909 21.7909 20 24 20C26.2091 20 28 21.7909 28 24Z"
        fill="none"/>
      <path
        d="M24 28C26.2091 28 28 26.2091 28 24C28 21.7909 26.2091 20 24 20C21.7909 20 20 21.7909 20 24C20 26.2091 21.7909 28 24 28ZM24 28V44M24 44H28M24 44H20"
        stroke="currentColor" strokeWidth="3" strokeLinecap="round" strokeLinejoin="round"/>
    </>
  ),
  Expressions: () => (
    <>
      <path d="M40 9L37 6H8L26 24L8 42H37L40 39" stroke="currentColor" strokeWidth="3" strokeLinecap="round"
            strokeLinejoin="round"/>
    </>
  )


}

const lines = [];

export type QueryExampleProps = {
  highlightedSnippets: HighlightedCodeSnippetMap
}

function QueryExamples(props: QueryExampleProps) {

  const [tab, setTab] = useState('Fetch')

  function drawConnectorLines() {
    // We used to do this:
    // import LeaderLine from 'leader-line';
    // LeaderLine isn't exported as a nice tidy module.
    // And unfortunately, because LeaderLine internally references window,
    // we can't use the webpack approaches documented here ( https://github.com/anseki/leader-line/issues/8)
    // as they fail in the gatsby build time with server-side-rendering.
    // So, we have to use a window global. :(
    //
    // Note: Scoping matters here, as placing this reference outside this function
    // will cause evaluation at build time (when gatsby renders the pages)
    // which will break the build
    const LeaderLine = (window as any).LeaderLine;

    removeLines()
    if (tabData[tab].lines !== undefined) {
      tabData[tab].lines.forEach((line) => {
        const from = line.from();
        const to = line.to();

        const MONO_FONT_SIZE = 16; //px
        if (from && to) {
          console.log('Adding line')
          lines.push(new LeaderLine({
            start: LeaderLine.pointAnchor(from, {
              x: line.offset * (MONO_FONT_SIZE * 0.6), y: 8
            }),
            end: LeaderLine.pointAnchor(to, {
              x: -20, y: 10
            }),
            startPlug: 'disc',
            endPlug: 'disc',
            color: '#059669',
            showEffectName: 'fade'
          }));
        }

      })
    }
  }

  function removeLines() {
    lines.forEach(line => line.remove());
    lines.splice(0, lines.length);
  }

  const doHighlight = () => {
    Prism.highlightAll();
    const mediaQuery = window.matchMedia('(min-width: 570px)');
    if (mediaQuery.matches) {
      drawConnectorLines();
    }

  }

  useEffect(() => {
    doHighlight();

    // unmount function
    return () => {
      removeLines();
    }
  });


  return (
    <section id="query-examples" className="relative">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 md:px-8">
        <div className={'flex flex-col items-center'}>
          <div className="max-w-3xl mx-auto flex flex-col items-center">
            <BigText className="text-center font-brand">
              <Widont>Connect without the glue</Widont>
            </BigText>
            <Paragraph>
              Query for the data you need, and Orbital integrates on-the-fly.
            </Paragraph>
            <Paragraph>
              From simple API calls, to complex multi-hop lookups, Oribtal automatically orchestrates your APIs,
              databases, queues and lambdas.
            </Paragraph>
            <Paragraph>
              No glue code required. As things change, Orbital adapts.
            </Paragraph>
            <Button href="https://docs.vyne.co/querying-with-vyne/writing-queries/" className={'mt-8 mb-8'}>
              Learn more
            </Button>
          </div>
          <div className="mt-10">
            <Tabs
              tabs={tabs}
              selected={tab}
              onChange={(tab) => setTab(tab)}
              className="text-sky-400 "
              iconClassName="text-sky-500 "
            />
          </div>

          {tabData[tab].paragraph()}
        </div>

        <GridLockup
          className="mt-10 xl:mt-2"
          left={
            <Snippet highlightedCode={props.highlightedSnippets[tab]} code={queryExampleCodeSnippets[tab]}/>
          }
          right={
            <AnimatePresence initial={false} exitBeforeEnter>
              <>
                {tabData[tab].integrationDiagrams.map((plan, idx) => {
                  return (<div key={`${tab}-PlanDiv-${idx}`} id={`${tab}-Plan-${idx}`}
                               className=" pt-8 flex flex-col items-center">
                    {/* We use this id for building lines from the query to the plan*/}
                    <IntegrationDiagram  {...plan}/>
                  </div>)
                })
                }

              </>
            </AnimatePresence>
          }

        />
      </div>
    </section>
  )
}

function lineCount(src: string): number {
  return src.split('\n')
    .length;
}

export default QueryExamples
