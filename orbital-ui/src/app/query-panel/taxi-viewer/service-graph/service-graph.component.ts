import {AfterViewInit, Component, ElementRef, Input, ViewChild} from '@angular/core';
import {QualifiedName, SchemaGraphLink, SchemaGraphNode, SchemaNodeSet} from '../../../services/schema';
import * as shape from 'd3-shape';
import {QueryResult} from '../../../services/query.service';

@Component({
  selector: 'app-service-graph',
  templateUrl: './service-graph.component.html',
  styleUrls: ['./service-graph.component.scss']
})
export class ServiceGraphComponent implements AfterViewInit {

  @ViewChild('chartOuterContianer', {static: true})
  chartContainer: ElementRef;

  private _queryResult: QueryResult;

  typeLinks: SchemaNodeSet;
  showLegend = false;
  curve = shape.curveBundle.beta(1);
  chartDimensions = [50, 50];
  autoZoom = true;

  colors = {
    'vyne': '#FAC51D',
    'caller': '#55ACD2',
    'operation': '#4bb04f'
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

  ngAfterViewInit(): void {
    setTimeout(() => {
      this.chartDimensions = [this.chartContainer.nativeElement.offsetWidth, this.chartContainer.nativeElement.offsetHeight];
      console.log('Sized chart to ' + this.chartDimensions);
    });
  }


  get result(): QueryResult {
    return this._queryResult;
  }

  @Input()
  set result(value: QueryResult) {
    this._queryResult = value;
    this.typeLinks = this.generateNodes();
  }

  getStroke(node) {
    return this.colors[node.type];
  }

  getNodeLabelYOffset(node) {
// maginc numbers I found worked and looked good.
    return node.returnType ? 7 : 9;
  }

  private generateNodes(): SchemaNodeSet {
    const vyneNode: SchemaGraphNode = {
      id: 'vyne',
      nodeId: 'vyne',
      type: 'VYNE',
      label: 'Vyne'
    };
    const callerNode: SchemaGraphNode = {
      id: 'caller',
      nodeId: 'caller',
      type: 'CALLER',
      label: 'Query client'
    };

    const resultTypes: string[] = Object.keys(this.result && this.result.results ? this.result.results : []);
    const queryLinks: SchemaGraphLink[] = [
      {
        label: 'Query',
        target: 'vyne',
        source: 'caller'
      },
      {
        label: resultTypes.length > 0 ? QualifiedName.nameOnly(resultTypes[0]) : 'Nothing',
        source: 'vyne',
        target: 'caller'
      }
    ];

    const nodePairs: NodePair[] = this.result.remoteCalls.map((remoteCall, index) => {
      return {
        node: {
          id: index.toString(),
          label: QualifiedName.nameOnly(remoteCall.operationQualifiedName) + '.' + remoteCall.operation,
          returnType: `${remoteCall.responseTypeName}`,
          nodeId: index.toString(),
          type: 'OPERATION'
        },
        link: {
          label: `${remoteCall.method} (${remoteCall.durationMs}ms)`,
          source: 'vyne',
          target: index.toString()
        }
      } as NodePair;
    });
    return {
      links: queryLinks.concat(nodePairs.map(p => p.link)),
      nodes: [callerNode, vyneNode].concat(nodePairs.map(p => p.node))
    };
  }


}

interface NodePair {
  node: SchemaGraphNode;
  link: SchemaGraphLink;
}
