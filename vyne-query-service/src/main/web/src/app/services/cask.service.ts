import {Injectable} from '@angular/core';
import {VyneServicesModule} from './vyne-services.module';
import {HttpClient} from '@angular/common/http';
import {Observable, of} from 'rxjs';
import {environment} from '../../environments/environment';

@Injectable({
  providedIn: VyneServicesModule
})
export class CaskService {
  constructor(private http: HttpClient) {

  }

  publishToCask(caskRequestUrl: string, content: string): Observable<any> {
    return this.http.post<any>(caskRequestUrl, content);
  }

  getCasks(): Observable<CaskConfigRecord[]> {
    return this.http.get<any>(`${environment.queryServiceUrl}/api/casks`);
  }

  getCaskDetais(tableName: string): Observable<CaskConfigDetails> {
    return this.http.get<any>(`${environment.queryServiceUrl}/api/casks/${tableName}/details`);
  }

  deleteCask(tableName: string): Observable<CaskConfigRecord[]> {
    return this.http.delete<any>(`${environment.queryServiceUrl}/api/casks/${tableName}`);
  }

  clearCask(tableName: string): Observable<CaskConfigRecord[]> {
    return this.http.put<any>(`${environment.queryServiceUrl}/api/casks/${tableName}`, null);
  }
}

export interface CaskConfigRecord {
  tableName: string;
  qualifiedTypeName: string;
  versionHash: string;
  sourceSchemaIds: string[];
  sources: string[];
  deltaAgainstTableName: string;
  insertedAt: string;
  details: CaskConfigDetails;
}

export interface CaskConfigDetails {
  recordsNumber: number;
}
