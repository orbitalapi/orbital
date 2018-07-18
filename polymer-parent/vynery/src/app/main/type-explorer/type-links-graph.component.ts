import {Component, ElementRef, Input, OnInit, ViewChild} from "@angular/core";
import {QualifiedName, SchemaGraph, SchemaGraphNode, SchemaNodeSet, TypesService} from "./types.service";
import {SchemaMember} from "./type-explorer.component";
import * as shape from 'd3-shape';
import {Observable} from "rxjs/Observable";

@Component({
    selector: 'type-links-graph',
    templateUrl: "./type-links-graph.component.html",
    styleUrls: ["./type-links-graph.component.scss"],
    providers: [TypesService]
})
export class TypeLinksGraphComponent implements OnInit {
    @Input()
    member: SchemaMember;

    @ViewChild('chartOuterContianer')
    chartContainer: ElementRef;

    schemaGraph: SchemaGraph = SchemaGraph.empty();
    typeLinks: SchemaNodeSet = this.schemaGraph.toNodeSet();

    showLegend = false;
    curve = shape.curveBundle.beta(1);
    chartDimensions = [1400, 800];
    autoZoom: boolean = false;
    colorScheme: any = {
        name: 'picnic',
        selectable: false,
        group: 'Ordinal',
        domain: [
            '#FAC51D', '#66BD6D', '#FAA026', '#29BB9C', '#E96B56', '#55ACD2', '#B7332F', '#2C83C9', '#9166B8', '#92E7E8'
        ]
    };


    orientation = "LR";

    constructor(
        private service: TypesService
    ) {
    }


    ngOnInit() {
        this.loadTypeLinks(this.member.name)
    }

    private loadTypeLinks(name: QualifiedName) {
        this.appendSchemaGraph(this.service.getLinks(name.fullyQualifiedName))
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
