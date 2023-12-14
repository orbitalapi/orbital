import {QueryService,} from '../services/query.service';
import {ChangeDetectorRef, Directive, EventEmitter, Output} from '@angular/core';
import {TypesService} from '../services/types.service';
import {BaseQueryResultWithSidebarComponent} from './BaseQueryResultWithSidebarComponent';

@Directive()
export abstract class BaseQueryResultDisplayComponent extends BaseQueryResultWithSidebarComponent {
  @Output() hasTypedInstanceDrawerClosed = new EventEmitter<boolean>();


  abstract get queryId(): string;

  protected constructor(protected queryService: QueryService, protected typeService: TypesService, protected changeDetector: ChangeDetectorRef) {
    super(queryService, typeService, changeDetector);
  }


}
