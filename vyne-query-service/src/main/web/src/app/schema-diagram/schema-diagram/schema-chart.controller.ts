import { findSchemaMember, QualifiedName, Schema, SchemaMember, ServiceMember } from '../../services/schema';
import { Edge, Node, ReactFlowInstance, UpdateNodeInternals, XYPosition } from 'react-flow-renderer';
import { SchemaChartNodeSet, SchemaChartState } from './schema-flow.react';
import { buildSchemaNode, collectionOperations, Link, MemberWithLinks } from './schema-chart-builder';
import { Subject } from 'rxjs';
import { Dispatch, SetStateAction, useState } from 'react';

const HORIZONTAL_GAP = 50;

export type State<T> = [T, Dispatch<SetStateAction<T>>]

export class SchemaChartController {

  private readonly operations: ServiceMember[];

  private _instance: ReactFlowInstance;
  set instance(value: ReactFlowInstance) {
    this._instance = value;
  }

  readonly state = new SchemaChartState([], []);

  get nodes(): Node<MemberWithLinks>[] {
    const [value, setter] = this.nodeState;
    return value;
  }

  get edges(): Edge[] {
    const [edges, _] = this.edgeState;
    return edges;
  }

  constructor(private readonly schema: Schema, private nodeState: State<Node<MemberWithLinks>[]>, private edgeState: State<Edge[]>, private updateNodeInternals:UpdateNodeInternals) {
    this.operations = collectionOperations(schema);
  }

  private calculatePosition(positionForNewNode: RelativeNodePosition | null): XYPosition {
    if (!positionForNewNode) {
      return {
        x: 0,
        y: 0
      }
    }

    const currentNode = this._instance.getNode(positionForNewNode.node.id)
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

  ensureMemberPresent(member: SchemaMember, positionForNewNode: RelativeNodePosition | null = null): Node<MemberWithLinks> {
    const position = this.calculatePosition(positionForNewNode)
    const newNode = buildSchemaNode(this.schema, member, this.operations, this, position)
    const [nodes, setNodes] = this.nodeState;
    setNodes(currentState => {
      if (hasNodeById(currentState, newNode.id)) {
        return currentState;
      } else {
        return currentState.concat(newNode);
      }
    })
    this.updateNodeInternals(newNode.id);
    return newNode;
  }

  ensureMemberPresentByName(typeName: string, relativePosition: RelativeNodePosition | null = null): Node<MemberWithLinks> {
    const schemaMember = findSchemaMember(this.schema, typeName);
    return this.ensureMemberPresent(schemaMember, relativePosition)
  }

  ensureMemberPresentByQualifiedName(typeName: QualifiedName, relativePosition: RelativeNodePosition | null = null): Node<MemberWithLinks> {
    return this.ensureMemberPresentByName(typeName.parameterizedName, relativePosition)
  }

  appendLinks(nodeRequestingLink: Node<MemberWithLinks>, sourceHandleId: string, links: Link[], direction: 'right' | 'left') {
    links.forEach(link => {
      // For whatever reason, looks like we're not getting parameterized names on some node types.
      // Will fix / investigate...eventually.
      const targetNodeName = link.targetNodeName.parameterizedName || link.targetNodeName.fullyQualifiedName;
      const targetNode = this.ensureMemberPresentByName(targetNodeName, { node: nodeRequestingLink, direction })
      const targetHandleId = link.targetHandleId;

      const sourceNodeName = link.sourceNodeName.parameterizedName || link.sourceNodeName.fullyQualifiedName;
      const sourceNode = this.ensureMemberPresentByName(sourceNodeName, { node: nodeRequestingLink, direction })
      const sourceTargetHandleId = link.sourceHandleId;

      const [edges,setEdges] = this.edgeState;
      setEdges(currentValue => currentValue.concat({
        id: [sourceNode.id, sourceTargetHandleId, '->', targetNode.id, targetHandleId].join('-'),
        source: sourceNode.id,
        sourceHandle: sourceTargetHandleId,
        target: targetNode.id,
        targetHandle: targetHandleId
      }))
    })
  }


}

function hasNodeById(nodes: Node<MemberWithLinks>[], id: string): boolean {
  return nodes.some(n => n.id === id);
}

export interface RelativeNodePosition {
  node: Node;
  direction: 'left' | 'right';
}
