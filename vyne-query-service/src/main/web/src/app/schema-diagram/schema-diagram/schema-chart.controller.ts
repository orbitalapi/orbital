import { findSchemaMember, Schema, SchemaMember, ServiceMember } from '../../services/schema';
import { Edge, Node, ReactFlowInstance, UpdateNodeInternals, XYPosition } from 'react-flow-renderer';
import { buildSchemaNode, collectionOperations, Link, MemberWithLinks } from './schema-chart-builder';
import { Dispatch, SetStateAction } from 'react';
import { applyElkLayout } from './elk-chart-layout';
import { Box, System } from 'detect-collisions';
import { BodyOptions, PotentialVector } from 'detect-collisions/dist/model';
import { CollisionDetector } from './collision-detection';

export const HORIZONTAL_GAP = 50;

export type State<T> = [T, Dispatch<SetStateAction<T>>]

export class SchemaChartController {

  private readonly operations: ServiceMember[];

  private _instance: ReactFlowInstance;
  set instance(value: ReactFlowInstance) {
    this._instance = value;
  }

  get nodes(): Node<MemberWithLinks>[] {
    const [value, setter] = this.nodeState;
    return value;
  }

  get edges(): Edge[] {
    const [edges, _] = this.edgeState;
    return edges;
  }

  constructor(private readonly schema: Schema, private nodeState: State<Node<MemberWithLinks>[]>, private edgeState: State<Edge[]>, private updateNodeInternals: UpdateNodeInternals) {
    this.operations = collectionOperations(schema);
  }

  private calculatePosition(positionForNewNode: RelativeNodePosition | null): XYPosition {
    if (!positionForNewNode) {
      return {
        x: 50,
        y: 50
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
    });
    return newNode;
  }

  ensureMemberPresentByName(typeName: string, relativePosition: RelativeNodePosition | null = null): Node<MemberWithLinks> {
    const schemaMember = findSchemaMember(this.schema, typeName);
    return this.ensureMemberPresent(schemaMember, relativePosition)
  }

  appendLinks(nodeRequestingLink: Node<MemberWithLinks>, sourceHandleId: string, links: Link[], direction: 'right' | 'left') {
    const affectedNodes = links.flatMap(link => {
      // For whatever reason, looks like we're not getting parameterized names on some node types.
      // Will fix / investigate...eventually.
      const targetNodeName = link.targetNodeName.parameterizedName || link.targetNodeName.fullyQualifiedName;
      const targetNode = this.ensureMemberPresentByName(targetNodeName, { node: nodeRequestingLink, direction })
      const targetHandleId = link.targetHandleId;

      const sourceNodeName = link.sourceNodeName.parameterizedName || link.sourceNodeName.fullyQualifiedName;
      const sourceNode = this.ensureMemberPresentByName(sourceNodeName, { node: nodeRequestingLink, direction })
      const sourceTargetHandleId = link.sourceHandleId;

      const [edges, setEdges] = this.edgeState;
      setEdges(currentValue => currentValue.concat({
        id: [sourceNode.id, sourceTargetHandleId, '->', targetNode.id, targetHandleId].join('-'),
        source: sourceNode.id,
        sourceHandle: sourceTargetHandleId,
        target: targetNode.id,
        targetHandle: targetHandleId
      }));
      return [targetNode, sourceNode];
    });
    setTimeout(() => {
      const adjustedNodes = new CollisionDetector(this._instance.getNodes(), affectedNodes, direction)
        .adjustLayout();
      const [nodex, setNodes] = this.nodeState;
      setNodes(() => {
        return adjustedNodes
      });
    }, 100);


  }

  adjustLayout(affectedNodes: Node<MemberWithLinks>[], direction: 'right' | 'left') {
    const physics: System = new System();
    // first, add all the nodes:
    const nodes = this._instance.getNodes()
    const boxToNode = new Map<Box, Node>();
    const nodeToBox = new Map<string, Box>();
    nodes.forEach(node => {
      try {
        const box = new Box(node.position, node.width, node.height);
        boxToNode.set(box, node);
        nodeToBox.set(node.id, box);
        physics.insert(box);
      } catch (e) {
        debugger;
      }
    });
    physics.update();


    // affectedNodes.forEach(node => {
    //   const nodeFromGraph = this._instance.getNode(node.id);
    //   const box = nodeToBox.get(node.id);
    //   physics.
    //
    // })
  }


  resetLayout(fixedLayouts: RelativeNodeXyPosition[] = []) {

    if (!this._instance) {
      console.debug('Not performing layout, as initialization not completed yet');
      return;
    }

    setTimeout(() => {
      applyElkLayout(
        this._instance.getNodes(),
        this._instance.getEdges(),
        fixedLayouts
      ).then(laidOutNodes => {
        const [_, setNodes] = this.nodeState;
        setNodes(() => laidOutNodes);
      })
    }, 50);


    // const laidOutNodes = applyDagreLayout(
    //     this._instance.getNodes(),
    //     this._instance.getEdges()
    // )
    // const [_, setNodes] = this.nodeState;
    // setNodes(() => laidOutNodes);


  }
}

function hasNodeById(nodes: Node<MemberWithLinks>[], id: string): boolean {
  return nodes.some(n => n.id === id);
}

export interface RelativeNodePosition {
  node: Node;
  direction: 'left' | 'right';
}

export interface RelativeNodeXyPosition extends RelativeNodePosition {
  position: XYPosition
}

class NodeBox extends Box {
  constructor(public readonly node: Node, position: PotentialVector, width: number, height: number, options?: BodyOptions) {
    super(position, width, height, options);
  }

  get id(): string {
    return this.node.id;
  }
}
