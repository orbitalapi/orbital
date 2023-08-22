import {BigText, Caption, LearnMoreLink, Paragraph, Widont} from '@/components/home/common';
import LearnMoreButton from '@/components/home/LearnMoreButton';
import {Snippet} from '@/components/Steps';
import * as React from 'react';
import {useState} from 'react';
import {Tabs} from '@/components/Tabs';
import {CodeSnippetMap} from '@/components/Guides/CodeSnippet';
//@ts-ignore
import {ReactComponent as ArrowCitrus} from '@/img/arrow-citrus.svg';
import clsx from 'clsx';
import OpenApiIcon from "@/img/open-api-icon";
import ProtobufIcon from "@/img/protobuf-icon";

const TaxiLogo = ({className = ''}) => (
  <svg width='80px' height='80px' version='1.1' viewBox='0 0 100 100' xmlns='http://www.w3.org/2000/svg'
       className={className}>
    <path
      d='m22.773 66.793c-2.2852 0-2.0391-0.003907-2.125 0-7.1719 0.03125-10.801 0.050781-13.766-3.8047-1.6094-2.0898-2.0586-4.3711-1.8086-6.9727 0.22656-2.3633 1.0312-4.9102 2.0391-7.8398l5.9219-17.211c1.0078-2.9258 2.1914-4.7383 3.7461-5.875 1.6172-1.1797 3.4414-1.5195 5.7305-1.5195h54.398c2.2891 0 4.1172 0.34375 5.7305 1.5195 1.5547 1.1328 2.7422 2.9492 3.7461 5.875l5.9219 17.211c1.0078 2.9297 1.8086 5.4727 2.0391 7.8398 0.25391 2.6055-0.19922 4.8828-1.8086 6.9727-2.9648 3.8555-6.5977 3.8398-13.766 3.8047h-2.125-53.879zm55.707-21.387h-8.2305v8.2344h-8.2461v-8.2461h-8.2422v8.2422h-8.2461v-8.2344h-8.207v8.2344h-8.2461v-8.2422h-8.2422v-8.2461h8.2461v8.2422h8.2148v-8.2344h8.2461v8.2344h8.2305v-8.2422h8.2461v8.2461h8.2305v-8.2344h8.2461zm-57.848 17.754c1.4336-0.007812 1.3438-0.015625 2.1367-0.015625h53.879c0.79297 0 0.70312 0.007813 2.1367 0.015625 5.9727 0.027344 9 0.042969 10.871-2.3945 0.96875-1.2578 1.2305-2.707 1.0664-4.4102-0.1875-1.9414-0.92969-4.2773-1.8672-6.9961l-5.9219-17.211c-0.75391-2.1836-1.5195-3.4531-2.4375-4.1211-0.85938-0.625-2.0312-0.80859-3.5938-0.80859h-54.398c-1.5625 0-2.7344 0.18359-3.5938 0.80859-0.91797 0.67188-1.6875 1.9375-2.4375 4.1211l-5.9219 17.211c-0.9375 2.7227-1.6797 5.0586-1.8672 6.9961-0.16406 1.7031 0.10156 3.1523 1.0664 4.4102 1.875 2.4375 4.8984 2.4219 10.871 2.3945z'
      fill='currentColor'/>
  </svg>);


export const publishYourApiCodeSnippets: CodeSnippetMap = {
  'taxi-simple': {
    name: 'src/types.taxi',
    lang: 'taxi',
    code: `   type PersonName inherits String
>  type CustomerId inherits String
   type Another inherits Date`
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
};


export default function PublishYourApi({highlightedSnippets}) {


  let languageTabs = {
    'Open API': OpenApiIcon,
    'Protobuf': ProtobufIcon,
    'Database': (selected) => (
      <>
        <svg xmlns='http://www.w3.org/2000/svg' fill='none' viewBox='0 0 24 24' strokeWidth={1.5} stroke='currentColor'
             className='w-6 h-6'>
          <path strokeLinecap='round' strokeLinejoin='round'
                d='M20.25 6.375c0 2.278-3.694 4.125-8.25 4.125S3.75 8.653 3.75 6.375m16.5 0c0-2.278-3.694-4.125-8.25-4.125S3.75 4.097 3.75 6.375m16.5 0v11.25c0 2.278-3.694 4.125-8.25 4.125s-8.25-1.847-8.25-4.125V6.375m16.5 0v3.75m-16.5-3.75v3.75m16.5 0v3.75C20.25 16.153 16.556 18 12 18s-8.25-1.847-8.25-4.125v-3.75m16.5 0c0 2.278-3.694 4.125-8.25 4.125s-8.25-1.847-8.25-4.125'/>
        </svg>

      </>
    )
  };
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
  };

  const [tab, setTab] = useState('Open API');

  return (
    <section id='publish-your-api' className='bg-midnight-blue'>
      <div className='max-w-7xl mx-auto px-4 sm:px-6 md:px-8 py-24'>
        <div className='max-w-3xl mx-auto flex flex-col items-center'>
          <BigText className='text-center font-brand'>
            <Widont>Powered by </Widont><span className='text-citrus'>Your APIs</span>
          </BigText>
          <Paragraph>
            Orbital uses by your existing API specs (with a little extra metadata), to create integration on demand. As
            things change, <span className='text-citrus font-bold'>Orbital automatically adapts</span>.
          </Paragraph>
        </div>

        <div className='pt-10 grid grid-cols-3 sm:grid-cols-1 gap-4'>
          <div className='flex flex-col items-center '>
            <Paragraph className='pb-8'>Create semantic data types with <a
              className='text-citrus font-semibold dark:text-citrus'
              href={'https://taxilang.org'}>Taxi</a>...</Paragraph>
            <div className={'flex flex-col items-center text-citrus'}>
              <TaxiLogo className='text-citrus'/>
              <span className='text-sm font-semibold'>Taxi</span>
            </div>
          </div>


          <div className='col-span-2 flex flex-col items-center'>
            <Paragraph className='pb-8'>...and embed them in your existing API specs</Paragraph>
            <Tabs
              tabs={languageTabs}
              selected={tab}
              onChange={(tab) => setTab(tab)}
              className='text-citrus'
              iconClassName='text-citrus'
            />
          </div>


        </div>
        <div className='pt-8 grid grid-cols-3 sm:grid-cols-1 gap-4'>
          <div className='flex flex-col items-center'>
            <div className={'w-full'}>
              <Snippet highlightedCode={highlightedSnippets['taxi-simple']}
                       code={publishYourApiCodeSnippets['taxi-simple']}/>
            </div>

            <LearnMoreButton href="https://taxilang.org"
                             text={() => <span>Learn more about Taxi<span className='hidden xl:inline'>, our metadata language</span></span>}/>
          </div>
          <div className='col-span-2'>
            <Snippet highlightedCode={tabCodeSnippets[tab].highlightedCode} code={tabCodeSnippets[tab].code}/>
          </div>


        </div>

      </div>
    </section>
  );
}

