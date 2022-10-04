import { findSchemaMember, Schema, SchemaMemberType, ServiceMember } from '../../services/schema';
import { Edge, EdgeMarkerType, MarkerType, Node, XYPosition } from 'reactflow';
import {
  buildSchemaNode,
  collectionOperations,
  collectLinks,
  EdgeParams,
  getNodeId,
  Link,
  MemberWithLinks,
  ModelLinks,
  ServiceLinks
} from './schema-chart-builder';
import { isUndefined } from 'util';
import { colors } from 'src/app/schema-diagram/schema-diagram/tailwind.colors';
import { CSSProperties } from 'react';
import {
  modelNodeBorderColor,
  serviceNodeBorderColor
} from 'src/app/schema-diagram/schema-diagram/diagram-nodes/schema-node-container';
import { AppendLinksHandler } from 'src/app/schema-diagram/schema-diagram/schema-flow.react';

export const HORIZONTAL_GAP = 50;

export interface ChartBuildResult {
  nodes: Node<MemberWithLinks>[];
  edges: Edge[];

  /**
   * Nodes which existed in the previous state, but
   * now appear to have a different definition.
   * Once the chart is updated, you need to call updateFlowInternals()
   * for these nodes.
   */
  nodesRequiringUpdate: Node<MemberWithLinks>[]
}

export class SchemaChartController {

  private readonly operations: ServiceMember[];
  private readonly currentNodesById: ReadonlyMap<string, Node<MemberWithLinks>>


  constructor(private readonly schema: Schema, private readonly currentNodes: ReadonlyArray<Node<MemberWithLinks>> = [], private readonly currentEdges: Edge[] = [], private readonly requiredMembers: string[] = []) {
    this.operations = collectionOperations(schema);
    const map = new Map<string, Node<MemberWithLinks>>();
    currentNodes.forEach(node => map.set(node.id, node));
    this.currentNodesById = map;
  }

  build(buildOptions: {
    autoAppendLinks: boolean,
    layoutAlgo: 'full' | 'incremental',
    appendLinksHandler: AppendLinksHandler
  }): ChartBuildResult {
    const builtNodesById = new Map<string, Node<MemberWithLinks>>();

    this.requiredMembers.map(member => {
      const schemaMember = findSchemaMember(this.schema, member);
      const nodeId = getNodeId(schemaMember.kind, schemaMember.name);
      const existingPosition = this.currentNodesById.get(nodeId)?.position;
      return buildSchemaNode(this.schema, schemaMember, this.operations, buildOptions.appendLinksHandler, existingPosition);
    }).forEach(node => builtNodesById.set(node.id, node));

    const builtEdgedById = new Map<string, Edge>();
    this.currentEdges
      .filter(edge => builtNodesById.has(edge.source) && builtNodesById.has(edge.target))
      .forEach(edge => builtEdgedById.set(edge.id, edge));

    if (buildOptions.autoAppendLinks) {
      this.createAllViableEdges(builtNodesById)
        .forEach(edge => builtEdgedById.set(edge.id, edge))
    }

    const builtEdges = Array.from(builtEdgedById.values());
    let builtNodes = Array.from(builtNodesById.values());

    // Nodes
    const nodesRequiringUpdate = builtNodes.filter(node => {
      if (!this.currentNodesById.has(node.id)) {
        return false;
      }

      function linkId(link: Link): string {
        return link.sourceNodeId + '-' + link.targetNodeId;
      }

      const previousNodeLinks = new Set(collectLinks(this.currentNodesById.get(node.id).data.links).map(link => linkId(link)))
      const thisNodeLinks = new Set(collectLinks(node.data.links).map(link => linkId(link)));

      if (previousNodeLinks.size !== thisNodeLinks.size) {
        return true; // Nodes have a different number of links, so needs to be updated
      }
      return Array.from(previousNodeLinks).every(linkId => thisNodeLinks.has(linkId))
    })

    return {
      nodes: builtNodes,
      edges: builtEdges,
      nodesRequiringUpdate: nodesRequiringUpdate
    }
  }


  private calculatePosition(positionForNewNode: RelativeNodePosition | XYPosition | null): XYPosition {
    if (!positionForNewNode) {
      return {
        x: 50,
        y: 50
      }
    }

    if (!isRelativeNodePosition(positionForNewNode)) {
      // Must be a XYPosition
      return positionForNewNode;
    }

    const currentNode = this.currentNodesById.get(positionForNewNode.node.id)
    // Typedef suggests position should be a property, but that's not what I'm seeing
    // at runtime.
    const currentPosition = currentNode.position;
    let xDelta;
    if (positionForNewNode.direction === 'left') {
      // Ideally, we'd know the width of the new node.
      // When positioning to the left, it *should* be newNode.width + horizontalGap.
      // However, at this stage, the node isn't drawn / measured. So, use the current node as a guesstimate
      xDelta = (currentNode.width + HORIZONTAL_GAP) * -1;
    } else {
      xDelta = currentNode.width + HORIZONTAL_GAP;
    }
    return {
      x: currentPosition.x + xDelta,
      y: currentPosition.y
    }
  }


  private collectAllLinks(data: MemberWithLinks) {
    let childLinks = [];
    if (data.links instanceof ModelLinks || data.links instanceof ServiceLinks) {
      childLinks = data.links.collectAllChildLinks()
    }
    return collectLinks(data.links).concat(childLinks);
  }

  private buildEdge(sourceNode: Node<MemberWithLinks>, sourceHandleId: string, sourceSchemaKind: SchemaMemberType, targetNode: Node<MemberWithLinks>, targetHandleId: string, targetSchemaKind: SchemaMemberType, linkId?: string): Edge {
    let label: string;
    let markerStart, markerEnd: EdgeMarkerType;
    // Default color
    let lineColor: string = colors.gray['800'];
    if (sourceSchemaKind === 'OPERATION' && targetSchemaKind === 'TYPE') {
      label = 'provides';
      markerEnd = {
        type: MarkerType.Arrow,
        width: 30,
        height: 30,
        color: serviceNodeBorderColor
      };
      lineColor = serviceNodeBorderColor;
    }
    if (sourceSchemaKind === 'TYPE' && targetSchemaKind === 'OPERATION') {
      label = 'Is input'
      markerEnd = {
        type: MarkerType.Arrow,
        width: 30,
        height: 30,
        color: modelNodeBorderColor
      };
      lineColor = modelNodeBorderColor;
    }
    const style: CSSProperties = {
    };
    if (sourceSchemaKind === 'TYPE' && targetSchemaKind === 'TYPE') {
      lineColor = colors.lime['300'];
      style.strokeDasharray = '5,5';
    }
    style.stroke = lineColor;
    const edgeParams: EdgeParams = {
      sourceCanFloat: sourceSchemaKind === 'TYPE',
      targetCanFloat: targetSchemaKind === 'TYPE',
      label
    }


    const id = linkId || [sourceNode.id, sourceHandleId, '->', targetNode.id, targetHandleId].join('-');
    return {
      id: id,
      data: edgeParams,
      source: sourceNode.id,
      sourceHandle: sourceHandleId,
      target: targetNode.id,
      targetHandle: targetHandleId,
      type: 'floating',
      label: label,
      markerStart,
      markerEnd,
      style
    };
  }

  /**
   * Iterates the map of nodes, and creates an edge for all elements
   * where both the source and target are already present.
   */
  private createAllViableEdges(nodes: Map<string, Node<MemberWithLinks>>): Map<string, Edge> {
    // Find the nodes where we have both the source and the destination already present,
    // and append an edge
    const createdEdges = new Map<string, Edge>();
    Array.from(nodes.values()).forEach(node => {
      const nodeLinks = this.collectAllLinks(node.data);
      nodeLinks.filter(link => {
        return nodes.has(link.sourceNodeId) && nodes.has(link.targetNodeId)
      }).forEach(link => {
        const edge = this.buildEdge(nodes.get(link.sourceNodeId), link.sourceHandleId, link.sourceMemberType, nodes.get(link.targetNodeId), link.targetHandleId, link.targetMemberType, link.linkId)
        createdEdges.set(edge.id, edge);
      });
    })
    return createdEdges;
  }

}


export interface RelativeNodePosition {
  node: Node;
  direction: 'left' | 'right';
}

export function isRelativeNodePosition(item: any): item is RelativeNodePosition {
  return !isUndefined(item.node) && !isUndefined(item.direction);
}

export interface RelativeNodeXyPosition extends RelativeNodePosition {
  position: XYPosition
}
