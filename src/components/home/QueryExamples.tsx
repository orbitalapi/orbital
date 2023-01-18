import * as React from 'react';
import { useEffect, useState } from 'react';
import { BigText, Caption, Link, Paragraph } from '../common';
import { Tabs } from '../Tabs';
import { GridLockup } from '../GridLockup';
import { AnimatePresence, motion } from 'framer-motion';
import { CodeWindow } from '../CodeWindow';
import { Widont } from '../Widont';
import ExplainPlan, { ExplainPlanProps } from './explain-plan';
import ResultsTable, { ResultsTableProps } from './results-table';
import Prism from 'prismjs';

const paragraphClass = 'text-lg font-semibold text-indigo-600 mb-8'
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
      <Paragraph className={paragraphClass}>Vyne automatically generates integration code for calling your APIs
      </Paragraph>
    ),
    explainPlan: [{
      title: ['fetch Customer'],
      rows: [
        { method: 'GET', operationName: 'Customer API', path: '/customers' },
        { method: 'TRANSFORM', operationName: '{ name, firstName, lastName }' },
        { method: 'EVALUATE', operationName: `concat(CustomerFirstName, ' ', CustomerLastName)` },

      ]
    }] as ExplainPlanProps[],
    lines: [
      {
        from: () => Array.from(document.getElementsByClassName('token keyword')).filter(t => t.innerText === 'find')[0],
        to: () => document.getElementById('Fetch-Plan-0').querySelector('img'),
        offset: 'find { Customer[] }'.length
      }
    ],
    results: {
      headers: ['id', 'firstName', 'lastName', 'name'],
      rows: [
        [1, 'Jimmy', 'Smitts', 'Jimmy Smitts'],
        [2, 'Jane', 'Splatt', 'Jane Splatt'],
      ]
    } as ResultsTableProps
  },
  Join: {
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
      <Paragraph className={paragraphClass}>Ask for data from multiple sources, Vyne automatically links data
        sources, as required
      </Paragraph>
    ),
    explainPlan: [{
      title: ['Customer', 'CardNumber'],
      rows: [
        { method: 'GET', operationName: 'Customer API', path: '/customers' },
        { method: 'SELECT', operationName: 'Cards Database', path: 'select c.cardNumber from...' },
      ]
    }, {
      title: ['Customer', 'AccountBalance'],
      rows: [
        { method: 'GET', operationName: 'Customer API', path: '/customers' },
        { method: 'SELECT', operationName: 'Cards Database', path: 'select c.cardNumber from...' },
        { method: 'GET', operationName: 'Accounts API', path: '/balances/{cardNumber}' },
      ]
    },] as ExplainPlanProps[],
    lines: [
      {
        from: () => Array.from(document.getElementsByClassName('token class-name')).filter(t => t.innerText === 'CardNumber')[0],
        to: () => document.getElementById('Join-Plan-0').querySelector('img'),
        offset: 'CardNumber'.length
      }, {
        from: () => Array.from(document.getElementsByClassName('token class-name')).filter(t => t.innerText === 'AccountBalance')[0],
        to: () => document.getElementById('Join-Plan-1').querySelector('img'),
        offset: 'AccountBalance'.length
      }
    ],
    results: {
      headers: ['id', 'firstName', 'lastName', 'cardNumber', 'balance'],
      rows: [
        [1, 'Jimmy', 'Smitts', '525-600-112-230', '$89.00'],
        [2, 'Jane', 'Splatt', '43-24601-33-23', '$5,230.23'],
      ]
    } as ResultsTableProps
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
    explainPlan: [
      {
        title: ['Stream PurchaseEvents'],
        rows: [
          { method: 'SUBSCRIBE', operationName: 'Kafka', path: 'transactions topic' },
        ]
      },
      // {
      //     title: ['PurchaseEvent', 'StoreLocation'],
      //     rows: [
      //         {method: 'SUBSCRIBE', operationName: 'Kafka', path: 'transactions topic'},
      //         {method: 'SELECT', operationName: 'Stores Database', path: 'SELECT location from stores...'},
      //     ]
      // },
      {
        title: ['PurchaseEvent', 'AccountBalance'],
        rows: [
          { method: 'SUBSCRIBE', operationName: 'Kafka', path: 'transactions topic' },
          { method: 'GET', operationName: 'Customer API', path: '/customers/{customerId}' },
          { method: 'SELECT', operationName: 'Cards Database', path: 'select c.cardNumber from...' },
          { method: 'GET', operationName: 'Accounts API', path: '/balances/{cardNumber}' },
        ]
      }
    ] as ExplainPlanProps[],
    results: {
      headers: ['id', 'name', 'storeLocation', 'txnId', 'value', 'remainingBalance'],
      rows: [
        [1, 'Jimmy Smitts', 'London', 23004, '$45.00', '$2,300.00'],
        [2, 'Jane Splatt', 'New York', 89003, '$11.50', '$5,230.23'],
      ]
    } as ResultsTableProps,
    lines: [
      {
        from: () => Array.from(document.getElementsByClassName('token class-name')).filter(t => t.innerText === 'AccountBalance')[0],
        to: () => document.getElementById('Stream-Plan-1').querySelector('img'),
        offset: 'AccountBalance'.length
      }
    ],
  },
  Expressions: {
    query: `// Create simple definitions to share,
// Vyne will find the data for you.
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
      <Paragraph className={paragraphClass}>Define your logic once, let Vyne do the working out.
      </Paragraph>
    ),
    explainPlan: [
      {
        title: ['PurchaseEvent', 'Profit'],
        rows: [
          { method: 'SUBSCRIBE', operationName: 'Kafka', path: 'transactions topic' },
          { method: 'GET', operationName: 'Stock API', path: '/stock/{productId}/wholesalePrice' },
          { method: 'SELECT', operationName: 'HR Database', path: 'select c.agentCommission from...' },
          { method: 'EVALUATE', operationName: 'CostOfSale = (PurchasePrice * AgentCommission) + UnitPrice' },
          { method: 'EVALUATE', operationName: 'Profit = PurchasePrice - CostOfSale' },
        ]
      }
    ] as ExplainPlanProps[],
    results: {
      headers: ['id', 'name', 'storeLocation', 'txnId', 'value', 'remainingBalance'],
      rows: [
        [1, 'Jimmy Smitts', 'London', 23004, '$45.00', '$2,300.00'],
        [2, 'Jane Splatt', 'New York', 89003, '$11.50', '$5,230.23'],
      ]
    } as ResultsTableProps,
    // lines: [
    //     {
    //         from: () => Array.from(document.getElementsByClassName('token class-name')).filter(t => t.innerText === 'Profit')[1],
    //         to: () => document.getElementById('Expressions-Plan-0').querySelector('img'),
    //         offset: 'Profit'.length
    //     }
    // ]
  },
}

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
  Join: (selected) => (
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

function QueryExamples() {

  const [tab, setTab] = useState('Fetch')
  Prism.languages.taxi = Prism.languages.extend('graphql', {
    'comment': [
      {
        pattern: /(^|[^\\])\/\*[\s\S]*?(?:\*\/|$)/,
        lookbehind: true,
        greedy: true
      },
      {
        pattern: /(^|[^\\:])\/\/.*/,
        lookbehind: true,
        greedy: true
      }
    ],
    'keyword': /\b(?:find|stream|model|type|inherits|as|namespace|import|parameter|by|when|else|closed|with|synonym|of|alias|extension|service|operation|lineage|query|)\b/,
    'scalar': /\b(?:String|Boolean|Int|Decimal|Date|Time|DateTime|INstant|Any|Double|Void)\b/,

  });

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
    const LeaderLine = window.LeaderLine;

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
    <section id="query-examples" className="relative bg-sky-50">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 md:px-8 md:py-32 py-4">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 md:px-8">
          <Caption className="text-indigo-500 ">Fetch anything</Caption>
          <BigText>
            <Widont>All your data, exactly how you need it, one API.</Widont>
          </BigText>
          <Paragraph>
            Ask for the data you need, and Vyne builds integrations between data sources on the fly
            to fetch, combine and transform the data you need.
          </Paragraph>
          <Paragraph>
            Vyne works with databases, streaming data sources, APIs, lambdas, the lot.
          </Paragraph>
          {/*<Paragraph>*/}
          {/*   Utility classes help you work within the constraints of a system instead of littering your*/}
          {/*   stylesheets with arbitrary values. They make it easy to be consistent with color choices,*/}
          {/*   spacing, typography, shadows, and everything else that makes up a well-engineered design*/}
          {/*   system.*/}
          {/*</Paragraph>*/}
          <a href="https://docs.vyne.co/querying-with-vyne/writing-queries/" color="indigo" darkColor="indigo">
            Learn more
          </a>
          <div className="mt-10">
            <Tabs
              tabs={tabs}
              selected={tab}
              onChange={(tab) => setTab(tab)}
              className="text-indigo-600 "
              iconClassName="text-indigo-500 "
            />
          </div>

          {tabData[tab].paragraph()}
        </div>

        <GridLockup
          className="mt-10 xl:mt-2"
          left={
            <CodeWindow>
              <AnimatePresence initial={true} exitBeforeEnter onExitComplete={doHighlight}>
                <motion.div
                  key={tab}
                  className="w-full flex-auto flex min-h-0"
                  initial={{ opacity: 0 }}
                  animate={{ opacity: 1 }}
                  exit={{ opacity: 0 }}
                >
                  <CodeWindow.Code2 lines={lineCount(tabData[tab].query)}
                                    language="taxi"
                  >
                    <code className="language-taxi" style={{ 'whiteSpace': 'pre-wrap' }}>
                      {tabData[tab].query}
                    </code>

                  </CodeWindow.Code2>
                </motion.div>
              </AnimatePresence>
            </CodeWindow>
          }
          right={
            <AnimatePresence initial={false} exitBeforeEnter>
              <>
                {/*Not showing the results table anymore, just show the explain plans*/}
                {/*{tabData[tab].results && (*/}
                {/*    <div className=' flex flex-col items-center'>*/}
                {/*        <ResultsTable headers={tabData[tab].results.headers}*/}
                {/*                      rows={tabData[tab].results.rows}/>*/}
                {/*    </div>)}*/}
                {tabData[tab].explainPlan.map((plan, idx) => {
                  return (<div key={`${tab}-PlanDiv-${idx}`} id={`${tab}-Plan-${idx}`}
                               className=" pt-8 flex flex-col items-center">
                    {/* We use this id for building lines from the query to the plan*/}
                    <ExplainPlan  {...plan}/>
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


function highlightTokens(query: string) {
  const languages = Object.keys(Prism.languages);
  const grammar = Prism.languages['graphql'];
  if (grammar === null || grammar === undefined) {
    return ' Taxi not found - found : ' + languages;
  }
  const tokens = Prism.tokenize(query, grammar)
  return JSON.stringify(tokens);
  // return tokens;
}

function lineCount(src: string): number {
  return src.split('\n')
    .length;
}

export default QueryExamples
