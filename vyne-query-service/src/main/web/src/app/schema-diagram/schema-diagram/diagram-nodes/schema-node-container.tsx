import styled from 'styled-components';
import { colors } from '../tailwind.colors';

const tableInnerBorderColor = colors.slate['300']
const tableBorderColor = colors.lime['500']
export const SchemaNodeContainer = styled.div`
  table {
    font-size: 0.8rem;
    border-collapse: separate;
    border-radius: 4px;
    border: 1px solid ${tableBorderColor};


    td {
      padding: 0 0.5rem;
      line-height: 1.4;
    }

    .node-type {
      font-size: 0.6rem;
      text-transform: uppercase;
      color: ${colors.gray['500']};
      font-weight: 400;

      th {
        padding-top: 0.5rem;
      }
    ;
    }

    .member-name {
      th {
        padding-bottom: 0.3rem;
        border-bottom: 1px solid ${tableInnerBorderColor};
      }
    }

    tbody {
      tr:first-of-type {
        td {
          padding-top: 0.3rem;
        }
      }

      tr:last-of-type {
        td {
          padding-bottom: 0.3rem;
        }
      }
    }
  }
`
