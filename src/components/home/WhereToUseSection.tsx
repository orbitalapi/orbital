import * as React from 'react';

import dynamic from 'next/dynamic';
import {Widont} from '../Widont';
import {BigText, Paragraph} from './common';
import Image from 'next/future/image';

import MicroservicesDiagram from './img/microservices.png';
import EventBasedArchitecturesDiagram from './img/event-based-architectures.png';
import Link from "next/link";
import OrbitalLogo from "@/img/wormhole-aqua-transparent.png";

const DynamicWhereToUseOrbital = dynamic(() => import('@/components/home/WhereToUseDiagram'), {
    loading: () => <p>Loading...</p>
})


function WhereToUseSection(props) {

    const boxCss = 'bg-gradient-to-b ease-in duration-150 rounded-2xl from-slate-900/75 to-slate-900/25 drop-shadow-lg backdrop-blur-xl  ring-1 ring-inset ring-white/20 p-6 hover:ring-violet-500 [&>img]:hover:grayscale-0 [&>img]:hover:opacity-100'
    const headerCss = 'text-xl font-bold text-white pb-4 text-center'
    const imgCss = 'grayscale opacity-60 ease-in duration-150 pb-8';
    const bodyCss = 'text-md text-slate-200 text-center pb-4'
    return (
        <section id="where-to-use" className={`relative bg-slate-900  ${props.className}`}>
            <Image src={OrbitalLogo} className={'absolute blur-3xl opacity-25 pointer-events-none'} alt="Orbital aqua wormhole background image" />
            <BigText className='text-center font-brand pb-4'>
                <span className='text-citrus'>Connect</span><Widont> without the glue</Widont>
            </BigText>

            <h2 className="mt-2 mb-10 text-2xl font-bold  text-white  text-center">
                <Widont>Orbital eliminates integration across modern tech stacks</Widont>
            </h2>
            {/*<DynamicWhereToUseOrbital className="inline-block w-full" />*/}
            <div className='grid grid-cols-4 gap-4 max-w-5xl mx-auto'>
                <div className={`${boxCss} col-span-2`}>
                    <h3 className={headerCss}>Microservices orchestration</h3>
                    <Image className={imgCss} alt='Microservices orchestration diagram' src={MicroservicesDiagram}/>

                    <p className={'text-md text-slate-100 text-center pb-2'}><span className='font-extrabold'>Orchestrate services on demand</span> </p>
                    <p><span className='text-slate-300'>Query for the
                        data your app needs, and Orbital orchestrates your APIs, Databases and Serverless Functions on demand</span> </p>
                </div>
                <div className={`${boxCss} col-span-2`}>
                    <h4 className={headerCss}>Event-based architectures</h4>
                    <Image className={imgCss} alt='Microservices orchestration diagram' src={EventBasedArchitecturesDiagram}/>

                    <p className={'text-md text-slate-100 text-center pb-2'}><span className='font-extrabold'>Bespoke events for consumers</span> </p>
                    <p><span className='text-slate-300'>Decouple producers and consumers, providing custom events for each consumer with exactly the data they need</span> </p>
                </div>

                <div className={`${boxCss} col-span-2`}>
                    <h4 className={headerCss}>Rapidly onboard external data feeds</h4>
                    <Image className={imgCss} alt='Microservices orchestration diagram' src={MicroservicesDiagram}/>

                    <p className={'text-md text-slate-100 text-center pb-2'}><span className='font-extrabold'>Declarative data pipelines</span> </p>
                    <p><span className='text-slate-300'>Ingest, transform and enrich data declaratively, without building & maintaining complex pipelines</span> </p>
                </div>

                <div className={`${boxCss} col-span-2`}>
                    <h4 className={headerCss}>Backends for Frontends</h4>
                    <Image className={imgCss} alt='Microservices orchestration diagram' src={MicroservicesDiagram}/>

                    <p className={'text-md text-slate-100 text-center pb-2'}><span className='font-extrabold'>Tailored APIs, without middleware tax</span> </p>
                    <p><span className='text-slate-300'>Tailored APIs for every UI, combining APIs, database calls and lambdas, without building and maintaining middleware</span> </p>


                </div>
            </div>
        </section>
    )
}

export default WhereToUseSection
