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
import { Observable } from 'rxjs';

export type NodeType = 'Model' | 'Service';
type ReactComponentFunction = ({ data }: { data: any }) => JSX.Element
type NodeMap = { [key in NodeType]: ReactComponentFunction }

const nodeTypes: NodeMap = {
  'Model': ModelNode,
  'Service': ApiNode,
}

interface SchemaFlowDiagramProps {
  schema$: Observable<Schema>;
  requiredMembers$: Observable<[Schema,string[]]>;
  width: number;
  height: number;
}

function SchemaFlowDiagram(props: SchemaFlowDiagramProps) {
  const [nodes, setNodes, onNodesChange] = useNodesState([]);
  const [edges, setEdges, onEdgesChange] = useEdgesState([]);
  const controller = new SchemaChartController(props.schema$,
    [nodes, setNodes],
    [edges, setEdges],
  )


  function initHandler(instance: ReactFlowInstance) {
    controller.instance = instance;
    props.requiredMembers$.subscribe(event => {
      const [schema,requiredMembers] = event;
      console.log('Required members has changed: ', requiredMembers);
      controller.updateCurrentMembers(schema,requiredMembers);
    })
    // Add a short timeout to let the UI render, so that elements are drawn & measured.
    setTimeout(() => {
      controller.resetLayout();
      // controller.forceDirectedLayoutsEnabled = true;
    }, 50);
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
    requiredMembers$: Observable<[Schema,string[]]>,
    schema$: Observable<Schema>,
    width: number = 1800,
    height: number = 1200
  ) {

    ReactDOM.render(
      React.createElement(SchemaFlowDiagramWithProvider, {
        schema$,
        requiredMembers$,
        width,
        height
      } as SchemaFlowDiagramProps),
      elementRef.nativeElement
    )
  }
}
