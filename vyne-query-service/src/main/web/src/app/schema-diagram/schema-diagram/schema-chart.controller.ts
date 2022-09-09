import { containsSchemaMember, findSchemaMember, Schema, SchemaMember, ServiceMember } from '../../services/schema';
import { Edge, Node, ReactFlowInstance, UpdateNodeInternals, XYPosition } from 'react-flow-renderer';
import { buildSchemaNode, collectionOperations, Link, MemberWithLinks } from './schema-chart-builder';
import { Dispatch, SetStateAction } from 'react';
import { applyElkLayout } from './elk-chart-layout';
import { Box, System } from 'detect-collisions';
import { BodyOptions, PotentialVector } from 'detect-collisions/dist/model';
import { CollisionDetector } from './collision-detection';
import { Observable } from 'rxjs';

export const HORIZONTAL_GAP = 50;

export type State<T> = [T, Dispatch<SetStateAction<T>>]

export class SchemaChartController {

  private operations: ServiceMember[];

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

  private currentSchema: Schema | null = null;

  constructor(private readonly schema$: Observable<Schema>,
              private nodeState: State<Node<MemberWithLinks>[]>,
              private edgeState: State<Edge[]>) {
    schema$.subscribe(schema => {
      this.operations = collectionOperations(schema);
      this.currentSchema = schema;
      this.onSchemaChanged();
    });

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


  private onSchemaChanged() {
    if (!this._instance) {
      return;
    }
    // Access the nodes directly from the instance, rather than the state,
    // as that gives us position.
    const currentNodes: Node<MemberWithLinks>[] = this._instance.getNodes();
    const [nodes, setNodes] = this.nodeState;
    const updatedNodes = currentNodes
      // Filter out any nodes that no longer have schema elements.
      // Given we setNodes() to the updatedNode list below, this has the effect
      // of removing the node.
      .filter(node => containsSchemaMember(this.currentSchema, node.data.member.name.fullyQualifiedName))
      .map(node => {
        const updatedSchemaMember = findSchemaMember(this.currentSchema, node.data.member.name.fullyQualifiedName);
        const updatedNode = buildSchemaNode(this.currentSchema, updatedSchemaMember, this.operations, this);
        updatedNode.position = node.position;
        return updatedNode;
      })
    setNodes(() => updatedNodes);
  }

  ensureMemberPresent(member: SchemaMember, positionForNewNode: RelativeNodePosition | null = null, schema: Schema = this.currentSchema): Node<MemberWithLinks> {
    if (schema === null) {
      throw new Error('Schema is not yet provided');
    }
    const position = this.calculatePosition(positionForNewNode)
    const newNode = buildSchemaNode(schema, member, this.operations, this, position)
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

  ensureMemberPresentByName(typeName: string, relativePosition: RelativeNodePosition | null = null, schema: Schema = this.currentSchema): Node<MemberWithLinks> {
    if (this.currentSchema === null) {
      throw new Error('Schema is not yet provided');
    }
    const schemaMember = findSchemaMember(schema, typeName);
    return this.ensureMemberPresent(schemaMember, relativePosition, schema);
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

  updateCurrentMembers(schema: Schema, requiredMembers: string[]) {
    const [nodes, setNodes] = this.nodeState
    const nodesToRemove = nodes.filter(node => {
      !requiredMembers.some(member => node.data.member.name.fullyQualifiedName === member)
    })
    setNodes((currentState) => {
      return currentState.filter(node => !nodesToRemove.includes(node))
    })
    requiredMembers.forEach(member => this.ensureMemberPresentByName(member, null, schema));
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
