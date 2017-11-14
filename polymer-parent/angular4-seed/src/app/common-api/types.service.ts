import { Http, Headers, Response, Jsonp, RequestOptions } from "@angular/http";
import { Injectable, OnInit } from "@angular/core";
import { Observable } from "rxjs/Observable";

import "rxjs/Rx";
import "rxjs/add/operator/map";
import "rxjs/add/operator/toPromise";
import "rxjs/add/operator/catch";
import * as _ from "lodash";
@Injectable()
export class TypesService  {

   schema:Schema
   constructor(private http: Http) {
         this.getTypes().subscribe( schema => {
               this.schema = schema;
         })
    }

   getTypes = (): Observable<Schema> => {
      if (this.schema) {
         return Observable.of(this.schema)
      }
      return this.http
         .get("api/types")
         .map(res => {
            let schema  = res.json() as Schema
            schema.types = _.sortBy(schema.types, [(t) => { return t.name.fullyQualifiedName }])
            return schema
         }
         );
   };
}

export interface QualifiedName {
   name: string
   fullyQualifiedName: string
}
export interface TypeReference {
   name: QualifiedName
   isCollection: Boolean
   constraints: Array<any>
   fullyQualifiedName: string
}
enum Modifier {
   PARAMETER_TYPE
}
export interface Type {
   name: QualifiedName
   attributes: Map<string, TypeReference>
   modifiers: Array<Modifier>
   scalar: boolean
   aliasForType: QualifiedName
}

export interface Schema {
   types: Array<Type>
   services: Array<Service>
   // TODO : Are these still required / meaningful?
   attributes: Set<QualifiedName>
}

export interface Parameter {
   type: Type
   name: string
   metadata: Array<Metadata>
   constraints: Array<any>
}

export interface Metadata {
   name: QualifiedName
   params: Map<string, any>
}



export interface Operation {
   name: String
   parameters: Array<Parameter>
   returnType: Type
   metadata: Array<Metadata>
   contract: OperationContract

}

export interface Service {
   qualifiedName: String
   operations: Array<Operation>
   metadata: Array<Metadata>
}

export interface OperationContract {
   returnType: Type
   constraints: Array<any>
}
