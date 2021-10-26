import {Component, Input, OnInit} from '@angular/core';
import {BaseGraphComponent} from '../inheritence-graph/base-graph-component';
import {QuerySankeyChartRow, SankeyNodeType} from '../services/query.service';
import {SchemaGraph, SchemaGraphLink, SchemaGraphNode, SchemaGraphNodeType, SchemaNodeSet} from '../services/schema';
import {ClusterNode} from '@swimlane/ngx-graph';
import {isNullOrUndefined} from 'util';
import {Subject} from 'rxjs/index';

@Component({
  selector: 'app-query-lineage',
  templateUrl: './query-lineage.component.html',
  styleUrls: ['./query-lineage.component.scss']
})
export class QueryLineageComponent extends BaseGraphComponent {

  fullscreen = false;

  redrawChart$ = new Subject();

  private _rows: QuerySankeyChartRow[];
  schemaNodeSet: SchemaNodeSet;

  clusters: ClusterNode[] = [];

  constructor() {
    super();
  }

  filteredNode: SchemaGraphNode | null;

  @Input()
  get rows(): QuerySankeyChartRow[] {
    return this._rows;
  }

  set rows(value: QuerySankeyChartRow[]) {
    if (this.rows === value) {
      return;
    }
    this._rows = value;
    if (this.rows) {
      this.refreshChartData();
    }
  }

  private refreshChartData() {
    let rowData: QuerySankeyChartRow[];
    if (isNullOrUndefined(this.filteredNode)) {
      rowData = this.rows;
    } else {
      rowData = this.filterRowSet(this.rows, new Set([this.filteredNode.id]));
    }
    const [schemaNodeSet, clusterNodes] = this.generateChartData(rowData);
    this.schemaNodeSet = schemaNodeSet;
    this.clusters = clusterNodes;
  }

  private filterRowSet(rows: QuerySankeyChartRow[], filterToIds: Set<string>): QuerySankeyChartRow[] {
    const discoveredIds = new Set<string>(filterToIds);
    const filteredRows = rows.filter(r => {
      const sourceId = this.toSafeId(r.sourceNode);
      const targetId = this.toSafeId(r.targetNode);
      if (filterToIds.has(targetId)) {
        // We only want to filter things that link here, not where this thing lings
        discoveredIds.add(sourceId);
        // discoveredIds.add(targetId);
        return true;
      } else {
        return false;
      }
    });
    if (discoveredIds.size === filterToIds.size) {
      return filteredRows;
    } else {
      // We discovered new ids, so expand the search to include them
      return this.filterRowSet(rows, discoveredIds);
    }
  }


  private toSafeId(input: string): string {
    const result = input
      .split(' ').join('')
      .split('.').join('')
      .split('"').join('')
      .split('(').join('')
      .split(')').join('')
      .split(',').join('')
      .split('@@').join('');
    if (result === '') {
      return 'EMPTY_STRING';
    } else {
      return result;
    }
  }

  private generateChartData(rows: QuerySankeyChartRow[]): [SchemaNodeSet, ClusterNode[]] {
    const nodes: Map<string, SchemaGraphNode> = new Map<string, SchemaGraphNode>();
    const links: Map<number, SchemaGraphLink> = new Map<number, SchemaGraphLink>();
    const attributeNodeIds = new Map<string, string>();


    function rowTypeToGraphType(rowType: SankeyNodeType): [SchemaGraphNodeType, string] {
      switch (rowType) {
        case 'AttributeName':
          return ['MEMBER', 'PROPERTY'];
        case 'Expression':
          return ['TYPE', 'FORMULA'];
        case 'ExpressionInput':
          return ['TYPE', 'FORMULA INPUT'];
        case 'ProvidedInput':
          return ['DATASOURCE', 'PROVIDED VALUE'];
        case 'QualifiedName':
          return ['OPERATION', 'SYSTEM'];
      }
    }

    function getNodeLabel(nodeText: string, nodeType: SankeyNodeType): [string, string | null] {
      if (nodeType === 'QualifiedName' && nodeText.includes('@@')) {
        const [serviceName, operationName] = nodeText.split('@@');
        const serviceDisplayName = serviceName.split('.').pop();
        return [serviceDisplayName, `Service: ${serviceName} | Operation: ${operationName}` ];
      } else if (nodeType === 'Expression') {
        return ['Custom formula', nodeText];
      } else {
        return [nodeText, null];
      }
    }

    rows.forEach((row, index) => {
      const sourceId = this.toSafeId(row.sourceNode);
      if (!nodes.has(sourceId)) {
        const [header, tooltip] = getNodeLabel(row.sourceNode, row.sourceNodeType);
        const [nodeType, nodeTypeLabel] = rowTypeToGraphType(row.sourceNodeType);
        nodes.set(sourceId, {
          id: sourceId,
          label: header,

          type: nodeType,
          subHeader: nodeTypeLabel,
          nodeId: sourceId,
          tooltip: tooltip
        });
      }
      if (row.sourceNodeType === 'AttributeName') {
        attributeNodeIds.set(sourceId, sourceId);
      }

      const targetId = this.toSafeId(row.targetNode);
      if (!nodes.has(targetId)) {
        const [header, tooltip] = getNodeLabel(row.targetNode, row.targetNodeType);
        const [nodeType, nodeTypeLabel] = rowTypeToGraphType(row.targetNodeType);
        nodes.set(targetId, {
          id: targetId,
          label: header,
          type: nodeType,
          subHeader: nodeTypeLabel,
          nodeId: targetId,
          tooltip: tooltip
        });
      }
      if (row.targetNodeType === 'AttributeName') {
        attributeNodeIds.set(targetId, targetId);
      }
      links.set(index, {
        source: sourceId,
        target: targetId,
        label: row.count.toString()
      });

    });
    const clusters = [
      {
        id: 'attributes',
        label: '',
        childNodeIds: Array.from(attributeNodeIds.keys())
      }
    ];
    return [new SchemaGraph(nodes, links).toNodeSet(), clusters];
  }

  onNodeClicked(clickedNode: SchemaGraphNode) {
    this.filteredNode = clickedNode;
    this.refreshChartData();
  }

  clearFilter() {
    this.filteredNode = null;
    this.refreshChartData();
  }

  toggleFullscreen() {
    this.fullscreen = !this.fullscreen;
    setTimeout(() => {
      this.redrawChart$.next('');
    });
  }
}
