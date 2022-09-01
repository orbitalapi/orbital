import { Edge, Node } from 'react-flow-renderer';
import ELK, { ElkExtendedEdge, ElkNode } from 'elkjs/lib/elk.bundled';

export function applyElkLayout(nodes: Node[], edges: Edge[]): Promise<Node[]> {
  // from here: https://github.com/kieler/elkjs#example
  const nodeMap: { [index: string]: Node } = {};
  const elkNodes = nodes.map(node => {
    // Naughty side effect, we're also storing an indexed map.
    nodeMap[node.id] = node;
    return {
      id: node.id,
      x: node.position.x,
      y: node.position.y,
      height: node.height,
      width: node.width
    } as ElkNode
  });
  const elkEdges = edges.map(edge => {
    return {
      id: edge.id,
      sources: [edge.source],
      targets: [edge.target],
    } as ElkExtendedEdge
  })
  const graph: ElkNode = {
    id: 'root',
    layoutOptions: {
      'elk.algorithm': 'layered',
      // https://rtsys.informatik.uni-kiel.de/elklive/examples.html?e=general%2Fspacing%2Fcomponents
      'spacing.componentComponent' : '100',
      'spacing.nodeNodeBetweenLayers': '100'
    },
    children: elkNodes,
    edges: elkEdges
  }
  const elk = new ELK()
  return elk.layout(graph)
    .then((graph: ElkNode) => {
      graph.children.forEach(elkNode => {
        const node = nodeMap[elkNode.id];
        node.position = {
          x: elkNode.x,
          y: elkNode.y
        }
      })
      return Object.values(nodeMap);
    })
}
