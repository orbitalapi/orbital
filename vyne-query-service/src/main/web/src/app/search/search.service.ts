import {Metadata, QualifiedName, ServiceKind} from '../services/schema';
import {Inject, Injectable} from '@angular/core';
import { Observable } from 'rxjs';
import { HttpClient } from '@angular/common/http';
import {ENVIRONMENT, Environment} from "../services/environment";


export interface SearchResult {
  qualifiedName: QualifiedName;
  typeDoc: string | null;
  matches: SearchMatch[];
  memberType: SearchEntryType;
  consumers: QualifiedName[];
  producers: QualifiedName[];
  metadata: Metadata[];
  matchedFieldName?: string;

  serviceKind?:ServiceKind
  typeKind?: 'Type' | 'Model';
  primitiveType?: QualifiedName;
}

export interface ExpendableProducersConsumers {
  consumersExpanded: boolean;
  producersExpanded: boolean;
}

export type ExpandableSearchResult = SearchResult & ExpendableProducersConsumers;

export type SearchEntryType = 'TYPE' | 'ATTRIBUTE' | 'POLICY' | 'SERVICE' | 'OPERATION' | 'UNKNOWN';

export interface SearchMatch {
  field: SearchField;
  highlightedMatch: string;
}

export type SearchField = 'QUALIFIED_NAME' | 'NAME' | 'TYPEDOC';

@Injectable({
  providedIn: 'root'
})
export class SearchService {

  constructor(private httpClient: HttpClient, @Inject(ENVIRONMENT) private environment: Environment) {
  }

  search(term: string): Observable<SearchResult[]> {
    const encodedTerm = encodeURIComponent(term);
    return this.httpClient.get<SearchResult[]>(`${this.environment.serverUrl}/api/search?query=${encodedTerm}`);
  }
}
