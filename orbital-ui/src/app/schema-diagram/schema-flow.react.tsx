import * as React from 'react';
import styled from 'styled-components';
import { useCallback, useEffect, useState } from 'react';
import {
  ReactFlow,
  ConnectionMode, ControlButton, Controls, Edge, EdgeTypes, FitViewOptions,
  Node,
  ReactFlowProvider,
  useEdgesState,
  useNodesState, useReactFlow, useStoreApi,
  useUpdateNodeInternals, useNodesInitialized, Background, BackgroundVariant
} from '@xyflow/react';
import { ElementRef } from '@angular/core';
import * as ReactDOM from 'react-dom';
import {SchemaDiagramSpec} from '../schema-diagram-markdown-wrapper/schema-diagram-markdown-wrapper.component';
import ModelNode from './diagram-nodes/model-node';
import ApiNode from './diagram-nodes/api-service-node';
import CopyAsMarkdownIcon from './icons/copy-as-markdown-icon';
import FlyoutMenu from './icons/flyout-menu';
import { SchemaChartController } from './schema-chart.controller';
import {
  arrayMemberTypeNameOrTypeNameFromName,
  emptySchema,
  Schema,
  SchemaMember,
  SchemaMemberNamed, SchemaMemberKind
} from '../services/schema';
import {Observable, Subject} from 'rxjs';
import {EdgeParams, Link, LinkKind, MemberWithLinks} from 'src/app/schema-diagram/schema-chart-builder';
import { applyElkLayout } from 'src/app/schema-diagram/elk-chart-layout';
import FloatingEdge from 'src/app/schema-diagram/diagram-nodes/floating-edge';
import DownloadIcon from 'src/app/schema-diagram/icons/download-icon';
import FullScreenIcon from 'src/app/schema-diagram/icons/fullscreen-icon';
import MinimizeIcon from 'src/app/schema-diagram/icons/minimize-icon';
import { colors } from './tailwind.colors';
import {
  useEscapeKey,
  fitViewOptions,
  downloadDiagramImage,
  useFlowHoverHandlers
} from './shared-flow-hooks';

export type NodeType = 'Model' | 'Service';
type ReactComponentFunction = ({ data }: { data: any }) => JSX.Element
type NodeMap = { [key in NodeType]: ReactComponentFunction }

const nodeTypes: NodeMap = {
  'Model': ModelNode,
  'Service': ApiNode
};

const edgeTypes = {
  'floating': FloatingEdge
};

interface SchemaFlowDiagramProps {
  schemaAndMembersToDisplay$: Observable<SchemaAndRequiredMembersProps>;
  width: number;
  height: number;
  linkKinds: LinkKind[];
  clickHandler$: Subject<ClickHandlerPayload>;
  memberUpdatedHandler$: Subject<Set<string>>
  fullScreenClickHandler$: Subject<boolean>
  isNavigable: boolean;
}

export interface SchemaAndRequiredMembersProps {
  schema: Schema | null;
  memberNames: string[];
  memberNamePositions?: Omit<SchemaDiagramSpec, 'showTypeToolbar'>
}

let previousDimensions: {width?: number, height?: number};

function SchemaFlowDiagram(props: SchemaFlowDiagramProps) {
  const store = useStoreApi();
  const [nodes, setNodes, onNodesChange] = useNodesState([]);
  const [edges, setEdges, onEdgesChange] = useEdgesState([]);
  const [isFullScreen, setIsFullScreen] = useState(false);
  const [isShiftKeyPressed, setIsShiftKeyPressed] = useState(false);
  const [layoutDirection, setLayoutDirection] = useState<'DOWN' | 'RIGHT' | 'NONE'>('RIGHT');

  const instance = useReactFlow();

  const nodesInitialized = useNodesInitialized();
  const [awaitingRefit, setAwaitingRefit] = useState<'immediate' | 'delayed'>();

  const [schema, setSchema] = useState<Schema>(emptySchema);
  const [requiredMembers, setRequiredMembers] = useState<string[]>([]);
  const [requiredMemberPositions, setRequiredMemberPositions] = useState<Omit<SchemaDiagramSpec, 'showTypeToolbar'>>();
  const updateNodeInternals = useUpdateNodeInternals();

  const appendNodesAndEdgesForLinks = (memberUpdatedHandler: Subject<Set<string>>, props: AppendLinksProps) => {
    const newMemberNames = new Set<string>(requiredMembers);
    props.links.forEach(link => {
      newMemberNames.add(arrayMemberTypeNameOrTypeNameFromName(link.sourceNodeName).fullyQualifiedName);
      newMemberNames.add(arrayMemberTypeNameOrTypeNameFromName(link.targetNodeName).fullyQualifiedName);
    });
    setSchema(schema);
    setRequiredMembers(Array.from(newMemberNames));
    memberUpdatedHandler.next(newMemberNames)
  };

  useEffect(() => {
    const subscription = props.schemaAndMembersToDisplay$.subscribe(event => {
      setSchema(event.schema);
      setRequiredMembers(event.memberNames);
      setRequiredMemberPositions(event.memberNamePositions);
      const nodesWithStaticPositions =
        Object.keys(event.memberNamePositions?.members ?? {}).filter(
          (key) =>
            event.memberNamePositions.members[key].x ||
            event.memberNamePositions.members[key].y
        ) ?? [];
      if (nodesWithStaticPositions.length) {
        setLayoutDirection('NONE')
      }
    });
    return () => {
      subscription.unsubscribe();
    };
  }, []); // Note for non-react devs:  Passing [] as deps means this useEffect() only runs on mount / unmount

  useEffect(() => {
    const handleShift = (event) => {
      if (event.keyCode === 16)
        setIsShiftKeyPressed(true)
    };
    window.addEventListener('keydown', handleShift);
    window.addEventListener('keyup', () => setIsShiftKeyPressed(false));

    return () => {
      window.removeEventListener('keydown', handleShift);
      window.removeEventListener('keyup', () => setIsShiftKeyPressed(false));
    };
  }, []);

  useEffect(() => {
    if (nodesInitialized) {
      performLayout();
      // reset this here so the user can move nodes around without everything reverting to the original layout passed in
      setRequiredMemberPositions({members: {}})
    }
  }, [nodesInitialized, layoutDirection]);

  useEffect(() => {
    if (awaitingRefit) {
      setTimeout(async () => {
        await instance.fitView({ ...fitViewOptions, duration: awaitingRefit === 'immediate' ? 0 : fitViewOptions.duration });
      }, 30)
      setAwaitingRefit(null);
    }
  }, [awaitingRefit] );

  useEffect(() => {
    if (!schema?.types.length || !requiredMembers) {
      return;
    }
    console.log('Required members or hash of schema has changed: ', requiredMembers, schema.hash);

    const clickHandler = (event: ClickHandlerPayload) => props.clickHandler$.next(event);

    const buildResult = new SchemaChartController(schema, nodes, edges, requiredMembers)
      .build({
        autoAppendLinks: true,
        appendLinksHandler: (event) => appendNodesAndEdgesForLinks(props.memberUpdatedHandler$, event),
        clickHandler,
        isNavigable: props.isNavigable,
        existingPositions: requiredMemberPositions
      });
    setNodes(buildResult.nodes);
    console.log("buildResult.nodesRequiringUpdate", buildResult.nodesRequiringUpdate);
    buildResult.nodesRequiringUpdate.forEach(node => updateNodeInternals(node.id));
    setEdges(buildResult.edges.filter((edge: Edge<EdgeParams>) => {
      return props.linkKinds.includes(edge.data.linkKind);
    }));
  }, [requiredMembers.join(','), schema.hash]);

  const performLayout = async () => {
    if (layoutDirection === 'NONE') {
      console.log('NOT performing layout');
      setAwaitingRefit('immediate');
      return;
    }

    console.log('Performing layout');
    const result = await applyElkLayout(
      instance.getNodes(),
      instance.getEdges(),
      layoutDirection
    );

    setNodes(result);
    if (result.length === 1) {
      // Avoid silly animation when the component starts up
      const node = result[0];
      instance.updateNode(node.id, { hidden: true });
      await instance.fitBounds(
        {
          x: node.position.x,
          y: node.position.y,
          width: node.measured?.width,
          height: node.measured?.height,
        },
        { padding: fitViewOptions.padding }
      );
      instance.updateNode(node.id, { hidden: false });
      setAwaitingRefit(null);
    } else {
      setAwaitingRefit('delayed');
    }
  };

  useEscapeKey(() => {
    setAwaitingRefit('immediate');
    props.fullScreenClickHandler$.next(false)
    setIsFullScreen(false)
  })

  async function copyAsMarkdown() {
    const textToCopy = `
\`\`\`schemaDiagram
{
  "members" : {
    ${nodes.map(node => `"${node.id}": {"x": ${Math.round(node.position.x)}, "y": ${Math.round(node.position.y)}}`).join(',\n    ')}
  }
}
\`\`\`
`
    await navigator.clipboard.writeText(textToCopy)
  }


  const ToggleFullScreenButton = isFullScreen ? <MinimizeIcon /> : <FullScreenIcon />;
  const styleProps = isFullScreen ? {} : {
    width: props.width,
    height: props.height
  };
  if (previousDimensions?.width !== props.width || previousDimensions?.height !== props.height) {
    if (previousDimensions && !isFullScreen) {
      setAwaitingRefit('immediate');
    }
  }
  previousDimensions = styleProps;

  useEscapeKey(() => {
    setAwaitingRefit('immediate');
    setIsFullScreen(false);
  });

  const { onNodeMouseEnter, onEdgeMouseEnter, onEdgeOrNodeMouseLeave } = useFlowHoverHandlers(
    store,
    setNodes,
    setEdges,
    { skipOnCondition: isShiftKeyPressed }
  );

  const onNodeDragStart = useCallback((_, node: Node) => {
    const { edges } = store.getState();
    const mappedEdges = edges.map(edge => {
      return {
        ...edge,
        style: {
          ...edge.style,
          transition: 'opacity 250ms ease-in-out'
        }
      };
    });
    setEdges(mappedEdges);
  }, [setEdges, store]);

  const onNodeDragStop = useCallback((_, node: Node) => {
    const { edges } = store.getState();
    const mappedEdges = edges.map(edge => {
      return {
        ...edge,
        style: {
          ...edge.style,
          transition: 'transform 500ms ease-in-out, opacity 250ms ease-in-out'
        }
      };
    });
    setEdges(mappedEdges);
  }, [setEdges, store]);

  const onNodesDelete = useCallback(( nodes: Node[]) => {
    const filteredMembers = requiredMembers.filter(
      member => !nodes.some(node => node.id.split('-')[1] === member)
    );
    props.memberUpdatedHandler$.next(new Set(filteredMembers))
  }, [requiredMembers]);

  return (<div className={isFullScreen ? 'fullscreen' : ''} style={styleProps}>
    <ReactFlow
      connectOnClick={false}
      nodesConnectable={false}
      nodes={nodes}
      edges={edges}
      nodeTypes={nodeTypes}
      edgeTypes={edgeTypes as EdgeTypes}
      onNodesChange={onNodesChange}
      onEdgesChange={onEdgesChange}
      connectionMode={ConnectionMode.Loose}
      onNodeMouseEnter={onNodeMouseEnter}
      onNodeMouseLeave={onEdgeOrNodeMouseLeave}
      onEdgeMouseEnter={onEdgeMouseEnter}
      onEdgeMouseLeave={onEdgeOrNodeMouseLeave}
      onNodeDragStart={onNodeDragStart}
      onNodeDragStop={onNodeDragStop}
      onNodesDelete={onNodesDelete}
      multiSelectionKeyCode={['Shift']}
      selectionKeyCode={['Shift']}
      deleteKeyCode={['Backspace', 'Delete']}
    >
      <Controls
        showInteractive={false}
      >
        <ControlButton>
          <FlyoutMenu onDirectionChange={setLayoutDirection} layoutDirection={layoutDirection} />
        </ControlButton>
        <ControlButton onClick={copyAsMarkdown} title={"copy as markdown"}>
          <CopyAsMarkdownIcon />
        </ControlButton>
        <ControlButton onClick={() => downloadDiagramImage('orbital-microservices-diagram.png')} title={"download image"}>
          <DownloadIcon />
        </ControlButton>
        <ControlButton title={!isFullScreen ? 'maximise view' : 'minimise view'} onClick={() => {
          setIsFullScreen(!isFullScreen)
          setAwaitingRefit('immediate');
          props.fullScreenClickHandler$.next(!isFullScreen)
        }}>
          {ToggleFullScreenButton}
        </ControlButton>
      </Controls>
      <Background color="#ccc" variant={BackgroundVariant.Dots} />
    </ReactFlow>
  </div>);
}

export const SchemaDiagramContainer = styled.div`
  .fullscreen {
    position: fixed;
    top: 1rem;
    left: 1rem;
    width: calc(100% - 2rem);
    height: calc(100% - 2rem);
    z-index: 2000;
    background-color: ${colors.slate['50']};
    border: 1px solid ${colors.slate['300']};
    border-radius: 4px;
    box-shadow: rgba(0, 0, 0, 0.35) 0 5px 15px;
  }
`;


function SchemaFlowDiagramWithProvider(props) {
  return (
    <SchemaDiagramContainer>
      <ReactFlowProvider>
        <SchemaFlowDiagram {...props}></SchemaFlowDiagram>
      </ReactFlowProvider>
    </SchemaDiagramContainer>
  );
}


export type SchemaMemberClickProps = SchemaMember | { name: SchemaMemberNamed, type: SchemaMemberKind }
export type ClickHandlerPayload = {member: SchemaMemberClickProps, command: 'navigate' | 'delete'}
export type SchemaMemberClickHandler = (event: ClickHandlerPayload) => void;
export type AppendLinksHandler = (event: AppendLinksProps) => void;

export interface AppendLinksProps {
  nodeRequestingLink: Node<MemberWithLinks>;
  links: Link[];
  direction?: 'right' | 'left';
}

export class SchemaFlowWrapper {
  static destroy(elementRef: ElementRef) {
    ReactDOM.unmountComponentAtNode(elementRef.nativeElement);
  }

  static initialize(
    elementRef: ElementRef,
    schemaAndMembersToDisplay$: Observable<SchemaAndRequiredMembersProps>,
    width: number = 1800,
    height: number = 1200,
    linkKinds: LinkKind[] = ['entity'],
    clickHandler$: Subject<ClickHandlerPayload>,
    memberUpdatedHandler$: Subject<Set<string>>,
    fullScreenClickHandler$: Subject<boolean>,
    isNavigable: boolean
  ) {
    ReactDOM.render(
      React.createElement(SchemaFlowDiagramWithProvider, {
        schemaAndMembersToDisplay$,
        width,
        height,
        linkKinds,
        clickHandler$,
        memberUpdatedHandler$,
        fullScreenClickHandler$,
        isNavigable
      } as SchemaFlowDiagramProps),
      elementRef.nativeElement
    );
  }
}
