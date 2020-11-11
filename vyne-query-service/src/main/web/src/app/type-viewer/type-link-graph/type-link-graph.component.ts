import {Component, ElementRef, EventEmitter, Input, Output, ViewChild} from '@angular/core';
import * as shape from 'd3-shape';
import {Observable} from 'rxjs/internal/Observable';
import {SchemaGraph, SchemaGraphNode, SchemaNodeSet} from '../../services/schema';
import {Subscription} from 'rxjs';
import {innerRectangle, outerRectangle} from './graph-utils';

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
  private resetEventsSubscription: Subscription;
  private _schemaGraph$: Observable<SchemaGraph>;

  @Input()
  get schemaGraph$(): Observable<SchemaGraph> {
    return this._schemaGraph$;
  }

  set schemaGraph$(value: Observable<SchemaGraph>) {
    if (this.schemaSubscription) {
      this.schemaSubscription.unsubscribe();
      this.schemaGraph = SchemaGraph.empty();
    }
    this._schemaGraph$ = value;
    if (this.schemaGraph$) {
      this.schemaSubscription = this.schemaGraph$.subscribe(schemaGraph => {
        this.appendSchemaGraph(schemaGraph);
      });
    }
  }

  private _resetGraphEvents$: Observable<void>;

  @Input()
  get resetGraphEvents$(): Observable<void> {
    return this._resetGraphEvents$;
  }

  set resetGraphEvents$(value: Observable<void>) {
    if (this.resetEventsSubscription) {
      this.resetEventsSubscription.unsubscribe();
      this.resetEventsSubscription = null;
    }
    this._resetGraphEvents$ = value;
    if (value) {
      this.resetEventsSubscription = this.resetGraphEvents$.subscribe(() => {
        this.schemaGraph = SchemaGraph.empty();
      });
    }
  }

  @Output()
  nodeClicked: EventEmitter<SchemaGraphNode> = new EventEmitter<SchemaGraphNode>();


  @ViewChild('chartOuterContianer', {static: true})
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
    return outerRectangle(width, height);
  }

  innerRectangle(width: number, height: number): string {
    return innerRectangle(width, height);
  }

  tooltipTitle(node) {
    return node && node.nodeId ? node.nodeId.replace(':', '.') : '';
  }
}

