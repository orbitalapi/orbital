import { Component, OnInit } from '@angular/core';
import * as _ from "lodash";
import { TypesService, Type } from 'app/common-api/types.service';
import { FormControl } from '@angular/forms';

@Component({
   selector: 'app-query-editor',
   templateUrl: './query-editor.component.html',
   styleUrls: ['./query-editor.component.scss']
})
export class QueryEditorComponent implements OnInit {

   typeSelectorCtrl : FormControl
   types: Array<Type> = []
   facts: Array<Fact> = []
   constructor(
      private typeService: TypesService
   ) { }

   ngOnInit() {
      this.typeService.getTypes()
         .subscribe(res => this.types = res.types);
      this.typeSelectorCtrl = new FormControl()
   }

   addNewFact() {
      this.facts.push(new Fact())
   }

}

export class Fact {
   constructor(public typeName: string = null, public value: any = null) {

   }
}
