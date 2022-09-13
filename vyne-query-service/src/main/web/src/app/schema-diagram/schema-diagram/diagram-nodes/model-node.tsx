import * as React from 'react';
import { Node } from 'react-flow-renderer';
import { Type } from '../../../services/schema';
import { SchemaNodeContainer } from './schema-node-container';
import { MemberWithLinks, ModelLinks } from '../schema-chart-builder';
import { LinkHandle } from './link-handle';

function ModelNode(node: Node<MemberWithLinks>) {

  const type: Type = node.data.member.member as Type;
  const links: ModelLinks = node.data.links as ModelLinks;

  return (
    <SchemaNodeContainer>
      <table>
        <thead>
        <tr className={'small-heading'}>
          <th colSpan={2}>Model</th>
        </tr>
        <tr className={'member-name'}>
          <th colSpan={2}>
            <div className={'handle-container'}>
              <LinkHandle node={node} links={links.inputs} handleType={'target'} handleId={'lhs'}></LinkHandle>
              {node.data.member.name.shortDisplayName}
              <LinkHandle node={node} links={links.outputs} handleType={'source'} handleId={'rhs'}></LinkHandle>
            </div>
          </th>
        </tr>
        </thead>
        <tbody>
        {Object.keys(type.attributes).map(fieldName => {
          const fieldLinks = links.attributeLinks[fieldName];
          return <tr key={'field-' + fieldName}>
            <td>
              <div className={'handle-container'}>
                {fieldName}
                <LinkHandle node={node} links={fieldLinks?.inputs} handleType={'target'}></LinkHandle>
              </div>
            </td>
            <td>
              <div className={'handle-container'}>
                {type.attributes[fieldName].type.shortDisplayName}
                <LinkHandle node={node} links={fieldLinks?.outputs} handleType={'source'}></LinkHandle>
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
