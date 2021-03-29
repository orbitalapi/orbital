import {Component, Input} from '@angular/core';
import {Fact, Query, QueryMode, QueryResult, QueryService, ResultMode} from '../services/query.service';
import {QualifiedName, InstanceLike, getTypeName} from '../services/schema';

@Component({
  selector: 'app-inline-query-runner',
  styleUrls: ['./inline-query-runner.component.scss'],
  template: `
    <div class="container">
          <mat-progress-bar mode="indeterminate" color="accent" *ngIf="loading"></mat-progress-bar>
          <mat-expansion-panel [(expanded)]="expanded">
              <mat-expansion-panel-header>
                  <mat-panel-title>{{ targetType.longDisplayName }}</mat-panel-title>
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
  targetType: QualifiedName;

  expanded = false;

  loading = false;

  queryResult: QueryResult;

  executeQueryClicked($event) {
    $event.stopImmediatePropagation();
    this.expanded = true;
    this.loading = true;

    const query = new Query(
      { typeNames: [this.targetType.parameterizedName] },
      this.facts.map(fact => {
        return new Fact(getTypeName(fact), fact.value);
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
