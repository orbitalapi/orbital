import * as React from 'react';
import { Node, Position } from '@xyflow/react';
import { SchemaNodeContainer } from './schema-node-container';
import { LinkHandle } from './link-handle';
import { collectLinks, MemberWithLinks } from '../schema-chart-builder';

/**
 * Operation node for query plan diagrams.
 * Shows operation name in the header, with parameters listed in the body similar to a Model node.
 */
function OperationNode(node: Node<MemberWithLinks>) {
  const links = node.data.links;

  // Combine all links for the header handles
  const headerLinks = links.inputs.concat(links.outputs);

  return (
    <SchemaNodeContainer>
      <table className={'service'}>
        <thead>
          <tr>
            <th colSpan={2}>
              <div className={'header handle-container'}>
                <LinkHandle
                  node={node}
                  links={headerLinks}
                  position={Position.Left}
                  allowConnectionToFloat
                />
                <div className={'left-content'}>
                  <span className={'member-name'}>{node.data.member.name.shortDisplayName}</span>
                  <span className={'badge service'}>Operation</span>
                </div>
                <LinkHandle
                  node={node}
                  links={headerLinks}
                  position={Position.Right}
                  allowConnectionToFloat
                />
              </div>
            </th>
          </tr>
        </thead>
        <tbody>
          {/* Note: In query plan diagrams, members will be populated from DiagramNodeMember list */}
          {/* This is intentionally left empty as the structure is handled by query-plan-node */}
        </tbody>
      </table>
    </SchemaNodeContainer>
  );
}

export default OperationNode;
