import {Injectable} from '@angular/core';
import {VyneServicesModule} from '../services/vyne-services.module';
import {HttpClient, HttpParams} from '@angular/common/http';
import {
  CreateEmptyProjectRequest,
  FileSystemPackageSpec,
  GitRepositoryConfig,
  LoadablePackageType
} from './project-import.models';
import {ConvertSchemaEvent} from '../data-source-import/data-source-import.models';
import {environment} from '../../environments/environment';
import {Observable} from 'rxjs/internal/Observable';
import {SavedQuery, SchemaSubmissionResult} from '../services/types.service';
import {PartialSchema, QualifiedName, SchemaMemberKind, VersionedSource} from '../services/schema';
import {PackageIdentifier, PackagesService, SourcePackageDescription} from '../package-viewer/packages.service';
import {switchMap, tap} from 'rxjs/operators';
import {WorkspacesService} from "../services/workspaces.service";
import {AppConfig, AppInfoService} from "../services/app-info.service";
import {TuiAlertService} from "@taiga-ui/core";
import {showAlertForMessages} from "../alert-with-dismiss/alert-with-dismiss.component";

@Injectable({
  providedIn: VyneServicesModule,
})
export class SchemaImporterService {
  editablePackage$: Observable<SourcePackageDescription | null> = this.packagesService.getEditablePackage();
  private appConfig: AppConfig;

  constructor(private httpClient: HttpClient,
              private packagesService: PackagesService,
              private workspaceService: WorkspacesService,
              private configService: AppInfoService,
              private alerts:TuiAlertService,

  ) {
    configService.getConfig().subscribe(next => this.appConfig = next)

  }

  convertSchema(event: ConvertSchemaEvent): Observable<SchemaSubmissionResult> {
    return this.httpClient.post<SchemaSubmissionResult>(`${environment.serverUrl}/api/schemas/import?validateOnly=true`, {
      format: event.schemaType,
      options: event.options,
      packageIdentifier: event.packageIdentifier
    } as SchemaConversionRequest);
  }

  submitSchemaEditOperation(edit: SchemaEdit): Observable<SchemaSubmissionResult> {
    return this.httpClient.post<SchemaSubmissionResult>(`${environment.serverUrl}/api/schemas/edits`, edit)
      .pipe(
        tap(next => {
          showAlertForMessages(next, "WARNING", this.alerts, null)
            .subscribe()
        })
      )
  }

  submitEditedSchema(schema: PartialSchema): Observable<any> {
    return this.editablePackage$.pipe(switchMap(editablePackage => {
      return this.httpClient.post(`${environment.serverUrl}/api/schemas/edit?packageIdentifier=${editablePackage.identifier.id}`, {
        types: schema.types,
        services: schema.services,
      });
    }));
  }

  testGitConnection(request: GitConnectionTestRequest): Observable<GitConnectionTestResult> {
    return this.httpClient.post<GitConnectionTestResult>(`${environment.serverUrl}/api/repositories/git?test`, request)
  }

  testFileConnection(request: FileRepositoryTestRequest): Observable<FileRepositoryTestResponse> {
    return this.httpClient.post<FileRepositoryTestResponse>(`${environment.serverUrl}/api/repositories/file?test`, request)

  }

  addNewGitRepository(request: GitRepositoryConfig): Observable<ModifyWorkspaceResponse> {
    if (this.appConfig.featureToggles.workspacesEnabled) {

    } else {
      return this.httpClient.post<ModifyWorkspaceResponse>(`${environment.serverUrl}/api/repositories/git`, request)
    }
  }

  uploadProject(uriSafeProjectId: string = null, parameters: {format: LoadablePackageType, defaultNamespace?: string, serviceBasePath?: string}, payload: string): Observable<ModifyWorkspaceResponse> {
    const params = Object.keys(parameters).reduce(
      (httpParams, key) => {
        return parameters[key] !== undefined ? httpParams.append(key, parameters[key]) : httpParams
      },
      new HttpParams()
    );
    return this.httpClient.post<ModifyWorkspaceResponse>(`${environment.serverUrl}/api/workspace/projects/${uriSafeProjectId}`, payload, {params});
  }

  addNewFileRepository(request: FileSystemPackageSpec): Observable<ModifyWorkspaceResponse> {
    return this.httpClient.post<ModifyWorkspaceResponse>(`${environment.serverUrl}/api/repositories/file`, request)
  }

  createEmptyProject(request: CreateEmptyProjectRequest): Observable<ModifyWorkspaceResponse> {
    return this.httpClient.post<ModifyWorkspaceResponse>(`${environment.serverUrl}/api/repositories/new`, request)
  }

  removeRepository(packageDescription: SourcePackageDescription): Observable<any> {
    return this.httpClient.delete<any>(`${environment.serverUrl}/api/packages/${packageDescription.identifier.uriSafeId}`)
  }

  // Util function
  getQueryStateFromEditResult(editResult: SchemaSubmissionResult, fileName: string): SavedQueryWithSource {
    const updatedFileSource = editResult.sourcePackage.sources.find(source => source.name === fileName)
    const updatedState: SavedQueryWithSource = {
      savedQuery: editResult.queries[0],
      sourceFile: updatedFileSource
    }
    return updatedState
  }

  // Util function
  extractFilenameFromVersionedSource(versionedSource: VersionedSource): string {
    // the filename on the saved query is in the form
    // [demo.vyne/films-demo/0.1.0]/a3.taxi
    // So just grab the value after the ]/
    return versionedSource.name.startsWith('[') ?
      versionedSource.name.split(']/')[1] :
      versionedSource.name;
  }
}

export interface SchemaEdit {
  packageIdentifier: PackageIdentifier
  edits: SchemaEditOperation[]
  dryRun: boolean
}

export type EditKind =
  'CreateOrReplace' |
  'CreateOrReplaceQuery' |
  'ChangeFieldType' |
  'AddOrRemoveFieldAnnotation' |
  'ChangeOperationReturnType' |
  'ChangeOperationParameterType' |
  'EditMemberDescription' |
  'ChangeInheritedType' |
  'AddOrRemoveHttpEndpointAnnotation' |
  'AddOrRemoveWebsocketEndpointAnnotation'

export interface SchemaEditOperation {
  editKind: EditKind
  loadExistingState?: boolean
}

export interface CreateOrReplaceQuery extends SchemaEditOperation {
  editKind: 'CreateOrReplaceQuery'
  sources: VersionedSource[]
}
export interface CreateOrReplaceSource extends SchemaEditOperation {
  editKind: 'CreateOrReplace'
  sources: VersionedSource[]
}

export interface ChangeFieldTypeEvent extends SchemaEditOperation {
  editKind: 'ChangeFieldType'
  symbol: QualifiedName
  fieldName: string
  newReturnType: QualifiedName
  nullable: boolean
}

export interface AddOrRemoveFieldAnnotationEvent extends SchemaEditOperation {
  editKind: 'AddOrRemoveFieldAnnotation'
  symbol: QualifiedName
  fieldName: string,
  annotationName: string,
  operation: 'Add' | 'Remove'
}

export interface ChangeOperationReturnTypeEvent extends SchemaEditOperation {
  editKind: 'ChangeOperationReturnType'
  symbol: QualifiedName
  newReturnType: QualifiedName
}

export interface ChangeOperationParameterTypeEvent extends SchemaEditOperation {
  editKind: 'ChangeOperationParameterType'
  symbol: QualifiedName
  parameterName: string
  newType: QualifiedName
}

export interface EditMemberDescriptionEvent extends SchemaEditOperation {
  editKind: 'EditMemberDescription'
  memberKind: SchemaMemberKind
  memberName: string | null
  symbol: QualifiedName
  typeDoc: string
}

export interface ChangeInheritedTypeEvent extends SchemaEditOperation {
  editKind: 'ChangeInheritedType'
  symbol: QualifiedName
  newBaseType: QualifiedName
}

interface AddOrRemoveEndpointAnnotationBaseEvent extends SchemaEditOperation {
  queryQualifiedName: QualifiedName
  path: string
  operation: 'Add' | 'Remove'
}

export interface AddOrRemoveHttpEndpointAnnotationEvent extends AddOrRemoveEndpointAnnotationBaseEvent {
  editKind: 'AddOrRemoveHttpEndpointAnnotation'
  method: HttpMethod,
}

export interface AddOrRemoveWebsocketEndpointAnnotationEvent extends AddOrRemoveEndpointAnnotationBaseEvent {
  editKind: 'AddOrRemoveWebsocketEndpointAnnotation'
}

export interface SchemaConversionRequest {
  format: string;
  options: any;
}

export interface GitConnectionTestRequest {
  uri: string;
}

export interface FileRepositoryTestRequest {
  path: string;
}

export interface FileRepositoryTestResponse {
  path: string;
  exists: boolean;
  identifier: PackageIdentifier | null;
  errorMessage?: string;
}

export interface GitConnectionTestResult {
  successful: boolean;
  errorMessage: string;
  branchNames: string[];
  defaultBranch: string;
}

export interface GitValidateFilePathRequest {
  repositoryUrl: string;
  filePath: string;
}

export type HttpMethod = 'GET' | 'POST' | 'PUT' | 'DELETE'

export interface SavedQueryWithSource {
  savedQuery: SavedQuery
  sourceFile: VersionedSource
}

export type ModifyWorkspaceResponse = {
  status: ModifyProjectResponseStatus
  message?: string
}

export type ModifyProjectResponseStatus = 'Ok' | 'Warning' | 'Failed'

/**
 * Converts a SavedQuery to a SavedQueryWithSource, or something resembling it.
 * Generally, this shouldn't be constructed like this, as there are subltle situations where
 * the source in a SavedQuery is different from the source that's in the VersionedSource that contains it.
 *
 * However, sometimes we don't have access to the versioned source - like when we're recovering state
 * from localStorage.
 *
 * A better option in these situations is to load the state from the server.
 * I wish that were the world we lived in. But it isn't. you could make it that kind of world, by
 * building that type of capability. Be the change you want to see.
 *
 * @param savedQuery
 */
export function dangerouslyConvertToSavedQueryWithSource(savedQuery: SavedQuery):SavedQueryWithSource {

  const savedQuerySource = savedQuery.sources[0]
  // name is stored in form of [demo.vyne/films-demo/0.1.0]/ass1.taxi:0.0.0
  const cleanedFilename = savedQuerySource.name.split(']/')[1]
  return {
    savedQuery: savedQuery,
    sourceFile:  {
      ...savedQuerySource,
      name: cleanedFilename
    }
  }
}
