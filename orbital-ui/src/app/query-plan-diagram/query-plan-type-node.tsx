import * as React from 'react';
import { Node } from '@xyflow/react';
import {
  BaseQueryPlanNode,
  BaseQueryPlanNodeData,
  getBadgeClass,
  getTableClass
} from './base-query-plan-node';

export interface QueryPlanTypeNodeData extends BaseQueryPlanNodeData {}

/**
 * Type/Model node for query plan diagrams.
 */
function QueryPlanTypeNode(node: Node<QueryPlanTypeNodeData>): React.JSX.Element {
  const diagramNode = node.data.node;

  return BaseQueryPlanNode(node, {
    tableClass: getTableClass(diagramNode.kind),
    badgeClass: getBadgeClass(diagramNode.kind),
  });
}

export default QueryPlanTypeNode;
