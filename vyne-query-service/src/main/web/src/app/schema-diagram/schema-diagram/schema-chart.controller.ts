import {
  arrayMemberTypeNameOrTypeNameFromName,
  findSchemaMember,
  Schema,
  SchemaMember,
  ServiceMember
} from '../../services/schema';
import { Edge, Node, XYPosition } from 'react-flow-renderer';
import {
  buildSchemaNode,
  collectionOperations,
  collectLinks,
  getNodeId,
  Link,
  Links,
  MemberWithLinks,
  ModelLinks,
  ServiceLinks
} from './schema-chart-builder';
import { applyElkLayout } from './elk-chart-layout';
import { isUndefined } from 'util';

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
    layoutAlgo: 'full' | 'incremental'
  }): ChartBuildResult {
    console.log('Performing chart rebuild');
    const builtNodesById = new Map<string, Node<MemberWithLinks>>();

    this.requiredMembers.map(member => {
      const schemaMember = findSchemaMember(this.schema, member);
      const nodeId = getNodeId(schemaMember.kind, schemaMember.name);
      const existingPosition = this.currentNodesById.get(nodeId)?.position;
      return buildSchemaNode(this.schema, schemaMember, this.operations, this, existingPosition);
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

  appendNodesAndEdgesForLinks(nodeRequestingLink: Node<MemberWithLinks>, links: Link[], direction?: 'right' | 'left') {
    throw new Error('fix me');
    // const affectedNodes = links.flatMap(link => {
    //   // For whatever reason, looks like we're not getting parameterized names on some node types.
    //   // Will fix / investigate...eventually.
    //   const targetNodeName = link.targetNodeName.parameterizedName || link.targetNodeName.fullyQualifiedName;
    //   const targetNode = this.ensureMemberPresentByName(targetNodeName, { node: nodeRequestingLink, direction })
    //   const targetHandleId = link.targetHandleId;
    //
    //   const sourceNodeName = link.sourceNodeName.parameterizedName || link.sourceNodeName.fullyQualifiedName;
    //   const sourceNode = this.ensureMemberPresentByName(sourceNodeName, { node: nodeRequestingLink, direction })
    //   const sourceHandleId = link.sourceHandleId;
    //
    //   this.appendEdge(sourceNode, sourceHandleId, targetNode, targetHandleId)
    //   return [targetNode, sourceNode];
    // });
    // setTimeout(() => {
    //   const adjustedNodes = new CollisionDetector(this._instance.getNodes(), affectedNodes, direction)
    //     .adjustLayout();
    //   const [_, setNodes] = this.nodeState;
    //   setNodes(adjustedNodes)
    // }, 25);
  }

  private buildEdge(sourceNode: Node<MemberWithLinks>, sourceHandleId: string, targetNode: Node<MemberWithLinks>, targetHandleId: string): Edge {
    const id = [sourceNode.id, sourceHandleId, '->', targetNode.id, targetHandleId].join('-');
    return {
      id: id,
      source: sourceNode.id,
      sourceHandle: sourceHandleId,
      target: targetNode.id,
      targetHandle: targetHandleId,
      type: 'floating'
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
        const edge = this.buildEdge(nodes.get(link.sourceNodeId), link.sourceHandleId, nodes.get(link.targetNodeId), link.targetHandleId)
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

