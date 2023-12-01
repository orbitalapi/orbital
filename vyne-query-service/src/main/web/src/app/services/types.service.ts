import {Inject, Injectable, Injector} from '@angular/core';
import {Observable, ReplaySubject, Subject} from 'rxjs';

import * as _ from 'lodash';
import {HttpClient} from '@angular/common/http';

import {concatAll, map, shareReplay} from 'rxjs/operators';
import {Policy} from '../policy-manager/policies';
import {
  CompilationMessage,
  Message,
  Operation,
  OperationKind,
  ParsedSource,
  PartialSchema,
  QualifiedName,
  Schema,
  SchemaGraph,
  SchemaGraphNode,
  SchemaMember,
  SchemaMemberKind,
  SchemaSpec,
  Service,
  ServiceKind,
  Type,
  TypedInstance,
  TypeKind,
  TypeNamedInstance,
  VersionedSource,
} from './schema';
import {SchemaNotificationService, SchemaUpdatedNotification} from './schema-notification.service';
import {ValueWithTypeName} from './models';
import {ENVIRONMENT, Environment} from './environment';
import {TuiDialogService} from '@taiga-ui/core';
import {PackageIdentifier, PackageMetadata, SourcePackageDescription} from "../package-viewer/packages.service";
import {SchemaEditOperation} from "../schema-importer/schema-importer.service";
import {SavedQuery} from "./type-editor.service";


@Injectable({
  providedIn: 'root',
})
export class TypesService {
  private schema: Schema;
  private schemaSubject: Subject<Schema> = new ReplaySubject(1);
  private schemaRequest: Observable<Schema>;

  constructor(
    @Inject(ENVIRONMENT) private environment: Environment,
    @Inject(TuiDialogService) private readonly dialogService: TuiDialogService,
    @Inject(Injector) private readonly injector: Injector,
    private http: HttpClient,
    private schemaNotificationService: SchemaNotificationService,
  ) {

    this.getTypes().subscribe(schema => {
      this.schema = schema;
    });

    this.schemaNotificationService.createSchemaNotificationsSubscription()
      .subscribe(() => {
        this.getTypes(true)
          .subscribe(schema => {
            this.schema = schema;
          });
      });
  }

  validateSchema(schema: string): Observable<Type[]> {
    return this.http.post<Type[]>(`${this.environment.serverUrl}/api/schemas/taxi/validate`, schema);
  }

  getRawSchema = (): Observable<string> => {
    return this.http
      .get<string>(`${this.environment.serverUrl}/api/schemas/raw`);
  };

  getSchemaSummary(): Observable<SchemaUpdatedNotification> {
    return this.http.get<SchemaUpdatedNotification>(`${this.environment.serverUrl}/api/schemas/summary`);
  }

  getVersionedSchemas(): Observable<VersionedSource[]> {
    return this.http.get<VersionedSource[]>(`${this.environment.serverUrl}/api/schemas`);
  }

  getParsedSources(): Observable<ParsedSource[]> {
    return this.http.get<ParsedSource[]>(`${this.environment.serverUrl}/api/parsedSources`);
  }

  getLinksForNode = (node: SchemaGraphNode): Observable<SchemaGraph> => {
    return this.http
      .get<SchemaGraph>(`${this.environment.serverUrl}/api/nodes/${node.type}/${node.nodeId}/links`);
  };

  getLinks = (typeName: string): Observable<SchemaGraph> => {
    return this.http
      .get<SchemaGraph>(`${this.environment.serverUrl}/api/types/${typeName}/links`);
  };

  getTypeLineage(typeName: string): Observable<SchemaGraph> {
    return this.http.get<SchemaGraph>(
      `${this.environment.serverUrl}/api/types/${typeName}/lineage`,
    );
  }

  getServiceLineage(serviceName: string): Observable<SchemaGraph> {
    return this.http.get<SchemaGraph>(
      `${this.environment.serverUrl}/api/services/${serviceName}/lineage`,
    );
  }

  getPolicies(typeName: string): Observable<Policy[]> {
    return this.http.get(`${this.environment.serverUrl}/api/types/${typeName}/policies`)
      .pipe(map((policyDto: any[]) => {
        return Policy.parseDtoArray(policyDto);
      }));
  }

  getDiscoverableTypes(typeName: string): Observable<QualifiedName[]> {
    return this.http.get<QualifiedName[]>(`${this.environment.serverUrl}/api/types/${typeName}/discoverable-types`);
  }

  getType(qualifiedName: string): Observable<Type> {
    return this.http.get<Type>(`${this.environment.serverUrl}/api/types/${qualifiedName}`);
  }

  getService(qualifiedName: string): Observable<Service> {
    return this.http.get<Service>(`${this.environment.serverUrl}/api/services/${qualifiedName}`);
  }

  getOperation(serviceName: string, operationName: string): Observable<Operation> {
    return this.http.get<Operation>(`${this.environment.serverUrl}/api/services/${serviceName}/${operationName}`);
  }

  getSchemaTree(nodeName: string | null): Observable<SchemaTreeNode[]> {
    if (nodeName) {
      return this.http.get<SchemaTreeNode[]>(`${this.environment.serverUrl}/api/schema/tree?node=${nodeName}`);
    } else {
      return this.http.get<SchemaTreeNode[]>(`${this.environment.serverUrl}/api/schema/tree`);
    }


  }

  parse(content: string, type: Type): Observable<ParsedTypeInstance[]> {
    return this.http.post<ParsedTypeInstance[]>(
      `${this.environment.serverUrl}/api/content/parse?type=${type.name.fullyQualifiedName}`,
      content);
  }

  parseCsvToType(content: string, type: Type, csvOptions: CsvOptions): Observable<ParsedTypeInstance[]> {
    const nullValueParam = csvOptions.nullValueTag ? '&nullValue=' + csvOptions.nullValueTag : '';
    const ignoreContentParam = csvOptions.ignoreContentBefore ? '&ignoreContentBefore='
      + encodeURIComponent(csvOptions.ignoreContentBefore) : '';
    const separator = encodeURIComponent(this.detectCsvDelimiter(content));
    return this.http.post<ParsedTypeInstance[]>(
      // eslint-disable-next-line max-len
      `${this.environment.serverUrl}/api/csv/parse?type=${type.name.fullyQualifiedName}&delimiter=${separator}&firstRecordAsHeader=${csvOptions.firstRecordAsHeader}${ignoreContentParam}${nullValueParam}`,
      content);
  }

  parseCsv(content: string, csvOptions: CsvOptions): Observable<ParsedCsvContent> {
    const nullValueParam = csvOptions.nullValueTag ? '&nullValue=' + csvOptions.nullValueTag : '';
    const ignoreContentParam = csvOptions.ignoreContentBefore ?
      '&ignoreContentBefore=' + encodeURIComponent(csvOptions.ignoreContentBefore)
      : '';
    const separator = encodeURIComponent(this.detectCsvDelimiter(content));
    return this.http.post<ParsedCsvContent>(
      // eslint-disable-next-line max-len
      `${this.environment.serverUrl}/api/csv?delimiter=${separator}&firstRecordAsHeader=${csvOptions.firstRecordAsHeader}${nullValueParam}${ignoreContentParam}`,
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
      schema: schema,
    };
    return this.http.post<ContentWithSchemaParseResponse>(
      // eslint-disable-next-line max-len
      `${this.environment.serverUrl}/api/csvAndSchema/parse?type=${typeName}&delimiter=${separator}&firstRecordAsHeader=${csvOptions.firstRecordAsHeader}${ignoreContentParam}${nullValueParam}`,
      request);
  }

  parseContentToTypeWithAdditionalSchema(content: string,
                                         typeName: string,
                                         schema: string): Observable<ContentWithSchemaParseResponse> {
    const request: ContentWithSchemaParseRequest = {
      content: content,
      schema: schema,
    };
    return this.http.post<ContentWithSchemaParseResponse>(
      // eslint-disable-next-line max-len
      `${this.environment.serverUrl}/api/contentAndSchema/parse?type=${typeName}`,
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
      schema: schema,
    };
    return this.http.post<ValueWithTypeName[]>(
      // eslint-disable-next-line max-len
      `${this.environment.serverUrl}/api/csvAndSchema/project?type=${parseType}&targetType=${projectionType}&clientQueryId=${queryId}&delimiter=${separator}&firstRecordAsHeader=${csvOptions.firstRecordAsHeader}${ignoreContentParam}${nullValueParam}`,
      request,
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
      ? `${this.environment.serverUrl}/api/xml/parse?type=${type.name.fullyQualifiedName}`
      + `&elementSelector=${encodeURIComponent(elementSelector)}`
      : `${this.environment.serverUrl}/api/xml/parse?type=${type.name.fullyQualifiedName}`;
    return this.http.post<ParsedTypeInstance>(url, content);
  }

  private detectCsvDelimiter = (input: string) => {
    const separators = [',', ';', '|', '\t'];
    const idx = separators
      .map((separator) => input.indexOf(separator))
      .reduce((prev, cur) =>
        prev === -1 || (cur !== -1 && cur < prev) ? cur : prev,
      );
    return (input[idx] || ',');
  };

  getTypes(refresh: boolean = false): Observable<Schema> {
    if (refresh || !this.schemaRequest) {
      this.schemaRequest = this.http
        .get<Schema>(`${this.environment.serverUrl}/api/types`)
        .pipe(
          map(schema => {
              return prepareSchema(schema);
            },
          ),
        );
      this.schemaRequest.subscribe(
        result => this.schemaSubject.next(result),
        err => this.schemaSubject.next(err),
      );
    }
    return this.schemaSubject.asObservable();
  }

  createExtensionSchemaFromTaxi(typeName: QualifiedName, schemaNameSuffix: string, schemaText: string): Observable<VersionedSource> {
    const spec: SchemaSpec = {
      name: `${typeName.fullyQualifiedName}.${typeName.name}${schemaNameSuffix}`,
      version: 'next-minor',
      defaultNamespace: typeName.namespace,
    };
    const request = new SchemaImportRequest(
      spec, 'taxi', schemaText,
    );

    return this.submitSchema(request);
  }


  createSchemaPreview(request: SchemaPreviewRequest): Observable<SchemaPreview> {
    return this.http.post<SchemaPreview>(
      `${this.environment.serverUrl}/api/schemas/preview`,
      request,
    );
  }

  getTypeUsages(typeName: string): Observable<OperationQueryResult> {
    return this.http.get<OperationQueryResult>(`${this.environment.serverUrl}/api/types/operations/${typeName}`);
  }

  submitSchema(request: SchemaImportRequest): Observable<VersionedSource> {
    return this.http.post<VersionedSource>(
      `${this.environment.serverUrl}/api/schemas`,
      request,
    );
  }

  getQueries(): Observable<SavedQuery[]> {
    return this.http.get<SavedQuery[]>(`${this.environment.serverUrl}/api/schemas/queries`,)
  }

  getQuery(qualifiedName: string): Observable<SavedQuery> {
    return this.http.get<SavedQuery>(`${this.environment.serverUrl}/api/schemas/queries/${qualifiedName}`,)
  }



  getAllMetadata(): Observable<QualifiedName[]> {
    return this.http.get<QualifiedName[]>(`${this.environment.serverUrl}/api/schema/annotations`);
  }


  /**
   * Returns ModelFormatSpec metadata for the given type.
   * @param type
   */
  getModelFormatSpecsForType(type: Type): Observable<QualifiedName[]> {
    return this.http.get<QualifiedName[]>(`${this.environment.serverUrl}/api/types/${type.name.fullyQualifiedName}/modelFormats`);
  }

  submitTaxi(taxi: string): Observable<SchemaSubmissionResult> {
    return this.http.post<SchemaSubmissionResult>(`${this.environment.serverUrl}/api/schema/taxi`, taxi);
  }

  validateTaxi(taxi: string): Observable<SchemaSubmissionResult> {
    return this.http.post<SchemaSubmissionResult>(`${this.environment.serverUrl}/api/schema/taxi?validate=true`, taxi);
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

export interface SchemaEditRequest {
  packageIdentifier: any;
  edits: VersionedSource[];
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
      case 'txt' :
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

export interface SchemaSubmissionResult extends PartialSchema {
  messages: CompilationMessage[];
  sourcePackage: SourcePackage
  pendingEdits: SchemaEditOperation[]
}

export interface SourcePackage {
  packageMetadata: PackageMetadata
  sources: VersionedSource[]
  identifier: PackageIdentifier
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

export interface SchemaTreeNode {
  element: QualifiedName;
  schemaMemberKind: SchemaMemberKind;
  hasChildren: boolean;
  typeDoc: string | null;
  serviceKind: ServiceKind | null;
  typeKind: TypeKind | null;
  operationKind: OperationKind | null;
  fieldName: string | null;
  primitiveType: QualifiedName | null;

}
