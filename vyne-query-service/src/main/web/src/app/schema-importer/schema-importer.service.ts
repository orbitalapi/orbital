import { Injectable } from '@angular/core';
import { VyneServicesModule } from '../services/vyne-services.module';
import { HttpClient } from '@angular/common/http';
import { ConvertSchemaEvent, FileSystemPackageSpec, GitRepositoryConfig } from './schema-importer.models';
import { environment } from '../../environments/environment';
import { Observable } from 'rxjs/internal/Observable';
import { SchemaSubmissionResult } from '../services/types.service';
import { PartialSchema } from '../services/schema';
import { PackageIdentifier, PackagesService, SourcePackageDescription } from '../package-viewer/packages.service';
import { switchMap } from 'rxjs/operators';

@Injectable({
  providedIn: VyneServicesModule,
})
export class SchemaImporterService {
  editablePackage$: Observable<SourcePackageDescription | null> = this.packagesService.getEditablePackage();

  constructor(private httpClient: HttpClient, private packagesService: PackagesService) {
  }

  convertSchema(event: ConvertSchemaEvent): Observable<SchemaSubmissionResult> {
    return this.httpClient.post<SchemaSubmissionResult>(`${environment.serverUrl}/api/schemas/import?validateOnly=true`, {
      format: event.schemaType,
      options: event.options,
    } as SchemaConversionRequest);
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

  addNewGitRepository(request: GitRepositoryConfig): Observable<any> {
    return this.httpClient.post<any>(`${environment.serverUrl}/api/repositories/git`, request)
  }

  addNewFileRepository(request: FileSystemPackageSpec): Observable<any> {
    return this.httpClient.post<any>(`${environment.serverUrl}/api/repositories/file`, request)
  }


  removeRepository(packageDescription: SourcePackageDescription): Observable<any> {
    return this.httpClient.delete<any>(`${environment.serverUrl}/api/packages/${packageDescription.identifier.uriSafeId}`)
  }
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
