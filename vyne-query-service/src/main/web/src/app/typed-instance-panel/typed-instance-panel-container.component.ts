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
  template: `
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


  @Output()
  close = new EventEmitter();

  private _instanceSelectedEvent$: Observable<QueryResultInstanceSelectedEvent>

  @Input()
  get instanceSelectedEvent$(): Observable<QueryResultInstanceSelectedEvent> {
    return this._instanceSelectedEvent$;
  }

  set instanceSelectedEvent$(value) {
    if (this._instanceSelectedEvent$ === value) {
      return;
    }
    this._instanceSelectedEvent$ = value;
    if (this.instanceSelectedEvent$) {
      this.instanceSelectedEvent$.subscribe(event => {
        this.onInstanceSelected(event);
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
