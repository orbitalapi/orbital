import * as React from 'react';
import { Node } from '@xyflow/react';
import { BaseQueryPlanNode, BaseQueryPlanNodeData } from './base-query-plan-node';

export interface QueryPlanOperationNodeData extends BaseQueryPlanNodeData {}

/**
 * Operation node for query plan diagrams.
 * Shows operation name in the header, with parameters listed in the body.
 */
function QueryPlanOperationNode(node: Node<QueryPlanOperationNodeData>): React.JSX.Element {
  return BaseQueryPlanNode(node, {
    tableClass: 'service',
    badgeClass: 'service',
  });
}

export default QueryPlanOperationNode;
