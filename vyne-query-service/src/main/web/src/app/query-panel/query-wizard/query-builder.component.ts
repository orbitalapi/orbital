import { Component, EventEmitter, Output } from '@angular/core';
import { Query, QueryProfileData, QueryService, ResponseStatus } from '../../services/query.service';
import { TypesService } from '../../services/types.service';
import { findType, InstanceLike, Schema, Type } from '../../services/schema';
import { nanoid } from 'nanoid';
import { HttpErrorResponse } from '@angular/common/http';
import { RunningQueryStatus } from '../../services/active-queries-notification-service';
import { isNullOrUndefined } from 'util';
import { QueryResultInstanceSelectedEvent } from '../result-display/BaseQueryResultComponent';
import { Observable, ReplaySubject } from 'rxjs/index';
import { FailedSearchResponse } from '../../services/models';
import { ExportFormat, ResultsDownloadService } from 'src/app/results-download/results-download.service';

@Component({
  selector: 'app-query-builder',
  template: `
    <as-split direction="horizontal" unit="percent">
      <as-split-area>
        <div class="query-wizard-container">
          <query-wizard
            (executeQuery)="submitQuery($event)"
          ></query-wizard>
          <div class="results-container">
            <mat-spinner *ngIf="loading" [diameter]=40></mat-spinner>
            <app-error-panel *ngIf="failure" [queryResult]="failure"></app-error-panel>
            <app-tabbed-results-view
              [profileData$]="queryProfileData$"
              (instanceSelected)="onInstanceSelected($event)"
              [instances$]="results$"
              [type]="resultType"
              (downloadClicked)="downloadQueryHistory($event.format)"
            ></app-tabbed-results-view>
          </div>
        </div>
      </as-split-area>
      <as-split-area *ngIf="valuePanelVisible" size="25">
        <app-typed-instance-panel-container
          [queryResultSelectedEvent$]="instanceSelected"
          (close)="valuePanelVisible = false"
        ></app-typed-instance-panel-container>
      </as-split-area>
    </as-split>
  `,
  styleUrls: ['./query-builder.component.scss']
})
export class QueryBuilderComponent {
  private schema: Schema;

  resultType: Type | null = null;
  queryStatus: RunningQueryStatus | null = null;
  results$: ReplaySubject<InstanceLike>;
  loading = false;
  failure: FailedSearchResponse | null = null;
  queryClientId: string | null = null;

  @Output()
  instanceSelected = new EventEmitter<QueryResultInstanceSelectedEvent>();
  queryProfileData$: Observable<QueryProfileData>;
  valuePanelVisible: boolean = false;

  onInstanceSelected($event: QueryResultInstanceSelectedEvent) {
    this.instanceSelected.emit($event);
    this.valuePanelVisible = true;
  }


  constructor(private queryService: QueryService,
              private typeService: TypesService,
              private fileService: ResultsDownloadService) {
    this.typeService.getTypes()
      .subscribe(schema => this.schema = schema);
  }


  submitQuery(query: Query) {
    this.results$ = new ReplaySubject<InstanceLike>(500);
    this.loading = true;
    this.resultType = null;
    this.failure = null;
    this.queryClientId = nanoid();
    this.queryService.submitQuery(query, this.queryClientId)
      .subscribe(result => {
          if (!isNullOrUndefined(result.typeName)) {
            this.resultType = findType(this.schema, result.typeName);
          }
          this.results$.next(result);
          this.queryProfileData$ = this.queryService.getQueryProfileFromClientId(this.queryClientId);
        },
        error => {
          this.loading = false;
          const errorResponse = error as HttpErrorResponse;
          const errorMessage = errorResponse.error.message;
          this.failure = {
            queryResponseId: null,
            clientQueryId: null,
            responseStatus: ResponseStatus.ERROR,
            remoteCalls: [],
            message: errorMessage
          };
        },
        () => {
          this.loading = false;
        });
  }

  downloadQueryHistory(fileType: ExportFormat) {
    if (fileType === ExportFormat.TEST_CASE) {
      this.fileService.downloadRegressionPackZipFileFromClientId(this.queryClientId, fileType);
    } else {
      this.fileService.downloadQueryHistoryFromClientQueryId(this.queryClientId, fileType);
    }
  }
}
