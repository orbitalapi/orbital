import {Component, Input, OnInit} from '@angular/core';
import {QuerySankeyChartRow, SankeyNodeType} from '../services/query.service';
import {splitOperationQualifiedName} from '../service-view/service-view.component';

@Component({
  selector: 'app-lineage-sankey-chart',
  styleUrls: ['./lineage-sankey-chart.component.scss'],
  template: `
    <google-chart type="Sankey" [options]="options" [data]="dataTable" width="1200" height="800"></google-chart>
  `
})
export class LineageSankeyChartComponent {
  private _rows: QuerySankeyChartRow[];

  options = {
    sankey: {
      node: {
        label: {
          fontName: 'nunito-sans',
          fontSize: 18,
          color: '#000',
          bold: true,
          italic: false
        },
      }
    }
  };

  dataTable: any[];

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
      this.dataTable = this.generateChartData(this.rows);
    }
  }


  private generateChartData(rows: QuerySankeyChartRow[]): any[] {
    const dataTable = new google.visualization.DataTable();
    dataTable.addColumn('string', 'From');
    dataTable.addColumn('string', 'To');
    dataTable.addColumn('number', 'Weight');

    // rows.forEach(row => {
    //   dataTable.addRow([
    //     row.sourceNode,
    //     row.targetNode,
    //     row.count
    //   ]);
    // });

    const r = rows.map(row => {

      return [
        this.getRowText(row.sourceNode, row.sourceNodeType),
        this.getRowText(row.targetNode, row.targetNodeType),
        row.count,
      ];
    });
    return r;
  }

  private getRowText(nodeText: string, sourceNodeType: SankeyNodeType): string {
    if (sourceNodeType === 'QualifiedName' && nodeText.includes('@@')) {
      const operationName = splitOperationQualifiedName(nodeText);
      return operationName.serviceDisplayName + '\n ' + operationName.operationName;
    } else if (sourceNodeType === 'Expression') {
      return nodeText.replace('by taxi.stdlib.', '');
    } else {
      return nodeText;
    }
  }
}
