import * as React from "react";
import styled from "styled-components";
import colors from 'tailwindcss/colors';

export interface ResultsTableProps {
    headers: string[];
    rows: any[][];
}


const StyledResultTable = styled.div`
  thead > tr > td {
    padding: 0.5rem 1rem;
    font-weight: 600;
  }

  table {
    font-size: 0.8rem;
  }

  tbody > tr:first-child > td {
    padding-top: 0.5rem;
  }
  tbody > tr > td {
    padding: 0.25rem 1rem 0.25rem 1px;
    text-align: center;
  }
  tbody > tr:nth-child(even) {
    background-color: ${colors.gray['100']};
    
  }
`;

const ResultTable = (props: ResultsTableProps) => {
    return (
        <StyledResultTable>
            <table className='bg-white shadow-md rounded-md border-collapse table-auto'>
                <thead>
                <tr className='rounded-t-md'>
                    {props.headers.map((header,idx) => {
                        return (<td key={idx} className='bg-slate-800 text-slate-50'>{header}</td>)
                    })}
                </tr>
                </thead>
                <tbody>
                {props.rows.map((row,rowIdx) => {
                    return (<tr key={rowIdx} className='even:bg-gray-50'>
                        {row.map((cell,idx) => {
                            return (<td key={idx}>{cell}</td>);
                        })}
                    </tr>)
                })}
                </tbody>
            </table>
        </StyledResultTable>
    )
}

export default ResultTable;
