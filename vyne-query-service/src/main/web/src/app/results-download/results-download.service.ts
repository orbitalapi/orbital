import { Injectable } from '@angular/core';
import { ResultsDownloadModule } from 'src/app/results-download/results-download.module';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from 'src/environments/environment';
import { Type } from 'src/app/services/schema';
import { CsvOptions } from 'src/app/services/types.service';
import { TestSpecFormComponent } from 'src/app/test-pack-module/test-spec-form.component';
import * as fileSaver from 'file-saver';
import { MatDialog } from '@angular/material/dialog';

// Don't provide in root, as we need
// to depend on UI components that aren't imported in the root.
@Injectable({ providedIn: ResultsDownloadModule })
export class ResultsDownloadService {

  constructor(private http: HttpClient, private dialogService: MatDialog) {
  }

  exportQueryHistoryFromClientQueryId(clientQueryId: string, type: ExportFormat): Observable<ArrayBuffer> {
    if (type === ExportFormat.CUSTOM_FORMAT) {
      return this.http
        .get(`${environment.serverUrl}/api/query/history/clientId/${clientQueryId}/export`, { responseType: 'arraybuffer' });
    } else {
      return this.http
        .get(`${environment.serverUrl}/api/query/history/clientId/${clientQueryId}/${type}/export`, { responseType: 'arraybuffer' });
    }
  }

  exportQueryHistory(id: string, type: ExportFormat): Observable<ArrayBuffer> {
    if (type === ExportFormat.CUSTOM_FORMAT) {
      return this.http.get(`${environment.serverUrl}/api/query/history/${id}/export`, { responseType: 'arraybuffer' });
    } else {
      return this.http.get(`${environment.serverUrl}/api/query/history/${id}/${type}/export`, { responseType: 'arraybuffer' });
    }
  }

  downloadQueryHistoryFromClientQueryId(clientQueryId: string, format: ExportFormat) {
    this.downloadQueryHistoryFromObservable(
      this.exportQueryHistoryFromClientQueryId(clientQueryId, format),
      format
    );
  }

  downloadQueryHistory(id: string, format: ExportFormat) {
    this.downloadQueryHistoryFromObservable(
      this.exportQueryHistory(id, format),
      format
    );

  }

  private downloadQueryHistoryFromObservable(content: Observable<ArrayBuffer>, type: ExportFormat) {
    content.subscribe(response => {
      const blob: Blob = new Blob([response], { type: `text/${type}; charset=utf-8` });
      fileSaver.saveAs(blob, `query-${new Date().getTime()}.${type}`);
    });
  }

  downloadRegressionPack(id: string, regressionPackName: string): Observable<ArrayBuffer> {
    return this.http.post(`${environment.serverUrl}/api/query/history/${id}/regressionPack`,
      { queryId: id, regressionPackName },
      { responseType: 'arraybuffer' }
    );
  }

  downloadRegressionPackFromClientId(id: string, regressionPackName: string): Observable<ArrayBuffer> {
    return this.http.post(`${environment.serverUrl}/api/query/history/clientId/${id}/regressionPack`,
      { queryId: id, regressionPackName },
      { responseType: 'arraybuffer' }
    );
  }

  downloadRegressionPackZipFile(id: string, regressionPackName: string) {
    this.downloadRegressionPack(id, regressionPackName).subscribe(response => {
      const blob: Blob = new Blob([response], { type: `application/zip; charset=utf-8` });
      fileSaver.saveAs(blob, `${regressionPackName}.zip`);
    });
  }

  downloadRegressionPackZipFileFromClientId(id: string, regressionPackName: string) {
    this.downloadRegressionPackFromClientId(id, regressionPackName).subscribe(response => {
      const blob: Blob = new Blob([response], { type: `application/zip; charset=utf-8` });
      fileSaver.saveAs(blob, `${regressionPackName}.zip`);
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
  };

  exportTestSpec(content: string, contentType: Type, csvOptions: CsvOptions, testSpecName: string): Observable<ArrayBuffer> {
    const nullValueParam = csvOptions.nullValueTag ? '&nullValue=' + csvOptions.nullValueTag : '';
    const ignoreContentParam = csvOptions.ignoreContentBefore ?
      '&ignoreContentBefore=' + encodeURIComponent(csvOptions.ignoreContentBefore)
      : '';
    const separator = encodeURIComponent(this.detectCsvDelimiter(content));
    return this.http.post(
      // eslint-disable-next-line max-len
      `${environment.serverUrl}/api/csv/downloadTypedParsedTestSpec?testSpecName=${testSpecName}&delimiter=${separator}&firstRecordAsHeader=${csvOptions.firstRecordAsHeader}${nullValueParam}${ignoreContentParam}&type=${contentType.name.fullyQualifiedName}`,
      content, { responseType: 'arraybuffer' });
  }

  exportParsedData(content: string, contentType: any, csvOptions: CsvOptions, isTypeIncluded: boolean): Observable<ArrayBuffer> {
    const nullValueParam = csvOptions.nullValueTag ? '&nullValue=' + csvOptions.nullValueTag : '';
    const ignoreContentParam = csvOptions.ignoreContentBefore ?
      '&ignoreContentBefore=' + encodeURIComponent(csvOptions.ignoreContentBefore)
      : '';
    const separator = encodeURIComponent(this.detectCsvDelimiter(content));
    if (isTypeIncluded) {
      return this.http.post(
        // eslint-disable-next-line max-len
        `${environment.serverUrl}/api/csv/downloadTypedParsed?delimiter=${separator}&firstRecordAsHeader=${csvOptions.firstRecordAsHeader}${nullValueParam}${ignoreContentParam}&type=${contentType.name.fullyQualifiedName}`,
        content, { responseType: 'arraybuffer' });
    } else {
      return this.http.post(
        // eslint-disable-next-line max-len
        `${environment.serverUrl}/api/csv/downloadParsed?delimiter=${separator}&firstRecordAsHeader=${csvOptions.firstRecordAsHeader}${nullValueParam}${ignoreContentParam}`,
        content, { responseType: 'arraybuffer' });
    }

  }

  promptToDownloadTestCase(queryId: string) {
    this.doPromptToDownloadTestCase()
      .subscribe(specName => {
        if (specName !== null) {
          this.downloadRegressionPackZipFile(queryId, specName);
        }
      });
  }

  promptToDownloadTestCaseFromClientId(clientQueryId: string) {
    this.doPromptToDownloadTestCase()
      .subscribe(specName => {
        if (specName !== null) {
          this.downloadRegressionPackZipFileFromClientId(clientQueryId, specName);
        }
      });
  }

  private doPromptToDownloadTestCase(): Observable<string> {
    const dialogRef = this.dialogService.open(TestSpecFormComponent, {
      width: '550px'
    });

    return dialogRef.afterClosed();
  }
}

export enum ExportFormat {JSON = 'JSON', CSV = 'CSV', TEST_CASE = 'ZIP', CUSTOM_FORMAT = 'CUSTOM'}