import * as React from 'react';
import { Handle, Node, Position } from 'react-flow-renderer';
import { Operation, SchemaMember, Service } from '../../../services/schema';
import { SchemaNodeContainer } from './schema-node-container';

function ApiNode(node: Node<SchemaMember>) {
  const service = node.data.member as Service;

  function OperationList() {
    return (<>
      <tr className={'small-heading'}>
        <td colSpan={2}>Operations</td>
      </tr>
      {service.operations.map(operation => {
        return <tr>
          <td>{operation.qualifiedName.shortDisplayName}</td>
          <td>{operation.returnTypeName.shortDisplayName}</td>
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
        return <tr>
          <td><span className={'tag'}>{operation.grammar}</span>{operation.qualifiedName.shortDisplayName}</td>
          <td>{operation.returnTypeName.shortDisplayName}</td>
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
          <th colSpan={2}>{node.data.name.shortDisplayName}</th>
        </tr>
        <VersionTags></VersionTags>

        </thead>
        <tbody>
        {service.operations.length > 0 && <OperationList></OperationList>}
        {service.queryOperations.length > 0 && <QueryOperationList></QueryOperationList>}
        </tbody>
      </table>
      <Handle type="source" position={Position.Bottom} id="a"/>
    </SchemaNodeContainer>
  )
}

export default ApiNode
