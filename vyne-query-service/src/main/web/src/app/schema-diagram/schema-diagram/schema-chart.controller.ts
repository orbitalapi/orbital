import { findSchemaMember, QualifiedName, Schema, SchemaMember, ServiceMember } from '../../services/schema';
import { Node, ReactFlowInstance, XYPosition } from 'react-flow-renderer';
import { SchemaChartNodeSet, SchemaChartState } from './schema-flow.react';
import { buildSchemaNode, collectionOperations } from './schema-chart-builder';
import { Subject } from 'rxjs';

const HORIZONTAL_GAP = 20;


export class SchemaChartController {

  private operations: ServiceMember[];

  private _instance: ReactFlowInstance;
  set instance(value: ReactFlowInstance) {
    this._instance = value;
  }

  readonly state = new SchemaChartState([], []);

  /**
   * when nodes are added, emits message with a subset of the state, containing
   * only new nodes and edges.
   */
  readonly nodesAdded$ = new Subject<SchemaChartNodeSet>()

  constructor(private readonly schema: Schema) {
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
    let xDelta = currentPosition.x + currentNode.width + HORIZONTAL_GAP;
    if (positionForNewNode.direction === 'left') {
      xDelta = xDelta * -1;
    }
    return {
      x: currentPosition.x + xDelta,
      y: currentPosition.y
    }
  }


  ensureMemberPresent(member: SchemaMember, positionForNewNode: RelativeNodePosition | null = null) {
    const position = this.calculatePosition(positionForNewNode)
    const node = buildSchemaNode(this.schema, member, this.operations, this, position)
    if (this.state.addNodeIfNotPresent(node) && this._instance) {
      this._instance.addNodes(node)
      this._instance.setNodes(this.state.nodes);
    }

  }

  ensureMemberPresentByName(typeName: QualifiedName, relativePosition: RelativeNodePosition | null = null) {
    const schemaMember = findSchemaMember(this.schema, typeName.parameterizedName);
    this.ensureMemberPresent(schemaMember, relativePosition)
  }
}

export interface RelativeNodePosition {
  node: Node;
  direction: 'left' | 'right';
}
