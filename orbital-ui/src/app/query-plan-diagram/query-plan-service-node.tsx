import * as React from 'react';
import { Node } from '@xyflow/react';
import { BaseQueryPlanNode, BaseQueryPlanNodeData } from './base-query-plan-node';

export interface QueryPlanServiceNodeData extends BaseQueryPlanNodeData {}

/**
 * Service node for query plan diagrams.
 */
function QueryPlanServiceNode(node: Node<QueryPlanServiceNodeData>): React.JSX.Element {
  return BaseQueryPlanNode(node, {
    tableClass: 'service',
    badgeClass: 'service',
  });
}

export default QueryPlanServiceNode;
