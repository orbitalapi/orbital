import * as React from 'react';
import styled from 'styled-components';
import { useCallback, useEffect, useState } from 'react';
import {
  ReactFlow,
  ConnectionMode,
  ControlButton,
  Controls,
  Edge,
  EdgeTypes,
  FitViewOptions,
  MarkerType,
  Node,
  ReactFlowProvider,
  useEdgesState,
  useNodesState,
  useReactFlow,
  useStoreApi,
  useNodesInitialized,
  Background,
  BackgroundVariant,
  Position,
} from '@xyflow/react';
import { ElementRef } from '@angular/core';
import * as ReactDOM from 'react-dom';
import { Observable } from 'rxjs';
import {
  QueryPlan,
  QueryPlanDiagramData,
  DiagramLink as DiagramLinkData,
  DiagramNode as DiagramNodeData,
  DiagramLink
} from '../services/query.service';
import { Message, CompilationMessage } from '../services/schema';
import QueryPlanNode, { QueryPlanNodeData } from './query-plan-node';
import QueryPlanOperationNode from './query-plan-operation-node';
import QueryPlanServiceNode from './query-plan-service-node';
import QueryPlanTypeNode from './query-plan-type-node';
import FloatingEdge from '../schema-diagram/diagram-nodes/floating-edge';
import { applyElkLayout, applyFixedPortElkLayout } from "../schema-diagram/elk-chart-layout";
import DownloadIcon from '../schema-diagram/icons/download-icon';
import FullScreenIcon from '../schema-diagram/icons/fullscreen-icon';
import MinimizeIcon from '../schema-diagram/icons/minimize-icon';
import FlyoutMenu from '../schema-diagram/icons/flyout-menu';
import LayoutIcon from '../schema-diagram/icons/layout-icon';
import { colors } from '../schema-diagram/tailwind.colors';
import { EdgeParams, Link } from '../schema-diagram/schema-chart-builder';
import { HandleIds } from '../schema-diagram/schema-chart-builder';
import {
  useEscapeKey,
  fitViewOptions,
  downloadDiagramImage,
  useFlowHoverHandlers
} from '../schema-diagram/shared-flow-hooks';
import { findPathsToMember } from './query-plan-filter';

const nodeTypes: any = {
  QueryPlanNode: QueryPlanNode,
  QueryPlanOperationNode: QueryPlanOperationNode,
  QueryPlanServiceNode: QueryPlanServiceNode,
  QueryPlanTypeNode: QueryPlanTypeNode,
};

const edgeTypes = {
  floating: FloatingEdge,
};

interface QueryPlanFlowDiagramProps {
  queryPlan$: Observable<QueryPlan | null | undefined>;
  compilationMessages$: Observable<CompilationMessage[] | null | undefined>;
  width: number;
  height: number;
}

let previousDimensions: { width?: number; height?: number };

const FilterPill = styled.div`
  position: absolute;
  top: 1rem;
  left: 50%;
  transform: translateX(-50%);
  z-index: 10;
  background-color: ${colors.blue['50']};
  border: 1px solid ${colors.blue['300']};
  border-radius: 1.5rem;
  padding: 0.5rem 1rem;
  display: flex;
  align-items: center;
  gap: 0.75rem;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
  font-size: 0.875rem;
  color: ${colors.slate['700']};
`;

const FilterText = styled.span`
  font-weight: 500;
`;

const FilterNodeName = styled.span`
  color: ${colors.blue['700']};
  font-weight: 600;
`;

const FilterMemberName = styled.span`
  color: ${colors.blue['600']};
  font-family: monospace;
  font-size: 0.8125rem;
`;

const ClearButton = styled.button`
  background: ${colors.blue['600']};
  color: white;
  border: none;
  border-radius: 0.75rem;
  padding: 0.25rem 0.75rem;
  font-size: 0.8125rem;
  font-weight: 500;
  cursor: pointer;
  transition: background-color 0.15s;

  &:hover {
    background: ${colors.blue['700']};
  }

  &:active {
    background: ${colors.blue['800']};
  }
`;

const ErrorContainer = styled.div`
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  padding: 2rem;
  background-color: ${colors.slate['50']};

  &.fullscreen {
    position: fixed;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    z-index: 9999;
    background-color: white;
  }
`;

const ErrorBox = styled.div`
  max-width: 800px;
  background-color: white;
  border: 2px solid ${colors.red['400']};
  border-radius: 0.5rem;
  padding: 1.5rem;
  box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
`;

const ErrorTitle = styled.h2`
  color: ${colors.red['600']};
  font-size: 1.5rem;
  font-weight: 600;
  margin: 0 0 1rem 0;
  display: flex;
  align-items: center;
  gap: 0.5rem;
`;

const ErrorList = styled.ul`
  list-style: none;
  padding: 0;
  margin: 0;
`;

const ErrorItem = styled.li`
  padding: 0.75rem;
  margin-bottom: 0.5rem;
  background-color: ${colors.red['50']};
  border-left: 4px solid ${colors.red['500']};
  border-radius: 0.25rem;

  &:last-child {
    margin-bottom: 0;
  }
`;

const ErrorMessage = styled.p`
  margin: 0;
  color: ${colors.slate['700']};
  font-size: 0.875rem;
  line-height: 1.5;
`;

interface ErrorDisplayProps {
  errors: Array<{ message: string; type: 'compilation' | 'execution' }>;
  isFullScreen: boolean;
  styleProps: any;
}

function ErrorDisplay({ errors, isFullScreen, styleProps }: ErrorDisplayProps): React.JSX.Element {
  const hasCompilationErrors = errors.some(e => e.type === 'compilation');
  const hasExecutionErrors = errors.some(e => e.type === 'execution');

  let title = 'Query Errors';
  if (hasCompilationErrors && !hasExecutionErrors) {
    title = 'Compilation Errors';
  } else if (!hasCompilationErrors && hasExecutionErrors) {
    title = 'Query Execution Errors';
  }

  return (
    <ErrorContainer className={isFullScreen ? 'fullscreen' : ''} style={isFullScreen ? {} : styleProps}>
      <ErrorBox>
        <ErrorTitle>
          <span>⚠️</span>
          {title}
        </ErrorTitle>
        <ErrorList>
          {errors.map((error, index) => (
            <ErrorItem key={index}>
              <ErrorMessage>{error.message}</ErrorMessage>
            </ErrorItem>
          ))}
        </ErrorList>
      </ErrorBox>
    </ErrorContainer>
  );
}

function QueryPlanFlowDiagram(props: QueryPlanFlowDiagramProps) {
  const store = useStoreApi();
  const [nodes, setNodes, onNodesChange] = useNodesState([]);
  const [edges, setEdges, onEdgesChange] = useEdgesState([]);
  const [isFullScreen, setIsFullScreen] = useState(false);
  const [layoutDirection, setLayoutDirection] = useState<'DOWN' | 'RIGHT' | 'NONE'>('RIGHT');

  const instance = useReactFlow();
  const nodesInitialized = useNodesInitialized();
  const [awaitingRefit, setAwaitingRefit] = useState<'immediate' | 'delayed'>();

  const [queryPlan, setQueryPlan] = useState<QueryPlan | null>(null);
  const [queryPlanData, setQueryPlanData] = useState<QueryPlanDiagramData | null>(null);
  const [compilationMessages, setCompilationMessages] = useState<CompilationMessage[]>([]);
  const [allErrors, setAllErrors] = useState<Array<{ message: string; type: 'compilation' | 'execution' }> | null>(null);

  // State for filtering
  const [filteredNodeIds, setFilteredNodeIds] = useState<Set<string> | null>(null);
  const [filteredEdgeIds, setFilteredEdgeIds] = useState<Set<string> | null>(null);
  const [selectedMember, setSelectedMember] = useState<{ nodeId: string; handleId: string } | null>(null);

  // Subscribe to compilationMessages
  useEffect(() => {
    const subscription = props.compilationMessages$.subscribe((messages) => {
      console.log('CompilationMessages received:', messages);
      setCompilationMessages(messages || []);
    });
    return () => {
      subscription.unsubscribe();
    };
  }, []);

  // Subscribe to queryPlan
  useEffect(() => {
    const subscription = props.queryPlan$.subscribe((plan) => {
      console.log('QueryPlan received:', plan);
      setQueryPlan(plan || null);
      setQueryPlanData(plan?.diagramData || null);
    });
    return () => {
      subscription.unsubscribe();
    };
  }, []);

  // Combine and filter errors whenever either source changes
  useEffect(() => {
    const errors: Array<{ message: string; type: 'compilation' | 'execution' }> = [];

    // Add compilation errors
    if (compilationMessages && compilationMessages.length > 0) {
      const compilationErrors = compilationMessages.filter((msg) => msg.severity === 'ERROR');
      compilationErrors.forEach((msg) => {
        errors.push({
          message: `${msg.sourceName}:${msg.line}:${msg.char} - ${msg.detailMessage}`,
          type: 'compilation'
        });
      });
    }

    // Add query execution errors
    if (queryPlan?.queryExecutionMessages && queryPlan.queryExecutionMessages.length > 0) {
      const executionErrors = queryPlan.queryExecutionMessages.filter(
        (msg) => msg.severity === 'ERROR' || msg.severity === 'FAILURE'
      );
      executionErrors.forEach((msg) => {
        errors.push({
          message: msg.message,
          type: 'execution'
        });
      });
    }

    console.log('Combined errors:', errors);
    setAllErrors(errors.length > 0 ? errors : null);
  }, [compilationMessages, queryPlan]);

  useEffect(() => {
    if (nodesInitialized) {
      performLayout();
    }
  }, [nodesInitialized, layoutDirection]);

  useEffect(() => {
    if (awaitingRefit) {
      setTimeout(async () => {
        await instance.fitView({
          ...fitViewOptions,
          duration: awaitingRefit === 'immediate' ? 0 : fitViewOptions.duration,
        });
      }, 30);
      setAwaitingRefit(null);
    }
  }, [awaitingRefit]);

  // Handle member click to filter diagram
  const handleMemberClick = useCallback((nodeId: string, handleId: string) => {
    if (!queryPlanData) return;

    console.log('Member clicked:', { nodeId, handleId });
    setSelectedMember({ nodeId, handleId });

    // Find all nodes and edges that lead to this member
    const { nodeIds, edgeIds } = findPathsToMember(queryPlanData, nodeId, handleId);

    setFilteredNodeIds(nodeIds);
    setFilteredEdgeIds(edgeIds);
  }, [queryPlanData]);

  // Clear filter
  const handleClearFilter = useCallback(() => {
    setSelectedMember(null);
    setFilteredNodeIds(null);
    setFilteredEdgeIds(null);
  }, []);

  useEffect(() => {
    if (!queryPlanData || !queryPlanData.nodes || queryPlanData.nodes.length === 0) {
      return;
    }

    console.log('Building query plan diagram with data:', queryPlanData);

    const buildResult = buildNodesAndEdges(queryPlanData, handleMemberClick);
    setNodes(buildResult.nodes);
    setEdges(buildResult.edges);
  }, [queryPlanData, handleMemberClick]);

  // Apply filtering when filter state changes
  useEffect(() => {
    if (!filteredNodeIds || !filteredEdgeIds) {
      // No filter active - show all nodes and edges
      setNodes(prevNodes =>
        prevNodes.map(node => ({
          ...node,
          hidden: false,
          style: { ...node.style, opacity: 1 }
        }))
      );
      setEdges(prevEdges =>
        prevEdges.map(edge => ({
          ...edge,
          hidden: false,
          style: { ...edge.style, opacity: 1 }
        }))
      );
    } else {
      // Filter active - hide nodes and edges not in the filtered sets
      setNodes(prevNodes =>
        prevNodes.map(node => ({
          ...node,
          hidden: !filteredNodeIds.has(node.id),
          style: {
            ...node.style,
            opacity: filteredNodeIds.has(node.id) ? 1 : 0.2
          }
        }))
      );
      setEdges(prevEdges =>
        prevEdges.map(edge => ({
          ...edge,
          hidden: !filteredEdgeIds.has(edge.id),
          style: {
            ...edge.style,
            opacity: filteredEdgeIds.has(edge.id) ? 1 : 0.2
          }
        }))
      );
    }
  }, [filteredNodeIds, filteredEdgeIds]);

  const buildNodesAndEdges = (data: QueryPlanDiagramData, onMemberClick: ((nodeId: string, handleId: string) => void) | null) => {
    // Helper function to convert DiagramLink to Link
    const convertLink = (link: DiagramLink): Link => ({
      sourceNodeId: link.sourceId,
      sourceHandleId: link.sourceHandleId,
      sourceNodeName: null as any, // Not needed for query plan
      sourceMemberType: 'TYPE' as any,
      targetNodeId: link.targetId,
      targetHandleId: link.targetHandleId,
      targetNodeName: null as any, // Not needed for query plan
      targetMemberType: 'TYPE' as any,
      linkKind: 'entity',
    });

    // Helper function to determine node type based on kind
    const getNodeType = (kind: string): string => {
      switch (kind) {
        case 'OPERATION':
          return 'QueryPlanOperationNode';
        case 'SERVICE':
          return 'QueryPlanServiceNode';
        case 'SCALAR_TYPE':
        case 'MODEL':
        case 'REQUEST_MODEL':
        case 'RESPONSE_MODEL':
        case 'REQUEST_RESPONSE_MODEL':
          return 'QueryPlanTypeNode';
        default:
          return 'QueryPlanNode'; // Fallback to generic node
      }
    };

    // Build React Flow nodes - use server-organized links directly
    const reactFlowNodes: Node[] = data.nodes.map((diagramNode) => {
      return {
        id: diagramNode.id,
        type: getNodeType(diagramNode.kind),
        position: { x: 0, y: 0 }, // Will be set by layout
        data: {
          node: diagramNode,
          inboundLinks: diagramNode.inboundHeaderLinks.map(convertLink),
          outboundLinks: diagramNode.outboundHeaderLinks.map(convertLink),
          memberLinks: Object.fromEntries(
            Object.entries(diagramNode.memberLinks).map(([handleId, links]) => [
              handleId,
              links.map(convertLink),
            ])
          ),
          onMemberClick,
        },
        draggable: true,
        selectable: true,
      };
    });

    // Build React Flow edges
    const reactFlowEdges: Edge[] = data.links.map((link) => {
      return {
        id: `${link.sourceId}-${link.sourceHandleId}-${link.targetId}-${link.targetHandleId}`,
        source: link.sourceId,
        target: link.targetId,
        sourceHandle: HandleIds.appendPositionToHandleId(link.sourceHandleId, Position.Right),
        targetHandle: HandleIds.appendPositionToHandleId(link.targetHandleId, Position.Left),
        type: 'smoothstep',
        markerEnd: {
          type: MarkerType.ArrowClosed,
          width: 10,
          height: 10,
          color: colors.slate['600'],
        },
        data: {
          sourceCanFloat: false,
          targetCanFloat: false,
          label: undefined,
          linkKind: 'entity',
        } as EdgeParams,
        style: {
          stroke: colors.slate['600'],
          strokeWidth: 2,
        },
      };
    });

    return { nodes: reactFlowNodes, edges: reactFlowEdges };
  };

  const performLayout = async () => {
    if (layoutDirection === 'NONE') {
      console.log('NOT performing layout');
      setAwaitingRefit('immediate');
      return;
    }

    console.log('Performing layout');
    const result = await applyFixedPortElkLayout(instance.getNodes(), instance.getEdges(), layoutDirection);

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
    setIsFullScreen(false);
  });

  const ToggleFullScreenButton = isFullScreen ? <MinimizeIcon /> : <FullScreenIcon />;
  const styleProps = isFullScreen
    ? {}
    : {
        width: props.width,
        height: props.height,
      };
  if (previousDimensions?.width !== props.width || previousDimensions?.height !== props.height) {
    if (previousDimensions && !isFullScreen) {
      setAwaitingRefit('immediate');
    }
  }
  previousDimensions = styleProps;

  // If there are any errors (compilation or execution), display them instead of the diagram
  if (allErrors && allErrors.length > 0) {
    return (
      <ErrorDisplay
        errors={allErrors}
        isFullScreen={isFullScreen}
        styleProps={styleProps}
      />
    );
  }

  // Get node and member names for the filter display
  const getFilterDisplayInfo = () => {
    if (!selectedMember || !queryPlanData) return null;

    const node = queryPlanData.nodes.find(n => n.id === selectedMember.nodeId);
    if (!node) return null;

    const member = node.members.find(m => m.handleId === selectedMember.handleId);
    if (!member) return null;

    return {
      nodeTitle: node.title,
      memberName: member.name
    };
  };

  const filterInfo = getFilterDisplayInfo();

  return (
    <div className={isFullScreen ? 'fullscreen' : ''} style={styleProps}>
      {filterInfo && (
        <FilterPill>
          <FilterText>Filtered to:</FilterText>
          <FilterNodeName>{filterInfo.nodeTitle}</FilterNodeName>
          <FilterMemberName>.{filterInfo.memberName}</FilterMemberName>
          <ClearButton onClick={handleClearFilter}>Clear</ClearButton>
        </FilterPill>
      )}
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
        defaultEdgeOptions={{
          type: 'smoothstep',
          animated: false,
          style: { strokeWidth: 2 }
        }}
      >
        <Controls showInteractive={false}>
          <ControlButton>
            <FlyoutMenu onDirectionChange={setLayoutDirection} layoutDirection={layoutDirection} />
          </ControlButton>
          <ControlButton onClick={() => {
            performLayout();
            setAwaitingRefit('immediate');
          }} title={'Reset layout'}>
            <LayoutIcon />
          </ControlButton>
          <ControlButton onClick={() => downloadDiagramImage('query-plan-diagram.png')} title={'download image'}>
            <DownloadIcon />
          </ControlButton>
          <ControlButton
            title={!isFullScreen ? 'maximise view' : 'minimise view'}
            onClick={() => {
              setIsFullScreen(!isFullScreen);
              setAwaitingRefit('immediate');
            }}
          >
            {ToggleFullScreenButton}
          </ControlButton>
        </Controls>
        <Background color="#ccc" variant={BackgroundVariant.Dots} />
      </ReactFlow>
    </div>
  );
}

export const QueryPlanDiagramContainer = styled.div`
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

function QueryPlanFlowDiagramWithProvider(props) {
  return (
    <QueryPlanDiagramContainer>
      <ReactFlowProvider>
        <QueryPlanFlowDiagram {...props}></QueryPlanFlowDiagram>
      </ReactFlowProvider>
    </QueryPlanDiagramContainer>
  );
}

export class QueryPlanFlowWrapper {
  static destroy(elementRef: ElementRef) {
    ReactDOM.unmountComponentAtNode(elementRef.nativeElement);
  }

  static initialize(
    elementRef: ElementRef,
    queryPlan$: Observable<QueryPlan | null | undefined>,
    compilationMessages$: Observable<CompilationMessage[] | null | undefined>,
    width: number = 1800,
    height: number = 1200
  ) {
    ReactDOM.render(
      React.createElement(QueryPlanFlowDiagramWithProvider, {
        queryPlan$,
        compilationMessages$,
        width,
        height,
      } as QueryPlanFlowDiagramProps),
      elementRef.nativeElement
    );
  }
}
