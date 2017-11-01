import { Component, OnInit } from '@angular/core';
import { Subject } from 'rxjs';
import { HttpClient } from '@angular/common/http';

import {environment} from '../../environments/environment';

@Component({
  selector: 'app-schema-editor',
  templateUrl: './schema-editor.component.html',
  styleUrls: ['./schema-editor.component.scss']
})
export class SchemaEditorComponent implements OnInit {

   constructor(private http: HttpClient) {
   }

  taxiDef: string = '';
  taxiDefUpdates: Subject<string> = new Subject();
  errors:Array<any> = [{
     line : 30,
     col : 35,
     message : "Hello, world"
  }];

  errorTableColumns = [
     { name: "Line" },
     { name : "Column"},
     { name : "Message" }
  ]


  onTaxiDefChanged(): void {
   this.taxiDefUpdates.next(this.taxiDef)
 }

  ngOnInit() {

     // TODO :  should be throttle(Observable.interval(500)), but that's not working.
     this.taxiDefUpdates.throttleTime(500)
     .subscribe(taxiDef => {
       this.http.post(`${environment.apiUrl}/schemas/taxi-graph`, taxiDef)
         .subscribe(data => {
           this.errors = [];
         //   this.graphData = data;
         }, errorResponse => {
           // compilation exceptions
           if (errorResponse.status == 406) {
             this.errors = errorResponse.error.errors
           }
         });
       console.debug(taxiDef)
     })
  }

}
