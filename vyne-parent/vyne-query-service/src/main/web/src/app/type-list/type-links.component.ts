import {Component, ElementRef, Input, OnInit, ViewChild} from '@angular/core';
import {TypesService} from "../services/types.service";
import * as shape from 'd3-shape';
import {Observable} from "rxjs/internal/Observable";
import {QualifiedName, SchemaGraph, SchemaGraphNode, SchemaMember, SchemaNodeSet} from "../services/schema";

@Component({
  selector: 'type-links',
  templateUrl: "./type-links.component.html",
  styleUrls: ["./type-links.component.scss"]
})
export class TypeLinksComponent implements OnInit {
  private _member: SchemaMember = null;

  @Input()
  set member(value: SchemaMember) {
    this._member = value;
    if (this._member) {
      this.schemaGraph = SchemaGraph.empty();
      this.typeLinks = this.schemaGraph.toNodeSet();
      this.loadTypeLinks(this._member.name)
    }
  };

  get member(): SchemaMember {
    return this._member
  }

  @ViewChild('chartOuterContianer')
  chartContainer: ElementRef;

  schemaGraph: SchemaGraph = SchemaGraph.empty();
  typeLinks: SchemaNodeSet = this.schemaGraph.toNodeSet();

  showLegend = false;
  curve = shape.curveBundle.beta(1);
  chartDimensions = [1400, 800];
  autoZoom: boolean = false;

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
  orientation = "LR";

  nodesToQualifiedName = {};

  constructor(
    private service: TypesService
  ) {
  }

  getStroke(node) {
    let nodeType = node.type;

    if (!this.colors[nodeType]) {
      console.log("No color defined for node type " + nodeType);
    }
    return this.colors[nodeType] || '#FAC51D';
  }

  ngOnInit() {
  }

  private loadTypeLinks(name: QualifiedName) {
    const sanitized = name.fullyQualifiedName.replace(" #", "@@");
    this.appendSchemaGraph(this.service.getLinks(sanitized))
  }

  private loadNodeLinks(node: SchemaGraphNode) {
    this.appendSchemaGraph(this.service.getLinksForNode(node))
  }

  private appendSchemaGraph(source: Observable<SchemaGraph>) {
    source.subscribe(
      res => {
        this.schemaGraph.add(res);
        this.typeLinks = this.schemaGraph.toNodeSet()
      },
      error => console.log("error : " + error)
    );


  }

  onLegendLabelClick(event) {
    console.log("On legend label click")
  }

  select(event) {
    console.log("Select");
    this.loadNodeLinks(this.schemaGraph.nodes.get(event.id))
  }

}
