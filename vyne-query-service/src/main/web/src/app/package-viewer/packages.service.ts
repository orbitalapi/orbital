import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from 'src/environments/environment';
import { ParsedSource, PartialSchema } from '../services/schema';
import { map } from 'rxjs/operators';
import { FileSystemPackageSpec, GitRepositoryConfig } from 'src/app/schema-importer/schema-importer.models';

@Injectable({
  providedIn: 'root',
})
export class PackagesService {

  constructor(private readonly httpClient: HttpClient) {
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

  getEditablePackage(): Observable<SourcePackageDescription> {
    return this.listPackages()
      .pipe(
        map((packages: SourcePackageDescription[]) => this.resolveEditablePackage(packages))
        // TODO : We should be caching this.
        // However, we need to get cache invalidation working, so that when
        // users add a repository via the UI, they can use it.
        // When caching is enabled, users are required to refresh after adding a repository.
        // :(
        // shareReplay(1),
      );
  }

  private resolveEditablePackage(packages: SourcePackageDescription[]): SourcePackageDescription {
    const editable = packages.filter(sourcePackage => sourcePackage.editable);
    if (editable.length === 0) {
      console.error('There are no editable packages configured - editing will fail');
      return null;
    } else if (editable.length > 1) {
      console.error('There are multiple editable packages configured - editing will fail');
      return null;
    }
    return editable[0];
  }
}


export interface PackageWithDescription {
  parsedPackage: ParsedPackage;
  description: SourcePackageDescription;
}

export interface ParsedPackage {
  metadata: PackageMetadata;
  sources: ParsedSource[];
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
