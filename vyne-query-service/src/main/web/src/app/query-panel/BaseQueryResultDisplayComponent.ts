import {
  QueryService,
} from '../services/query.service';
import {EventEmitter, Output, Directive} from '@angular/core';
import {
  DataSource,
  findType,
  InstanceLike,
  isTypedInstance,
  isTypeNamedInstance,
  isUntypedInstance,
  Schema,
  Type, TypeNamedInstance
} from '../services/schema';
import {QueryResultInstanceSelectedEvent} from './result-display/BaseQueryResultComponent';
import {TypesService} from '../services/types.service';
import {QueryResultMemberCoordinates} from './instance-selected-event';
import {BaseQueryResultWithSidebarComponent} from './BaseQueryResultWithSidebarComponent';

@Directive()
export abstract class BaseQueryResultDisplayComponent extends BaseQueryResultWithSidebarComponent {
  @Output() hasTypedInstanceDrawerClosed = new EventEmitter<boolean>();


  abstract get queryId(): string;

  protected constructor(protected queryService: QueryService, protected typeService: TypesService) {
    super(queryService, typeService);
  }


}
