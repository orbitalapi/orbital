import {Component, EventEmitter, Input, Output} from '@angular/core';
import {DataSource, InstanceLike, QualifiedName, Type} from '../services/schema';
import {TypesService} from '../services/types.service';
import {buildInheritable, Inheritable} from '../inheritence-graph/inheritance-graph.component';
import {InstanceSelectedEvent, QueryResultMemberCoordinates} from '../query-panel/instance-selected-event';
import {BaseQueryResultDisplayComponent} from '../query-panel/BaseQueryResultDisplayComponent';
import {QueryService} from '../services/query.service';
import {BaseQueryResultWithSidebarComponent} from '../query-panel/BaseQueryResultWithSidebarComponent';
import {Observable} from 'rxjs/internal/Observable';
import {QueryResultInstanceSelectedEvent} from '../query-panel/result-display/BaseQueryResultComponent';

@Component({
  selector: 'app-typed-instance-panel-container',
  styleUrls: ['./typed-instance-panel-container.component.scss'],
  template: `
    <app-panel-header [title]="panelTitle" *ngIf="showPanelHeader">
      <button
        (click)="close.emit()"
        tuiIconButton
        type="button"
        appearance="icon"
        size="xs"
        icon="tuiIconClose"
      ></button>
    </app-panel-header>
    <app-typed-instance-panel
      [type]="selectedTypeInstanceType"
      [instance]="selectedTypeInstance"
      [inheritanceView]="inheritanceView"
      [dataSource]="selectedTypeInstanceDataSource"
      [discoverableTypes]="discoverableTypes"
      [instanceQueryCoordinates]="selectedInstanceQueryCoordinates"
    ></app-typed-instance-panel>
  `
})
export class TypedInstancePanelContainerComponent extends BaseQueryResultWithSidebarComponent {

  @Input()
  showPanelHeader = true
  @Input()
  panelTitle = 'Value details'

  @Output()
  close = new EventEmitter();

  private _queryResultSelectedEvent$: Observable<QueryResultInstanceSelectedEvent>

  @Input()
  get queryResultSelectedEvent$(): Observable<QueryResultInstanceSelectedEvent> {
    return this._queryResultSelectedEvent$;
  }

  set queryResultSelectedEvent$(value) {
    if (this._queryResultSelectedEvent$ === value) {
      return;
    }
    this._queryResultSelectedEvent$ = value;
    if (this.queryResultSelectedEvent$) {
      this.queryResultSelectedEvent$.subscribe(event => {
        this.onQueryResultSelected(event);
      })
    }
  }


  private _instanceSelected$: Observable<InstanceSelectedEvent>

  @Input()
  get instanceSelected$(): Observable<InstanceSelectedEvent> {
    return this._instanceSelected$;
  }

  set instanceSelected$(value) {
    if (this._instanceSelected$ === value) {
      return;
    }
    this._instanceSelected$ = value;
    if (this.instanceSelected$) {
      this.instanceSelected$.subscribe(event => {
        this.onTypedInstanceSelected(event);
      })
    }
  }

  constructor(protected queryService: QueryService, protected typeService: TypesService) {
    super(queryService, typeService);
  }

  onCloseTypedInstanceDrawer($event: any) {
    this.close.emit();
  }

}
