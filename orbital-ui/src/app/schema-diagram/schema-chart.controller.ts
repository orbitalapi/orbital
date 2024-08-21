import {findSchemaMember, Schema, SchemaMember, SchemaMemberKind, ServiceMember, Type} from '../services/schema';
import { Edge, EdgeMarkerType, MarkerType, Node, XYPosition } from '@xyflow/react';
import {
  buildSchemaNode, collectAllLinks,
  collectionOperations,
  collectLinks,
  EdgeParams,
  getNodeId,
  Link, edgeSourceAndTargetExist,
  MemberWithLinks,
  LinkKind
} from './schema-chart-builder';
import { colors } from 'src/app/schema-diagram/tailwind.colors';
import { CSSProperties } from 'react';
import {
  typeColor,
  modelColor,
  serviceColor,
  lineageDependencyColor
} from 'src/app/schema-diagram/diagram-nodes/schema-node-container';
import { AppendLinksHandler, SchemaMemberClickHandler } from 'src/app/schema-diagram/schema-flow.react';
import { isNullOrUndefined } from "../utils/utils";

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

  constructor(
    private readonly schema: Schema,
    private readonly currentNodes: ReadonlyArray<Node<MemberWithLinks>> = [],
    private readonly currentEdges: Edge[] = [],
    private readonly requiredMembers: string[] = []
  ) {
    this.operations = collectionOperations(schema);
    const map = new Map<string, Node<MemberWithLinks>>();
    currentNodes.forEach(node => map.set(node.id, node));
    this.currentNodesById = map;
  }

  build(buildOptions: {
    autoAppendLinks: boolean,
    layoutAlgo: 'full' | 'incremental',
    appendLinksHandler: AppendLinksHandler,
    clickHandler: SchemaMemberClickHandler,
    isNavigable: boolean,
  }): ChartBuildResult {
    const builtNodesById = new Map<string, Node<MemberWithLinks>>();

    this.requiredMembers.map(member => {
      let schemaMember:SchemaMember;
      try {
        schemaMember = findSchemaMember(this.schema, member);
      } catch (e) {
        // Catch errors that can occur when we get an update event, but the schema isn't up to date.
        // Don't throw it - this lets the loop continue, and we'll get an up-to-date schema eventually
        console.log(`Cannot build chart, as requested type ${member} is not present in the schema`);
        // Filter these out below
        return null;
      }

      const nodeId = getNodeId(schemaMember.kind, schemaMember.name);
      const existingPosition = this.currentNodesById.get(nodeId)?.position;
      return buildSchemaNode(
        this.schema,
        schemaMember,
        this.operations,
        buildOptions.appendLinksHandler,
        buildOptions.clickHandler,
        existingPosition,
        buildOptions.isNavigable,
      );
    })
      .filter(node => node !== null)
      .forEach(node => builtNodesById.set(node.id, node));

    const builtEdgedById = new Map<string, Edge>();
    this.currentEdges
      .filter(edge => builtNodesById.has(edge.source) && builtNodesById.has(edge.target))
      .filter(edge => {
        const sourceAndTargetExist = edgeSourceAndTargetExist(builtNodesById, edge);
        return sourceAndTargetExist;
      })
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

      // TODO: this has potential to go pear, should check the actual array contents as well!
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

  private buildEdge(sourceNode: Node<MemberWithLinks>, sourceHandleId: string, sourceSchemaKind: SchemaMemberKind, targetNode: Node<MemberWithLinks>, targetHandleId: string, targetSchemaKind: SchemaMemberKind, linkKind: LinkKind, linkId?: string): Edge {
    let label: string;
    let markerStart, markerEnd: EdgeMarkerType;
    // Default color
    let lineColor: string = colors.gray['800'];

    if (linkKind === 'lineage') {
      label = 'Is consumed by'
      markerEnd = {
        type: MarkerType.Arrow,
        width: 30,
        height: 30,
        color: lineageDependencyColor
      };
      lineColor = lineageDependencyColor;
    }
    if (sourceSchemaKind === 'OPERATION' && targetSchemaKind === 'TYPE') {
      label = 'provides';
      markerEnd = {
        type: MarkerType.Arrow,
        width: 30,
        height: 30,
        color: serviceColor
      };
      lineColor = serviceColor;
    } else if (sourceSchemaKind === 'TYPE' && targetSchemaKind === 'OPERATION') {
      label = 'Is input'
      markerEnd = {
        type: MarkerType.Arrow,
        width: 30,
        height: 30,
        color: (sourceNode.data.member.member as Type).isScalar ? typeColor : modelColor
      };
      lineColor = (sourceNode.data.member.member as Type).isScalar ? typeColor : modelColor;
    }
    const style: CSSProperties = {};
    if (sourceSchemaKind === 'TYPE' && targetSchemaKind === 'TYPE') {
      lineColor = (sourceNode.data.member.member as Type).isScalar ? typeColor : modelColor;
      style.strokeDasharray = '5,5';
    }
    style.stroke = lineColor;
    style.transition = "all 500ms ease-in-out, opacity 250ms ease-in-out"
    const edgeParams: EdgeParams = {
      sourceCanFloat: sourceSchemaKind === 'TYPE',
      targetCanFloat: targetSchemaKind === 'TYPE' ,
      label,
      linkKind: linkKind
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
      const nodeLinks = collectAllLinks(node.data);
      // NOTE: buildLinksForModelWithAttributes was created as the logic below doesn't account for
      //       all of the attribute links which aren't currently nominated to be rendered
      nodeLinks.filter(link => {
        return nodes.has(link.sourceNodeId) && nodes.has(link.targetNodeId)
      }).forEach(link => {
        const edge = this.buildEdge(
          nodes.get(link.sourceNodeId),
          link.sourceHandleId,
          link.sourceMemberType,
          nodes.get(link.targetNodeId),
          link.targetHandleId,
          link.targetMemberType,
          link.linkKind,
          link.linkId
        )
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
  return !isNullOrUndefined(item.node) && !isNullOrUndefined(item.direction);
}

export interface RelativeNodeXyPosition extends RelativeNodePosition {
  position: XYPosition
}

