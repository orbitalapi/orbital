import { Edge, Node } from '@xyflow/react';
// TODO: what was the reason for importing via 'elkjs/lib/elk.bundled' instead of declaring it in package.json and using like below?
import ELK, { ElkExtendedEdge, ElkNode } from 'elkjs';
// import ELK, { ElkExtendedEdge, ElkNode } from 'elkjs/lib/elk-api';
import { RelativeNodeXyPosition } from './schema-chart.controller';
// import { Worker } from 'elkjs/lib/elk-worker';

export function applyElkLayout(nodes: Node[], edges: Edge[]): Promise<Node[]> {
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
      targets: [edge.target],
    } as ElkExtendedEdge
  })
  const graph: ElkNode = {
    id: 'root',
    layoutOptions: {
      // https://eclipse.dev/elk/reference/algorithms/org-eclipse-elk-layered.html
      'elk.algorithm': 'layered',
      //'elk.direction': 'DOWN',
      'cycleBreaking.strategy': 'INTERACTIVE',
      'layering.strategy': 'INTERACTIVE',
      'crossingMinimization.semiInteractive': 'true',
      //'separateConnectedComponents': 'false',
      // 'elk.direction': width > height ? 'RIGHT' : 'UP',
      // 'layered.edgeRouting.splines.mode': 'CONSERVATIVE',
      'spacing.nodeNode': '40',
      'spacing.componentComponent': '80',
      'spacing.nodeNodeBetweenLayers': '100',
      // EXAMPLES >>> https://rtsys.informatik.uni-kiel.de/elklive/examples.html?e=general%2Fspacing%2Fcomponents
    },
    children: elkNodes,
    edges: elkEdges,
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
