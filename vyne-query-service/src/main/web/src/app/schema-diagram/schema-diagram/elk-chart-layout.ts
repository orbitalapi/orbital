import { Edge, Node } from 'react-flow-renderer';
import ELK, { ElkExtendedEdge, ElkNode } from 'elkjs/lib/elk.bundled';
// import ELK, { ElkExtendedEdge, ElkNode } from 'elkjs/lib/elk-api';
import { RelativeNodeXyPosition } from './schema-chart.controller';
// import { Worker } from 'elkjs/lib/elk-worker';

export function applyElkLayout(nodes: Node[], edges: Edge[]): Promise<Node[]> {
  const nodeMap: { [index: string]: Node } = {};
  const elkNodes = nodes.map(node => {
    // Naughty side effect, we're also storing an indexed map.
    nodeMap[node.id] = node;
    const elkNode = {
      id: node.id,
      height: node.height,
      width: node.width
    } as ElkNode;
    return elkNode;
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
      'spacing.componentComponent': '100',
      'spacing.nodeNodeBetweenLayers': '100'
    },
    children: elkNodes,
    edges: elkEdges
  }
  const elk = new ELK()
  // const elk = new ELK({
  //   workerFactory: function (url) { // the value of 'url' is irrelevant here
  //     // const { Worker } = require('elkjs/lib/elk-worker.js') // non-minified
  //     return new Worker(url)
  //   }
  // })
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
