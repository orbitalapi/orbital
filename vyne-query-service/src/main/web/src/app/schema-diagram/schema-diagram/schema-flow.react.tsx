import * as React from 'react';
import styled from 'styled-components';
import { useEffect, useState } from 'react';
import ReactFlow, {
  ConnectionMode, ControlButton, Controls, FitViewOptions,
  Node,
  ReactFlowProvider,
  useEdgesState,
  useNodesState, useReactFlow,
  useUpdateNodeInternals
} from 'reactflow';
import { ElementRef } from '@angular/core';
import * as ReactDOM from 'react-dom';
import ModelNode from './diagram-nodes/model-node';
import ApiNode from './diagram-nodes/api-service-node';
import { SchemaChartController } from './schema-chart.controller';
import { arrayMemberTypeNameOrTypeNameFromName, emptySchema, Schema } from '../../services/schema';
import { Observable } from 'rxjs';
import { Link, LinkKind, MemberWithLinks } from 'src/app/schema-diagram/schema-diagram/schema-chart-builder';
import { applyElkLayout } from 'src/app/schema-diagram/schema-diagram/elk-chart-layout';
import FloatingEdge from 'src/app/schema-diagram/schema-diagram/diagram-nodes/floating-edge';
import { isNullOrUndefined } from 'util';
import { toPng } from 'html-to-image';
import DownloadIcon from 'src/app/schema-diagram/schema-diagram/icons/download-icon';
import FullScreenIcon from 'src/app/schema-diagram/schema-diagram/icons/fullscreen-icon';
import MinimizeIcon from 'src/app/schema-diagram/schema-diagram/icons/minimize-icon';
import { colors } from './tailwind.colors';

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
  linkKinds: LinkKind[]
}

export interface RequiredMembersProps {
  schema: Schema | null;
  memberNames: string[];
}

const fitViewOptions: FitViewOptions = { padding: 1, includeHiddenNodes: true };

function SchemaFlowDiagram(props: SchemaFlowDiagramProps) {
  const [nodes, setNodes, onNodesChange] = useNodesState([]);
  const [edges, setEdges, onEdgesChange] = useEdgesState([]);
  const [isFullScreen, setFullScreen] = useState(false);

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
    const buildResult = new SchemaChartController(requiredMembers.schema, nodes, edges, requiredMembers.memberNames)
      .build({
        autoAppendLinks: true,
        layoutAlgo: 'full',
        appendLinksHandler: appendNodesAndEdgesForLinks
      })
    setNodes(buildResult.nodes);
    buildResult.nodesRequiringUpdate.forEach(node => updateNodeInternals(node.id));
    setEdges(buildResult.edges.filter(edge => {
      return props.linkKinds.includes(edge.data.linkKind)
    }));

    console.log('Requesting layout');
    setAwaitingLayout(true);
  }, [requiredMembers.memberNames.join(','), requiredMembers.schema.hash])

  function downloadImage() {
    toPng(document.querySelector<HTMLElement>('.react-flow__viewport'), {
      filter: (node) => {
        // we don't want to add the minimap and the controls to the image
        if (
          node?.classList?.contains('toolbar')
        ) {
          return false;
        }

        return true;
      },
    }).then((dataUrl) => {
      const a = document.createElement('a');

      a.setAttribute('download', 'orbital-microservices-diagram.png');
      a.setAttribute('href', dataUrl);
      a.click();
    });
  }

  const ToggleFullScreenButton = isFullScreen ? <MinimizeIcon/> : <FullScreenIcon/>
  const styleProps = isFullScreen ? {} : {
    height: props.height,
    width: props.width
  }

  return (<div className={isFullScreen ? 'fullscreen' : ''} style={styleProps}>
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
    >
      <Controls
        showInteractive={false}
      >
        <ControlButton onClick={downloadImage}>
          <DownloadIcon/>
        </ControlButton>
        <ControlButton onClick={() => setFullScreen(!isFullScreen)}>
          {ToggleFullScreenButton}
        </ControlButton>
      </Controls>
    </ReactFlow>
  </div>)
}

export const SchemaDiagramContainer = styled.div`
  .fullscreen {
    position: fixed;
    top: 2rem;
    height: calc(100vh - 4rem);
    left: 20px;
    width: calc(100vw - 60px);
    z-index: 2000;
    background-color: white;

    border: 1px solid ${colors.slate['300']};
    border-radius: 8px;

    box-shadow: rgba(0, 0, 0, 0.35) 0px 5px 15px;
  }

`;


function SchemaFlowDiagramWithProvider(props) {
  return (
    <SchemaDiagramContainer>
      <ReactFlowProvider>
        <SchemaFlowDiagram {...props}></SchemaFlowDiagram>
      </ReactFlowProvider>
    </SchemaDiagramContainer>
  )
}


export type AppendLinksHandler = (AppendLinksProps) => void;

export interface AppendLinksProps {
  nodeRequestingLink: Node<MemberWithLinks>;
  links: Link[];
  direction?: 'right' | 'left';
}

export class SchemaFlowWrapper {
  static destroy(
    elementRef: ElementRef
  ) {
    ReactDOM.unmountComponentAtNode(elementRef.nativeElement)
  }
  static initialize(
    elementRef: ElementRef,
    requiredMembers$: Observable<RequiredMembersProps>,
    schema$: Observable<Schema>,
    width: number = 1800,
    height: number = 1200,
    linkKinds: LinkKind[] = ['entity']
  ) {

    ReactDOM.render(
      React.createElement(SchemaFlowDiagramWithProvider, {
        schema$,
        requiredMembers$,
        width,
        height,
        linkKinds
      } as SchemaFlowDiagramProps),
      elementRef.nativeElement
    )
  }
}
