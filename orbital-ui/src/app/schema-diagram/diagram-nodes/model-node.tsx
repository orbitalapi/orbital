import * as React from 'react';
import {Node, Position} from '@xyflow/react';
import { Type } from 'src/app/services/schema';
import {ClickHandlerPayload} from '../schema-flow.react';
import { SchemaNodeContainer } from './schema-node-container';
import { collectLinks, MemberWithLinks, ModelLinks } from '../schema-chart-builder';
import { LinkHandle } from './link-handle';

function ModelNode(node: Node<MemberWithLinks>) {

  const type: Type = node.data.member.member as Type;
  const links: ModelLinks = node.data.links as ModelLinks;

  const modelLinks = links.inputs.concat(links.outputs);

  const clickHandler = (event, command: ClickHandlerPayload['command']) => {
    node.data.clickHandler({member: node.data.member, command});
    event.preventDefault();
    event.stopPropagation();
  }

  const heading = type.isScalar ? 'Type' : 'Model';
  const typeClass = type.isScalar ? 'type' : '';

  return (
    <SchemaNodeContainer>
      <table className={typeClass}>
        <thead>
          <tr>
            <th colSpan={2}>
              <div className={'header handle-container'}>
                <LinkHandle node={node} links={modelLinks} position={Position.Left} allowConnectionToFloat></LinkHandle>
                <div className={'left-content'}>
                  {node.data.isNavigable ?
                    <a href="#" onClick={(event) => clickHandler(event, 'navigate')}>{node.data.member.name.shortDisplayName}</a> :
                    <span className={'member-name'}>{node.data.member.name.shortDisplayName}</span>
                  }
                  <span className={'badge ' + heading.toLowerCase()}>{heading}</span>
                </div>
                <img className={'delete-btn'} onClick={(event) => clickHandler(event, 'delete')} src="assets/img/tabler/trash.svg"/>
                <LinkHandle node={node} links={modelLinks} position={Position.Right} allowConnectionToFloat></LinkHandle>
              </div>
            </th>
          </tr>
        </thead>
        <tbody>
        {Object.keys(type.attributes).map(fieldName => {
          const fieldLinks = collectLinks(links.attributeLinks[fieldName]);
          return <tr key={'field-' + fieldName}>
            <td>
              <div className={'handle-container'}>
                {fieldName}
                <LinkHandle node={node} links={fieldLinks} position={Position.Left} allowConnectionToFloat></LinkHandle>
              </div>
            </td>
            <td>
              <div className={'handle-container'}>
                {type.attributes[fieldName].type.shortDisplayName}
                <LinkHandle node={node} links={fieldLinks} position={Position.Right}  allowConnectionToFloat></LinkHandle>
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
