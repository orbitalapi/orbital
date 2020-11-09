import {
  DataSource,
  isTypedInstance, isTypeNamedInstance,
  isUntypedInstance,
  QueryService,
  TypeNamedInstance
} from '../services/query.service';
import {EventEmitter, Output} from '@angular/core';
import {InstanceLike} from '../object-view/object-view.component';
import {findType, Schema, Type} from '../services/schema';
import {QueryResultInstanceSelectedEvent} from './result-display/BaseQueryResultComponent';
import {TypesService} from '../services/types.service';

export abstract class BaseQueryResultDisplayComponent {
  @Output() hasTypedInstanceDrawerClosed = new EventEmitter<boolean>();
  shouldTypedInstancePanelBeVisible: boolean;

  selectedTypeInstanceDataSource: DataSource;
  selectedTypeInstance: InstanceLike;
  selectedTypeInstanceType: Type;

  protected schema: Schema;

  abstract get queryId(): string;

  get showSidePanel(): boolean {
    return this.selectedTypeInstanceType !== undefined && this.selectedTypeInstance !== null;
  }

  set showSidePanel(value: boolean) {
    if (!value) {
      this.selectedTypeInstance = null;
    }
  }

  protected constructor(protected queryService: QueryService, typeService: TypesService) {
    typeService.getTypes()
      .subscribe(schema => this.schema = schema);
  }


  onInstanceSelected($event: QueryResultInstanceSelectedEvent) {
    const eventTypeInstance = $event.instanceSelectedEvent.selectedTypeInstance;
    if ($event.instanceSelectedEvent.nodeId) {
      this.queryService.getQueryResultNodeDetail(
        this.queryId, $event.queryTypeName, $event.instanceSelectedEvent.nodeId
      )
        .subscribe(result => {
          this.selectedTypeInstanceDataSource = result.source;
          this.selectedTypeInstanceType = findType(this.schema, result.typeName.fullyQualifiedName);
          if (isUntypedInstance(eventTypeInstance)) {
            this.selectedTypeInstance = {
              typeName: result.typeName.parameterizedName,
              value: $event.instanceSelectedEvent.selectedTypeInstance.value
            } as TypeNamedInstance;
          }
        });
    }
    this.shouldTypedInstancePanelBeVisible = true;
    if (isTypedInstance(eventTypeInstance) || isTypeNamedInstance(eventTypeInstance)) {
      this.selectedTypeInstance = eventTypeInstance;
    }

  }
}
