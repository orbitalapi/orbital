import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from 'src/environments/environment';
import { ParsedSource } from '../services/schema';

@Injectable({
  providedIn: 'root'
})
export class PackagesService {

  constructor(private readonly httpClient: HttpClient) {
  }

  loadPackage(packageUri: string): Observable<ParsedPackage> {
    return this.httpClient.get<ParsedPackage>(`${environment.queryServiceUrl}/api/schema/packages/${packageUri}`)
  }

  listPackages(): Observable<SourcePackageDescription[]> {
    // TODO  :Need to actually wire up this service.
    return this.httpClient.get<SourcePackageDescription[]>(`${environment.queryServiceUrl}/api/schema/packages`)
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
