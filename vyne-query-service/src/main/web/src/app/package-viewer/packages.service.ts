import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from 'src/environments/environment';
import { ParsedSource, PartialSchema, Schema } from '../services/schema';

@Injectable({
  providedIn: 'root'
})
export class PackagesService {

  constructor(private readonly httpClient: HttpClient) {
  }

  loadPackage(packageUri: string): Observable<ParsedPackage> {
    return this.httpClient.get<ParsedPackage>(`${environment.serverUrl}/api/packages/${packageUri}`)
  }

  listPackages(): Observable<SourcePackageDescription[]> {
    return this.httpClient.get<SourcePackageDescription[]>(`${environment.serverUrl}/api/packages`)
  }

  getPartialSchemaForPackage(packageUri: string): Observable<PartialSchema> {
    return this.httpClient.get<PartialSchema>(`${environment.serverUrl}/api/packages/${packageUri}/schema`)
  }
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

export interface SourcePackageDescription {
  identifier: PackageIdentifier;
  health: PublisherHealth;
  sourceCount: number;
  warningCount: number;
  errorCount: number;

  uriPath: string;

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
}

export type UnversionedPackageIdentifier = string;
