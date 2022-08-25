import * as React from 'react';
import { useCallback, useState } from 'react';
import ReactFlow, { addEdge, useEdgesState, useNodesState } from 'react-flow-renderer';
import { ElementRef } from '@angular/core';
import * as ReactDOM from 'react-dom';

const initialNodes = [
  {
    id: '1',
    type: 'input',
    data: { label: 'Input ddddsNode' },
    position: { x: 250, y: 25 },
  },

  {
    id: '2',
    // you can also pass a React component as a label
    data: { label: 'Foo' },
    position: { x: 100, y: 125 },
  },
  {
    id: '3',
    type: 'output',
    data: { label: 'Output Node' },
    position: { x: 250, y: 250 },
  },
];

const initialEdges = [
  { id: 'e12', source: '1', target: '2' },
  { id: 'e23', source: '2', target: '3', animated: true },
];

const onInit = (reactFlowInstance) => {
  console.log('flow loaded:', reactFlowInstance);
  console.log('edges: ', reactFlowInstance.getEdges())
}

function Flow() {
  const [nodes, setNodes, onNodesChange] = useNodesState(initialNodes);
  const [edges, setEdges, onEdgesChange] = useEdgesState(initialEdges);
  const onConnect = useCallback((params) => setEdges((eds) => addEdge(params, eds)), []);

  // const [nodes, setNodes] = useState(initialNodes);
  // const [edges, setEdges] = useState(initialEdges);

  return <div style={{ height: 800 }}>
    <ReactFlow nodes={nodes}
               edges={edges}
               onConnect={onConnect}
               onInit={onInit}
    />;
  </div>
}

export const REACT_FLOW_TEST_STATE: SchemaChartState = {
  nodes: initialNodes,
  edges: initialEdges
}
export default Flow;

export class SchemaFlowWrapper {
  static initialize(
    elementRef: ElementRef,
    state: SchemaChartState
  ) {

    ReactDOM.render(
      React.createElement(Flow),
      // <div style={{ height: 800, width: 600 }}>
      //   <ReactFlow nodes={state.nodes} edges={state.edges}/>
      // </div>,
      elementRef.nativeElement
    )
  }
}

export interface SchemaChartState {
  nodes: any[]
  edges: any[]
}
