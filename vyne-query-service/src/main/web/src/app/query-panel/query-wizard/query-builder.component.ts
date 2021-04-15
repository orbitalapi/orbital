import {Component, EventEmitter, OnInit, Output} from '@angular/core';
import {FailedSearchResponse, Query, QueryService} from '../../services/query.service';
import {TypesService} from '../../services/types.service';
import {findType, InstanceLike, Schema, Type} from '../../services/schema';
import {nanoid} from 'nanoid';
import {HttpErrorResponse} from '@angular/common/http';
import {QueryFailure} from './query-wizard.component';
import {Subject} from 'rxjs';
import {RunningQueryStatus} from '../../services/active-queries-notification-service';
import {isNullOrUndefined} from 'util';
import {QueryResultInstanceSelectedEvent} from '../result-display/BaseQueryResultComponent';
import {ExportFileService, ExportFormat} from '../../services/export.file.service';

@Component({
  selector: 'app-query-builder',
  template: `
    <div class="query-wizard-container">
      <query-wizard
        (executeQuery)="submitQuery($event)"
      ></query-wizard>
      <div class="results-container">
        <mat-spinner *ngIf="loading" [diameter]=40></mat-spinner>

        <app-tabbed-results-view
          (instanceSelected)="onInstanceSelected($event)"
          [instances$]="results$"
          [type]="resultType"
          (downloadClicked)="downloadQueryHistory($event.format)"
        ></app-tabbed-results-view>
      </div>
    </div>
  `,
  styleUrls: ['./query-builder.component.scss']
})
export class QueryBuilderComponent {
  private schema: Schema;

  resultType: Type | null = null;
  queryStatus: RunningQueryStatus | null = null;
  results$: Subject<InstanceLike>;
  loading = false;
  failure: FailedSearchResponse | null = null;
  queryClientId: string | null = null;

  @Output()
  instanceSelected = new EventEmitter<QueryResultInstanceSelectedEvent>();

  onInstanceSelected($event: QueryResultInstanceSelectedEvent) {
    this.instanceSelected.emit($event);
  }


  constructor(private queryService: QueryService,
              private typeService: TypesService,
              private fileService: ExportFileService) {
    this.typeService.getTypes()
      .subscribe(schema => this.schema = schema);
  }


  submitQuery(query: Query) {
    this.results$ = new Subject<InstanceLike>();
    this.loading = true;
    this.resultType = null;
    this.queryClientId = nanoid();
    this.queryService.submitQuery(query, this.queryClientId)
      .subscribe(result => {
          if (!isNullOrUndefined(result.typeName)) {
            this.resultType = findType(this.schema, result.typeName);
          }
          this.results$.next(result.value);
        },
        error => {
          this.loading = false;
          const errorResponse = error as HttpErrorResponse;
          const failure = errorResponse.error as FailedSearchResponse;
          this.failure = failure;
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
