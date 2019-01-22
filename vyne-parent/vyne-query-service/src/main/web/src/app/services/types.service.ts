import {Injectable} from "@angular/core";
import {Observable, of} from "rxjs/index";

import * as _ from "lodash";
import {HttpClient} from "@angular/common/http";

import {environment} from 'src/environments/environment';
import {map} from "rxjs/operators";
import {Policy} from "../policy-manager/policies";
import {Message, Schema, SchemaGraph, SchemaGraphNode, SchemaSpec, VersionedSchema} from "./schema";

@Injectable()
export class TypesService {

  private schema: Schema;

  constructor(private http: HttpClient) {
    this.getTypes().subscribe(schema => {
      this.schema = schema;
    })
  }

  getRawSchema = (): Observable<string> => {
    return this.http
      .get<string>(`${environment.queryServiceUrl}/schemas/raw`)
  };

  getVersionedSchemas(): Observable<VersionedSchema[]> {
    return this.http.get<VersionedSchema[]>(`${environment.queryServiceUrl}/schemas`)
  }

  getLinksForNode = (node: SchemaGraphNode): Observable<SchemaGraph> => {
    return this.http
      .get<SchemaGraph>(`${environment.queryServiceUrl}/nodes/${node.type}/${node.nodeId}/links`)
  };
  getLinks = (typeName: string): Observable<SchemaGraph> => {
    return this.http
      .get<SchemaGraph>(`${environment.queryServiceUrl}/types/${typeName}/links`)
  };

  getPolicies(typeName: string): Observable<Policy[]> {
    return this.http.get(`${environment.queryServiceUrl}/types/${typeName}/policies`)
      .pipe(map((policyDto: any[]) => {
        return Policy.parseDtoArray(policyDto)
      }))
  }

  getTypes = (): Observable<Schema> => {
    if (this.schema) {
      return of(this.schema)
    }
    return this.http
      .get<Schema>(`${environment.queryServiceUrl}/types`)
      .pipe(
        map(schema => {
            schema.types = _.sortBy(schema.types, [(t) => {
              return t.name.fullyQualifiedName
            }]);
            return schema
          }
        )
      );
  };

  createSchemaPreview(request: SchemaPreviewRequest): Observable<SchemaPreview> {
    return this.http.post<SchemaPreview>(
      `${environment.queryServiceUrl}/schemas/preview`,
      request
    )
  }

  submitSchema(request: SchemaImportRequest): Observable<VersionedSchema> {
    return this.http.post<VersionedSchema>(
      `${environment.queryServiceUrl}/schemas`,
      request
    )
  }
}

export class SchemaPreviewRequest {
  constructor(public spec: SchemaSpec, public format: string, public text?: string, public url?: string) {
  }
}

export class SchemaImportRequest {
  constructor(readonly spec: SchemaSpec, readonly format: string, readonly  content: string) {
  }
}

export interface SchemaPreview {
  spec: SchemaSpec
  content: string
  messages: Message[]
}
