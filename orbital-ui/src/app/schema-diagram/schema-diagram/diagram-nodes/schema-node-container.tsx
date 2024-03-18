import styled from 'styled-components';
import { colors } from '../tailwind.colors';

const tableInnerBorderColor = colors.slate['300'];
export const modelColor = colors.lime['500'];
export const typeColor = colors.purple['500'];
export const serviceColor = colors.sky['400'];
export const lineageDependencyColor = colors.yellow['500'];

export const SchemaNodeContainer = styled.div`
  box-shadow: rgb(0 0 0 / 10%) 0 2px 5px 0;

  :hover {
    box-shadow: rgb(0 0 0 / 25%) 0 2px 5px 0;
  }

  .handle-container {
    position: relative;

    .react-flow__handle-left {
      left: calc(-1rem + 2px);
    }

    .react-flow__handle-right {
      right: calc(-1rem + 2px);
    }

    .react-flow__handle {
      width: 8px;
      height: 8px;
      outline: 1px solid ${colors.gray['500']};
      background: ${colors.white};
      transition: 150ms ease-in-out background, 150ms ease-in-out outline;

      &:hover {
        outline: 2.5px solid ${colors.sky['600']};
        background: ${colors.sky['200']};
      }
    }
  }

  a {
    color: unset;
    text-decoration: none;
    &:hover {
      text-decoration: underline;
      text-decoration-color: ${colors.slate['600']};
    }
  }

  .node-icon-outer-container {
    position: relative;;
    .node-icon-container {
      position: absolute;
      top: -16px;
      left: -16px;

      border: 2px solid ${serviceColor};
      background-color: white;
      border-radius: 50%;
      padding: 4px;
      display: flex;
      align-items: center;
      justify-content: center;

      img {
        width: 24px;
        height: 24px;
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
    border-spacing: 0;
    font-size: 0.8rem;
    border-collapse: separate;
    border-radius: 4px;
    border: 2px solid ${modelColor};
    background-color: white;

    &.service {
      border-color: ${serviceColor};
    }

    &.type {
      border-color: ${typeColor};
    }

    td, th {
      padding: 0 0.5rem;
      line-height: 1.8;
    }

    thead {
      tr {
        background-color: ${colors.slate['50']};
      }

      tr:first-of-type {
        th:first-child {
          border-top-left-radius: 4px;
        }
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
