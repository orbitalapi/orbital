import {Directive} from '@angular/core';
import {QueryResultInstanceSelectedEvent} from './result-display/BaseQueryResultComponent';
import {
  asNearestTypedInstance,
  DataSource,
  findType, InstanceLike,
  isTypedInstance,
  isTypeNamedInstance,
  isUntypedInstance, QualifiedName, Schema, Type,
  TypeNamedInstance
} from '../services/schema';
import {QueryService} from '../services/query.service';
import {TypesService} from '../services/types.service';
import {InstanceSelectedEvent, QueryResultMemberCoordinates} from './instance-selected-event';
import {buildInheritable, Inheritable} from '../inheritence-graph/inheritance-graph.component';

@Directive()
export abstract class BaseQueryResultWithSidebarComponent {

  shouldTypedInstancePanelBeVisible: boolean;
  selectedTypeInstanceDataSource: DataSource;
  selectedTypeInstance: InstanceLike;
  selectedTypeInstanceType: Type;
  selectedInstanceQueryCoordinates: QueryResultMemberCoordinates;
  inheritanceView: Inheritable;
  discoverableTypes: QualifiedName[];

  schema: Schema;

  // get showSidePanel(): boolean {
  //   return this.selectedTypeInstanceType !== undefined && this.selectedTypeInstance !== null;
  // }
  //
  // set showSidePanel(value: boolean) {
  //   if (!value) {
  //     this.selectedTypeInstance = null;
  //   }
  // }

  protected constructor(protected queryService: QueryService, protected typeService: TypesService) {
    typeService.getTypes()
      .subscribe(schema => this.schema = schema);
  }

  onTypedInstanceSelected(event: InstanceSelectedEvent) {
    if (isUntypedInstance(event.selectedTypeInstance) && event.selectedTypeInstance.nearestType !== null) {
      const typedInstance = asNearestTypedInstance(event.selectedTypeInstance);
      this.shouldTypedInstancePanelBeVisible = true;
      this.selectedTypeInstance = typedInstance;
      this.selectedTypeInstanceType = typedInstance.type;
    } else if (event.selectedTypeInstanceType !== null) {
      this.shouldTypedInstancePanelBeVisible = true;
      this.selectedTypeInstance = event.selectedTypeInstance as InstanceLike;
      this.selectedTypeInstanceType = event.selectedTypeInstanceType;
    }

  }

  onQueryResultSelected($event: QueryResultInstanceSelectedEvent) {
    const eventTypeInstance = $event.instanceSelectedEvent.selectedTypeInstance;
    if ($event.instanceSelectedEvent.rowValueId) {
      this.queryService.getQueryResultNodeDetail(
        $event.instanceSelectedEvent.queryId, $event.instanceSelectedEvent.rowValueId, $event.instanceSelectedEvent.attributeName
      )
        .subscribe(result => {
          this.selectedInstanceQueryCoordinates = $event.instanceSelectedEvent;
          this.selectedTypeInstanceDataSource = result.source;
          this.selectedTypeInstanceType = findType(this.schema, result.typeName.fullyQualifiedName);
          this.typeService.getDiscoverableTypes(this.selectedTypeInstanceType.name.parameterizedName)
            .subscribe(result => {
              this.discoverableTypes = result;
            });

          this.typeService.getTypes().subscribe(schema => {
            this.inheritanceView = buildInheritable(this.selectedTypeInstanceType, schema);
          });
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
