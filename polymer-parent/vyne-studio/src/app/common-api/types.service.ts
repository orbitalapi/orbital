import { Http, Headers, Response, Jsonp, RequestOptions } from "@angular/http";
import { Injectable, OnInit } from "@angular/core";
import { Observable } from "rxjs/Observable";

import "rxjs/Rx";
import "rxjs/add/operator/map";
import "rxjs/add/operator/toPromise";
import "rxjs/add/operator/catch";
import * as _ from "lodash";
@Injectable()
export class TypesService {

   schema: Schema;
   constructor(private http: Http) {
      this.getTypes().subscribe(schema => {
         this.schema = schema;
      })
   }

   getRawSchema = ():Observable<string> => {
      return this.http
         .get(`http://localhost:9022/schemas/raw`)
         .map ( result => result.text() )
   };

   getLinksForNode = (node: SchemaGraphNode):Observable<SchemaGraph> => {
      return this.http
         .get(`http://localhost:9022/nodes/${node.type}/${node.nodeId}/links`)
         .map( result => result.json() as SchemaGraph)
   };
   getLinks = (typeName: string): Observable<SchemaGraph> => {
      return this.http
         .get(`http://localhost:9022/types/${typeName}/links`)
         .map( result => result.json() as SchemaGraph )
   };

   getTypes = (): Observable<Schema> => {
      if (this.schema) {
         return Observable.of(this.schema)
      }
      return this.http
         // .get("api/types")
         .get("http://localhost:9022/types")
         .map(res => {
            let schema = res.json() as Schema;
            schema.types = _.sortBy(schema.types, [(t) => { return t.name.fullyQualifiedName }]);
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
   aliasForType: QualifiedName,
   sources: Array<SourceCode>
}

export interface SourceCode {
   origin: string
   language: string
   content: string
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
   name: string
   parameters: Array<Parameter>
   returnType: Type
   metadata: Array<Metadata>
   contract: OperationContract

}

export interface Service {
   name: QualifiedName
   operations: Array<Operation>
   metadata: Array<Metadata>,
   sourceCode: SourceCode
}

export interface OperationContract {
   returnType: Type
   constraints: Array<any>
}



export interface SchemaGraphNode {
   id: string
   label: string
   type:string // Consider adding the enum ElementType here
   nodeId: string
}

export interface SchemaGraphLink {
   source: string
   target: string
   label: string
}

export class SchemaGraph {

   constructor(
      public readonly nodes:Map<string,SchemaGraphNode>,
      public readonly links:Map<number,SchemaGraphLink>
   ) {}

   add(other:SchemaGraph) {
      this.mergeToMap(other.nodes, this.nodes);
      this.mergeToMap(other.links, this.links)
   }

   toNodeSet():SchemaNodeSet {
      return {
         nodes: Array.from(this.nodes.values()),
         links: Array.from(this.links.values())
      }
   }


   mergeToMap(source:Map<any,any>, target) {
      Object.keys(source).forEach (key => {
         if (!target[key]) {
            target.set(key, source[key])
         }
      })
   }
   static empty():SchemaGraph {
      return new SchemaGraph(new Map(), new Map())
   }
}


export interface SchemaNodeSet {
   nodes:SchemaGraphNode[]
   links:SchemaGraphLink[]
}
