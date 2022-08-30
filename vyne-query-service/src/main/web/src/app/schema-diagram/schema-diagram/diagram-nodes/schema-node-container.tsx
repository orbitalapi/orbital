import styled from 'styled-components';
import { colors } from '../tailwind.colors';

const tableInnerBorderColor = colors.slate['300']
const tableBorderColor = colors.lime['500']
export const SchemaNodeContainer = styled.div`
  box-shadow: rgb(0 0 0 / 10%) 0 2px 5px 0;

  .handle-container {
    position: relative;

    .react-flow__handle-left {
      left: calc(-1rem + 4px); //1rem to offset the padding, 3px is the width of the connector node, 1px border
    }

    .react-flow__handle-right {
      right: calc(-1rem + 4px); //1rem to offset the padding, 3px is the width of the connector node, 1px border
    }
  }

  .node-icon-outer-container {
    position: relative;;
    .node-icon-container {
      position: absolute;
      top: -26px;
      left: -26px;

      border: 2px solid ${tableBorderColor};
      background-color: white;
      border-radius: 50%;
      padding: 6px;
      display: flex;
      align-items: center;
      justify-content: center;

      img {
        width: 36px;
        height: 36px;
      }
    }
  }


  .small-heading {
    font-size: 0.7rem;
    text-transform: uppercase;
    color: ${colors.gray['500']};
    font-weight: 600;
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
    background-color: white;


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

      tr.operation-params {
        .parameter-list.handle-container {
          .react-flow__handle-left {
            left: -4px; // Just from messing about
            top: calc(50% - 3px);
          }
        }

        td {
          border-bottom: 1px solid ${tableInnerBorderColor};
          padding-bottom: 0.25rem;
        }
      }

      td.operation-name {
        font-size: 0.9rem;
        font-weight: 600;
        padding-top: 0.5rem;
      }
    }
  }
`
