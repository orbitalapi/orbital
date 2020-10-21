import {Injectable} from '@angular/core';
import {HttpClient, HttpResponse} from '@angular/common/http';
import {Observable} from 'rxjs';
import {environment} from 'src/environments/environment';
import {DownloadFileType} from '../query-panel/result-display/result-container.component';
import {CsvOptions, ParsedCsvContent} from './types.service';
import {Type} from './schema';
import {QueryResult} from './query.service';
import * as fileSaver from 'file-saver';


@Injectable({providedIn: 'root'})
export class ExportFileService {

  constructor(private http: HttpClient) {
  }

  exportQueryHistory(id: string, type: DownloadFileType): Observable<ArrayBuffer> {
    return this.http.get(`${environment.queryServiceUrl}/api/query/history/${id}/${type}/export`, {responseType: 'arraybuffer'});
  }

  downloadQueryHistory(id: string, type: DownloadFileType) {
    this.exportQueryHistory(id, type).subscribe(response => {
      const blob: Blob = new Blob([response], {type: `text/${type}; charset=utf-8`});
      fileSaver.saveAs(blob, `query-${new Date().getTime()}.${type}`);
    });
  }


  public detectCsvDelimiter = (input: string) => {
    const separators = [',', ';', '|', '\t'];
    const idx = separators
      .map((separator) => input.indexOf(separator))
      .reduce((prev, cur) =>
        prev === -1 || (cur !== -1 && cur < prev) ? cur : prev
      );
    return (input[idx] || ',');
  }

  exportTestSpec(content: string, contentType: Type, csvOptions: CsvOptions, testSpecName: String): Observable<ArrayBuffer> {
    const nullValueParam = csvOptions.nullValueTag ? '&nullValue=' + csvOptions.nullValueTag : '';
    const ignoreContentParam = csvOptions.ignoreContentBefore ?
      '&ignoreContentBefore=' + encodeURIComponent(csvOptions.ignoreContentBefore)
      : '';
    const separator = encodeURIComponent(this.detectCsvDelimiter(content));
    return this.http.post(
      // tslint:disable-next-line:max-line-length
      `${environment.queryServiceUrl}/api/csv/downloadTypedParsedTestSpec?testSpecName=${testSpecName}&delimiter=${separator}&firstRecordAsHeader=${csvOptions.firstRecordAsHeader}${nullValueParam}${ignoreContentParam}&type=${contentType.name.fullyQualifiedName}`,
      content, {responseType: 'arraybuffer'});
  }

  exportParsedData(content: string, contentType: any, csvOptions: CsvOptions, isTypeIncluded: boolean)
    : Observable<ArrayBuffer> {
    const nullValueParam = csvOptions.nullValueTag ? '&nullValue=' + csvOptions.nullValueTag : '';
    const ignoreContentParam = csvOptions.ignoreContentBefore ?
      '&ignoreContentBefore=' + encodeURIComponent(csvOptions.ignoreContentBefore)
      : '';
    const separator = encodeURIComponent(this.detectCsvDelimiter(content));
    if (isTypeIncluded) {
      return this.http.post(
        // tslint:disable-next-line:max-line-length
        `${environment.queryServiceUrl}/api/csv/downloadTypedParsed?delimiter=${separator}&firstRecordAsHeader=${csvOptions.firstRecordAsHeader}${nullValueParam}${ignoreContentParam}&type=${contentType.name.fullyQualifiedName}`,
        content, {responseType: 'arraybuffer'});
    } else {
      return this.http.post(
        // tslint:disable-next-line:max-line-length
        `${environment.queryServiceUrl}/api/csv/downloadParsed?delimiter=${separator}&firstRecordAsHeader=${csvOptions.firstRecordAsHeader}${nullValueParam}${ignoreContentParam}`,
        content, {responseType: 'arraybuffer'});
    }

  }
}
