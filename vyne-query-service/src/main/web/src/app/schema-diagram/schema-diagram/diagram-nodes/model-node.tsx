import * as React from 'react';
import { Handle, Node, Position } from 'react-flow-renderer';
import { SchemaMember, Type } from '../../../services/schema';
import { SchemaNodeContainer } from './schema-node-container';
import { MemberWithLinks, ModelLinks } from '../schema-chart-builder';

function ModelNode(node: Node<MemberWithLinks>) {

  const type: Type = node.data.member.member as Type;
  const links: ModelLinks = node.data.links as ModelLinks;

  function hasConsumedBy(fieldName: string): boolean {
    return (links.attributeLinks[fieldName]?.consumedBy || []).length > 0
  }

  function hasProducedBy(fieldName: string): boolean {
    return (links.attributeLinks[fieldName]?.producedBy || []).length > 0
  }

  return (
    <SchemaNodeContainer>
      <table>
        <thead>
        <tr className={'small-heading'}>
          <th colSpan={2}>Model</th>
        </tr>
        <tr className={'member-name'}>
          <th colSpan={2}>{node.data.member.name.shortDisplayName}</th>
        </tr>
        </thead>
        <tbody>
        {Object.keys(type.attributes).map(fieldName => {
          return <tr key={'field-' + fieldName}>
            <td>
              <div className={'handle-container'}>
                {fieldName}
                {hasProducedBy(fieldName) ?
                  <Handle type="target" position={Position.Left} id={'input-' + fieldName}/> : <></>}
              </div>
            </td>
            <td>
              <div className={'handle-container'}>
                {type.attributes[fieldName].type.shortDisplayName}
                {hasConsumedBy(fieldName) ?
                  <Handle type="target" position={Position.Right} id={'output-' + fieldName}/> : <></>}
              </div>
            </td>
          </tr>
        })}
        </tbody>
      </table>
    </SchemaNodeContainer>
  )
}

export default ModelNode
