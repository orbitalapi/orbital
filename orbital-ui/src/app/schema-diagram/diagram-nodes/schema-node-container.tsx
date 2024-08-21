import styled from 'styled-components';
import { colors } from '../tailwind.colors';

const tableInnerBorderColor = colors.slate['300'];
export const modelColor = colors.lime['500'];
export const typeColor = colors.purple['500'];
export const serviceColor = colors.sky['400'];
export const lineageDependencyColor = colors.yellow['500'];

export const SchemaNodeContainer = styled.div`
  box-shadow: rgb(0 0 0 / 10%) 0 2px 5px 0;
  border-radius: 4px;
  transition: box-shadow 150ms ease-in-out;

  :hover {
    box-shadow: rgb(0 0 0 / 25%) 0 2px 5px 0;

    .delete-btn {
      opacity: 0.7;
    }
  }

  .handle-container {
    position: relative;

    .react-flow__handle-left {
      left: calc(-1rem + 6px);
    }

    .react-flow__handle-right {
      right: calc(-1rem + 6px);
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
    color: ${colors.sky['600']};
    text-decoration: none;

    &:hover, &:focus {
      text-decoration: underline;
    }

    &:visited, &:active {
      color: ${colors.sky['500']};
    }
  }

  // Taken from %type-badge within typography.scss as we can't extend that from within React

  .badge {
    font-family: var(--body-font);
    font-weight: normal;
    padding: 2px 4px;
    font-size: 0.6rem;
    margin-left: 0.75rem;
    align-self: center;
    text-transform: capitalize;
    border-radius: 4px;

    &.model {
      // rgba doesn't work here, have to use actual RGB figures
      background-color: rgba(77, 124, 15, 0.1);
      color: rgba(77, 124, 15, 0.8);
    }

    &.type {
      // rgba doesn't work here, have to use actual RGB figures
      background-color: rgba(168, 85, 247, 0.1);
      color: rgba(168, 85, 247, 0.8);
    }

    &.service {
      // rgba doesn't work here, have to use actual RGB figures
      background-color: rgba(2, 132, 199, 0.1);
      color: rgba(2, 132, 199, 1);
      display: flex;
      align-items: center;
      gap: 0.25rem;
    }
  }

  .delete-btn {
    width: 1rem;
    height: 1rem;
    opacity: 0;
    cursor: pointer;
    transition: opacity 150ms ease-in-out, border 150ms ease-in-out;
    border: 1px solid transparent;
    padding: 3px;
    border-radius: 3px;
    margin-left: 0.25rem;

    &:hover {
      opacity: 1;
      border: 1px solid #CCC;
    }
  }

  .service-icon {
    width: 18px;
    height: 18px;
    // Taken from .filter-schema-service-color
    filter: invert(35%) sepia(63%) saturate(2692%) hue-rotate(178deg) brightness(90%) contrast(98%);
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

    th {
      text-align: left;
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
          padding-top: 0.3rem;
        }
      }

      tr:last-of-type {
        th {
          padding-bottom: 0.3rem;
          border-bottom: 1px dotted ${tableInnerBorderColor};
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

      .header {
        display: flex;
        justify-content: space-between;
        gap: 0.5rem;
        padding-right: 0.15rem;

        .left-content {
          display: flex;
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
            left: -2px; // Just from messing about
            top: calc(50% - 3px);
          }
        }

        &:not(:last-of-type) {
          td {
            border-bottom: 1px dashed ${tableInnerBorderColor};
            padding-bottom: 0.25rem;
          }
        }
      }

      td.operation-name {
        font-size: 0.9rem;
        font-weight: 600;
        padding-top: 0.25rem;
      }
    }
  }
`
