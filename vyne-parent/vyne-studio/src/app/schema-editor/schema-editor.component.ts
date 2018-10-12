import {Component, OnInit} from '@angular/core';
import {Subject} from 'rxjs';
import {HttpClient} from '@angular/common/http';

import {environment} from '../../environments/environment';
import {Schema, TypesService} from 'app/common-api/types.service';
import {SchemaNodeSet} from "../common-api/types.service";
import * as shape from 'd3-shape';
import {colorSets} from "@swimlane/ngx-charts-dag/src/utils/index";
import {SchemaMember} from "../type-explorer/type-explorer.component";
import {FormControl} from "@angular/forms";
import {Observable} from "rxjs/Observable";
import {MatOptionSelectionChange} from "@angular/material";

@Component({
   selector: 'app-schema-editor',
   templateUrl: './schema-editor.component.html',
   styleUrls: ['./schema-editor.component.scss']
})
export class SchemaEditorComponent implements OnInit {

   constructor(private typeService: TypesService, private http: HttpClient) {
      this.colorScheme = colorSets.find(s => {
         return s.name === 'picnic'
      });
   }

   taxiDef: string = '';
   taxiDefUpdates: Subject<string> = new Subject();
   errors: Array<any> = [{
      line: 30,
      col: 35,
      message: "Hello, world"
   }];

   errorTableColumns = [
      {name: "Line"},
      {name: "Column"},
      {name: "Message"}
   ];

   schemaLinks: SchemaNodeSet = {
      links: [],
      nodes: []
   };


   schemaMembers: SchemaMember[] = [];
   showLegend = false;
   curve = shape.curveBundle.beta(1);
   chartDimensions = [1400, 800];
   autoZoom: boolean = true;
   colorScheme: any = null;
   orientation = "LR";

   explorerStartingTypeCtrl = new FormControl();
   typeNames: string[] = [];
   filteredTypes: Observable<string[]>;

   selectedMember:SchemaMember;

   onTaxiDefChanged(): void {
      this.taxiDefUpdates.next(this.taxiDef)
   }

   ngOnInit() {
      this.typeService.getRawSchema()
         .subscribe(taxi => this.taxiDef = taxi);
      // TODO :  should be throttle(Observable.interval(500)), but that's not working.
      this.taxiDefUpdates.throttleTime(500)
         .subscribe(taxiDef => {
               this.updateSchemaLinks(taxiDef);
               this.updateSchemaTypes(taxiDef);
            }
         );

      this.filteredTypes = this.explorerStartingTypeCtrl.valueChanges
         .map((value) => {
            return this._filter(value)
         });
   }


   onStartingTypeSelected(event:MatOptionSelectionChange) {
      let selectedItem = event.source.value;
      this.selectedMember = this.schemaMembers.find(m => m.name.fullyQualifiedName == selectedItem);
   }
   private updateSchemaTypes(taxiDef) {
      this.http.post(`${environment.apiUrl}/schemas`, taxiDef)
         .subscribe(data => {
            let schema = data as Schema;
            this.schemaMembers = SchemaMember.fromSchema(schema);
            this.typeNames = this.schemaMembers.map(s => s.name.fullyQualifiedName)
         })
   }


   private updateSchemaLinks(taxiDef) {
      this.http.post(`${environment.apiUrl}/schemas/taxi-graph`, taxiDef)
         .subscribe(data => {
            this.errors = [];
            let anyData = data as any;

            this.schemaLinks = {
               nodes: Object.keys(anyData.nodes).map(key => anyData.nodes[key]),
               links: Object.keys(anyData.links).map(key => anyData.links[key])
            }
         }, errorResponse => {
            // compilation exceptions
            if (errorResponse.status == 406) {
               this.errors = errorResponse.error.errors
            }
         });
   }

   private _filter(value: string): string[] {
      const filterValue = value.toLowerCase();

      return this.typeNames.filter(option => option.toLowerCase().includes(filterValue));
   }
}
