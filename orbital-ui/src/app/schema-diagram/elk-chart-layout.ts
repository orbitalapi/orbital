import { Edge, Node } from "@xyflow/react";
// TODO: what was the reason for importing via 'elkjs/lib/elk.bundled' instead of declaring it in package.json and using like below?
import ELK, { ElkExtendedEdge, ElkNode, ElkPort } from "elkjs";
import { LayoutOptions } from "elkjs/lib/elk-api";
import { QueryPlanNodeData } from "src/app/query-plan-diagram/query-plan-node";
import { Link } from "src/app/schema-diagram/schema-chart-builder";


function layoutOptions(layoutDirection: "DOWN" | "RIGHT"): LayoutOptions {
  //  https://eclipse.dev/elk/reference/algorithms/org-eclipse-elk-layered.html
  // https://rtsys.informatik.uni-kiel.de/elklive/examples.html?e=general%2Fspacing%2Fcomponents
  return {
    "elk.algorithm": "layered",
    "elk.direction": layoutDirection,
    "elk.edgeRouting": "ORTHOGONAL",
    // 'elk.layered.nodePlacement.strategy': 'SIMPLE',
    "elk.layered.layering.strategy": "LONGEST_PATH",
    "elk.layered.nodePlacement.strategy": "BRANDES_KOEPF",
    "elk.layered.spacing.edgeNodeBetweenLayers": "40",
    "elk.layered.spacing.edgeEdgeBetweenLayers": "15",
    "elk.layered.spacing.nodeNodeBetweenLayers": "100",
    "elk.layered.considerModelOrder.strategy": "NODES_AND_EDGES", // Respect model order
    "elk.layered.crossingMinimization.semiInteractive": "true", // Better layer assignment
    "elk.spacing.nodeNode": "80",
    "elk.spacing.componentComponent": "80",

  };
}

/**
 * Applies the ELK layout algorithm where none of the nodes have "floating" ports.
 * Creates a better layout, but links can only be from LHS or RHS, not both.
 * Based on https://reactflow.dev/examples/layout/elkjs-multiple-handles
 */
export function applyFixedPortElkLayout(nodes: Node[], edges: Edge[], layoutDirection: "DOWN" | "RIGHT"): Promise<Node[]> {
  const nodeMap: { [index: string]: Node } = {};

  function toElkPort(link: Link, direction: "INBOUND" | "OUTBOUND"): any {
    let side: "NORTH" | "SOUTH" | "EAST" | "WEST";
    if (layoutDirection == "DOWN") {
      side = (direction == "INBOUND") ? "NORTH" : "SOUTH";
    } else {
      side = (direction == "INBOUND") ? "EAST" : "WEST";
    }
    return {
      id: link.linkId ?? `${link.sourceNodeId}.${link.sourceHandleId}->${link.targetNodeId}.${link.targetHandleId}`,
      layoutOptions: {
        "org.eclipse.elk.port.side": side
      },
    };
  }

  const elkNodes = nodes.map(node => {
    nodeMap[node.id] = node;
    const data = node.data as QueryPlanNodeData;
    const targetPorts = data.inboundLinks.map(link => toElkPort(link, "INBOUND"));
    const sourcePorts = data.outboundLinks.map(link => toElkPort(link, "OUTBOUND"));
    // const targetPorts = node.data.
    return {
      id: node.id,
      height: node.measured?.height ?? 100,
      width: node.measured?.width ?? 100,
      layoutOptions: {
        "org.eclipse.elk.portConstraints": "FIXED_ORDER"
      },
      properties: {
        "org.eclipse.elk.portConstraints": "FIXED_ORDER"
      },
      ports: [ ...targetPorts, ...sourcePorts]
    } as ElkNode;
  });
  const elkEdges = edges.map(edge => {
    return {
      id: edge.id,
      sources: [edge.source],
      targets: [edge.target]
    } as ElkExtendedEdge;
  });
  return doLayout(layoutDirection, elkNodes, elkEdges, nodeMap);
}


export function applyElkLayout(nodes: Node[], edges: Edge[], layoutDirection: "DOWN" | "RIGHT"): Promise<Node[]> {
  const nodeMap: { [index: string]: Node } = {};
  const elkNodes = nodes.map(node => {
    nodeMap[node.id] = node;
    const elkNode = {
      id: node.id,
      height: node.measured?.height || 100,
      width: node.measured?.width || 100
    } as ElkNode;
    return elkNode;
  });
  const elkEdges = edges.map(edge => {
    return {
      id: edge.id,
      sources: [edge.source],
      targets: [edge.target]
    } as ElkExtendedEdge;
  });
  return doLayout(layoutDirection, elkNodes, elkEdges, nodeMap);
}


function doLayout(layoutDirection: "DOWN" | "RIGHT", elkNodes: ElkNode[], elkEdges: ElkExtendedEdge[], nodeMap: {
  [p: string]: Node
}) {
  const graph: ElkNode = {
    id: "root",
    layoutOptions: layoutOptions(layoutDirection),
    children: elkNodes,
    edges: elkEdges
  };
  const elk = new ELK();
  return elk.layout(graph)
    .then((graph: ElkNode) => {
      graph.children.forEach(elkNode => {
        const node = nodeMap[elkNode.id];
        node.position = {
          x: elkNode.x,
          y: elkNode.y
        };
      });
      return Object.values(nodeMap);
    });
}
