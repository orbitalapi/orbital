import {QualifiedName} from '../services/schema';
import {Injectable} from '@angular/core';
import {Observable} from 'rxjs';
import {HttpClient} from '@angular/common/http';
import {environment} from '../../environments/environment';
import {SearchModule} from './search.module';


export interface SearchResult {
  qualifiedName: QualifiedName;
  typeDoc: string | null;
  matches: SearchMatch[];
}

export interface SearchMatch {
  field: SearchField;
  highlightedMatch: string;
}

export type SearchField = 'QUALIFIED_NAME' | 'NAME' | 'TYPEDOC';

@Injectable({
  providedIn: 'root'
})
export class SearchService {

  constructor(private httpClient: HttpClient) {
  }

  search(term: string): Observable<SearchResult[]> {
    return this.httpClient.get<SearchResult[]>(`${environment.queryServiceUrl}/search?query=${term}`);
  }
}
