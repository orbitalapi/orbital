import { Component, Input } from '@angular/core';
import { QualifiedName, SchemaGraphLink, SchemaGraphNode, SchemaNodeSet } from '../services/schema';
import { innerRectangle, outerRectangle } from '../type-viewer/type-link-graph/graph-utils';
import * as shape from 'd3-shape';
import { Router } from '@angular/router';
import { Inheritable } from 'src/app/inheritence-graph/build.inheritable';

@Component({
  selector: 'app-inheritance-graph',
  styleUrls: ['./inheritance-graph.component.scss'],
  templateUrl: 'inheritance-graph.html'
})
export class InheritanceGraphComponent {

  private _inheritable: Inheritable;

  schemaGraph: SchemaNodeSet = this.emptyGraph();

  constructor(private router: Router) {
  }

  curve = shape.curveBundle.beta(1);
  colors = {
    'TYPE': '#66BD6D',
    'MEMBER': '#FA783B',
    'OPERATION': '#55ACD2'
  };

  colorScheme = {
    domain: [
      '#FAC51D',
      '#66BD6D',
      '#FAA026',
      '#29BB9C',
      // '#E96B56',
      '#55ACD2',
      // '#B7332F',
      // '#2C83C9',
      // '#9166B8',
      '#92E7E8',
      '#16aa6d',
      '#aebfc9'
    ]
  };
  orientation = 'LR';

  getStroke(node) {
    const nodeType = node.type;

    if (!this.colors[nodeType]) {
      console.log('No color defined for node type ' + nodeType);
    }
    return this.colors[nodeType] || '#FAC51D';
  }


  @Input()
  get inheritable(): Inheritable {
    return this._inheritable;
  }

  set inheritable(value: Inheritable) {
    this._inheritable = value;
    this.buildGraph();
  }

  private emptyGraph(): SchemaNodeSet {
    return {
      nodes: [],
      links: []
    };
  }


  private buildGraph() {
    if (!this.inheritable) {
      this.schemaGraph = this.emptyGraph();
    } else {
      this.schemaGraph = this.getGraph(this.inheritable);
    }
  }


  outerRectangle(width: number, height: number): string {
    return outerRectangle(width, height);
  }

  innerRectangle(width: number, height: number): string {
    return innerRectangle(width, height);
  }

  private getGraph(inheritable: Inheritable, appendSelf: boolean = true): SchemaNodeSet {
    function nameToNode(name: QualifiedName): SchemaGraphNode {
      return {
        id: name.fullyQualifiedName.split('.').join(''),
        nodeId: name.fullyQualifiedName.split('.').join(''),
        label: name.shortDisplayName,
        subHeader: name.namespace,
        fullyQualifiedName: name.fullyQualifiedName,
        type: 'TYPE'
      } as SchemaGraphNode;
    }

    const node = nameToNode(inheritable.name);
    const nodes = appendSelf ? [node] : [];
    const links: SchemaGraphLink[] = [];
    const nodeSet = {
      nodes,
      links
    };

    function appendToNodeSet(inheritableTarget: Inheritable | null, label: string) {
      if (inheritable === null) {
        return;
      }

    }

    if (inheritable.inheritsFrom) {
      const inheritedNode = nameToNode(inheritable.inheritsFrom.name);
      nodes.push(inheritedNode);
      links.push({
        source: node.nodeId,
        target: inheritedNode.nodeId,
        label: 'inherits'
      });
      const inheritedGraph = this.getGraph(inheritable.inheritsFrom, false);
      nodeSet.nodes.push.apply(nodeSet.nodes, inheritedGraph.nodes);
      nodeSet.links.push.apply(nodeSet.links, inheritedGraph.links);
    }

    if (inheritable.aliasFor) {
      const inheritedNode = nameToNode(inheritable.aliasFor.name);
      nodes.push(inheritedNode);
      links.push({
        source: node.nodeId,
        target: inheritedNode.nodeId,
        label: 'alias'
      });
      const inheritedGraph = this.getGraph(inheritable.aliasFor, false);
      nodeSet.nodes.push.apply(nodeSet.nodes, inheritedGraph.nodes);
      nodeSet.links.push.apply(nodeSet.links, inheritedGraph.links);
    }


    return nodeSet;
  }


  select($event: any) {
    const typeName = $event.fullyQualifiedName;
    this.router.navigate(['/catalog', typeName]);
  }
}

