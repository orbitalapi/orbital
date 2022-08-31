import * as React from 'react';
import { useCallback } from 'react';
import ReactFlow, {
  addEdge,
  Edge,
  Node,
  ReactFlowInstance,
  ReactFlowProvider,
  useEdgesState,
  useNodesState,
  useUpdateNodeInternals
} from 'react-flow-renderer';
import { ElementRef } from '@angular/core';
import * as ReactDOM from 'react-dom';
import ModelNode from './diagram-nodes/model-node';
import ApiNode from './diagram-nodes/api-service-node';
import { MemberWithLinks } from './schema-chart-builder';
import { SchemaChartController } from './schema-chart.controller';
import { Schema } from '../../services/schema';

export type NodeType = 'Model' | 'Service';
type ReactComponentFunction = ({ data }: { data: any }) => JSX.Element
type NodeMap = { [key in NodeType]: ReactComponentFunction }

const nodeTypes: NodeMap = {
  'Model': ModelNode,
  'Service': ApiNode,
}

interface SchemaFlowDiagramProps {
  schema: Schema;
  initialMembers: string[];
  width: number;
  height: number;
}

function SchemaFlowDiagram(props: SchemaFlowDiagramProps) {
  const [nodes, setNodes, onNodesChange] = useNodesState([]);
  const [edges, setEdges, onEdgesChange] = useEdgesState([]);
  const updateNodeInternals = useUpdateNodeInternals();
  const controller = new SchemaChartController(props.schema,
    [nodes, setNodes],
    [edges, setEdges],
    updateNodeInternals
  )




  function initHandler(instance: ReactFlowInstance) {
    controller.instance = instance;
    props.initialMembers.forEach(member => controller.ensureMemberPresentByName(member))
  }

  const onConnect = useCallback((params) => setEdges((eds) => addEdge(params, eds)), []);
  return (<div style={{ height: props.height, width: props.width }}>
    <ReactFlow
      onInit={initHandler}
      connectOnClick={false}
      nodes={controller.nodes}
      edges={controller.edges}
      nodeTypes={nodeTypes}
      onNodesChange={onNodesChange}
      onEdgesChange={onEdgesChange}
      onConnect={onConnect}
    />
  </div>)
}

function SchemaFlowDiagramWithProvider(props) {
  return (
    <ReactFlowProvider>
      <SchemaFlowDiagram {...props}></SchemaFlowDiagram>
    </ReactFlowProvider>
  )
}

export class SchemaFlowWrapper {
  static initialize(
    elementRef: ElementRef,
    initialMembers: string[],
    schema: Schema,
    width: number = 1800,
    height: number = 1200
  ) {

    ReactDOM.render(
      React.createElement(SchemaFlowDiagramWithProvider, {
        schema,
        initialMembers,
        width,
        height
      } as SchemaFlowDiagramProps),
      elementRef.nativeElement
    )
  }
}

export interface SchemaChartNodeSet {
  nodes: Node<MemberWithLinks>[];
  edges: Edge[]
}

export class SchemaChartState implements SchemaChartNodeSet {


  constructor(public readonly nodes: Node<MemberWithLinks>[], public readonly edges: Edge[]) {

  }


  /**
   * Adds the node into the state, if another node
   * with the same id is not already present.
   *
   * Returns a boolean indicating if the node was added.
   */
  addNodeIfNotPresent(node: Node<MemberWithLinks>): boolean {
    if (this.nodes.find(n => n.id === node.id)) {
      return false;
    } else {
      // this.setNodes(prev => prev.concat(node))
      return true;
    }

  }
}
