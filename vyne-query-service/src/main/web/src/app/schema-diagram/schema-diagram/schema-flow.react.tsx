import * as React from 'react';
import { useEffect, useState } from 'react';
import ReactFlow, {
  ConnectionMode, FitViewOptions,
  Node,
  ReactFlowProvider,
  useEdgesState,
  useNodesState, useReactFlow,
  useUpdateNodeInternals
} from 'react-flow-renderer';
import { ElementRef } from '@angular/core';
import * as ReactDOM from 'react-dom';
import ModelNode from './diagram-nodes/model-node';
import ApiNode from './diagram-nodes/api-service-node';
import { SchemaChartController } from './schema-chart.controller';
import { arrayMemberTypeNameOrTypeNameFromName, emptySchema, Schema } from '../../services/schema';
import { Observable } from 'rxjs';
import { Link, MemberWithLinks } from 'src/app/schema-diagram/schema-diagram/schema-chart-builder';
import { applyElkLayout } from 'src/app/schema-diagram/schema-diagram/elk-chart-layout';
import FloatingEdge from 'src/app/schema-diagram/schema-diagram/diagram-nodes/floating-edge';
import { isNullOrUndefined } from 'util';

export type NodeType = 'Model' | 'Service';
type ReactComponentFunction = ({ data }: { data: any }) => JSX.Element
type NodeMap = { [key in NodeType]: ReactComponentFunction }

const nodeTypes: NodeMap = {
  'Model': ModelNode,
  'Service': ApiNode,
}

const edgeTypes = {
  'floating': FloatingEdge
}

interface SchemaFlowDiagramProps {
  schema$: Observable<Schema>;
  requiredMembers$: Observable<RequiredMembersProps>;
  width: number;
  height: number;
}

export interface RequiredMembersProps {
  schema: Schema | null;
  memberNames: string[];
}

const fitViewOptions: FitViewOptions = { padding: 1, includeHiddenNodes: true };

function SchemaFlowDiagram(props: SchemaFlowDiagramProps) {
  const [nodes, setNodes, onNodesChange] = useNodesState([]);
  const [edges, setEdges, onEdgesChange] = useEdgesState([]);
  const instance = useReactFlow();

  const [awaitingLayout, setAwaitingLayout] = useState(false);
  const [awaitingRefit, setAwaitingRefit] = useState(false);

  const [requiredMembers, setRequiredMembers] = useState<RequiredMembersProps>({
    schema: emptySchema(),
    memberNames: []
  })
  const updateNodeInternals = useUpdateNodeInternals();

  const appendNodesAndEdgesForLinks = (props: AppendLinksProps) => {
    const newMemberNames = new Set<string>(requiredMembers.memberNames)
    props.links.forEach(link => {
      newMemberNames.add(arrayMemberTypeNameOrTypeNameFromName(link.sourceNodeName).fullyQualifiedName)
      newMemberNames.add(arrayMemberTypeNameOrTypeNameFromName(link.targetNodeName).fullyQualifiedName)
    })
    setRequiredMembers({
      schema: requiredMembers.schema,
      memberNames: Array.from(newMemberNames)
    })
  }

  useEffect(() => {
    const subscription = props.requiredMembers$.subscribe(event => {
      setRequiredMembers(event)
    });
    return () => {
      subscription.unsubscribe();
    }
  }, []); // Note for non-react devs:  Passing [] as deps means this useEffect() only runs on mount / unmount

  useEffect(() => {
    if (awaitingLayout) {
      const readyForLayout = nodes.length > 0 && nodes.every(node => !isNullOrUndefined((node.width) && !isNullOrUndefined(node.height)));
      if (!readyForLayout) {
        return;
      }
      console.log('Performing layout');

      applyElkLayout(nodes, edges)
        .then(result => {
          setAwaitingLayout(false);
          setNodes(result);
          setAwaitingRefit(true);
        });
    }
    if (awaitingRefit) {
      instance.fitView({ includeHiddenNodes: true });
      setAwaitingRefit(false);
    }
  });


  useEffect(() => {
    if (!requiredMembers.schema) {
      return;
    }
    console.log('Required members has changed: ', requiredMembers.memberNames);
    const buildResult = new SchemaChartController(requiredMembers.schema, nodes, edges, requiredMembers.memberNames).build({
      autoAppendLinks: true,
      layoutAlgo: 'full',
      appendLinksHandler: appendNodesAndEdgesForLinks
    })
    setNodes(buildResult.nodes);
    buildResult.nodesRequiringUpdate.forEach(node => updateNodeInternals(node.id));
    setEdges(buildResult.edges);

    console.log('Requesting layout');
    setAwaitingLayout(true);
  }, [requiredMembers.memberNames.join(','), requiredMembers.schema.hash])

  return (<div style={{ height: props.height, width: props.width }}>
    <ReactFlow
      connectOnClick={false}
      nodes={nodes}
      edges={edges}
      nodeTypes={nodeTypes}
      edgeTypes={edgeTypes}
      onNodesChange={onNodesChange}
      onEdgesChange={onEdgesChange}
      connectionMode={ConnectionMode.Loose}
      fitView
      fitViewOptions={fitViewOptions}
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


export type AppendLinksHandler = (AppendLinksProps) => void;

export interface AppendLinksProps {
  nodeRequestingLink: Node<MemberWithLinks>;
  links: Link[];
  direction?: 'right' | 'left';
}

export class SchemaFlowWrapper {
  static initialize(
    elementRef: ElementRef,
    requiredMembers$: Observable<RequiredMembersProps>,
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
