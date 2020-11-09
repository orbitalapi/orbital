import {TypesService} from '../../services/types.service';
import {findType, InstanceLikeOrCollection, QualifiedName, Schema, Type, TypedInstance} from '../../services/schema';
import {EventEmitter, Input, Output} from '@angular/core';
import {QueryResult, ResponseStatus, ResultMode} from '../../services/query.service';
import {QueryFailure} from '../query-wizard/query-wizard.component';
import {InstanceSelectedEvent} from '../instance-selected-event';

/**
 * Query results contain an entry for each top-level type that was requested.
 * Instance selected events are relative to the typed isntance that contained the attribute
 * Therefore, this event combines the two - the instance selected event, within the
 * specific requestedType
 */
export class QueryResultInstanceSelectedEvent {
  constructor(public readonly queryTypeName: QualifiedName, public readonly instanceSelectedEvent: InstanceSelectedEvent) {
  }
}


export abstract class BaseQueryResultComponent {
  protected constructor(protected typeService: TypesService) {
    typeService.getTypes().subscribe(schema => this.schema = schema);

  }

  schema: Schema;
  protected _result: QueryResult | QueryFailure;

  get result(): QueryResult | QueryFailure {
    return this._result;
  }

  @Input()
  set result(value: QueryResult | QueryFailure) {
    if (this._result === value) {
      return;
    }
    this._result = value;
    this.queryResultTypeNames = this.buildQueryResultTypeNames();

    this.updateDataSources();
  }

  @Output()
  instanceSelected = new EventEmitter<QueryResultInstanceSelectedEvent>();

  queryResultTypeNames: QualifiedName[] = [];

  protected abstract updateDataSources();

  get error(): string {
    const queryResult = <QueryResult>this.result;
    return queryResult.error ? queryResult.error : '';
  }

  getResultForTypeName(qualifiedName: QualifiedName): InstanceLikeOrCollection {
    const queryResult = <QueryResult>this.result;
    if (queryResult.resultMode === ResultMode.VERBOSE) {
      const results = <{ [key: string]: InstanceLikeOrCollection }>queryResult.results;
      return results[qualifiedName.parameterizedName] as InstanceLikeOrCollection;
    } else {
      const results = <{ [key: string]: TypedInstance }>queryResult.results;
      return results[qualifiedName.parameterizedName];
    }
  }

  /**
   * Returns the actual type for the queried typeName.
   * Note that if the resultMode was Verbose (ie., included type data),
   * we favour that over the queried type data, since return types can be
   * polymorphic.  In this scenario, we return null, so that the viewer
   * will read the type from the instance.
   */
  getTypeIfNotIncluded(queryTypeName: QualifiedName): Type {
    const queryResult = <QueryResult>this.result;
    if (queryResult.resultMode === ResultMode.VERBOSE) {
      return null;
    } else {
      return findType(this.schema, queryTypeName.parameterizedName);
    }
  }

  get isSuccess(): Boolean {
    return this.result && isQueryResult(this.result) && this.result.fullyResolved;
  }


  get isVerboseResult(): Boolean {
    const queryResult = <QueryResult>this.result;
    return queryResult && queryResult.resultMode === ResultMode.VERBOSE;
  }

  get isError(): Boolean {
    return this.result && this.result.responseStatus === ResponseStatus.ERROR;
  }


  get isUnsuccessfulSearch(): Boolean {
    return this.result && this.result.responseStatus === ResponseStatus.INCOMPLETE;
  }


  private buildQueryResultTypeNames(): QualifiedName[] {
    if (!this.isSuccess) {
      return [];
    }
    return Object.keys((<QueryResult>this.result).results)
      .map(elementTypeName => findType(this.schema, elementTypeName).name);
  }

  instanceClicked(event: InstanceSelectedEvent, queryRequestedTypeName: QualifiedName) {
    this.instanceSelected.emit(new QueryResultInstanceSelectedEvent(queryRequestedTypeName, event));
  }

}

export function isQueryResult(result: QueryResult | QueryFailure): result is QueryResult {
  if (!result) {
    return false;
  }
  return (<QueryResult>result).results !== undefined;
}

export function isQueryFailure(result: QueryResult | QueryFailure): result is QueryFailure {
  return (<QueryFailure>result).message !== undefined && result.responseStatus === ResponseStatus.ERROR;
}
