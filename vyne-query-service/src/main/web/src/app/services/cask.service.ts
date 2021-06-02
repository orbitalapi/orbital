import {Injectable} from '@angular/core';
import {VyneServicesModule} from './vyne-services.module';
import {HttpClient, HttpResponse} from '@angular/common/http';
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

  getCaskDetails(tableName: string): Observable<CaskConfigDetails> {
    return this.http.get<any>(`${environment.queryServiceUrl}/api/casks/${tableName}/details`);
  }

  deleteCask(tableName: string, force: boolean): Observable<CaskConfigRecord> {
    return this.http.delete<any>(`${environment.queryServiceUrl}/api/casks/${tableName}?force=${force}`);
  }

  clearCask(tableName: string): Observable<HttpResponse<string>> {
    return this.http.put<any>(`${environment.queryServiceUrl}/api/casks/${tableName}`, null);
  }

  fetchCaskIngestionErrors(tableName: string, content: CaskIngestionErrorsRequestDto): Observable<CaskIngestionErrorDtoPage> {
    return this.http.post<CaskIngestionErrorDtoPage>(`${environment.queryServiceUrl}/api/casks/${tableName}/errors`, content);
  }

  downloadIngestedMessageUrl(caskMessageId: string) {
    return `${environment.queryServiceUrl}/api/casks/${caskMessageId}`;
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
  exposesType: boolean;
  details: CaskConfigDetails;
}

export interface CaskConfigDetails {
  recordsNumber: number;
  ingestionErrorsInLast24Hours: number;
  dependencies: string[];
}

export interface CaskIngestionErrorDto {
  caskMessageId: string;
  createdAt: string;
  fqn: string;
  error: string;
}

interface CaskIngestionErrorDtoPage {
  items: CaskIngestionErrorDto[];
  currentPage: number;
  totalItem: number;
  totalPages: number;
}

interface CaskIngestionErrorsRequestDto {
  pageNumber: number;
  pageSize: number;
  searchStart: string;
  searchEnd: string;
}
