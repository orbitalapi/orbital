import {outerRectangle, innerRectangle} from '../type-viewer/type-link-graph/graph-utils';
import * as shape from 'd3-shape';
import {SchemaGraphNode, SchemaNodeSet} from '../services/schema';

export class BaseGraphComponent {

  curve = shape.curveBundle.beta(1);
  colors = {
    'TYPE': '#66BD6D',
    'MEMBER': '#FA783B',
    'OPERATION': '#55ACD2',
    'DATASOURCE' : '#9166B8',
    'ERROR' : '#B7332F'
  };

  colorScheme = {
    domain: [
      '#FAC51D',
      '#66BD6D',
      '#FAA026',
      '#29BB9C',
      '#E96B56',
      '#55ACD2',
      '#b7332f',
      '#2C83C9',
      '#9166B8',
      '#92E7E8',
      '#16aa6d',
      '#aebfc9'
    ]
  };
  // orientation = 'TD';
  orientation = 'LR';

  getBorderColor(node: SchemaGraphNode) {
    const nodeType = node.type;
    if (node.type === 'ERROR') {
      return '#b7332f';
    } else {
      return 'none';
    }
  }
  getStroke(node: SchemaGraphNode) {
    const nodeType = node.type;

    if (!this.colors[nodeType]) {
      console.log('No color defined for node type ' + nodeType);
    }
    return this.colors[nodeType] || '#FAC51D';
  }

  getFill(node: SchemaGraphNode) {
    if (node.type === 'ERROR') {
      return '#ffffff';
    } else {
      return '#ffffff';
    }
  }

  outerRectangle(width: number, height: number): string {
    return outerRectangle(width, height);
  }

  innerRectangle(width: number, height: number): string {
    return innerRectangle(width, height);
  }

  protected makeSafeId(id: string): string {
    return id.split('.').join('')
      .split('@').join('')
      .split(' ').join('')
      ;
  }

  protected mergeNodeSet(source: SchemaNodeSet, target: SchemaNodeSet): SchemaNodeSet {
    const merged = this.emptyGraph();
    this.appendNodeSet(source, merged);
    this.appendNodeSet(target, merged);
    return merged;
  }

  protected appendNodeSet(source: SchemaNodeSet, target: SchemaNodeSet) {
    const newNodes = source.nodes.filter(sourceNode => {
      const alreadyExists = target.nodes.some(existingNode => sourceNode.nodeId === existingNode.nodeId);
      return !alreadyExists;
    });
    target.nodes.push.apply(target.nodes, newNodes);

    const newLinks = source.links.filter(sourceLink => {
      const alreadyExists = target.links.some(existingLink => {
        return sourceLink.target === existingLink.target && sourceLink.source === existingLink.source;
      });
      return !alreadyExists;
    });
    target.links.push.apply(target.links, newLinks);
  }

  protected emptyGraph(): SchemaNodeSet {
    return {
      nodes: [],
      links: []
    };
  }
}
