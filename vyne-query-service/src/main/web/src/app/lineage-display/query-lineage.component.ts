import {Component, Input, OnInit} from '@angular/core';
import {BaseGraphComponent} from '../inheritence-graph/base-graph-component';
import {QuerySankeyChartRow, SankeyNodeType} from '../services/query.service';
import {SchemaGraph, SchemaGraphLink, SchemaGraphNode, SchemaGraphNodeType, SchemaNodeSet} from '../services/schema';

@Component({
  selector: 'app-query-lineage',
  templateUrl: './query-lineage.component.html',
  styleUrls: ['./query-lineage.component.scss']
})
export class QueryLineageComponent extends BaseGraphComponent {

  private _rows: QuerySankeyChartRow[];
  schemaNodeSet: SchemaNodeSet;

  constructor() {
    super();
  }

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
      this.schemaNodeSet = this.generateChartData(this.rows);
    }
  }


  private generateChartData(rows: QuerySankeyChartRow[]): SchemaNodeSet {
    const nodes: Map<string, SchemaGraphNode> = new Map<string, SchemaGraphNode>();
    const links: Map<number, SchemaGraphLink> = new Map<number, SchemaGraphLink>();

    function toSafeId(input: string): string {
      return input
        .split(' ').join('')
        .split('.').join('')
        .split('"').join('')
        .split('(').join('')
        .split(')').join('')
        .split(',').join('')
        .split('@@').join('');
    }

    function rowTypeToGraphType(rowType: SankeyNodeType): SchemaGraphNodeType {
      switch (rowType) {
        case 'AttributeName':
          return 'MEMBER';
        case 'Expression':
          return 'TYPE';
        case 'ProvidedInput':
          return 'DATASOURCE';
        case 'QualifiedName':
          return 'OPERATION';
      }
    }

    rows.forEach((row, index) => {
      const sourceId = toSafeId(row.sourceNode);
      if (!nodes.has(sourceId)) {
        nodes.set(sourceId, {
          id: sourceId,
          label: row.sourceNode,
          type: rowTypeToGraphType(row.sourceNodeType),
          nodeId: sourceId
        });
      }

      const targetId = toSafeId(row.targetNode);
      if (!nodes.has(targetId)) {
        nodes.set(targetId, {
          id: targetId,
          label: row.targetNode,
          type: rowTypeToGraphType(row.targetNodeType),
          nodeId: targetId
        });
      }

      links.set(index, {
        source: sourceId,
        target: targetId,
        label: row.count.toString()
      });

    });
    return new SchemaGraph(nodes, links).toNodeSet();
  }
}
