import {Component, Input} from '@angular/core';
import {Fact, Query, QueryMode, QueryService, ResultMode} from '../services/query.service';
import {findType, getTypeName, InstanceLike, QualifiedName, Schema, Type} from '../services/schema';
import {nanoid} from 'nanoid';
import {Observable} from 'rxjs';
import {tap} from 'rxjs/operators';
import {TypesService} from '../services/types.service';
import {ValueWithTypeName} from '../services/models';

@Component({
  selector: 'app-inline-query-runner',
  styleUrls: ['./inline-query-runner.component.scss'],
  template: `
    <div class="container">
      <mat-progress-bar mode="indeterminate" color="accent" *ngIf="loading"></mat-progress-bar>
      <mat-expansion-panel [(expanded)]="expanded">
        <mat-expansion-panel-header>
          <mat-panel-title>{{ targetTypeName.longDisplayName }}</mat-panel-title>
          <mat-panel-description>
            <button mat-stroked-button (click)="executeQueryClicked($event)">Run</button>
          </mat-panel-description>
        </mat-expansion-panel-header>

        <app-tabbed-results-view *ngIf="results$"
                                 [instances$]="results$"
                                 [downloadSupported]="false"
                                 [type]="targetType"></app-tabbed-results-view>

      </mat-expansion-panel>
    </div>
  `
})
export class InlineQueryRunnerComponent {
  @Input()
  get schema(): Schema {
    return this._schema;
  }

  set schema(value: Schema) {
    if (this._schema === value) {
      return;
    }
    this._schema = value;
    this.setTargetType();
  }

  private _schema: Schema;

  private _targetTypeName: QualifiedName;
  targetType: Type;

  @Input()
  get targetTypeName(): QualifiedName {
    return this._targetTypeName;
  }

  set targetTypeName(value: QualifiedName) {
    if (this._targetTypeName === value) {
      return;
    }
    this._targetTypeName = value;
    this.setTargetType();
  }

  expanded = false;

  loading = false;

  results$: Observable<ValueWithTypeName>;
  @Input()
  facts: InstanceLike[];


  constructor(private queryService: QueryService, private typeService: TypesService) {
    typeService.getTypes()
      .subscribe(next => this.schema = next);
  }


  executeQueryClicked($event) {
    $event.stopImmediatePropagation();
    this.expanded = true;
    this.loading = true;

    const query = new Query(
      {typeNames: [this._targetTypeName.parameterizedName]},
      this.facts.map(fact => {
        return new Fact(getTypeName(fact), fact.value);
      }),
      QueryMode.DISCOVER,
      ResultMode.SIMPLE
    );

    this.results$ = this.queryService.submitQuery(query, nanoid())
      .pipe(
        tap({
          error: (err) => {
            console.error('Failed to execute query: ' + JSON.stringify(err));
            this.loading = false;
          },
          complete: () => this.loading = false
        })
      );
  }

  private setTargetType() {
    if (!this.schema || !this.targetTypeName) {
      return;
    }
    this.targetType = findType(this._schema, this.targetTypeName.parameterizedName);
  }
}
