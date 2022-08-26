import styled from 'styled-components';
import { colors } from '../tailwind.colors';

const tableInnerBorderColor = colors.slate['300']
const tableBorderColor = colors.lime['500']
export const SchemaNodeContainer = styled.div`
  box-shadow: rgb(0 0 0 / 10%) 0 2px 5px 0;

  .small-heading {
    font-size: 0.6rem;
    text-transform: uppercase;
    color: ${colors.gray['500']};
    font-weight: 400;
  }
  .tag {
    background-color: ${colors.blue['50']};
    padding: 0.1rem 0.25rem;
    border-radius: 5px;
    margin-right: 0.5rem;
  }
  table {
    font-size: 0.8rem;
    border-collapse: separate;
    border-radius: 4px;
    border: 1px solid ${tableBorderColor};


    td, th {
      padding: 0 0.5rem;
      line-height: 1.8;
    }

    thead {
      tr {
        background-color: ${colors.slate['50']};
      }
      tr:first-of-type {
        th {
          padding-top: 0.5rem;
        }
      }

      tr:last-of-type {
        th {
          padding-bottom: 0.3rem;
          border-bottom: 1px solid ${tableInnerBorderColor};
        }
      }

      .version-tags {
        display: flex;

        .version-tag {
          font-size: 0.7rem;
          margin-right: 1rem;
          color: ${colors.gray['600']};
        }
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
