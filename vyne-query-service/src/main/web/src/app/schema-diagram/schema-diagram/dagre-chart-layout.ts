import { Edge, Node } from 'react-flow-renderer';
import dagre from 'dagre';

export function applyDagreLayout(nodes: Node[], edges: Edge[], layoutDirection: 'TB' | 'LR' = 'TB'): Node[] {
  const dagreGraph = new dagre.graphlib.Graph();
  dagreGraph.setDefaultEdgeLabel(() => ({}));

  dagreGraph.setGraph({
    rankdir: layoutDirection,
    ranker: 'tight-tree',
    align: 'DR'
  });

  nodes.forEach((node) => {
    dagreGraph.setNode(node.id, { width: node.width, height: node.height });
  });

  edges.forEach((edge) => {
    dagreGraph.setEdge(edge.source, edge.target);
  });

  dagre.layout(dagreGraph);

  nodes.forEach(node => {
    const nodeWithPosition = dagreGraph.node(node.id);
    node.position = {
      x: nodeWithPosition.x,
      y: nodeWithPosition.y
    }
  })

  return nodes;
}
