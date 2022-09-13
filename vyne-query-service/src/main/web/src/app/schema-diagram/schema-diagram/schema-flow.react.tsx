import * as React from 'react';
import { useCallback, useEffect, useState } from 'react';
import ReactFlow, {
  addEdge, Node,
  ReactFlowInstance,
  ReactFlowProvider,
  useEdgesState, useNodes,
  useNodesState,
  useUpdateNodeInternals
} from 'react-flow-renderer';
import { ElementRef } from '@angular/core';
import * as ReactDOM from 'react-dom';
import ModelNode from './diagram-nodes/model-node';
import ApiNode from './diagram-nodes/api-service-node';
import { RelativeNodePosition, SchemaChartController } from './schema-chart.controller';
import { findSchemaMember, Schema } from '../../services/schema';
import { Observable } from 'rxjs';
import { MemberWithLinks } from 'src/app/schema-diagram/schema-diagram/schema-chart-builder';

export type NodeType = 'Model' | 'Service';
type ReactComponentFunction = ({ data }: { data: any }) => JSX.Element
type NodeMap = { [key in NodeType]: ReactComponentFunction }

const nodeTypes: NodeMap = {
  'Model': ModelNode,
  'Service': ApiNode,
}

interface SchemaFlowDiagramProps {
  schema$: Observable<Schema>;
  requiredMembers$: Observable<[Schema, string[]]>;
  width: number;
  height: number;
}

function SchemaFlowDiagram(props: SchemaFlowDiagramProps) {
  const [nodes, setNodes, onNodesChange] = useNodesState([]);
  const [edges, setEdges, onEdgesChange] = useEdgesState([]);

  const [awaitingLayout, setAwaitingLayout] = useState(false);
  const updateNodeInternals = useUpdateNodeInternals();

  const nodes$ = useNodes();

  useEffect(() => {
    const subscription = props.requiredMembers$.subscribe(event => {
      const [schema, requiredMembers] = event;

      console.log('Required members has changed: ', requiredMembers);
      const buildResult = new SchemaChartController(schema, nodes, edges, requiredMembers).build({
        autoAppendLinks: true,
        layoutAlgo: 'full'
      })
      setNodes(buildResult.nodes);
      buildResult.nodesRequiringUpdate.forEach(node => updateNodeInternals(node.id));
      setEdges(buildResult.edges);

      // setAwaitingLayout(true);
    });
    return () => {
      subscription.unsubscribe();
    }
  });

  useEffect(() => {
    console.log('nodes: ', nodes$);
  }, [nodes$])


  function ensureMemberPresentByName(typeName: string, relativePosition: RelativeNodePosition | null = null, schema: Schema = this.currentSchema): Node<MemberWithLinks> {
    if (this.currentSchema === null) {
      throw new Error('Schema is not yet provided');
    }
    const schemaMember = findSchemaMember(schema, typeName);
    return this.appendOrUpdateMember(schemaMember, relativePosition, schema);
  }


  function resetLayout() {
    // useEffect(() => {
    //   controller.resetLayout(nodes, edges)
    //     .then(laidOutNodes => setNodes(laidOutNodes));
    // });

  }

  // const onConnect = useCallback((params) => setEdges((eds) => addEdge(params, eds)), []);
  return (<div style={{ height: props.height, width: props.width }}>
    <ReactFlow
      connectOnClick={false}
      nodes={nodes}
      edges={edges}
      nodeTypes={nodeTypes}
      onNodesChange={onNodesChange}
      onEdgesChange={onEdgesChange}
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
    requiredMembers$: Observable<[Schema, string[]]>,
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
