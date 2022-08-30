import * as React from 'react';
import { Node, Position } from 'react-flow-renderer';
import { Operation, QueryOperation, Service } from '../../../services/schema';
import { SchemaNodeContainer } from './schema-node-container';
import { MemberWithLinks, ServiceLinks } from '../schema-chart-builder';
import { LinkHandle } from './link-handle';

interface OperationsListProps {
  operations: (Operation | QueryOperation)[]
}


function ApiNode(node: Node<MemberWithLinks>) {
  const service = node.data.member.member as Service;

  function OperationList(props: OperationsListProps) {
    return (<>
      <tr className={'small-heading'}>
        <td colSpan={3}>Operations</td>
      </tr>
      {props.operations.map(operation => {
        const serviceLinks = node.data.links as ServiceLinks;
        const operationLinks = serviceLinks.operationLinks[operation.name];
        return <React.Fragment key={operation.qualifiedName.parameterizedName}>
          <tr>
            <td colSpan={3} className="operation-name">
              {operation.qualifiedName.shortDisplayName}
            </td>
          </tr>
          <tr>
            <td className={'small-heading'}>Inputs</td>
            <td></td>
            <td className={'small-heading'}>Output</td>
          </tr>
          <tr className={'operation-params'}>
            <td className={'parameter-list handle-container'}>
              <div className="parameter-list">
                {operation.parameters.map(param => {
                  return param.typeName.shortDisplayName
                }).join(', ')
                }
                <LinkHandle node={node} links={operationLinks?.inputs} handleType={'target'}></LinkHandle>
              </div>
            </td>
            <td>→</td>
            <td>
              <div className={'handle-container'}>
                {operation.returnTypeName.shortDisplayName}
                <LinkHandle node={node} links={operationLinks?.outputs} handleType={'source'}></LinkHandle>
              </div>
            </td>
          </tr>
        </React.Fragment>
      })}
    </>)
  }

  function VersionTags() {
    if (!service.version) {
      return <></>
    } else {
      return <tr>
        <th colSpan={2}>
          <div className={'version-tags'}>
            {service.version.map(version => {
              return <div id={'version.version'} className={'version-tag'}>{version.version}</div>
            })}
          </div>
        </th>
      </tr>
    }
  }


  return (
    <SchemaNodeContainer>
      <div className={'node-icon-outer-container'}>
        <div className={'node-icon-container'}>
          <img src={'assets/img/chart-icons/api-icon.svg'}></img>
        </div>
      </div>


      <table>
        <thead>
        <tr className={'small-heading'}>
          <th colSpan={3}>API</th>
        </tr>
        <tr className={'member-name'}>
          <th colSpan={3}>{node.data.member.name.shortDisplayName}</th>
        </tr>
        <VersionTags></VersionTags>

        </thead>
        <tbody>
        {service.operations.length > 0 && <OperationList operations={service.operations}></OperationList>}
        {service.queryOperations.length > 0 && <OperationList operations={service.queryOperations}></OperationList>}
        </tbody>
      </table>
    </SchemaNodeContainer>
  )
}

export default ApiNode


function singleItemOrNull<T>(array: T[], errorMessage: string): T | null {
  if (array.length > 1) {
    throw new Error(errorMessage);
  }
  if (array.length === 0) {
    return null
  }
  return array[0];
}
