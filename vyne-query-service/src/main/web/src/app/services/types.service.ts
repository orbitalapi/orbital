import {Injectable} from '@angular/core';
import {Observable, ReplaySubject, Subject} from 'rxjs/index';

import * as _ from 'lodash';
import {HttpClient} from '@angular/common/http';

import {environment} from 'src/environments/environment';
import {concatAll, map, shareReplay} from 'rxjs/operators';
import {Policy} from '../policy-manager/policies';
import {
  CompilationMessage,
  Message, Metadata, Operation,
  ParsedSource,
  QualifiedName,
  Schema,
  SchemaGraph,
  SchemaGraphNode, SchemaMember, SchemaNodeSet,
  SchemaSpec, Service,
  Type,
  TypedInstance,
  TypeNamedInstance,
  VersionedSource
} from './schema';
import {VyneServicesModule} from './vyne-services.module';
import {SchemaNotificationService, SchemaUpdatedNotification} from './schema-notification.service';
import {ValueWithTypeName} from './models';
import {VyneUser} from './user-info.service';

@Injectable({
  providedIn: VyneServicesModule
})
export class TypesService {

  private schema: Schema;
  private schemaSubject: Subject<Schema> = new ReplaySubject(1);
  private schemaRequest: Observable<Schema>;

  constructor(private http: HttpClient, private schemaNotificationService: SchemaNotificationService) {
    this.getTypes().subscribe(schema => {
      this.schema = schema;
    });
    this.schemaNotificationService.createSchemaNotificationsSubscription()
      .subscribe(() => {
        this.getTypes(true)
          .subscribe(schema => {
            console.log('updating typeService schema');
            this.schema = schema;
          });
      });
  }

  validateSchema(schema: string): Observable<Type[]> {
    return this.http.post<Type[]>(`${environment.queryServiceUrl}/api/schemas/taxi/validate`, schema);
  }

  getRawSchema = (): Observable<string> => {
    return this.http
      .get<string>(`${environment.queryServiceUrl}/api/schemas/raw`);
  }

  getSchemaSummary(): Observable<SchemaUpdatedNotification> {
    return this.http.get<SchemaUpdatedNotification>(`${environment.queryServiceUrl}/api/schemas/summary`);
  }

  getVersionedSchemas(): Observable<VersionedSource[]> {
    return this.http.get<VersionedSource[]>(`${environment.queryServiceUrl}/api/schemas`);
  }

  getParsedSources(): Observable<ParsedSource[]> {
    return this.http.get<ParsedSource[]>(`${environment.queryServiceUrl}/api/parsedSources`);
  }

  getLinksForNode = (node: SchemaGraphNode): Observable<SchemaGraph> => {
    return this.http
      .get<SchemaGraph>(`${environment.queryServiceUrl}/api/nodes/${node.type}/${node.nodeId}/links`);
  }

  getLinks = (typeName: string): Observable<SchemaGraph> => {
    return this.http
      .get<SchemaGraph>(`${environment.queryServiceUrl}/api/types/${typeName}/links`);
  }

  getTypeLineage(typeName: string): Observable<SchemaGraph> {
    return this.http.get<SchemaGraph>(
      `${environment.queryServiceUrl}/api/types/${typeName}/lineage`
    );
  }

  getServiceLineage(serviceName: string): Observable<SchemaGraph> {
    return this.http.get<SchemaGraph>(
      `${environment.queryServiceUrl}/api/services/${serviceName}/lineage`
    );
  }

  getPolicies(typeName: string): Observable<Policy[]> {
    return this.http.get(`${environment.queryServiceUrl}/api/types/${typeName}/policies`)
      .pipe(map((policyDto: any[]) => {
        return Policy.parseDtoArray(policyDto);
      }));
  }

  getDiscoverableTypes(typeName: string): Observable<QualifiedName[]> {
    return this.http.get<QualifiedName[]>(`${environment.queryServiceUrl}/api/types/${typeName}/discoverable-types`);
  }

  getType(qualifiedName: string): Observable<Type> {
    return this.http.get<Type>(`${environment.queryServiceUrl}/api/types/${qualifiedName}`);
  }

  getService(qualifiedName: string): Observable<Service> {
    return this.http.get<Service>(`${environment.queryServiceUrl}/api/services/${qualifiedName}`);
  }

  getOperation(serviceName: string, operationName: string): Observable<Operation> {
    return this.http.get<Operation>(`${environment.queryServiceUrl}/api/services/${serviceName}/${operationName}`);
  }

  parse(content: string, type: Type): Observable<ParsedTypeInstance> {
    return this.http.post<ParsedTypeInstance>(
      `${environment.queryServiceUrl}/api/content/parse?type=${type.name.fullyQualifiedName}`,
      content);
  }

  parseCsvToType(content: string, type: Type, csvOptions: CsvOptions): Observable<ParsedTypeInstance[]> {
    const nullValueParam = csvOptions.nullValueTag ? '&nullValue=' + csvOptions.nullValueTag : '';
    const ignoreContentParam = csvOptions.ignoreContentBefore ? '&ignoreContentBefore='
      + encodeURIComponent(csvOptions.ignoreContentBefore) : '';
    const separator = encodeURIComponent(this.detectCsvDelimiter(content));
    return this.http.post<ParsedTypeInstance[]>(
      // tslint:disable-next-line:max-line-length
      `${environment.queryServiceUrl}/api/csv/parse?type=${type.name.fullyQualifiedName}&delimiter=${separator}&firstRecordAsHeader=${csvOptions.firstRecordAsHeader}${ignoreContentParam}${nullValueParam}`,
      content);
  }

  parseCsv(content: string, csvOptions: CsvOptions): Observable<ParsedCsvContent> {
    const nullValueParam = csvOptions.nullValueTag ? '&nullValue=' + csvOptions.nullValueTag : '';
    const ignoreContentParam = csvOptions.ignoreContentBefore ?
      '&ignoreContentBefore=' + encodeURIComponent(csvOptions.ignoreContentBefore)
      : '';
    const separator = encodeURIComponent(this.detectCsvDelimiter(content));
    return this.http.post<ParsedCsvContent>(
      // tslint:disable-next-line:max-line-length
      `${environment.queryServiceUrl}/api/csv?delimiter=${separator}&firstRecordAsHeader=${csvOptions.firstRecordAsHeader}${nullValueParam}${ignoreContentParam}`,
      content);
  }

  parseCsvToTypeWithAdditionalSchema(content: string,
                                     typeName: string,
                                     csvOptions: CsvOptions,
                                     schema: string): Observable<ContentWithSchemaParseResponse> {
    const nullValueParam = csvOptions.nullValueTag ? '&nullValue=' + csvOptions.nullValueTag : '';
    const ignoreContentParam = csvOptions.ignoreContentBefore ? '&ignoreContentBefore='
      + encodeURIComponent(csvOptions.ignoreContentBefore) : '';
    const separator = encodeURIComponent(this.detectCsvDelimiter(content));
    const request: ContentWithSchemaParseRequest = {
      content: content,
      schema: schema
    };
    return this.http.post<ContentWithSchemaParseResponse>(
      // tslint:disable-next-line:max-line-length
      `${environment.queryServiceUrl}/api/csvAndSchema/parse?type=${typeName}&delimiter=${separator}&firstRecordAsHeader=${csvOptions.firstRecordAsHeader}${ignoreContentParam}${nullValueParam}`,
      request);
  }

  parseContentToTypeWithAdditionalSchema(content: string,
                                     typeName: string,
                                     schema: string): Observable<ContentWithSchemaParseResponse> {
    const request: ContentWithSchemaParseRequest = {
      content: content,
      schema: schema
    };
    return this.http.post<ContentWithSchemaParseResponse>(
      // tslint:disable-next-line:max-line-length
      `${environment.queryServiceUrl}/api/contentAndSchema/parse?type=${typeName}`,
      request);
  }

  parseCsvToProjectedTypeWithAdditionalSchema(content: string,
                                              parseType: string,
                                              projectionType: string,
                                              csvOptions: CsvOptions,
                                              schema: string,
                                              queryId: string): Observable<ValueWithTypeName> {
    const nullValueParam = csvOptions.nullValueTag ? '&nullValue=' + csvOptions.nullValueTag : '';
    const ignoreContentParam = csvOptions.ignoreContentBefore ? '&ignoreContentBefore='
      + encodeURIComponent(csvOptions.ignoreContentBefore) : '';
    const separator = encodeURIComponent(this.detectCsvDelimiter(content));
    const request: ContentWithSchemaParseRequest = {
      content: content,
      schema: schema
    };
    return this.http.post<ValueWithTypeName[]>(
      // tslint:disable-next-line:max-line-length
      `${environment.queryServiceUrl}/api/csvAndSchema/project?type=${parseType}&targetType=${projectionType}&clientQueryId=${queryId}&delimiter=${separator}&firstRecordAsHeader=${csvOptions.firstRecordAsHeader}${ignoreContentParam}${nullValueParam}`,
      request
    ).pipe(
        // the legaacy (blocking) endpoint returns a ValueWithTypeName[].
        // however, we want to unpack that to multiple emitted items on our observable
        // therefore, concatAll() seems to do this.
        // https://stackoverflow.com/questions/42482705/best-way-to-flatten-an-array-inside-an-rxjs-observable
        concatAll(),
        shareReplay({bufferSize: 500, refCount: false}),
      );

  }


  parseXmlToType(content: string, type: Type, xmlIngestionParameters: XmlIngestionParameters): Observable<ParsedTypeInstance> {
    const elementSelector = xmlIngestionParameters.elementSelector;
    const url = elementSelector
      ? `${environment.queryServiceUrl}/api/xml/parse?type=${type.name.fullyQualifiedName}`
      + `&elementSelector=${encodeURIComponent(elementSelector)}`
      : `${environment.queryServiceUrl}/api/xml/parse?type=${type.name.fullyQualifiedName}`;
    return this.http.post<ParsedTypeInstance>(url, content);
  }

  private detectCsvDelimiter = (input: string) => {
    const separators = [',', ';', '|', '\t'];
    const idx = separators
      .map((separator) => input.indexOf(separator))
      .reduce((prev, cur) =>
        prev === -1 || (cur !== -1 && cur < prev) ? cur : prev
      );
    return (input[idx] || ',');
  }

  getTypes(refresh: boolean = false): Observable<Schema> {
    if (refresh || !this.schemaRequest) {
      this.schemaRequest = this.http
        .get<Schema>(`${environment.queryServiceUrl}/api/types`)
        .pipe(
          map(schema => {
              return prepareSchema(schema);
            }
          )
        );
      this.schemaRequest.subscribe(
        result => this.schemaSubject.next(result),
        err => this.schemaSubject.next(err)
      );
    }
    return this.schemaSubject.asObservable();
  }

  createExtensionSchemaFromTaxi(typeName: QualifiedName, schemaNameSuffix: string, schemaText: string): Observable<VersionedSource> {
    const spec: SchemaSpec = {
      name: `${typeName.fullyQualifiedName}.${typeName.name}${schemaNameSuffix}`,
      version: 'next-minor',
      defaultNamespace: typeName.namespace
    };
    const request = new SchemaImportRequest(
      spec, 'taxi', schemaText
    );

    return this.submitSchema(request);
  }

  createSchemaPreview(request: SchemaPreviewRequest): Observable<SchemaPreview> {
    return this.http.post<SchemaPreview>(
      `${environment.queryServiceUrl}/api/schemas/preview`,
      request
    );
  }

  getTypeUsages(typeName: string): Observable<OperationQueryResult> {
    return this.http.get<OperationQueryResult>(`${environment.queryServiceUrl}/api/types/operations/${typeName}`);
  }

  submitSchema(request: SchemaImportRequest): Observable<VersionedSource> {
    return this.http.post<VersionedSource>(
      `${environment.queryServiceUrl}/api/schemas`,
      request
    );
  }

  getAllMetadata(): Observable<QualifiedName[]> {
    return this.http.get<QualifiedName[]>(`${environment.queryServiceUrl}/api/schema/annotations`);
  }

  setTypeDataOwner(type: Type, owner: VyneUser): Observable<Type> {
    return this.http.post<Type>(`${environment.queryServiceUrl}/api/types/${type.name.fullyQualifiedName}/owner`,
      {
        id: owner.userId,
        name: owner.name
      } as UpdateDataOwnerRequest
    );
  }

  setTypeMetadata(type: Type, $event: QualifiedName[]): Observable<Type> {
    return this.http.post<Type>(`${environment.queryServiceUrl}/api/types/${type.name.fullyQualifiedName}/annotations`,
      {
        annotations: $event.map(name => name.fullyQualifiedName)
      }
    );
  }

  /**
   * Returns ModelFormatSpec metadata for the given type.
   * @param type
   */
  getModelFormatSpecsForType(type: Type): Observable<QualifiedName[]> {
    return this.http.get<QualifiedName[]>(`${environment.queryServiceUrl}/api/types/${type.name.fullyQualifiedName}/modelFormats`);
  }
}

export class SchemaPreviewRequest {
  constructor(public spec: SchemaSpec, public format: string, public text?: string, public url?: string) {
  }
}

export class SchemaImportRequest {
  constructor(readonly spec: SchemaSpec, readonly format: string, readonly content: string) {
  }
}

export interface SchemaPreview {
  spec: SchemaSpec;
  content: string;
  messages: Message[];
}

export interface ParsedTypeInstance {
  instance: TypedInstance;
  typeNamedInstance: TypeNamedInstance;
  raw: any;
}

export interface VyneHttpServiceError {
  timestamp: Date;
  error: string;
  message: string;
  path: string;
  status: number;
}

export interface ParsedCsvContent {
  headers: string[];
  records: string[][];
}

export class CsvOptions {
  constructor(public firstRecordAsHeader: boolean = true, public separator: string = ',', public nullValueTag: string | null = null,
              public ignoreContentBefore: string | null = null,
              public containsTrailingDelimiters: boolean = false) {
  }

  static isCsvContent(fileExtension: string): boolean {
    if (!fileExtension) {
      return false;
    }
    switch (fileExtension.toLowerCase()) {
      case 'csv' :
        return true;
      case 'psv' :
        return true;
      default:
        return false;
    }
  }
}

export function prepareSchema(schema: Schema): Schema {
  schema.types = _.sortBy(schema.types, [(t) => {
    return t.name.fullyQualifiedName;
  }]);
  const typesAsSchemaMembers: SchemaMember[] = schema.types.map(t => SchemaMember.fromType(t));
  const servicesAsSchemaMembers: SchemaMember[] = schema.services.flatMap(s => SchemaMember.fromService(s));
  const schemaMembers: SchemaMember[] = typesAsSchemaMembers.concat(servicesAsSchemaMembers);
  schema.members = _.sortBy(schemaMembers, [(schemaMember: SchemaMember) => {
    return schemaMember.name.fullyQualifiedName;
  }]);
  return schema;
}

export class XmlIngestionParameters {
  constructor(public elementSelector: string | null = null) {
  }

  static isXmlContent(fileExtension: string): boolean {
    if (!fileExtension) {
      return false;
    }
    switch (fileExtension) {
      case 'xml' :
        return true;
      default:
        return false;
    }
  }
}

export interface ContentWithSchemaParseRequest {
  content: string;
  schema: string;
}

export interface ContentWithSchemaParseResponse {
  parsedTypedInstances: ParsedTypeInstance[];
  types: Type[];
}
export interface TaxiSubmissionResult {
  types: Type[];
  services: Service[];
  messages: CompilationMessage[];
  taxi: string;
}

export interface OperationQueryResult {
  typeName: string;
  results: OperationQueryResultItem[];
}

export interface OperationQueryResultItem {
  serviceName: QualifiedName;
  operationDisplayName: string | null;
  operationName: QualifiedName | null;
  role: 'Input' | 'Output';
}

export interface UpdateDataOwnerRequest {
  id: string;
  name: string;
}

