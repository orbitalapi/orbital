import * as React from 'react';
import { useCallback } from 'react';
import ReactFlow, {
  addEdge,
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
    props.initialMembers.forEach(member => controller.ensureMemberPresentByName(member));
    // Add a short timeout to let the UI render, so that elements are drawn & measured.
    setTimeout(() => controller.resetLayout(), 50);
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
