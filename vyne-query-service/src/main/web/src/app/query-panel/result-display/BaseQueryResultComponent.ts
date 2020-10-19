import {TypesService} from '../../services/types.service';
import {findType, QualifiedName, Schema, Type, TypedInstance} from '../../services/schema';
import {EventEmitter, Input, Output} from '@angular/core';
import {QueryResult, ResponseStatus, ResultMode} from '../../services/query.service';
import {QueryFailure} from '../query-wizard/query-wizard.component';
import {InstanceLike, InstanceLikeOrCollection, typeName} from '../../object-view/object-view.component';
import {InstanceSelectedEvent} from './result-container.component';

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
    this._result = value;
    this.queryResultTypeNames = this.buildQueryResultTypeNames();

    this.updateDataSources();
  }

  @Output()
  instanceSelected = new EventEmitter<InstanceSelectedEvent>();

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

  instanceClicked(instance: InstanceLike) {
    const instanceTypeName = typeName(instance);
    const selectedTypeInstanceType = findType(this.schema, instanceTypeName);

    this.instanceSelected.emit(new InstanceSelectedEvent(instance, selectedTypeInstanceType));
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
