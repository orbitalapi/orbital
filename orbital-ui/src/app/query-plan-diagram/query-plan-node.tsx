import * as React from 'react';
import { Node } from '@xyflow/react';
import {
  BaseQueryPlanNode,
  BaseQueryPlanNodeData,
  getBadgeClass,
  getTableClass
} from './base-query-plan-node';

export interface QueryPlanNodeData extends BaseQueryPlanNodeData {}

/**
 * Generic fallback node for query plan diagrams.
 * Handles CONSTANT, EXPRESSION, and other node kinds.
 */
function QueryPlanNode(node: Node<QueryPlanNodeData>): React.JSX.Element {
  const diagramNode = node.data.node;

  // CONSTANT nodes only have outputs (right side)
  const showLeftHandle = diagramNode.kind !== 'CONSTANT';

  return BaseQueryPlanNode(node, {
    tableClass: getTableClass(diagramNode.kind),
    badgeClass: getBadgeClass(diagramNode.kind),
    showLeftHandle,
    showRightHandle: true, // All nodes can have outputs
  });
}

export default QueryPlanNode;
