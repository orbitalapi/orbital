import Head from 'next/head';
import Image from 'next/image';
import {Footer} from '@/components/home/Footer';
import GetStartedButton from '@/components/GetStartedButton';
import WatchADemoButton from '../components/DemosModal';
import Wormhole from '@/components/Wormhole';
import {ReactComponent as DataPatternLight} from '@/img/data-pattern.svg';
import {ReactComponent as ArrowCitrus} from '@/img/arrow-citrus.svg';
import {
    CodeBracketIcon,
    ArrowPathIcon,
    EyeIcon,
    CpuChipIcon,
    WrenchIcon,
    GlobeAsiaAustraliaIcon
} from '@heroicons/react/20/solid'
import OrbitalLogo from '@/img/wormhole-aqua-transparent.png';
import OrbitalQuery from '@/components/home/img/orbital-query.png';
import dynamic from 'next/dynamic';
import BookADemoButton from "@/components/BookADemoButton";
import {LinkButton} from "@/components/LinkButton";
import {ReactComponent as GithubIcon} from '@/img/icons/github-mark-white.svg'
import {SnippetGroup} from "@/components/SnippetGroup";
import {CodeSnippetMap} from "@/components/Guides/CodeSnippet";
import {Tabs} from "@/components/Tabs";
import * as React from "react";
import {Snippet} from "@/components/Steps";
import {publishYourApiCodeSnippets} from "@/components/home/PublishYourApi";
import {highlightCodeSnippets} from "@/components/Guides/Snippets";
import {queryExampleCodeSnippets} from "@/components/home/QueryExamples";

const DynamicHowOrbitalWorks = dynamic(() => import('@/components/home/HowOrbitalWorks'), {
    loading: () => <p>Loading...</p>
})

const breakingChangesFeatures = [
    {
        name: 'Consumer-defined queries',
        description:
            'Define your integrations based on the shape you want to receive it, instead of being at the mercy of how it\'s published.',
        href: '#',
        icon: CodeBracketIcon,
    },
    {
        name: 'CI / CD tooling',
        description:
            'Integrate rich build time checks to ensure your queries are still compatible with your data producer\'s schemas.',
        href: '#',
        icon: ArrowPathIcon,
    },
    {
        name: 'Zero maintenance',
        description:
            'When schemas are updated, your app isn\'t affected. Orbital updates the mappings on-the-fly to fulfill the queries you originally designed.',
        href: '#',
        icon: WrenchIcon,
    },
]

const generalOrbitalFeatures = [
    {
        name: 'Universal microservices API',
        description: 'Let Orbital do the heavy lifting of stitching together data from your microservices ecosystem.',
        icon: GlobeAsiaAustraliaIcon,
    },
    {
        name: 'Full observability',
        description: 'See full traces of which systems were called as part of your query and what data was provided.',
        icon: EyeIcon,
    },
    {
        name: 'Inbuilt API and data catalogs',
        description: 'Explore the systems and data available to you with Orbital\'s auto-generated catalogs.',
        icon: CpuChipIcon,
    },
]


const faqs = [
    {
        question: 'What\'s wrong with standard API versioning?',
        answer:
            'Change is inevitable, and API versioning has worked great for helping to manage different rates of change between producers and consumers of data. Producers can release new versions of their API when they need to and consumers have the flexibility to update when it\'s the right time for them. \
            <br /><br />\
            The downside for both parties is that maintenance burden of keeping up with these changes. Producers need to maintain multiple version of their API in the codebase, including the mappings to their back end systems. Consumers need to schedule busy work that they didn\'t necessarily ask for and doesn\'t help achieve their goals but is required simply to keep their system running.',
    },
    {
        question: 'Do you really get rid of breaking changes?',
        answer:
            'Ok, you got us, we don\'t get rid of ALL breaking changes. We do prevent all types of structural, format, and protocol related changes. For example, if fields are renamed, or moved from a flat to a nested structure, then Orbital handles those without skipping a beat. \
            <br /><br />\
            We don\'t store any data ourselves (and that\'d introduce a whole other set of problems), so if data is completely removed so it\'s no longer available anywhere then we can\'t help.',
    },
    {
        question: 'What types of API\'s and data sources do you support?',
        answer:
            'We currently support multiple HTTP (REST) based schemas, including OpenAPI and older versions of Swagger, various databases and streaming sources such as Kafka. \
            <br /><br />\
            Our architecture is designed to be easily extensible for creating connections to different data sources so if we don\'t have one you need, we can built it in quickly.',
    },
    {
        question: 'How can there be no maintenance?',
        answer:
            'Orbital lets your ask for data in the specific shape, or data model, that you want. We then handle the transformation between the way the data is provided by upstream systems and the shape you\'ve defined. \
            <br /><br />\
            That means when upstream systems change the way they\'re providing data, Orbital simply adjust the transformations it runs to fulfill your query.',
    },
]


export const microservicesCodeSnippets: CodeSnippetMap = {
    'taxi-simple': {
        name: 'types.taxi',
        lang: 'taxi',
        code: `   type PersonName inherits String
>  type CustomerId inherits String
   type Another inherits Date`
    },
    'taxi-query' : {
        name: 'query.taxi',
        lang: 'taxi',
        code: `find { Movies(ReleaseYear > 2018)[] }
as {
   // Orbital works out where to fetch data from...
   title : MovieTitle // .. read from a db
   review : ReviewScore // .. call a REST API to find this
   awards : MovieAwards[] // ... and a gRPC service to find this.
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

export default function MicroservicesOrchestration({ highlightedSnippets }) {

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
                                <p className="mt-16 text-xl lg:text-2xl leading-relaxed text-slate-400">
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

                            <Wormhole className="opacity-25"/>

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
                            Create integrations automatically...
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
                                    metadata</a> to build and repair integration on-the-fly.
                                </p>
                            </section>
                        </div>

                    </div>
                    <div className='mt-32'>
                        <h3 className='text-4xl text-white lg:text-center'>...using your existing specs</h3>


                        <div
                            className='mt-4 grid w-full grid-cols-1 lg:grid-cols-2 grid-flow-col text-slate-200 text-lg text-left gap-8'>

                            <section>
                                <p className='mb-8'>Define a set of terms, and embed them in your API specs</p>
                                <Snippet highlightedCode={highlightedSnippets['taxi-simple']}
                                         code={microservicesCodeSnippets['taxi-simple']} />

                            </section>
                            <section>
                                <p>Use those same terms to query for data. Orbital handles connecting to the right
                                    systems</p>
                                <Snippet highlightedCode={highlightedSnippets['taxi-query']}
                                         code={microservicesCodeSnippets['taxi-query']} />

                            </section>
                            {/*<section>*/}
                            {/*    <p>As teams update their API specs, the integration automatically adapts, leaving*/}
                            {/*        clients unaffected</p>*/}
                            {/*</section>*/}
                        </div>
                    </div>
                </div>
            </div>

            <DynamicHowOrbitalWorks className="hidden lg:block"/>

            <div className="py-24">
                <img src={OrbitalLogo.src} className={'absolute blur-3xl opacity-25 pointer-events-none'}/>
                <div className="mx-auto max-w-7xl sm:px-6 lg:px-8">
                    <div
                        className="relative isolate overflow-hidden py-20 sm:rounded-3xl sm:py-24 sm:px-10 lg:py-24">
                        <div
                            className="mx-auto grid max-w-2xl grid-cols-1 gap-y-16 gap-x-8 sm:gap-y-20 lg:mx-0 lg:max-w-none lg:grid-cols-2 lg:items-center lg:gap-y-0">
                            <div className="lg:row-start-2 lg:max-w-md">
                                <h2 className="text-3xl font-bold tracking-tight text-white sm:text-4xl">
                                    Enterprise integration
                                    <br/>
                                    built for <span className="text-citrus">developers</span>
                                </h2>
                                <p className="mt-6 text-lg leading-8 text-gray-300">
                                    Powerful enterprise integration features, redesigned to delight engineers and
                                    fit
                                    snugly in their
                                    existing tooling and workflows.
                                    <br/><br/>
                                    As well as protection from breaking changes, Orbital offers a complete set of
                                    features to change your
                                    integration game.
                                </p>
                            </div>
                            <div
                                className="relative -z-20 max-w-xl h-fit rounded-xl shadow-xl ring-1 ring-white/10 lg:row-span-4 lg:w-[64rem] lg:max-w-none"
                            >
                                <Image
                                    src={OrbitalQuery}
                                    alt="Orbital product screenshot"
                                    fill
                                    // width={2432}
                                    // height={1442}
                                    style={{
                                        borderRadius: "0.75rem",
                                    }}
                                />
                            </div>
                            <div
                                className="max-w-xl lg:row-start-3 lg:mt-10 lg:max-w-md lg:border-t lg:border-white/10 lg:pt-10">
                                <dl className="max-w-xl space-y-8 text-base leading-7 text-gray-300 lg:max-w-none">
                                    {generalOrbitalFeatures.map((feature) => (
                                        <div key={feature.name} className="relative">
                                            <dt className="ml-9 inline-block font-semibold text-white">
                                                <feature.icon className="absolute top-1 left-1 h-5 w-5 text-citrus"
                                                              aria-hidden="true"/>
                                                {feature.name}
                                            </dt>
                                            {' '}
                                            <dd className="inline">{feature.description}</dd>
                                        </div>
                                    ))}
                                </dl>
                            </div>
                        </div>

                    </div>
                </div>
            </div>

            <div className="">
                <div className="mx-auto max-w-7xl px-6 py-24 sm:pt-32 lg:py-40 lg:px-8">
                    <div className="lg:grid lg:grid-cols-12 lg:gap-8">
                        <div className="lg:col-span-5">
                            <h2 className="text-2xl font-bold leading-10 tracking-tight text-citrus">Frequently
                                asked
                                questions</h2>
                            <p className="mt-4 text-base leading-7 text-gray-400">
                                Got another gnarly question? We'd love to hear it. Come and chat on {' '}
                                <a href="https://join.slack.com/t/orbitalapi/shared_invite/zt-697laanr-DHGXXak5slqsY9DqwrkzHg"
                                   className="font-semibold text-white hover:text-citrus">
                                    Slack.
                                </a>
                            </p>
                        </div>
                        <div className="mt-10 lg:col-span-7 lg:mt-0">
                            <dl className="space-y-10">
                                {faqs.map((faq) => (
                                    <div key={faq.question}>
                                        <dt className="text-base font-semibold leading-7 text-slate-200">{faq.question}</dt>
                                        <dd className="mt-2 text-base leading-7 text-slate-400"
                                            dangerouslySetInnerHTML={{__html: faq.answer}}></dd>
                                    </div>
                                ))}
                            </dl>
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
