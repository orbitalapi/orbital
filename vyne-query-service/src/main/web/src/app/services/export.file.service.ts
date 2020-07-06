import {Injectable} from '@angular/core';
import {HttpClient, HttpResponse} from '@angular/common/http';
import {Observable} from 'rxjs';
import {environment} from 'src/environments/environment';
import {DownloadFileType} from '../query-wizard/result-display/result-container.component';


@Injectable({ providedIn: 'root' })
export class ExportFileService {

  constructor(private http: HttpClient) {}

  exportQueryHistory(id: string, type: DownloadFileType): Observable<ArrayBuffer> {
    return this.http.get(`${environment.queryServiceUrl}/api/query/history/${id}/${type}/export`, {responseType: 'arraybuffer'});
  }
}
