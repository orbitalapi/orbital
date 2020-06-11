import {Component, Input} from '@angular/core';
import {InstanceLike, typeName} from '../object-view/object-view.component';
import {Fact, Query, QueryMode, QueryResult, QueryService, ResultMode} from '../services/query.service';

@Component({
  selector: 'app-inline-query-runner',
  styleUrls: ['./inline-query-runner.component.scss'],
  template: `
    <div class="container">
      <mat-progress-bar mode="indeterminate" color="accent" *ngIf="loading"></mat-progress-bar>
      <mat-expansion-panel [(expanded)]="expanded">
        <mat-expansion-panel-header>
          <mat-panel-title>{{ targetType }}</mat-panel-title>
          <mat-panel-description>
            <button mat-stroked-button (click)="executeQueryClicked($event)">Run</button>
          </mat-panel-description>
        </mat-expansion-panel-header>

        <div *ngIf="queryResult">
          <query-result-container [result]="queryResult"></query-result-container>
        </div>
      </mat-expansion-panel>
    </div>

  `,

})
export class InlineQueryRunnerComponent {

  constructor(private queryService: QueryService) {
  }

  @Input()
  facts: InstanceLike[];

  @Input()
  targetType: string;

  expanded = false;

  loading = false;

  queryResult: QueryResult;

  executeQueryClicked($event) {
    $event.stopImmediatePropagation();
    this.expanded = true;
    this.loading = true;

    const query = new Query(
      [this.targetType],
      this.facts.map(fact => {
        return new Fact(typeName(fact), fact.value);
      }),
      QueryMode.DISCOVER,
      ResultMode.VERBOSE
    );

    this.queryService.submitQuery(query)
      .subscribe(result => {
        this.queryResult = result;
        this.loading = false;
      });
  }
}
