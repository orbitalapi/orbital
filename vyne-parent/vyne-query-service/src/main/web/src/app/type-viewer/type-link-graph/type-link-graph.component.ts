import {Component, ElementRef, EventEmitter, Input, Output, ViewChild} from '@angular/core';
import * as shape from 'd3-shape';
import {Observable} from 'rxjs/internal/Observable';
import {SchemaGraph, SchemaGraphNode, SchemaNodeSet} from '../../services/schema';
import {Subscription} from 'rxjs';

@Component({
  selector: 'app-type-link-graph',
  templateUrl: './type-link-graph.component.html',
  styleUrls: ['./type-link-graph.component.scss']
})
export class TypeLinkGraphComponent {

  schemaGraph: SchemaGraph = SchemaGraph.empty();
  typeLinks: SchemaNodeSet = this.schemaGraph.toNodeSet();

  get hasContent() {
    return this.typeLinks.links.length > 0 && this.typeLinks.nodes.length > 0;
  }

  private schemaSubscription: Subscription;
  private _schemaGraphs: Observable<SchemaGraph>;
  @Input()
  get schemaGraphs(): Observable<SchemaGraph> {
    return this._schemaGraphs;
  }

  set schemaGraphs(value: Observable<SchemaGraph>) {
    if (this.schemaSubscription) {
      this.schemaSubscription.unsubscribe();
      this.schemaGraph = SchemaGraph.empty();
    }
    this._schemaGraphs = value;
    if (this.schemaGraphs) {
      this.schemaSubscription = this.schemaGraphs.subscribe(schemaGraph => {
        this.appendSchemaGraph(schemaGraph);
      });
    }
  }

  @Output()
  nodeClicked: EventEmitter<SchemaGraphNode> = new EventEmitter<SchemaGraphNode>();


  @ViewChild('chartOuterContianer')
  chartContainer: ElementRef;


  showLegend = false;
  curve = shape.curveBundle.beta(1);
  // chartDimensions = [1400, 800];
  autoZoom = false;

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

  nodesToQualifiedName = {};

  constructor() {
  }

  getStroke(node) {
    const nodeType = node.type;

    if (!this.colors[nodeType]) {
      console.log('No color defined for node type ' + nodeType);
    }
    return this.colors[nodeType] || '#FAC51D';
  }

  showServiceName(node): boolean {
    return node.type === 'OPERATION' || node.type === 'MEMBER';
  }

  serviceName(node): string {
    return node.label.split('/')[0];
  }

  operationName(node): string {
    return node.label.split('/')[1];
  }


  private appendSchemaGraph(schemaGraph: SchemaGraph) {
    this.schemaGraph.add(schemaGraph);
    this.typeLinks = this.schemaGraph.toNodeSet();
    console.log("Updated typeLinks.  Now:")
    console.log(JSON.stringify(this.typeLinks));
  }

  onLegendLabelClick(event) {
    console.log('On legend label click');
  }

  select(event) {
    console.log('Select');
    const node: SchemaGraphNode = this.schemaGraph.nodes.get(event.id);
    this.nodeClicked.emit(node);
  }


  outerRectangle(width: number, height: number): string {
    return this.roundedRectangle(0, 0, width, height, 4, 4, 4, 4);
  }

  innerRectangle(width: number, height: number): string {
    return this.roundedRectangle(0, 0, width, height, 4, 0, 0, 4);
  }

  roundedRectangle(x: number, y: number, width: number, height: number, tl: number, tr: number, br: number, bl: number) {

    const TR = [1, 1];
    const BR = [-1, 1];
    const BL = [-1, -1];
    const TL = [1, -1];

    function arc(rad: number, corner) {
      // If the radius is 0 (ie., no arc), then just bail.
      if (rad === 0) {
        return '';
      }
      // Convert radius to -radius, depending on corner we're in.
      const arcX = rad * corner[0];
      const arcY = rad * corner[1];
      return `a${rad},${rad} 0 0 1 ${arcX},${arcY}`;
    }

    function horizontal(s: number) {
      return `h${s}`;
    }

    function vertical(s: number) {
      return `v${s}`;
    }

    // From https://stackoverflow.com/a/38118843
    // M100,100
    // h200
    // a20,20 0 0 1 20,20
    // v200
    // a20,20 0 0 1 -20,20
    // h-200
    // a20,20 0 0 1 -20,-20
    // v-200
    // a20,20 0 0 1 20,-20 z
    return [
      `M${tl},0`,
      horizontal(width - (tl + tr)),
      arc(tr, TR),
      vertical(height - (tr + br)),
      arc(br, BR),
      horizontal((width * -1) + (tr + bl)),
      arc(bl, BL),
      vertical((height * -1) + (bl + tl)),
      arc(tl, TL),
      'z'
    ].join(' ');
  }

}
