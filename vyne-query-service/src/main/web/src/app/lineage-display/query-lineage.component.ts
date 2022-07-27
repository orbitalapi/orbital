import { Component, Input, OnInit } from '@angular/core';
import { BaseGraphComponent } from '../inheritence-graph/base-graph-component';
import { QuerySankeyChartRow, SankeyNodeType, SankeyOperationNodeDetails } from '../services/query.service';
import { SchemaGraph, SchemaGraphLink, SchemaGraphNode, SchemaGraphNodeType, SchemaNodeSet } from '../services/schema';
import { ClusterNode } from '@swimlane/ngx-graph';
import { isNullOrUndefined } from 'util';
import { Subject } from 'rxjs';
import Tooltip from 'rich-markdown-editor/dist/components/Tooltip';

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
      .split('_').join('')
      .split(',').join('')
      .split('@@').join('');
    if (result === '') {
      return 'EMPTY_STRING';
    } else {
      // The graphing library throws exceptions if the
      // id of a node is the same as an attribute on the collection.
      // (eg., you can't have a node with the id of 'length')
      // So, append a safe character to the end.
      // Need to be careful here, as lots of characters aren't safe.
      return result + '$';
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

    function getNodeLabel(nodeText: string, nodeType: SankeyNodeType, operationData: SankeyOperationNodeDetails | null): [string, (string | null)] {
      if (operationData != null) {
        switch (operationData.operationType) {
          case 'Database':
            const dbHeader = 'Db Query: ' + operationData.connectionName;
            const dbSubHeader = 'Tables: ' + operationData.tableNames.join(', ');
            return [dbHeader, dbSubHeader]
          case 'KafkaTopic':
            const kafkaHeader = 'Kafka connection: ' + operationData.connectionName
            const kafkaSubHeader = 'Topic: ' + operationData.topic;
            return [kafkaHeader, kafkaSubHeader]
          case 'Http':
            const httpHeader = 'HTTP ' + operationData.verb
            const httpSubheader = operationData.operationName.shortDisplayName;
            return [httpHeader, httpSubheader]
        }
      }

      if (nodeType === 'QualifiedName' && nodeText.includes('@@')) {
        const [serviceName, operationName] = nodeText.split('@@');
        const serviceDisplayName = serviceName.split('.').pop();
        return [serviceDisplayName, `${serviceName} | Operation: ${operationName}`];
      } else if (nodeType === 'Expression') {
        return ['Custom formula', nodeText];
      } else {
        return [nodeText, null];
      }
    }

    rows.forEach((row, index) => {
      const sourceId = this.toSafeId(row.sourceNode);
      if (!nodes.has(sourceId)) {
        const [header, subheader] = getNodeLabel(row.sourceNode, row.sourceNodeType, row.sourceNodeOperationData);
        const [nodeType, nodeTypeLabel] = rowTypeToGraphType(row.sourceNodeType);
        nodes.set(sourceId, {
          id: sourceId,
          label: header,
          data: {
            nodeOperationData: row.sourceNodeOperationData,
            ...row
          },
          type: nodeType,
          subHeader: subheader,
          nodeId: sourceId,
          tooltip: nodeTypeLabel
        });
      }
      if (row.sourceNodeType === 'AttributeName') {
        attributeNodeIds.set(sourceId, sourceId);
      }

      const targetId = this.toSafeId(row.targetNode);
      if (!nodes.has(targetId)) {
        const [header, subheader] = getNodeLabel(row.targetNode, row.targetNodeType, row.targetNodeOperationData);
        const [nodeType, nodeTypeLabel] = rowTypeToGraphType(row.targetNodeType);
        nodes.set(targetId, {
          id: targetId,
          label: header,
          type: nodeType,
          data: {
            nodeOperationData: row.targetNodeOperationData,
            ...row
          },
          subHeader: subheader,
          nodeId: targetId,
          tooltip: nodeTypeLabel
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
    if (clickedNode.type !== 'MEMBER') {
      // We don't currently do drill-down for things other than members (attributes)
      return;
    }
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
      this.redrawChart$.next('xx');
    });
  }

  nodeIcon(node: SchemaGraphNode) {
    // if (node.subHeader.includes('getStreaming')) {
    //   debugger;
    // }
    if (node.type !== 'OPERATION') {
      return null;
    }
    return this.nodeDetails(node.data)?.operationType;
  }

  private nodeDetails(data: QueryLineageOperationalNode | null): SankeyOperationNodeDetails | null {
    if (!data) return null;
    return data.nodeOperationData;
  }
}

interface QueryLineageOperationalNode extends QuerySankeyChartRow {
  nodeOperationData?: SankeyOperationNodeDetails
}
