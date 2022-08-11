import { Injectable } from '@angular/core';
import { UnversionedPackageIdentifier } from '../package-viewer/packages.service';
import { Metadata, QualifiedName, QualifiedNameAsString } from '../services/schema';
import { HttpClient } from '@angular/common/http';
import {environment} from 'src/environments/environment';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class ChangelogService {

  constructor(private httpClient:HttpClient) {
  }

  getChangelog():Observable<ChangeLogEntry[]> {
    return this.httpClient.get<ChangeLogEntry[]>(`${environment.queryServiceUrl}/api/changelog`)
  }
}

export interface ChangeLogEntry {
  timestamp: string;
  affectedPackages: UnversionedPackageIdentifier[];
  diffs: ChangeLogDiffEntry[];
}

export interface ChangeLogDiffEntry {
  displayName: string;
  kind: DiffKind;
  schemaMember: QualifiedNameAsString; // QualifiedName
  children: ChangeLogDiffEntry[];
  oldDetails: ChangeLogDiffEntryDetails | null;
  newDetails: ChangeLogDiffEntryDetails | null;
}

export type ChangeLogDiffEntryDetails = QualifiedName | ParameterDiff[] | Metadata[];

export type DiffKind =
  'TypeAdded' |
  'TypeRemoved' |
  'ModelAdded' |
  'ModelRemoved' |
  'ModelChanged' |
  'DocumentationChanged' |
  'FieldAddedToModel' |
  'FieldRemovedFromModel' |
  'ServiceAdded' |
  'ServiceRemoved' |
  'ServiceChanged' |
  'OperationAdded' |
  'OperationRemoved' |
  'OperationMetadataChanged' |
  'OperationParametersChanged' |
  'OperationReturnValueChanged';

export interface ParameterDiff {
  name: string | null;
  type: QualifiedName;
}
