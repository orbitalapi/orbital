import * as React from 'react';
import { Handle, Node, Position } from 'react-flow-renderer';
import { Operation, QualifiedName, SchemaMember, Service } from '../../../services/schema';
import { SchemaNodeContainer } from './schema-node-container';
import { MemberWithLinks } from '../schema-chart-builder';

function ApiNode(node: Node<MemberWithLinks>) {
  const service = node.data.member.member as Service;

  function returnTypeConnectionClicked(type: QualifiedName) {
    node.data.chartController.ensureMemberPresentByName(type, {
      node: node,
      direction: 'right'
    });
  }

  function OperationList() {
    return (<>
      <tr className={'small-heading'}>
        <td colSpan={2}>Operations</td>
      </tr>
      {service.operations.map(operation => {
        return <tr key={operation.qualifiedName.parameterizedName}>
          <td>{operation.qualifiedName.shortDisplayName}</td>
          <td>
            <div className={'handle-container'}>
              {operation.returnTypeName.shortDisplayName}
              <Handle type="target" position={Position.Right}
                      onMouseUp={() => returnTypeConnectionClicked(operation.returnTypeName)}
                      id={'output-' + operation.name + '-' + operation.returnTypeName.parameterizedName}></Handle>
            </div>
          </td>
        </tr>
      })}
    </>)
  }

  function QueryOperationList() {
    return (<>
      <tr className={'small-heading'}>
        <td colSpan={2}>Query Operations</td>
      </tr>
      {service.queryOperations.map(operation => {
        return <tr key={operation.qualifiedName.parameterizedName}>
          <td><span className={'tag'}>{operation.grammar}</span>{operation.qualifiedName.shortDisplayName}</td>
          <td>
            <div className={'handle-container'}>
              {operation.returnTypeName.shortDisplayName}
              <Handle type="target" position={Position.Right}
                      id={'output-' + operation.returnTypeName.parameterizedName}/>
            </div>
          </td>
        </tr>
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
      <table>
        <thead>
        <tr className={'small-heading'}>
          <th colSpan={2}>API</th>
        </tr>
        <tr className={'member-name'}>
          <th colSpan={2}>{node.data.member.name.shortDisplayName}</th>
        </tr>
        <VersionTags></VersionTags>

        </thead>
        <tbody>
        {service.operations.length > 0 && <OperationList></OperationList>}
        {service.queryOperations.length > 0 && <QueryOperationList></QueryOperationList>}
        </tbody>
      </table>
    </SchemaNodeContainer>
  )
}

export default ApiNode
