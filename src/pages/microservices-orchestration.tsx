import Head from 'next/head';
import {Footer} from '@/components/home/Footer';
import WatchADemoButton from '../components/DemosModal';
import Wormhole from '@/components/Wormhole';
import {ReactComponent as DataPatternLight} from '@/img/data-pattern.svg';
import OrbitalLogo from '@/img/wormhole-aqua-transparent.png';
import BookADemoButton from "@/components/BookADemoButton";
import {LinkButton} from "@/components/LinkButton";
import {ReactComponent as GithubIcon} from '@/img/icons/github-mark-white.svg'
import {CodeSnippetMap} from "@/components/Guides/CodeSnippet";

import {useState} from "react";
import {Snippet} from "@/components/Steps";
import {ArrowsRightLeftIcon, BoltIcon, CpuChipIcon, MinusSmallIcon, PlusSmallIcon} from "@heroicons/react/24/outline";
import Link from "next/link";
import OpenApiIcon from "@/img/open-api-icon";
import AwsServiceIcon from "@/img/aws-service-icon";
import ProtobufIcon from "@/img/protobuf-icon";
import KafkaIcon from "@/img/kafka-icon";
import {Disclosure} from "@headlessui/react";


const generalOrbitalFeatures = [
  {
    name: 'Instant edge caching',
    description: 'Enable caching at the Edge, to instantly increase performance, drop latency, and reduce the load on your services.',
    icon: BoltIcon,
  },
  {
    name: 'Full observability',
    description: 'See full traces of which systems were called as part of your query and what data was provided.',
    icon: ArrowsRightLeftIcon,
  },
  {
    name: 'Serverless runtime',
    description: 'Scale without limits, and keep your resource costs low, with full isolation for each query.',
    icon: CpuChipIcon,
  },
]


const faqs = [
  {
    question: 'How does this compare to GraphQL?',
    answer:
      `Orbital gives you many of the benefits of GraphQL (API federation, custom response schemas), without having to move your tech stack over to GraphQl - instead working with your existing tech stack(s).`
  },
  {
    question: 'Where do I write my resolvers?',
    answer: "Resolvers create friction to change, as they need to be updated whenever systems change. So, we got rid of 'em. Instead, Orbital uses Taxi Metadata embedded directly in your specs, which evolve with your systems.  This is rich enough for Orbital to work out (and optimize) which systems to call, and when."
  },
  {
    question: 'Does this mean all my systems have to have the same ID schemes and request/response models?',
    answer:
      'Nope. Taxi is designed to encourage teams to evolve independently, without sharing common models.  Instead, semantic scalars are used to compose models together automatically.',
  },
  {
    question: "Can I self host Orbital?",
    answer: "Yep, Orbital currently runs on-prem, in your private cloud.  A cloud version is coming soon."

  },
  {
    question: "What do you mean by 'Adapts Automatically'?",
    answer: "Orbital generates integration on-the-fly, so as your APIs change, integration automatically adapts.  We also have CI/CD tooling that can detect changes which Orbital can't recover from, and warn you ahead of deployment."
  },
  {
    question: 'How do my teams publish their API specs to Orbital?',
    answer: "There are a few options, so teams and mix and match the workflow that suits them. Services can publish auto-generated schemas to Orbital, Orbital can monitor an OpenAPI spec endpoint, or watch a Git repository"
  },
  {
    question: 'How many FAQs are appropriate?',
    answer: "This feels (at least) one too many."
  },
]


export const microservicesCodeSnippets: CodeSnippetMap = {
  'taxi-simple': {
    name: 'types.taxi',
    lang: 'taxi',
    code: `// Define semantic types for the attributes
// that are shared between systems.
type MovieId inherits Int
type MovieTitle inherits String
type ReviewScore inherits Int
type AwardTitle inherits String
`
  },
  'taxi-query': {
    name: 'query.taxi',
    lang: 'taxi',
    code: `find { Movies(ReleaseYear > 2018)[] }
as {
   // Consumers define the schema they want.
   // Orbital works out where to fetch data from,
   // and builds integration on demand.
   title : MovieTitle // .. read from a db
   review : ReviewScore // .. call a REST API to find this
   awards : AwardTitle[] // ... and a gRPC service to find this.
}
        `
  },
  'open-api-example': {
    name: 'customer-api.oas.yaml',
    lang: 'yaml',
    code: `   # An extract of an OpenAPI spec:
   components:
     schemas:
       Reviews:
         properties:
           id:
             type: string
>             # Embed semantic type metadata directly in OpenAPI
>             x-taxi-type:
>                name: MovieId
             `
  },
  'protobuf-example': {
    name: 'reviews-api.proto',
    lang: 'protobuf',
    code: `   import "org/taxilang/dataType.proto";

   // rpc service that returns the awards movies have won
   service AwardsService {
      rpc GetMovieAwards(MovieAwardRequest) returns (MovieAwardResponse)
   }
   message MovieAwardRequest {
>      int32 movie_id = 1 [(taxi.dataType)="MovieId"];
   }
   message MovieAwardResponse {
       int32 movie_id = 1 [(taxi.dataType)="MovieId"];
>      repeated awards = 2 [(taxi.dataType)="AwardTitle[]"];
   }
    `
  },
  'database-example': {
    name: `database.taxi`,
    lang: 'taxi',
    code: `database FilmsDatabase {
   table films : Film[]
}

model Film {
   id : MovieId
   name : MovieTitle
   ...
}
`
  }
};

const supportedTechStacks = [
  {
    'label': 'OpenAPI', icon: () => {
      return <OpenApiIcon width={48} height={48}/>
    }
  },
  {
    'label': 'Protobuf', icon: () => {
      return <ProtobufIcon width={48} height={48}/>
    }
  },
  {'label': 'gRPC', icon: null},
  {'label': 'SOAP', icon: null},
  {'label': 'Postgres', icon: null},
  {
    'label': null, icon: () => {
      return (<AwsServiceIcon serviceName={'DynamoDb'}/>)
    }
  },
  {
    'label': null, icon: () => {
      return (<AwsServiceIcon serviceName={'Lambda'}/>)
    }
  },
  {
    'label': null, icon: () => {
      return (<AwsServiceIcon serviceName={'S3'}/>)
    }
  },
  {
    'label': null, icon: () => {
      return (<AwsServiceIcon serviceName={'SQS'}/>)
    }
  },
  {
    'label': 'Kafka', icon: () => {
      return <KafkaIcon width={48} height={48}/>
    }
  },
  {'label': 'MSSQL', icon: null},
]

interface FaqProps {
  faqs: { question: string, answer: string }[];
}

const FAQs = ({faqs}: FaqProps) => {
  return (
    <div className="w-full divide-y divide-white/10">
      <dl className="mt-10 space-y-6 divide-y divide-white/10">
        {faqs.map((faq) => (
          <Disclosure as="div" key={faq.question} className="pt-6">
            {({open}) => (
              <>
                <dt>
                  <Disclosure.Button className="flex w-full items-start justify-between text-left text-white">
                    <span className="text-base font-semibold leading-7">{faq.question}</span>
                    <span className="ml-6 flex h-7 items-center">
                          {open ? (
                            <MinusSmallIcon className="h-6 w-6" aria-hidden="true"/>
                          ) : (
                            <PlusSmallIcon className="h-6 w-6" aria-hidden="true"/>
                          )}
                        </span>
                  </Disclosure.Button>
                </dt>
                <Disclosure.Panel as="dd" className="mt-2 pr-12">
                  <p className="text-base leading-7 text-gray-300">{faq.answer}</p>
                </Disclosure.Panel>
              </>
            )}
          </Disclosure>
        ))}
      </dl>
    </div>
  )
}

export default function MicroservicesOrchestration({highlightedSnippets}) {

  const codeSnippetButtons = [
    {label: 'Definitions (Taxi)', snippet: 'taxi-simple'},
    {label: 'OpenAPI', snippet: 'open-api-example'},
    {label: 'Protobuf (gRPC)', snippet: 'protobuf-example'},
    {label: 'Database', snippet: 'database-example'},
  ]

  const [activeCodeSnippet, setActiveCodeSnippet] = useState('taxi-simple')

  return (
    <>
      <Head>
        <meta
          key='twitter:title'
          name='twitter:title'
          content='Orbital - Automatically adapt to breaking API changes'
        />
        <meta
          key='og:title'
          property='og:title'
          content='Orbital - Automatically adapt to breaking API changes'
        />
        <title>Orbital - Automated integration for microservices.</title>
      </Head>

      {/* template - header */}
      <div className="isolate">
        <main>
          <div className="relative px-6 lg:px-8 overflow-hidden">
            <div className="mx-auto max-w-4xl py-32 sm:py-48 lg:py-40">
              <div className="text-center">
                <h1
                  className="text-5xl lg:text-7xl tracking-wide font-bold text-gray-900 dark:text-white leading-tight">
                  More building, less plumbing.
                </h1>
                <p className="mt-16 text-xl lg:text-3xl leading-relaxed text-slate-100">
                  Orbital handles the stitching between your microservices
                </p>
                <div className="mt-20 flex items-center justify-center gap-x-6">
                  <LinkButton styles={'border-citrus'}
                              icon={<GithubIcon className={'w-6 h-auto'}/>}
                              label='Try this template'
                              link='https://github.com'></LinkButton>
                  <WatchADemoButton defaultVideo="breakingChanges"/>
                  <BookADemoButton/>
                </div>


              </div>

              <Wormhole className="opacity-15 -z-20"/>

              <DataPatternLight
                className='sm:hidden md:hidden lg:inline left-1/2 absolute lg:top-[500px] lg:w-[1000px] fill-black dark:fill-sky-100 pointer-events-none overflow-hidden'></DataPatternLight>
            </div>
          </div>
        </main>
      </div>


      <div className="bg-gray-900 py-24 sm:py-32">
        <div className="mx-auto max-w-7xl px-6 lg:px-8">
          <div className="mx-auto lg:text-center">
            <h3 className="mt-2 text-4xl lg:text-5xl font-bold tracking-tight text-white">
              Build features, not glue-code.
            </h3>

            <div
              className='grid w-full grid-cols-1 lg:grid-cols-2 grid-flow-col text-slate-200 text-lg text-left gap-8 mt-8'>
              <section>
                <h3 className='mt-4'>
                  <div className='font-semibold text-teal-300'>The problem:</div>
                  <div className='font-semibold'>Microservices require lots of API plumbing</div>
                </h3>
                <p className='mt-2 text-base'>
                  Microservices are great, but they bring new complexity stitching together multiple
                  APIs - finding the right APIs, and countless generated clients. As APIs change,
                  integrations break.
                </p>
              </section>
              <section>
                <h3 className='mt-4'>
                  <div className='font-semibold text-teal-300'>The Solution:</div>
                  <div className='font-semibold'>Automated, self-adapting integration</div>
                </h3>
                <p className='mt-2 text-base'>
                  Your teams are already writing API specs (OpenAPI, Protobuf, Avro, etc). Orbital
                  uses these API specs, along with <a href='https://taxilang.org'>semantic
                  metadata</a> to build integration on-the-fly.
                </p>
                <p className='mt-2 text-base'><span
                  className='font-bold'>As things change, Orbital automatically adapts</span>, so you can focus on
                  features, rather than repairing broken integration.
                </p>
              </section>
            </div>

          </div>
          <div className='mt-36'>
            <h3 className='text-5xl font-extralight text-white lg:text-center'>Write your API. We'll handle the
              integration</h3>
            <div className='mt-8 mb-20 text-lg text-slate-300 lg:text-center max-w-4xl mx-auto'>
              <p>Orbital eliminates integration code, rather than shifting it to another tool. </p>
              <p>There's no resolvers to maintain, API clients to generate, or field mappings YAML files. </p>
              <p>Drive everything from your API specs, and deploy from Git</p>
            </div>

            <div
              className='mt-6 grid w-full grid-cols-1 lg:grid-cols-2 text-slate-200 text-lg text-left gap-8'>

              <section>
                <p className='mb-8'>Define a set of terms, and embed them in your API specs</p>

              </section>
              <section>
                <p>Use those same terms to query for data. </p>
                <p>Orbital handles connecting to the right systems</p>
              </section>
              <section>
                <div className='mb-6'>
                  {codeSnippetButtons.map(btn => {
                    return (<button onClick={() => setActiveCodeSnippet(btn.snippet)}
                                    type="button"
                                    className="mr-4 rounded-full border-indigo-400 hover:border-indigo-500 border-2 px-3.5 py-2 text-sm font-semibold text-slate-200 shadow-sm hover:bg-indigo-300/10"
                    >
                      {btn.label}
                    </button>)
                  })}
                </div>
                <Snippet highlightedCode={highlightedSnippets[activeCodeSnippet]}
                         code={microservicesCodeSnippets[activeCodeSnippet]}/>
              </section>
              <section>
                <Snippet highlightedCode={highlightedSnippets['taxi-query']}
                         code={microservicesCodeSnippets['taxi-query']}/>
              </section>
              {/*<section>*/}
              {/*    <p>As teams update their API specs, the integration automatically adapts, leaving*/}
              {/*        clients unaffected</p>*/}
              {/*</section>*/}
            </div>
          </div>
        </div>
      </div>

      {/*<DynamicHowOrbitalWorks className="hidden lg:block"/>*/}

      <div className="py-24 sm:py-32">
        <img src={OrbitalLogo.src} className={'-z-10 absolute blur-3xl opacity-25 pointer-events-none'}/>
        <div className="mx-auto max-w-7xl px-6 lg:px-8">
          <div className="mx-auto lg:text-center">
            <h3 className="mt-2 text-4xl lg:text-5xl font-bold tracking-tight text-white">
              API composition, for every tech stack.
            </h3>
          </div>
          <div className='mt-8 mb-20 text-lg text-slate-300 lg:text-center max-w-4xl mx-auto'>
            <p>Stitch your databases, message queues, RESTful APIs and gRPC services together, without requiring a
              GraphQL adaptor.</p>
            <p className='mt-4'>Powered by <Link href='https://taxilang.org'>Taxi</Link> - our open source metadata
              language - you can seamlessly link between multiple data sources, without needing a specialist adaptor</p>
          </div>

          <div className='mt-8 grid grid-cols-4 gap-6'>
            {supportedTechStacks.map(tech => {
              const labelClassName = (tech.icon === null) ? 'text-3xl font-light' : 'text-base'
              return (<div
                className='flex flex-col items-center text-slate-100  rounded-2xl justify-center bg-gradient-to-b p-px from-sky-600 to-sky-800'>
                <div
                  className={'bg-gray-900/90 w-full h-full rounded-2xl flex flex-col items-center text-slate-100 py-4 justify-center'}>
                  {tech.icon !== null ? (<div className={'mb-2'}>
                    <tech.icon/>
                  </div>) : <></>}
                  {tech.label !== null ? <div className={labelClassName}>{tech.label}</div> : <></>}
                </div>
              </div>);
            })}
          </div>
        </div>

      </div>

      <div className="py-24 sm:py-32 bg-gray-900">
        <div className="mx-auto max-w-7xl px-6 lg:px-8">
          <div className="mx-auto lg:text-center">
            <h3 className="mt-2 text-4xl lg:text-5xl font-bold tracking-tight text-white">
              Batteries included.
            </h3>
            <div className='mt-8 mb-20 text-xl text-slate-300 lg:text-center max-w-4xl mx-auto'>
              <p>The tools you want from your middleware, redesigned to delight engineers and fit snugly in their
                existing tooling and workflows.</p>
            </div>
          </div>

          <div className='mt-16 grid grid-cols-3 gap-6'>
            {generalOrbitalFeatures.map(feature => {
              return (<div className='rounded-2xl p-px bg-gradient-to-b from-slate-700 via-slate-800 to-darker'>

                <div className='bg-slate-900 rounded-2xl p-6 w-full h-full'>
                  <feature.icon className="h-6 mb-6 text-citrus"
                                aria-hidden="true"/>
                  <h4 className='text-white font-semibold text-xl'>{feature.name}</h4>
                  <p className='mt-4'>{feature.description}</p>
                </div>
              </div>)
            })}
          </div>
        </div>
      </div>


      <div className="">
        <div className="mx-auto max-w-7xl px-6 py-24 sm:pt-32 lg:py-40 lg:px-8">
          <div className="lg:grid lg:grid-cols-12 lg:gap-8">
            <div className="lg:col-span-5">
              <h2 className="text-4xl font-bold leading-10 tracking-tight text-citrus">Frequently
                asked
                questions</h2>
              <p className="mt-4 text-xl leading-7 text-gray-400">
                Got another gnarly question? We'd love to hear it. Come and chat on {' '}
                <a href="https://join.slack.com/t/orbitalapi/shared_invite/zt-697laanr-DHGXXak5slqsY9DqwrkzHg"
                   className="font-semibold text-white hover:text-citrus">
                  Slack.
                </a>
              </p>
            </div>
            <div className={'lg:col-span-7'}>
              <FAQs faqs={faqs}/>
            </div>
          </div>
        </div>
      </div>

      <Footer/>

    </>

  );
}


export function getStaticProps() {
  let {highlightCodeSnippets} = require('@/components/Guides/Snippets');

  return {
    props: {
      highlightedSnippets: highlightCodeSnippets(microservicesCodeSnippets),
    }
  };
}

