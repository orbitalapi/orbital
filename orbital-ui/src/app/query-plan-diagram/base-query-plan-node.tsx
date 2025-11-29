import * as React from 'react';
import { Node, Position } from '@xyflow/react';
import { SchemaNodeContainer } from '../schema-diagram/diagram-nodes/schema-node-container';
import { LinkHandle } from '../schema-diagram/diagram-nodes/link-handle';
import { DiagramNode as DiagramNodeData, DiagramNodeKind } from '../services/query.service';
import { Link } from '../schema-diagram/schema-chart-builder';
import { NodeBadge } from './node-badge';

export interface BaseQueryPlanNodeData extends Record<string, unknown> {
  node: DiagramNodeData;
  inboundLinks: Link[];
  outboundLinks: Link[];
  memberLinks: { [handleId: string]: Link[] };
  onMemberClick?: ((nodeId: string, handleId: string) => void) | null;
}

interface NodeConfig {
  tableClass: string;
  badgeClass: string;
  showLeftHandle?: boolean;
  showRightHandle?: boolean;
}

/**
 * Shared base component for all query plan diagram nodes.
 * Handles the common structure: header with title/badge/handles, and tbody with members.
 */
export function BaseQueryPlanNode(
  node: Node<BaseQueryPlanNodeData>,
  config: NodeConfig
): React.JSX.Element {
  const diagramNode = node.data.node;
  const {
    tableClass,
    badgeClass,
    showLeftHandle = true,
    showRightHandle = true,
  } = config;

  // Use badgeLabel from server with fallback to kind
  const badgeLabel = diagramNode.badgeLabel || diagramNode.kind;

  // Combine all links for the header handles
  const headerInboundLinks = showLeftHandle ? node.data.inboundLinks : [];
  const headerOutboundLinks = showRightHandle ? node.data.outboundLinks : [];

  return (
    <SchemaNodeContainer>
      <table className={tableClass}>
        <thead>
          <tr>
            <th colSpan={2}>
              <div className={'header handle-container'}>
                {showLeftHandle && (
                  <LinkHandle
                    node={node as any}
                    links={headerInboundLinks}
                    position={Position.Left}
                    allowConnectionToFloat
                  />
                )}
                <div className={'left-content'}>
                  <span className={'member-name'}>{diagramNode.title}</span>
                  <NodeBadge label={badgeLabel} cssClass={badgeClass} iconId={diagramNode.icon} />
                </div>
                {showRightHandle && (
                  <LinkHandle
                    node={node as any}
                    links={headerOutboundLinks}
                    position={Position.Right}
                    allowConnectionToFloat
                  />
                )}
              </div>
            </th>
          </tr>
        </thead>
        <tbody>
          {diagramNode.members.map(member => {
            const memberLinks = node.data.memberLinks[member.handleId] || [];
            const handleClick = () => {
              if (node.data.onMemberClick) {
                node.data.onMemberClick(diagramNode.id, member.handleId);
              }
            };

            return (
              <tr
                key={member.handleId}
                onClick={handleClick}
                style={{ cursor: node.data.onMemberClick ? 'pointer' : 'default' }}
              >
                <td>
                  <div className={'handle-container'}>
                    {member.name}
                    <LinkHandle
                      node={node as any}
                      links={memberLinks}
                      position={Position.Left}
                      allowConnectionToFloat
                    />
                  </div>
                </td>
                <td>
                  <div className={'handle-container'}>
                    {member.typeName}
                    <LinkHandle
                      node={node as any}
                      links={memberLinks}
                      position={Position.Right}
                      allowConnectionToFloat
                    />
                  </div>
                </td>
              </tr>
            );
          })}
        </tbody>
      </table>
    </SchemaNodeContainer>
  );
}

/**
 * Helper to get CSS class for badge styling based on node kind.
 */
export function getBadgeClass(kind: DiagramNodeKind): string {
  switch (kind) {
    case 'MODEL':
      return 'model';
    case 'SCALAR_TYPE':
      return 'type';
    case 'REQUEST_MODEL':
      return 'request-model';
    case 'RESPONSE_MODEL':
      return 'response-model';
    case 'REQUEST_RESPONSE_MODEL':
      return 'request-response-model';
    case 'SERVICE':
    case 'OPERATION':
      return 'service';
    case 'EXPRESSION':
      return 'expression';
    case 'CONSTANT':
      return 'constant';
    default:
      return 'model';
  }
}

/**
 * Helper to get table CSS class for border styling based on node kind.
 */
export function getTableClass(kind: DiagramNodeKind): string {
  switch (kind) {
    case 'MODEL':
      return '';  // Default model color
    case 'SCALAR_TYPE':
      return 'type';
    case 'REQUEST_MODEL':
      return 'request-model';
    case 'RESPONSE_MODEL':
      return 'response-model';
    case 'REQUEST_RESPONSE_MODEL':
      return 'request-response-model';
    case 'SERVICE':
    case 'OPERATION':
      return 'service';
    case 'EXPRESSION':
      return 'expression';
    case 'CONSTANT':
      return 'constant';
    default:
      return '';
  }
}
