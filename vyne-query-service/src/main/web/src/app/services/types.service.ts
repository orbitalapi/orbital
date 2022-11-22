import { Inject, Injectable, Injector } from '@angular/core';
import { BehaviorSubject, Observable, of, ReplaySubject, Subject } from 'rxjs';

import * as _ from 'lodash';
import { HttpClient } from '@angular/common/http';

import { concatAll, map, mapTo, share, shareReplay, switchMap, tap } from 'rxjs/operators';
import { Policy } from '../policy-manager/policies';
import {
  CompilationMessage,
  Message,
  Metadata,
  Operation,
  ParsedSource,
  PartialSchema,
  QualifiedName,
  Schema,
  SchemaGraph,
  SchemaGraphNode,
  SchemaMember,
  SchemaSpec,
  Service,
  Type,
  TypedInstance,
  TypeNamedInstance,
  VersionedSource,
} from './schema';
import { SchemaNotificationService, SchemaUpdatedNotification } from './schema-notification.service';
import { ValueWithTypeName } from './models';
import { VyneUser } from './user-info.service';
import { ENVIRONMENT, Environment } from './environment';
import { TuiDialogService } from '@taiga-ui/core';
import { PolymorpheusComponent } from '@tinkoff/ng-polymorpheus';
import { ChangesetNameDialogComponent, ChangesetNameDialogSaveHandler } from '../changeset-name-dialog/changeset-name-dialog.component';

// TODO How to generate this?
export const fakePackageIdentifier = {
  organisation: 'io.vyne',
  name: 'films',
  version: 'next-minor',
};

export const defaultChangesetName = 'main';

@Injectable({
  providedIn: 'root',
})
export class TypesService {
  activeChangeset$ = new BehaviorSubject<Changeset>({ name: defaultChangesetName, isActive: true });
  availableChangesets$ = new BehaviorSubject<Changeset[]>([]);

  private schema: Schema;
  private schemaSubject: Subject<Schema> = new ReplaySubject(1);
  private schemaRequest: Observable<Schema>;

  constructor(
    @Inject(ENVIRONMENT) private environment: Environment,
    @Inject(TuiDialogService) private readonly dialogService: TuiDialogService,
    @Inject(Injector) private readonly injector: Injector,
    private http: HttpClient,
    private schemaNotificationService: SchemaNotificationService) {

    this.updateChangelogs();

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

  ensureChangesetExists(): Observable<string> {
    return this.openNameDialog(false, name => this.createChangeset(name));
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
      shareReplay({ bufferSize: 500, refCount: false }),
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

  openNameDialog(forceOpen: boolean, saveHandler: ChangesetNameDialogSaveHandler): Observable<string> {
    if (!forceOpen && this.activeChangeset$.value.name !== defaultChangesetName) {
      return of(this.activeChangeset$.value.name);
    }
    return this.dialogService
      .open<string | null>(new PolymorpheusComponent(ChangesetNameDialogComponent, this.injector), {
        data: {
          name: this.activeChangeset$.value.name,
          saveHandler: saveHandler,
        },
      });
  }

  setActiveChangeset(changesetName: string): Observable<void> {
    return this.http.post<void>(`${this.environment.serverUrl}/api/repository/changesets/active`, {
      packageIdentifier: fakePackageIdentifier,
      changesetName: changesetName,
    }).pipe(tap(() => this.activeChangeset$.next({ name: changesetName, isActive: true })));
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

  createChangeset(name: string): Observable<any> { // TODO Typing
    const sanitizedName = this.sanitizeChangesetName(name);
    return this.http.post<any>(
      `${this.environment.serverUrl}/api/repository/changeset/create`,
      { changesetName: sanitizedName, packageIdentifier: fakePackageIdentifier },
    ).pipe(tap(() => {
      this.updateChangelogs();
      console.log('changeset created', sanitizedName);
    }));
  }

  addChangesToChangeset(typeName: QualifiedName, schemaNameSuffix: string, schemaText: string): Observable<VersionedSource> {
    return this.ensureChangesetExists()
      .pipe(
        switchMap(changesetName => {
          const request: AddChangesToChangesetRequest = {
            edits: [
              {
                // TODO Add typeName.namespace below ??
                name: `${typeName.fullyQualifiedName}.${typeName.name}${schemaNameSuffix}`,
                content: schemaText,
                version: 'next-minor',
              },
            ],
            packageIdentifier: fakePackageIdentifier,
            changesetName: changesetName,
          };
          return this.http.post<VersionedSource>(
            `${this.environment.serverUrl}/api/repository/changeset/add`,
            request,
          );

        }));
  }

  finalizeChangeset(): Observable<FinalizeChangesetResponse> {
    return this.ensureChangesetExists()
      .pipe(
        switchMap(changesetName => {
          const body: FinalizeChangesetRequest = { changesetName, packageIdentifier: fakePackageIdentifier };
          return this.http.post<FinalizeChangesetResponse>(
            `${this.environment.serverUrl}/api/repository/changeset/finalize`,
            body,
          );
        }),
        tap(() => this.activeChangeset$.next({ name: 'main', isActive: true })),
      );
  }

  getAllMetadata(): Observable<QualifiedName[]> {
    return this.http.get<QualifiedName[]>(`${this.environment.serverUrl}/api/schema/annotations`);
  }

  setTypeDataOwner(type: Type, owner: VyneUser): Observable<Type> {
    return this.ensureChangesetExists()
      .pipe(
        switchMap(() => this.http.post<Type>(`${this.environment.serverUrl}/api/types/${type.name.fullyQualifiedName}/owner`,
          {
            id: owner.userId,
            name: owner.name,
            changesetName: this.activeChangeset$.value.name,
          } as UpdateDataOwnerRequest,
        )),
        tap(() => this.updateChangelogs()),
      );
  }

  setTypeMetadata(type: Type, metadata: Metadata[]): Observable<Type> {
    return this.ensureChangesetExists()
      .pipe(
        switchMap(() => this.http.post<Type>(`${this.environment.serverUrl}/api/types/${type.name.fullyQualifiedName}/annotations`,
          {
            changesetName: this.activeChangeset$.value.name,
            annotations: metadata,
          },
        )),
        tap(() => this.updateChangelogs()),
      );
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

  getAvailableChangesets(): Observable<Changeset[]> {
    return this.http.post<ChangesetResponse>(`${this.environment.serverUrl}/api/repository/changesets`, { packageIdentifier: fakePackageIdentifier })
      .pipe(map(response => response.changesets));
  }

  isCustomChangesetSelected(): boolean {
    return this.activeChangeset$.value.name !== defaultChangesetName;
  }

  rename(): Observable<void> {
    return this.openNameDialog(true, changesetName => this.updateChangeset(changesetName)).pipe(mapTo(void 0));
  }

  selectDefaultChangeset() {
    this.setActiveChangeset(defaultChangesetName).subscribe();
  }

  private updateChangelogs() {
    const availableChangesets$ = this.getAvailableChangesets().pipe(share());

    availableChangesets$.pipe(
      map(changesets => {
        return changesets.find(changeset => changeset.isActive)!!;
      }),
    ).subscribe(value => this.activeChangeset$.next(value));

    availableChangesets$
      .subscribe(value => this.availableChangesets$.next(value));
  }

  private updateChangeset(changesetName: string): Observable<void> {
    return this.http.put<void>(`${this.environment.serverUrl}/api/repository/changeset/update`, {
      packageIdentifier: fakePackageIdentifier,
      changesetName: this.activeChangeset$.value.name,
      newChangesetName: changesetName,
    }).pipe(
      tap(() => this.activeChangeset$.next({ name: changesetName, isActive: true })),
      tap(() => this.updateChangelogs()),
    );
  }

  private sanitizeChangesetName(name: string): string {
    return name.replaceAll(' ', '-').replace(/\W/g, '').toLowerCase();
  }
}

// TODO Make a separate type for the Changeset the consumers care about which does not include e.g. isActive
export interface Changeset {
  name: string;
  isActive: boolean;
}

export interface ChangesetResponse {
  changesets: Changeset[];
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

export interface CreateChangesetRequest {
  changesetName: string;
  packageIdentifier: any;
}

export interface AddChangesToChangesetRequest {
  changesetName: string;
  packageIdentifier: any;
  edits: VersionedSource[];
}

export interface FinalizeChangesetRequest {
  changesetName: string;
  packageIdentifier: any;
}

export interface FinalizeChangesetResponse {
  link: string | null;
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

export interface SchemaSubmissionResult extends PartialSchema {
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

