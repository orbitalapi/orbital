import { BigText, Caption, LearnMoreLink, Paragraph, Widont } from '@/components/home/common';
import LearnMoreButton from '@/components/home/LearnMoreButton';
import { Snippet } from '@/components/Steps';
import * as React from 'react';
import { useState } from 'react';
import { Tabs } from '@/components/Tabs';
import { CodeSnippetMap } from '@/components/Guides/CodeSnippet';
//@ts-ignore
import { ReactComponent as ArrowCitrus } from '@/img/arrow-citrus.svg';
import clsx from 'clsx';

const TaxiLogo = ({ className = '' }) => (
  <svg width='80px' height='80px' version='1.1' viewBox='0 0 100 100' xmlns='http://www.w3.org/2000/svg'
       className={className}>
    <path
      d='m22.773 66.793c-2.2852 0-2.0391-0.003907-2.125 0-7.1719 0.03125-10.801 0.050781-13.766-3.8047-1.6094-2.0898-2.0586-4.3711-1.8086-6.9727 0.22656-2.3633 1.0312-4.9102 2.0391-7.8398l5.9219-17.211c1.0078-2.9258 2.1914-4.7383 3.7461-5.875 1.6172-1.1797 3.4414-1.5195 5.7305-1.5195h54.398c2.2891 0 4.1172 0.34375 5.7305 1.5195 1.5547 1.1328 2.7422 2.9492 3.7461 5.875l5.9219 17.211c1.0078 2.9297 1.8086 5.4727 2.0391 7.8398 0.25391 2.6055-0.19922 4.8828-1.8086 6.9727-2.9648 3.8555-6.5977 3.8398-13.766 3.8047h-2.125-53.879zm55.707-21.387h-8.2305v8.2344h-8.2461v-8.2461h-8.2422v8.2422h-8.2461v-8.2344h-8.207v8.2344h-8.2461v-8.2422h-8.2422v-8.2461h8.2461v8.2422h8.2148v-8.2344h8.2461v8.2344h8.2305v-8.2422h8.2461v8.2461h8.2305v-8.2344h8.2461zm-57.848 17.754c1.4336-0.007812 1.3438-0.015625 2.1367-0.015625h53.879c0.79297 0 0.70312 0.007813 2.1367 0.015625 5.9727 0.027344 9 0.042969 10.871-2.3945 0.96875-1.2578 1.2305-2.707 1.0664-4.4102-0.1875-1.9414-0.92969-4.2773-1.8672-6.9961l-5.9219-17.211c-0.75391-2.1836-1.5195-3.4531-2.4375-4.1211-0.85938-0.625-2.0312-0.80859-3.5938-0.80859h-54.398c-1.5625 0-2.7344 0.18359-3.5938 0.80859-0.91797 0.67188-1.6875 1.9375-2.4375 4.1211l-5.9219 17.211c-0.9375 2.7227-1.6797 5.0586-1.8672 6.9961-0.16406 1.7031 0.10156 3.1523 1.0664 4.4102 1.875 2.4375 4.8984 2.4219 10.871 2.3945z'
      fill='currentColor' />
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


export default function PublishYourApi({ highlightedSnippets }) {


  let languageTabs = {
    'Open API': (selected) => (
      <>
        <svg version='1.1' xmlns='http://www.w3.org/2000/svg' viewBox='0 0 512 512'>
          <path fill='currentColor'
                d='M0.2041784,294.3757629l135.0729218-0.0019836c1.0510101,10.3673706,3.8841248,20.5690308,8.4522095,30.170166l-115.790062,69.7536621C12.7490072,368.1030579,3.1661,334.7828064,0.2041784,294.3757629z M132.006485,491.1787415l51.4067841-124.8407288c-5.2182617-2.7826233-10.2264862-6.0957642-14.9382172-9.9553528l-95.4706802,95.4720459C92.494194,469.4117737,112.5672379,482.5641174,132.006485,491.1787415z M66.4017639,445.5316162l95.2843628-95.2860413c-4.9330902-4.9628906-9.4824219-10.8501282-13.6674042-17.6151123L32.5120583,402.2131653C43.0031548,419.2817993,54.6602745,434.0601501,66.4017639,445.5316162z M380.3990784,451.8591919l-95.5295105-95.5253601c-1.0485229,0.8474121-2.1081848,1.671936-3.184845,2.4666138l69.7098999,115.7191772C361.1253052,468.4653931,370.7845154,460.6970215,380.3990784,451.8591919z M343.6587219,479.390625l-69.5648804-115.4782104c-25.9638519,14.4993591-53.3182526,17.5528564-82.4063263,6.3365173l-51.3009338,124.5834045C210.5871124,523.298645,283.3482666,515.1446533,343.6587219,479.390625z M171.712616,211.6706696L102.0046768,95.9503479c-10.1640396,6.5237503-19.8186493,14.1064529-29.0061646,22.6601715l95.5275803,95.5278625C169.5743561,213.3020477,170.6356812,212.4786224,171.712616,211.6706696z M0,285.2357178l134.7621918-0.0019531c0.291153-23.8374939,8.7095032-45.6818237,26.9275513-65.005722l-95.2863083-95.2865982C22.4163055,171.5531464,0.5115053,225.0562439,0,285.2357178z M226.6937714,193.2982483l0.0058594-134.7585754c-44.7272644,0.2738724-84.0045013,12.32621-116.959053,32.5391159l69.5626297,115.4798889C192.3796692,198.881958,207.9673767,193.5783997,226.6937714,193.2982483z M413.098053,156.1804504l-99.4490967,99.4510498c2.2626953,6.6661987,3.7337646,13.5315552,4.4528503,20.4605103h135.0907898C452.3086853,235.1112671,438.7878723,195.1533508,413.098053,156.1804504z M453.3783569,285.2320862h-134.747406c-0.4680481,25.240448-9.8990479,48.2441101-26.923645,65.0134888l95.2901917,95.2860413C430.8166809,402.4093628,452.1193237,348.5843811,453.3783569,285.2320862z M235.8396912,58.7399521l-0.0058594,135.0865784c7.082489,0.8026276,13.8835602,2.3424988,20.4658203,4.4556732l99.4153442-99.4150772C320.5830383,74.2917404,279.1752625,60.5052757,235.8396912,58.7399521z M430.9497681,2.5972993c-39.1646423,11.456749-55.5329285,55.1829491-38.7815857,88.6712189L254.8082123,228.6303864c-32.4068756-16.1573944-74.9024811-1.5699463-87.7949829,36.0340729c-15.7162628,45.8401489,24.2427673,91.8019104,71.7535858,82.5325317c42.0110779-8.1963196,62.3093567-54.1882019,44.4657593-90.0109253l137.4894714-137.4859314c34.6590576,17.2256775,79.5329285-1.0651627,89.3959961-41.653656C521.5482178,31.0087166,477.4600525-11.0083342,430.9497681,2.5972993z' />
        </svg>
      </>
    ),
    'Protobuf': (selected) => (
      <>
        <svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 32 32'>
          <polygon points='15.996 3.22 26.966 9.7 26.954 22.3 16 28.78 5.038 22.408 5.034 9.628 15.996 3.22'
                   opacity='0.1' fill='currentColor' />
          <polygon points='25.569 13.654 19.946 16.964 19.943 24.89 25.59 21.565 25.569 13.654'
                   opacity='0.3' fill='currentColor' />
          <polygon
            points='23.282 12.303 25.569 13.654 19.946 16.964 19.943 24.89 17.327 23.37 17.348 15.875 23.282 12.303'
            opacity='0.8' fill='currentColor' />
          <polygon
            points='22.512 10.35 22.514 11.816 16.411 15.498 16.418 23.597 14.998 24.431 14.994 14.856 22.512 10.35'
            opacity='0.3' fill='currentColor' />
          <polygon
            points='20.008 8.871 22.512 10.35 14.994 14.856 14.998 24.431 12.194 22.801 12.189 13.413 20.008 8.871'
            opacity='0.8' fill='currentColor' />
          <polygon points='19.226 6.606 19.226 8.374 11.21 13.074 11.21 23.172 9.808 23.988 9.835 12.277 19.226 6.606'
                   opacity='0.3' fill='currentColor' />
          <polygon points='16.16 4.784 6.53 10.394 6.529 22.071 9.827 23.988 9.835 12.277 19.235 6.606 16.16 4.784'
                   opacity='0.8' fill='currentColor' />
        </svg>
      </>
    ),
    'Database': (selected) => (
      <>
        <svg xmlns='http://www.w3.org/2000/svg' fill='none' viewBox='0 0 24 24' strokeWidth={1.5} stroke='currentColor'
             className='w-6 h-6'>
          <path strokeLinecap='round' strokeLinejoin='round'
                d='M20.25 6.375c0 2.278-3.694 4.125-8.25 4.125S3.75 8.653 3.75 6.375m16.5 0c0-2.278-3.694-4.125-8.25-4.125S3.75 4.097 3.75 6.375m16.5 0v11.25c0 2.278-3.694 4.125-8.25 4.125s-8.25-1.847-8.25-4.125V6.375m16.5 0v3.75m-16.5-3.75v3.75m16.5 0v3.75C20.25 16.153 16.556 18 12 18s-8.25-1.847-8.25-4.125v-3.75m16.5 0c0 2.278-3.694 4.125-8.25 4.125s-8.25-1.847-8.25-4.125' />
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
          Orbital uses by your existing API specs (with a little extra metadata), to create integration on demand.  As things change, <span className='text-citrus font-bold'>Orbital automatically adapts</span>.
        </Paragraph>
        </div>

        <div className='pt-10 grid grid-cols-3 sm:grid-cols-1 gap-4'>
          <div className='flex flex-col items-center '>
            <Paragraph className='pb-8'>Create semantic data types with <a
              className='text-citrus font-semibold dark:text-citrus'
              href={'https://taxilang.org'}>Taxi</a>...</Paragraph>
            <div className={'flex flex-col items-center text-citrus'}>
              <TaxiLogo className='text-citrus' />
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
                       code={publishYourApiCodeSnippets['taxi-simple']} />
            </div>

            <LearnMoreButton href="https://taxilang.org" text={() => <span>Learn more about Taxi<span className='hidden xl:inline'>, our metadata language</span></span>} />
          </div>
          <div className='col-span-2'>
            <Snippet highlightedCode={tabCodeSnippets[tab].highlightedCode} code={tabCodeSnippets[tab].code} />
          </div>


        </div>

      </div>
    </section>
  );
}

