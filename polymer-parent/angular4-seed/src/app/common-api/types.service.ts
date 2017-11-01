import { Http, Headers, Response, Jsonp, RequestOptions } from "@angular/http";
import { Injectable } from "@angular/core";
import { Observable } from "rxjs/Observable";

import "rxjs/Rx";
import "rxjs/add/operator/map";
import "rxjs/add/operator/toPromise";
import "rxjs/add/operator/catch";
import * as _ from "lodash";
@Injectable()
export class TypesService {
   constructor(private http: Http) { }

   getTypes = (): Observable<Schema> => {
      return this.http
         .get("api/types")
         .map(res => {
            let schema  = res.json() as Schema
            schema.types = _.sortBy((res as any).types, [(t) => { return t.name.fullyQualifiedName }])
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
   fullyQualifiedName: String
}
enum Modifier {
   PARAMETER_TYPE
}
export interface Type {
   name: QualifiedName
   attributes: Map<String, TypeReference>
   modifiers: Array<Modifier>
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
