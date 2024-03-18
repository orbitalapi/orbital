import * as React from 'react';
import styled from 'styled-components';
import { useCallback, useEffect, useState } from 'react';
import ReactFlow, {
  ConnectionMode, ControlButton, Controls, Edge, FitViewOptions,
  Node,
  ReactFlowProvider,
  useEdgesState,
  useNodesState, useReactFlow, useStoreApi,
  useUpdateNodeInternals
} from 'reactflow';
import { ElementRef } from '@angular/core';
import * as ReactDOM from 'react-dom';
import ModelNode from './diagram-nodes/model-node';
import ApiNode from './diagram-nodes/api-service-node';
import { SchemaChartController } from './schema-chart.controller';
import {
  arrayMemberTypeNameOrTypeNameFromName,
  emptySchema,
  Schema,
  SchemaMember,
  SchemaMemberNamed, SchemaMemberKind
} from '../../services/schema';
import { Observable, Subject } from 'rxjs';
import { Link, LinkKind, MemberWithLinks } from 'src/app/schema-diagram/schema-diagram/schema-chart-builder';
import { applyElkLayout } from 'src/app/schema-diagram/schema-diagram/elk-chart-layout';
import FloatingEdge from 'src/app/schema-diagram/schema-diagram/diagram-nodes/floating-edge';
import { toPng } from 'html-to-image';
import DownloadIcon from 'src/app/schema-diagram/schema-diagram/icons/download-icon';
import FullScreenIcon from 'src/app/schema-diagram/schema-diagram/icons/fullscreen-icon';
import MinimizeIcon from 'src/app/schema-diagram/schema-diagram/icons/minimize-icon';
import { colors } from './tailwind.colors';
import {isNullOrUndefined} from "../../utils/utils";

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
  clickHandler: Subject<SchemaMember>;
  isNavigable: boolean;
}

export interface SchemaAndRequiredMembersProps {
  schema: Schema | null;
  memberNames: string[];
}

const fitViewOptions: FitViewOptions = { padding: 0.25, includeHiddenNodes: true };
let previousDimensions: {width?: number, height?: number};

function SchemaFlowDiagram(props: SchemaFlowDiagramProps) {
  const store = useStoreApi();
  const [nodes, setNodes, onNodesChange] = useNodesState([]);
  const [edges, setEdges, onEdgesChange] = useEdgesState([]);
  const [isFullScreen, setFullScreen] = useState(false);

  const instance = useReactFlow();

  const [awaitingLayout, setAwaitingLayout] = useState(false);
  const [awaitingRefit, setAwaitingRefit] = useState(false);

  const [schema, setSchema] = useState<Schema>(emptySchema);
  const [requiredMembers, setRequiredMembers] = useState<string[]>([]);
  const updateNodeInternals = useUpdateNodeInternals();

  const appendNodesAndEdgesForLinks = (props: AppendLinksProps) => {
    const newMemberNames = new Set<string>(requiredMembers);
    props.links.forEach(link => {
      newMemberNames.add(arrayMemberTypeNameOrTypeNameFromName(link.sourceNodeName).fullyQualifiedName);
      newMemberNames.add(arrayMemberTypeNameOrTypeNameFromName(link.targetNodeName).fullyQualifiedName);
    });
    setSchema(schema);
    setRequiredMembers(Array.from(newMemberNames));
  };

  useEffect(() => {
    const subscription = props.schemaAndMembersToDisplay$.subscribe(event => {
      setSchema(event.schema);
      setRequiredMembers(event.memberNames);
    });
    return () => {
      subscription.unsubscribe();
    };
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
      instance.fitView(fitViewOptions);
      setAwaitingRefit(false);
    }
  }, );

  useEffect(() => {
    if (!schema || !requiredMembers?.length) {
      return;
    }
    console.log('Required members or hash of schema has changed: ', requiredMembers, schema.hash);

    const clickHandler = (schemaMember: SchemaMember) => props.clickHandler.next(schemaMember);

    const buildResult = new SchemaChartController(schema, nodes, edges, requiredMembers)
      .build({
        autoAppendLinks: true,
        layoutAlgo: 'full',
        appendLinksHandler: appendNodesAndEdgesForLinks,
        clickHandler,
        isNavigable: props.isNavigable
      });
    setNodes(buildResult.nodes);
    console.log("buildResult.nodesRequiringUpdate", buildResult.nodesRequiringUpdate);
    buildResult.nodesRequiringUpdate.forEach(node => updateNodeInternals(node.id));
    setEdges(buildResult.edges.filter(edge => {
      return props.linkKinds.includes(edge.data.linkKind);
    }));

    console.log('Requesting layout');
    setAwaitingLayout(true);
  }, [requiredMembers.join(','), schema.hash]);

  function downloadImage() {
    toPng(document.querySelector<HTMLElement>('.react-flow__viewport'), {
      filter: (node) => {
        // we don't want to add the minimap and the controls to the image
        return !node?.classList?.contains('toolbar');


      }
    }).then((dataUrl) => {
      const a = document.createElement('a');

      a.setAttribute('download', 'orbital-microservices-diagram.png');
      a.setAttribute('href', dataUrl);
      a.click();
    });
  }

  const ToggleFullScreenButton = isFullScreen ? <MinimizeIcon /> : <FullScreenIcon />;
  const styleProps = isFullScreen ? {} : {
    width: props.width,
    height: props.height
  };
  if (previousDimensions?.width !== props.width || previousDimensions?.height !== props.height) {
    if (previousDimensions && !isFullScreen) {
      setAwaitingRefit(true);
    }
  }
  previousDimensions = styleProps;

  // Highlight edges and nodes that are linked to those edges emanating from the hovered node
  const onNodeMouseEnter = useCallback(
    (_, node: Node) => {
      const { edges, getNodes } = store.getState();
      const id = node.id;
      let hasChange = false;
      const activeEdges = [];
      const mappedEdges = edges.map((edge) => {
        let targetOpacity = 1;
        if (edge.source !== id && edge.target !== id) {
          hasChange = true;
          targetOpacity = 0.2;
        } else {
          activeEdges.push(edge);
        }
        return {
          ...edge,
          style: {
            ...edge.style,
            opacity: targetOpacity
          }
        };
      });
      if (hasChange) {
        setEdges(mappedEdges);
      }

      if (getNodes().length <= 2) return;

      // If the node doesn't interact with the edge, then make it opaque
      hasChange = false;
      const filteredNodes = getNodes().map(node => {
        let targetOpacity = 1;
        if (!activeEdges.filter(edge => node.id === edge.source || node.id === edge.target).length && node.id !== id) {
          hasChange = true;
          targetOpacity = 0.2;
        }
        return {
          ...node,
          style: {
            ...node.style,
            opacity: targetOpacity
          }
        };
      })
      if (hasChange) setNodes(filteredNodes);
    },
    [setEdges, setNodes, store]
  );

  const onEdgeMouseEnter = useCallback(
    (_, edge: Edge) => {
      const { edges, getNodes } = store.getState();
      const id = edge.id;
      let hasChange = false;

      const mappedNodes = getNodes().map(node => {
        let targetOpacity = 1;
        if (node.id !== edge.source && node.id !== edge.target) {
          hasChange = true;
          targetOpacity = 0.2;
        }
        return {
          ...node,
          style: {
            ...node.style,
            opacity: targetOpacity
          }
        };
      })
      if (hasChange) setNodes(mappedNodes);

      hasChange = false;
      const mappedEdges = edges.map((edge) => {
        let targetOpacity = 1;
        if (edge.id !== id) {
          hasChange = true;
          targetOpacity = 0.2;
        }
        return {
          ...edge,
          style: {
            ...edge.style,
            opacity: targetOpacity
          }
        };
      });
      if (hasChange) setEdges(mappedEdges);
    },
    [setEdges, setNodes, store]
  );

  const onEdgeOrNodeMouseLeave = useCallback((_, edge: Edge | Node) => {
    const { edges, getNodes } = store.getState();
    const mappedNodes = getNodes().map(node => {
      return {
        ...node,
        style: {
          ...node.style,
          opacity: 1
        }
      };
    });
    const mappedEdges = edges.map(edge => {
      return {
        ...edge,
        style: {
          ...edge.style,
          opacity: 1
        }
      };
    });
    setNodes(mappedNodes);
    setEdges(mappedEdges);
  }, [setEdges, setNodes, store]);

  return (<div className={isFullScreen ? 'fullscreen' : ''} style={styleProps}>
    <ReactFlow
      connectOnClick={false}
      nodesConnectable={false}
      nodes={nodes}
      edges={edges}
      nodeTypes={nodeTypes}
      edgeTypes={edgeTypes}
      onNodesChange={onNodesChange}
      onEdgesChange={onEdgesChange}
      connectionMode={ConnectionMode.Loose}
      onNodeMouseEnter={onNodeMouseEnter}
      onNodeMouseLeave={onEdgeOrNodeMouseLeave}
      onEdgeMouseEnter={onEdgeMouseEnter}
      onEdgeMouseLeave={onEdgeOrNodeMouseLeave}
      fitView
      fitViewOptions={fitViewOptions}
    >
      <Controls
        showInteractive={false}
      >
        <ControlButton onClick={downloadImage} title={"download image"}>
          <DownloadIcon />
        </ControlButton>
        <ControlButton title={!isFullScreen ? 'maximise view' : 'minimise view'} onClick={() => {
          setFullScreen(!isFullScreen)
          setAwaitingRefit(true);
        }}>
          {ToggleFullScreenButton}
        </ControlButton>
      </Controls>
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
    background-color: white;
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
export type SchemaMemberClickHandler = (SchemaMemberClickProps) => void;
export type AppendLinksHandler = (AppendLinksProps) => void;

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
    clickHandler: Subject<SchemaMemberClickProps>,
    isNavigable: boolean
  ) {
    ReactDOM.render(
      React.createElement(SchemaFlowDiagramWithProvider, {
        schemaAndMembersToDisplay$,
        width,
        height,
        linkKinds,
        clickHandler,
        isNavigable
      } as SchemaFlowDiagramProps),
      elementRef.nativeElement
    );
  }
}
