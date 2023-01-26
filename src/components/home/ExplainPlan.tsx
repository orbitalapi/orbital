import * as React from 'react';
import styled from 'styled-components';

import OribtalLogo from '@/img/wormhole-aqua-transparent.png';

export interface ExplainPlanProps {
  title: string[];
  startRowText: string;
  endRowText: string;
  rows: {
    method: string;
    operationName: string;
    path: string;
  }[]
}

const StyledExplainPlan = styled.div`
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
    font-size: 0.7rem;
    font-weight: 200;
  }
`

const ExplainPlan = (props: ExplainPlanProps) => {
  return (
    <StyledExplainPlan>
      <div>
        <table className="drop-shadow-md border-collapse rounded-md bg-slate-800/75 ring-1 ring-inset ring-white/10">
          <thead>
          <tr>
            <td colSpan={3} className="border-b border-gray-200">
              <div className="flex items-center">
                <img src={OribtalLogo.src} className="mr-4 w-[45px]"/>
                <div className='text-white'>
                  <h4 className="text-sky-300">INTEGRATION PLAN</h4>
                  {(props.title.length > 1) && <><span>
                                {props.title[0]}
                    <span className="mono"> -> </span>
                    {props.title[1]}
                                </span></>
                  }
                  {props.title.length === 1 && <span>{props.title[0]}</span>}
                </div>

              </div>
            </td>
          </tr>
          </thead>
          <tbody>
          {props.startRowText && <tr className={'startRow'}>
            <td className={'method'}>START</td>
            <td colSpan={2} className={'mono'}>{props.startRowText}</td>
          </tr>}
          {props.rows.map((row, idx) => {
            return (
              <tr key={idx}>
                <td className={'method'}>
                  <div>{row.method}</div>
                </td>
                {row.path && (<>
                  <td>{row.operationName}</td>
                  <td className={'path mono'}>{row.path}</td>
                </>)}
                {
                  row.path === undefined && (
                    <td colSpan={2} className={'path mono'}>{row.operationName}</td>
                  )
                }
              </tr>
            )
          })}
          {props.endRowText &&
            <tr className={'finishRow'}>
              <td className={'method'}>END</td>
              <td colSpan={2} className={'path mono'}>{props.endRowText}</td>
            </tr>
          }
          </tbody>
        </table>
      </div>
    </StyledExplainPlan>
  );
}

export default ExplainPlan
