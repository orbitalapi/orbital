import { Injectable, Injector } from '@angular/core';
import { ResultsDownloadModule } from 'src/app/results-download/results-download.module';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from 'src/environments/environment';
import { Type } from 'src/app/services/schema';
import { CsvOptions } from 'src/app/services/types.service';
import {
  RegressionPackFormat,
  TestSpecFormComponent,
  TestSpecFormResult
} from 'src/app/test-pack-module/test-spec-form.component';
import * as fileSaver from 'file-saver';
import { TuiDialogService } from '@taiga-ui/core';
import { PolymorpheusComponent } from '@taiga-ui/polymorpheus';

// Don't provide in root, as we need
// to depend on UI components that aren't imported in the root.
@Injectable({ providedIn: ResultsDownloadModule })
export class ResultsDownloadService {

  constructor(
    private http: HttpClient,
    private dialogService: TuiDialogService,
    private injector: Injector
  ) {
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

  downloadRegressionPack(id: string, regressionPackName: string, format: RegressionPackFormat, description?: string): Observable<ArrayBuffer> {
    return this.http.post(`${environment.serverUrl}/api/query/history/${id}/regressionPack`,
      { queryId: id, regressionPackName, format, description },
      { responseType: 'arraybuffer' }
    );
  }

  downloadRegressionPackFromClientId(id: string, regressionPackName: string, format: RegressionPackFormat, description?: string): Observable<ArrayBuffer> {
    return this.http.post(`${environment.serverUrl}/api/query/history/clientId/${id}/regressionPack`,
      { queryId: id, regressionPackName, format, description },
      { responseType: 'arraybuffer' }
    );
  }

  downloadRegressionPackFile(id: string, result: TestSpecFormResult) {
    this.downloadRegressionPack(id, result.name, result.format, result.description).subscribe(response => {
      this.saveRegressionPackResponse(response, result);
    });
  }

  downloadRegressionPackFileFromClientId(id: string, result: TestSpecFormResult) {
    this.downloadRegressionPackFromClientId(id, result.name, result.format, result.description).subscribe(response => {
      this.saveRegressionPackResponse(response, result);
    });
  }

  private saveRegressionPackResponse(response: ArrayBuffer, result: TestSpecFormResult) {
    if (result.format === 'Preflight') {
      const blob = new Blob([response], { type: 'text/markdown; charset=utf-8' });
      fileSaver.saveAs(blob, `${result.name}.spec.md`);
    } else {
      const blob = new Blob([response], { type: 'application/zip; charset=utf-8' });
      fileSaver.saveAs(blob, `${result.name}.zip`);
    }
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
      .subscribe(result => {
        if (result !== null) {
          this.downloadRegressionPackFile(queryId, result);
        }
      });
  }

  promptToDownloadTestCaseFromClientId(clientQueryId: string) {
    this.doPromptToDownloadTestCase()
      .subscribe(result => {
        if (result !== null) {
          this.downloadRegressionPackFileFromClientId(clientQueryId, result);
        }
      });
  }

  private doPromptToDownloadTestCase(): Observable<TestSpecFormResult | null> {
    return this.dialogService.open<TestSpecFormResult | null>(
      new PolymorpheusComponent(TestSpecFormComponent, this.injector),
      {
        size: 'm'
      }
    );
  }
}

export enum ExportFormat {JSON = 'JSON', CSV = 'CSV', TEST_CASE = 'ZIP', CUSTOM_FORMAT = 'CUSTOM'}
