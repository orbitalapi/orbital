import {Injectable} from '@angular/core';
import {HttpClient, HttpResponse} from '@angular/common/http';
import {Observable} from 'rxjs';
import {environment} from 'src/environments/environment';
import {DownloadFileType} from '../query-panel/result-display/result-container.component';
import {ParsedCsvContent} from './types.service';


@Injectable({providedIn: 'root'})
export class ExportFileService {

  constructor(private http: HttpClient) {
  }

  exportQueryHistory(id: string, type: DownloadFileType): Observable<ArrayBuffer> {
    return this.http.get(`${environment.queryServiceUrl}/api/query/history/${id}/${type}/export`, {responseType: 'arraybuffer'});
  }

  exportParsedData(parsedContent: ParsedCsvContent, fileType: DownloadFileType): Observable<ArrayBuffer> {
    return this.http.post(`${environment.queryServiceUrl}/api/downloadParsedData/${fileType}`,
      parsedContent, {responseType: 'arraybuffer'});
  }
}
