import * as React from 'react';
import styled from 'styled-components';
import VyneLogo from '../../images/vyne_logo_only_white_background.png';

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
    background-color: #BAE6FD;
    border-radius: 4px;
    padding: 4px 0.5rem;
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

  .logo {
    width: 22px;
    height: 20px;
    margin-right: 1rem;
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
        <table className="drop-shadow-md border-collapse rounded-md bg-white">
          <thead>
          <tr>
            <td colSpan={3} className="border-b border-gray-200">
              <div className="flex items-center">
                <img src={VyneLogo} className="logo"/>
                <div>
                  <h4 className="text-slate-600">INTEGRATION PLAN</h4>
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
