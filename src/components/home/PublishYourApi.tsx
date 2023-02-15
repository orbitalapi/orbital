import { BigText, Caption, LearnMoreButton, Paragraph, Widont } from '@/components/home/common';
import { Snippet } from '@/components/Steps';
import * as React from 'react';
import { useState } from 'react';
import { Tabs } from '@/components/Tabs';
import { Button } from '@/components/Button';
import { CodeSnippetMap } from '@/components/Guides/CodeSnippet';
//@ts-ignore
import {ReactComponent as ArrowCitrus} from '@/img/arrow-citrus.svg';
import clsx from 'clsx';

const TaxiLogo = ({className = ''}) => (
  <svg width="80px" height="80px" version="1.1" viewBox="0 0 100 100" xmlns="http://www.w3.org/2000/svg" className={className}>
    <path
      d="m22.773 66.793c-2.2852 0-2.0391-0.003907-2.125 0-7.1719 0.03125-10.801 0.050781-13.766-3.8047-1.6094-2.0898-2.0586-4.3711-1.8086-6.9727 0.22656-2.3633 1.0312-4.9102 2.0391-7.8398l5.9219-17.211c1.0078-2.9258 2.1914-4.7383 3.7461-5.875 1.6172-1.1797 3.4414-1.5195 5.7305-1.5195h54.398c2.2891 0 4.1172 0.34375 5.7305 1.5195 1.5547 1.1328 2.7422 2.9492 3.7461 5.875l5.9219 17.211c1.0078 2.9297 1.8086 5.4727 2.0391 7.8398 0.25391 2.6055-0.19922 4.8828-1.8086 6.9727-2.9648 3.8555-6.5977 3.8398-13.766 3.8047h-2.125-53.879zm55.707-21.387h-8.2305v8.2344h-8.2461v-8.2461h-8.2422v8.2422h-8.2461v-8.2344h-8.207v8.2344h-8.2461v-8.2422h-8.2422v-8.2461h8.2461v8.2422h8.2148v-8.2344h8.2461v8.2344h8.2305v-8.2422h8.2461v8.2461h8.2305v-8.2344h8.2461zm-57.848 17.754c1.4336-0.007812 1.3438-0.015625 2.1367-0.015625h53.879c0.79297 0 0.70312 0.007813 2.1367 0.015625 5.9727 0.027344 9 0.042969 10.871-2.3945 0.96875-1.2578 1.2305-2.707 1.0664-4.4102-0.1875-1.9414-0.92969-4.2773-1.8672-6.9961l-5.9219-17.211c-0.75391-2.1836-1.5195-3.4531-2.4375-4.1211-0.85938-0.625-2.0312-0.80859-3.5938-0.80859h-54.398c-1.5625 0-2.7344 0.18359-3.5938 0.80859-0.91797 0.67188-1.6875 1.9375-2.4375 4.1211l-5.9219 17.211c-0.9375 2.7227-1.6797 5.0586-1.8672 6.9961-0.16406 1.7031 0.10156 3.1523 1.0664 4.4102 1.875 2.4375 4.8984 2.4219 10.871 2.3945z"
      fill="currentColor" />
  </svg>);


export const publishYourApiCodeSnippets: CodeSnippetMap = {
  'taxi-simple': {
    name: 'src/types.taxi',
    lang: 'taxi',
    code: `   type PersonName inherits String
>  type CustomerId inherits String
   type Another inherits Date`,
  },
  'open-api-example': {
    name: 'customer-api.oas.yaml',
    lang: 'yaml',
    code: `   # An extract of an OpenAPI spec:
   components:
     schemas:
       Customer:
         properties:
           id:
             type: string
>             # Embed semantic type metadata directly in OpenAPI
>             x-taxi-type:
>                name: CustomerId
             `
  },
  'protobuf-example': {
    name: 'customer-api.proto',
    lang: 'protobuf',
    code: `   import "org/taxilang/dataType.proto";

   message Customer {
      optional string customer_name = 1 [(taxi.dataType)="CustomerName"];
>     optional int32 customer_id = 2 [(taxi.dataType)="CustomerId"];
   }
    `
  },
  'database-example': {
    name: `database.taxi`,
    lang: 'taxi',
    code: `database CustomerDatabase {
   table customers : Customer
}

model Customer {
   id : CustomerId
   name : CustomerName
   ...
}
`
  }
}


export default function PublishYourApi({ highlightedSnippets }) {


  let languageTabs = {
    'Open API': (selected) => (
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
    'Protobuf': (selected) => (
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
    'Database': (selected) => (
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
  }
  const tabCodeSnippets = {
    'Open API': {
      code: publishYourApiCodeSnippets['open-api-example'], highlightedCode: highlightedSnippets['open-api-example']
    },
    'Protobuf': {
      code: publishYourApiCodeSnippets['protobuf-example'], highlightedCode: highlightedSnippets['protobuf-example']
    },
    'Database': {
      code: publishYourApiCodeSnippets['database-example'], highlightedCode: highlightedSnippets['database-example']
    }
  }

  const [tab, setTab] = useState('Open API')

  return (
    <section id="publish-your-api" className="bg-midnight-blue">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 md:px-8 py-24">
        <Caption className="text-citrus text-center">Discover, and be discovered</Caption>
        <BigText className="text-center font-brand">
          <Widont>Powered by your APIs</Widont>
        </BigText>
        <Paragraph>
          Orbital uses your existing API specs and Database schemas - enriched with Taxi metadata to describe links.
        </Paragraph>
        <Paragraph>
          Orbital turns this into rich API and data catalog, letting you explore all your data, and how it connects
        </Paragraph>

        <div className="pt-10 grid grid-cols-3 sm:grid-cols-1 gap-4">
          <div className="flex flex-col items-center ">
            <Paragraph className="pb-8">Create semantic data types with <a
              className="text-citrus font-semibold dark:text-citrus"
              href={'https://taxilang.org'}>Taxi</a>...</Paragraph>
            <div className={'flex flex-col items-center text-citrus'}>
              <TaxiLogo className="text-citrus" />
              <span className="text-sm font-semibold">Taxi</span>
            </div>
          </div>


          <div className="col-span-2 flex flex-col items-center">
            <Paragraph className="pb-8">...and embed them in your existing API specs</Paragraph>
            <Tabs
              tabs={languageTabs}
              selected={tab}
              onChange={(tab) => setTab(tab)}
              className="text-citrus"
              iconClassName="text-citrus"
            />
          </div>


        </div>
        <div className="pt-8 grid grid-cols-3 sm:grid-cols-1 gap-4">
          <div className="flex flex-col items-center">
            <div className={'w-full'}>
              <Snippet highlightedCode={highlightedSnippets['taxi-simple']} code={publishYourApiCodeSnippets['taxi-simple']}/>
            </div>

            <button
              type="button"
              className="mt-4 inline-flex items-center px-3.5 py-2 border border-transparent text-sm leading-4 font-medium rounded-full shadow-sm text-midnight-blue bg-citrus hover:bg-citrus-300 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-citrus-700"
            >
              Learn more about Taxi<span className="hidden lg:inline">, our metadata language</span>

              <svg
                className={clsx(
                  'overflow-visible ml-5'
                )}
                width="3"
                height="6"
                viewBox="0 0 3 6"
                fill="none"
                stroke="currentColor"
                strokeWidth="2"
                strokeLinecap="round"
                strokeLinejoin="round"
              >
                <path d={'M0 0L3 3L0 6'}/>
        </svg>
            </button>
          </div>
          <div className="col-span-2">
            <Snippet highlightedCode={tabCodeSnippets[tab].highlightedCode} code={tabCodeSnippets[tab].code}/>
          </div>


        </div>

      </div>
    </section>
  )
}

