import * as React from 'react';
import styled from 'styled-components';
import clsx from 'clsx';
import Image from 'next/future/image';

import OrbitalLogo from '@/img/wormhole-aqua-transparent.png';
import { ApiIcon } from '@/components/ApiIcon';
import { DatabaseIcon } from '@/components/DatabaseIcon';
import { KafkaIcon } from '@/components/KafkaIcon';

export interface IntegrationDiagramProps {
  title: string[];
  nodes: ServiceNode[];
}

type ServiceType = 'api' | 'database' | 'kafka' | 'orbital';

export interface ServiceNode {
  type: ServiceType
  title: string;
  linkText?: string;
  lastItem?: boolean;
}


const StyledIntegrationDiagram = styled.div`
  .method {
    font-family: 'DM Sans', monospace;
    text-align: center;
    font-size: 0.8rem;
    text-align: center;
  }

  table {
    font-size: 0.8rem;
  }

  tbody > tr:first-child > td {
    padding-top: 0.5rem;
  }

  tr > td:first-child {
    padding-left: 1rem;
  }

  thead > tr > td {
    font-family: 'DM Sans', monospace;
    padding: 1rem;
    font-size: 1rem;
  }

  .method > div {
    background-color: #7dd3fc; // sky-300
    border-radius: 4px;
    padding: 4px 0.5rem;
    color: #1e293b; // slate-800
  }

  td {
    padding-right: 1rem;
    padding-bottom: 0.5rem;
  }

  .mono {
    font-family: 'JetBrains Mono', monospace;
  }

  .path {
    font-size: 0.8rem;
  }

  h4 {
    font-size: 0.8rem;
    font-weight: 500;
  }
`

const ConnectorLine = ({ label, ...props }) => {
  return (
    <div {...props} className={clsx("ml-5", props.className)}>
      {/* Border which becomes the arrow*/}
      <div className={'border-t border-white text-white relative border-r'}>
        <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 20 20" fill="currentColor" className="w-5 h-5 absolute sm:bottom-[-15px] md:top-[-10px] right-[-11px] sm:rotate-90">
          <path fillRule="evenodd" d="M7.21 14.77a.75.75 0 01.02-1.06L11.168 10 7.23 6.29a.75.75 0 111.04-1.08l4.5 4.25a.75.75 0 010 1.08l-4.5 4.25a.75.75 0 01-1.06-.02z" clipRule="evenodd" />
        </svg>
      </div>
      <div className={'font-mono m-[0.5rem] text-xs p-1 border rounded text-white border-white'}>{label}</div>
    </div>
  )
}

const ServiceIcon = ({ serviceType, ...props }) => {
  switch (serviceType as ServiceType) {
    case 'api':
      return (<ApiIcon {...props} />)
    case 'database':
      return (<DatabaseIcon {...props} />)
    case 'kafka':
      return (<KafkaIcon {...props} />)
    case 'orbital' :
      return <Image src={OrbitalLogo.src} alt="Orbital" width={45} height={45} {...props} />
  }
}
const DiagramServiceNode = (props: ServiceNode) => {
  return (
    <div className={clsx("flex items-center sm:flex-col md:justify-between", !props.lastItem && "w-full")}>
      <div className="flex flex-col items-center">
        <ServiceIcon serviceType={props.type} style={{ width: '48px', height: '48px' }}
                     className={clsx('mb-3 text-aqua', props.linkText && 'grow')}/>
        <span className={'text-white text-sm text-center whitespace-pre-wrap'}>{props.title}</span>
      </div>
      {props.linkText && (
        <div className="sm:w-full md:max-w-max">
          <ConnectorLine label={props.linkText} className="flex sm:flex-row-reverse sm:mr-[6.5rem] sm:mt-4 md:flex-col md:mt-[-0.75rem]" />
        </div>
      )}
    </div>
  );
}

const IntegrationDiagram = (props: IntegrationDiagramProps) => {
  // const orientation = props.orientation || 'horizontal';

  return (
    <StyledIntegrationDiagram>
      <div className="drop-shadow-md border-collapse rounded-md bg-slate-800/75 ring-1 ring-inset ring-white/10 p-4 flex flex-col space-y-4">
        <div className={'integration-header border-b border-slate-700 pb-2'}>
          <div className="flex items-center">
            <Image src={OrbitalLogo.src} className="mr-4 w-[45px]" width={45} height={45} alt="Orbital" />
            <div className="text-white">
              <h4 className="text-aqua font-semibold ">INTEGRATION PLAN</h4>
              {(props.title.length > 1) && <><span>
                                {props.title[0]}
                <span className="mono">{' -> '}</span>
                {props.title[1]}
                                </span></>
              }
              {props.title.length === 1 && <span>{props.title[0]}</span>}
            </div>
          </div>
        </div>
        <div>
          {/*  Body*/}
          <div className="flex sm:flex-col sm:items-center sm:space-y-4 md:space-x-4 gap-6">
            {props.nodes.map((node, index) => {
              return (<DiagramServiceNode key={`diagram-service-node-${index}`} {...node}
                                          lastItem={props.nodes.length - 1 === index}></DiagramServiceNode>);
            })}
          </div>
        </div>

      </div>
    </StyledIntegrationDiagram>
  )
}


export default IntegrationDiagram
