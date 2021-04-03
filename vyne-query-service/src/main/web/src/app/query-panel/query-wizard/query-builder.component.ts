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
import {DownloadFileType} from '../result-display/result-container.component';

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
          *ngIf="resultType"
          (instanceSelected)="onInstanceSelected($event)"
          [instances$]="results$"
          [type]="resultType"
          (downloadClicked)="downloadQueryHistory($event.format)"
        ></app-tabbed-results-view>
        <!--                  <mat-card *ngIf="lastQueryResult" style="width: 100%">-->
        <!--                    <mat-card-header>-->
        <!--                      <mat-card-title>Results</mat-card-title>-->
        <!--                    </mat-card-header>-->
        <!--                    <mat-card-content>-->
        <!--                      <query-result-container [result]="lastQueryResult"-->
        <!--                                              (instanceSelected)="onInstanceSelected($event)"></query-result-container>-->
        <!--                    </mat-card-content>-->
        <!--                  </mat-card>-->
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

  @Output()
  instanceSelected = new EventEmitter<QueryResultInstanceSelectedEvent>();

  onInstanceSelected($event: QueryResultInstanceSelectedEvent) {
    this.instanceSelected.emit($event);
  }


  constructor(private queryService: QueryService,
              private typeService: TypesService) {
    this.typeService.getTypes()
      .subscribe(schema => this.schema = schema);
  }


  submitQuery(query: Query) {
    this.results$ = new Subject<InstanceLike>();
    this.loading = true;
    this.resultType = null;
    this.queryService.submitQuery(query, nanoid())
      .subscribe(result => {
          if (!isNullOrUndefined(result.typeName)) {
            this.resultType = findType(this.schema, result.typeName.parameterizedName);
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

  downloadQueryHistory(format: DownloadFileType) {
    console.error('Download not implemented in query builder yet');
  }
}
