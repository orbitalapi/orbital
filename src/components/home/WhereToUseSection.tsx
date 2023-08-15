import * as React from 'react';

import dynamic from 'next/dynamic';
import {Widont} from '../Widont';
import {BigText, Caption, LearnMoreLink, Paragraph} from './common';
import Image from 'next/future/image';

import MicroservicesDiagram from './img/microservices.png';
import EventBasedArchitecturesDiagram from './img/event-based-architectures.png';
import Link from "next/link";
import OrbitalLogo from "@/img/wormhole-aqua-transparent.png";
import {useState} from "react";
import {ReactComponent as ArrowCitrus} from '@/img/arrow.svg';

const DynamicWhereToUseOrbital = dynamic(() => import('@/components/home/WhereToUseDiagram'), {
    loading: () => <p>Loading...</p>
})


function WhereToUseSection(props) {


    const bodyCss = 'text-md text-slate-200 text-center pb-4'


    const useCases = [
        {
            header: 'Microservices orchestration',
            img: MicroservicesDiagram,
            subtitle: 'Orchestrate services on demand',
            text: 'Query for the data your app needs, and Orbital orchestrates your APIs, Databases and Serverless Functions on demand',
            link: '/docs'
        },
        {
            header: 'Event-based architectures',
            img: EventBasedArchitecturesDiagram,
            subtitle: 'Bespoke events for consumers',
            text: 'Decouple producers and consumers, providing custom events for each consumer with exactly the data they need',
            link: '/docs'
        },
        {
            header: 'Rapidly onboard external data feeds',
            img: EventBasedArchitecturesDiagram,
            subtitle: 'Declarative data pipelines',
            text: 'Ingest, transform and enrich data declaratively, without building & maintaining complex pipelines',
            link: '/docs'
        },
        // {
        //     header: 'Backends for Frontends',
        //     img: EventBasedArchitecturesDiagram,
        //     subtitle: 'Tailored APIs, without middleware tax',
        //     text: 'Tailored APIs for every UI, combining APIs, database calls and lambdas, without building and maintaining middleware',
        //     link: '/docs'
        // },
    ];
    const [activeItemIdx, setActiveItemIdx] = useState(0);

    return (
        <section id="where-to-use" className={`relative bg-slate-900 pt-20 ${props.className}`}>
            <Image src={OrbitalLogo} className={'absolute blur-3xl opacity-25 pointer-events-none'}
                   alt="Orbital aqua wormhole background image"/>
            <BigText className='text-center font-brand pb-4'>

                <span className='text-citrus'>Connect</span><Widont> without the glue</Widont>
            </BigText>

            <h2 className="mt-2 mb-10 text-2xl text-white text-center">
                <Widont>Orbital builds integration for you, so you can focus on features</Widont>
            </h2>

            <div className='max-w-7xl mx-auto px-4 sm:px-8 md:px-8 flex'>
                <div className='flex-none w-2/5'>
                    {useCases.map((useCase, index) => {
                        const isActive = activeItemIdx === index;
                        const boxCss = 'mb-6 bg-gradient-to-b ease-in duration-150 rounded-lg from-slate-900/75 to-slate-900/25 drop-shadow-lg backdrop-blur-xl ring-1 ring-inset ring-white/20 px-6 py-4 ' +
                            (isActive ? 'ring-violet-500' : 'ring-white/20');
                        const headerCss = 'text-xl font-bold text-white pb-4'
                        const imgCss = 'grayscale opacity-60 ease-in duration-150 pb-8';


                        return (<div className={boxCss} onMouseEnter={e => setActiveItemIdx(index)}>
                            <h4 className={headerCss}>{useCase.header}</h4>
                            {/*<p className={'text-sm text-slate-100 pb-1'}><span*/}
                            {/*    className='font-extrabold'>{useCase.subtitle}</span></p>*/}
                            <p><span className='text-slate-300'>{useCase.text}</span></p>


                            <a href={useCase.link}
                               className={'block mt-4 text-md font-bold ' + ((isActive) ? 'text-citrus' : 'text-slate-300')}>
                                <span className="flex align-center">Learn more
                                   <ArrowCitrus className={"-rotate-90 stroke-[6px] ml-4 " + ((isActive) ? 'stroke-citrus' : 'stroke-slate-300')} width='12'></ArrowCitrus>
                                </span>
                            </a>
                        </div>)
                    })}
                </div>
                <div className='grow w-full h-full my-auto'>
                    <Image
                        className='w-[80%] h-[80%] bg-slate-900/75 z-10 backdrop-blur-md shadow-2xl rounded-md ml-8 p-12 ring-white/10 ring-1 ring-inset'
                        alt='Microservices orchestration diagram' src={useCases[activeItemIdx].img}/>
                </div>
            </div>


        </section>
    )
}

export default WhereToUseSection
