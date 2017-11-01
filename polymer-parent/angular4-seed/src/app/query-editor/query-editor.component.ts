import { Component, OnInit } from '@angular/core';
import * as _ from "lodash";
import { TypesService, Type } from 'app/common-api/types.service';

@Component({
   selector: 'app-query-editor',
   templateUrl: './query-editor.component.html',
   styleUrls: ['./query-editor.component.scss']
})
export class QueryEditorComponent implements OnInit {

   types: Array<Type> = []
   facts: Array<Fact> = []
   constructor(
      private typeService: TypesService
   ) { }

   ngOnInit() {
      this.typeService.getTypes()
         .subscribe(
         res => {
            this.types = res.types
         },
         error => console.log("error : " + error))
   }

   addNewFact() {
      this.facts.push(new Fact())
   }

}

class Fact {
   constructor(public typeName: string = null, public value: any = null) {

   }
}
