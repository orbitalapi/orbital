import {
   Component,
   ViewEncapsulation,
   ViewChild,
   OnInit,
   HostListener,
   ElementRef,
   HostBinding,
   Input
} from "@angular/core";
import {GlobalState} from "../app.state";
import {ConfigService} from "../shared/services/config/config.service";
import {MatSidenav} from "@angular/material";
import {DataService} from "../shared/services/data/data.service";
import * as _ from "lodash";
import {
   TypesService,
   QualifiedName,
   Service,
   Type,
   SourceCode,
   SchemaGraph,
   SchemaNodeSet,
   SchemaGraphNode
} from "app/common-api/types.service";
import {colorSets} from '@swimlane/ngx-charts-dag/src/utils'
import {SchemaMember} from "app/type-explorer/type-explorer.component";
import * as shape from 'd3-shape';
import {Observable} from "rxjs/Observable";

@Component({
   selector: 'type-links-graph',
   templateUrl: "./type-links-graph.component.html",
   styleUrls: ["./type-links-graph.component.scss"]
})
export class TypeLinksGraphComponent implements OnInit {
   private _member: SchemaMember = null;
   @Input()
   set member(value: SchemaMember) {
      this._member = value;
      if (this._member) {
         this.schemaGraph = SchemaGraph.empty();
         this.typeLinks = this.schemaGraph.toNodeSet()
         this.loadTypeLinks(this._member.name)
      }
   };

   get member(): SchemaMember {
      return this._member
   }

   @ViewChild('chartOuterContianer')
   chartContainer: ElementRef;

   schemaGraph: SchemaGraph = SchemaGraph.empty()
   typeLinks: SchemaNodeSet = this.schemaGraph.toNodeSet()

   showLegend = false;
   curve = shape.curveBundle.beta(1);
   chartDimensions = [1400, 800]
   autoZoom: boolean = false;
   colorScheme: any = null;
   orientation = "LR"

   nodesToQualifiedName = {}

   constructor(
      private service: TypesService
   ) {
      this.colorScheme = colorSets.find(s => {
         return s.name === 'picnic'
      });

   }


   ngOnInit() {
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
            this.schemaGraph.add(res)
            this.typeLinks = this.schemaGraph.toNodeSet()
         },
         error => console.log("error : " + error)
      );


   }

   onLegendLabelClick(event) {
      console.log("On legend label click")
   }

   select(event) {
      console.log("Select")
      this.loadNodeLinks(this.schemaGraph.nodes.get(event.id))
   }

}
