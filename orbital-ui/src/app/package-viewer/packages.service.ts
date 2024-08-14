import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import {Observable, of} from 'rxjs';
import { environment } from 'src/environments/environment';
import {ParsedSource, PartialSchema, VersionedSource} from '../services/schema';
import { map } from 'rxjs/operators';
import { FileSystemPackageSpec, GitRepositoryConfig } from 'src/app/project-import/project-import.models';

@Injectable({
  providedIn: 'root',
})
export class PackagesService {

  constructor(private readonly httpClient: HttpClient) {
  }

  loadProjectLoadersWithErrors():Observable<ProjectLoaderWithStatus[]> {
    return this.httpClient.get<ProjectLoaderWithStatus[]>(`${environment.serverUrl}/api/projectLoaders/unhealthy`);
  }

  loadWorkspaceConfigStatus():Observable<LoaderStatus> {
    return this.httpClient.get<LoaderStatus>(`${environment.serverUrl}/api/workspace/status`);
  }

  loadPackage(packageUri: string): Observable<PackageWithDescription> {
    return this.httpClient.get<PackageWithDescription>(`${environment.serverUrl}/api/packages/${packageUri}`);
  }

  listPackages(): Observable<SourcePackageDescription[]> {
    return this.httpClient.get<SourcePackageDescription[]>(`${environment.serverUrl}/api/packages`);
  }

  getPartialSchemaForPackage(packageUri: string): Observable<PartialSchema> {
    return this.httpClient.get<PartialSchema>(`${environment.serverUrl}/api/packages/${packageUri}/schema`);
  }

  /**
   * @deprecated We no longer have a concept
   * of an "editable package" - there are multiple.
   */
  getEditablePackage(): Observable<SourcePackageDescription> {
    return of(null)
  }
}


export interface PackageWithDescription {
  parsedPackage: ParsedPackage;
  description: SourcePackageDescription;
}

export interface ParsedPackage {
  metadata: PackageMetadata;
  sources: ParsedSource[];
  additionalSources: {[index: string]: VersionedSource[]}
  isValid: boolean;
  identifier: PackageIdentifier;
  sourcesWithErrors: ParsedSource[];
  readme: string;
}

export interface PackageMetadata {
  identifier: PackageIdentifier;
  submissionDate: string;
  dependencies: PackageIdentifier[];
}

export type PublisherType = 'GitRepo' | 'FileSystem' | 'Pushed';

export interface SourcePackageDescription {
  identifier: PackageIdentifier;
  health: PublisherHealth;
  sourceCount: number;
  warningCount: number;
  errorCount: number;
  uriPath: string;
  editable: boolean;
  publisherType: PublisherType;
  submissionDate: Date;
  packageConfig: GitRepositoryConfig | FileSystemPackageSpec;
}

export interface PublisherHealth {
  status: PublisherHealthStatus;
  message: string | null;
  timestamp: string;
}

export type PublisherHealthStatus = 'Healthy' | 'Unhealthy' | 'Unknown';

export interface PackageIdentifier {
  organisation: string;
  name: string;
  version: string;

  id: string;
  unversionedId: UnversionedPackageIdentifier;
  uriSafeId?: string;
}

export type UnversionedPackageIdentifier = string;

export interface ProjectLoaderWithStatus {
  loaderDescription: string;
  status: LoaderStatus
}

export interface LoaderStatus {
  state: 'OK' | 'ERROR' | 'STARTING'
  message: string | null;
}
