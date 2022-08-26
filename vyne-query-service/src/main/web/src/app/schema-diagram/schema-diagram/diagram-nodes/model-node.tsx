import * as React from 'react';
import { useCallback } from 'react';
import { Handle, Position, Node } from 'react-flow-renderer';
import { SchemaMember, Type } from '../../../services/schema';
import '../schema-styles.scss';
import { SchemaNodeContainer } from './schema-node-container';

function ModelNode(node: Node<SchemaMember>) {

  const type: Type = node.data.member as Type;
  return (
    <SchemaNodeContainer>
      <table>
        <thead>
        <tr className={'node-type'}>
          <th colSpan={2}>Model</th>
        </tr>
        <tr className={'member-name'}>
          <th colSpan={2}>{node.data.name.shortDisplayName}</th>
        </tr>
        </thead>
        <tbody>
        {Object.keys(type.attributes).map(fieldName => {
          return <tr key={'field-' + fieldName}>
            <td>{fieldName}</td>
            <td>{type.attributes[fieldName].type.shortDisplayName} </td>
          </tr>
        })}
        </tbody>
      </table>
      <Handle type="source" position={Position.Bottom} id="a"/>
    </SchemaNodeContainer>
  )
}

export default ModelNode
