import {TypesService} from '../../services/types.service';
import {findType, InstanceLikeOrCollection, QualifiedName, Schema, Type, TypedInstance} from '../../services/schema';
import { EventEmitter, Input, Output, Directive } from '@angular/core';
import {QueryResult, ResponseStatus, ResultMode} from '../../services/query.service';
import {QueryFailure} from '../query-wizard/query-wizard.component';
import {InstanceSelectedEvent} from '../instance-selected-event';
import {isNullOrUndefined} from 'util';
import {FailedSearchResponse} from '../../services/models';

/**
 * Query results contain an entry for each top-level type that was requested.
 * Instance selected events are relative to the typed isntance that contained the attribute
 * Therefore, this event combines the two - the instance selected event, within the
 * specific requestedType
 */
export class QueryResultInstanceSelectedEvent {
  constructor(public readonly queryTypeName: QualifiedName,
              public readonly instanceSelectedEvent: InstanceSelectedEvent) {
  }
}


@Directive()
export abstract class BaseQueryResultComponent {
  protected constructor(protected typeService: TypesService) {
    typeService.getTypes().subscribe(schema => this.schema = schema);

  }

  @Input()
  failedSearchResponse: FailedSearchResponse;
  @Input()
  typeName: QualifiedName;
  schema: Schema;
  // protected _result: QueryResult | QueryFailure;
  //
  // get result(): QueryResult | QueryFailure {
  //   return this._result;
  // }
  //
  // @Input()
  // set result(value: QueryResult | QueryFailure) {
  //   if (this._result === value) {
  //     return;
  //   }
  //   this._result = value;
  //   this.queryResultTypeNames = this.buildQueryResultTypeNames();
  //
  //   this.updateDataSources();
  // }

  @Output()
  instanceSelected = new EventEmitter<QueryResultInstanceSelectedEvent>();

  protected abstract updateDataSources();


  get error(): string {
    return isNullOrUndefined(this.failedSearchResponse) ? '' : this.failedSearchResponse.message;
  }

  get isSuccess(): Boolean {
    // TODO.
    return true;
    // return this.result && isQueryResult(this.result) && this.result.fullyResolved;
  }

  get isError(): Boolean {
    return !isNullOrUndefined(this.failedSearchResponse);
  }


  get isUnsuccessfulSearch(): Boolean {
    // TODO:
    return false;
    // return this.result && this.result.responseStatus === ResponseStatus.INCOMPLETE;
  }

  instanceClicked(event: InstanceSelectedEvent, queryRequestedTypeName: QualifiedName) {
    this.instanceSelected.emit(new QueryResultInstanceSelectedEvent(queryRequestedTypeName, event));
  }

}

export function isQueryResult(result: QueryResult | QueryFailure | FailedSearchResponse): result is QueryResult {
  if (!result) {
    return false;
  }
  return (<QueryResult>result).results !== undefined;
}

export function isQueryFailure(result: QueryResult | QueryFailure): result is QueryFailure {
  return (<QueryFailure>result).message !== undefined && result.responseStatus === ResponseStatus.ERROR;
}
