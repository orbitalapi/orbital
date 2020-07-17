import {Component, EventEmitter, Input, OnInit, Output} from '@angular/core';
import {
  ProfilerOperation, QueryHistoryRecord,
  QueryResult,
  QueryService,
  RestfulQueryHistoryRecord,
  ResultMode
} from '../../services/query.service';
import {QueryFailure} from '../query-wizard/query-wizard.component';
import {MatTreeNestedDataSource} from '@angular/material';
import {NestedTreeControl} from '@angular/cdk/tree';
import {TypesService} from '../../services/types.service';
import {findType, QualifiedName, Schema, Type, TypedInstance} from '../../services/schema';
import {InstanceLike, InstanceLikeOrCollection, typeName} from '../../object-view/object-view.component';
import {ExportFileService} from '../../services/export.file.service';
import * as fileSaver from 'file-saver';
import {Router} from "@angular/router";

export class InstanceSelectedEvent {
  constructor(public readonly selectedTypeInstance: InstanceLike,
              public readonly selectedTypeInstanceType: Type
  ) {
  }
}

export enum DownloadFileType {JSON = 'JSON', CSV = 'CSV'}


@Component({
  selector: 'query-result-container',
  templateUrl: './result-container.component.html',
  styleUrls: ['./result-container.component.scss']
})
export class ResultContainerComponent implements OnInit {

  constructor(private typeService: TypesService, private fileService: ExportFileService, private router: Router) {
    typeService.getTypes().subscribe(schema => this.schema = schema);

  }

  private _result: QueryResult | QueryFailure;
  downloadFileType = DownloadFileType;

  nestedTreeControl: NestedTreeControl<ProfilerOperation>;
  nestedDataSource: MatTreeNestedDataSource<ProfilerOperation> = new MatTreeNestedDataSource<ProfilerOperation>();

  objectKeys = Object.keys;
  objectValues = Object.values;

  @Output()
  instanceSelected = new EventEmitter<InstanceSelectedEvent>();

  schema: Schema;

  remoteCallMermaid = '';

  queryResultTypeNames: QualifiedName[] = [];

  get duration(): number {
    return this._result.profilerOperation.duration;
  }

  get showMermaid(): boolean {
    return this.remoteCallMermaid.length > 0;
  }

  hasChildren(node: ProfilerOperation) {
    return node.children && node.children.length > 0;
  }

  get result(): QueryResult | QueryFailure {
    return this._result;
  }
  get unmatchedNodes(): string {
    const queryResult = <QueryResult>this.result;
    return queryResult.unmatchedNodes.map(qn => qn.name).join(', ');
  }

  get error(): string {
    const queryResult = <QueryResult>this.result;
    return queryResult.error ? queryResult.error : ""
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

  @Input()
  set result(value: QueryResult | QueryFailure) {
    this._result = value;
    this.updateDataSources();
  }

  get iconClass(): string {
    if (!this.result) {
      return '';
    }
    return (this.isSuccess) ? 'check_circle' : 'error';
  }

  private updateDataSources() {
    if (!this.result) {
      return;
    }
    this.nestedTreeControl = new NestedTreeControl<ProfilerOperation>(this._getChildren);
    if (this.isSuccess) {
      this.nestedDataSource.data = [(<QueryResult>this.result).profilerOperation];
    } else {

      this.nestedDataSource.data = [(<QueryFailure>this.result).profilerOperation];
    }

    this.generateRemoteCallMermaid();
    this.queryResultTypeNames = this.buildQueryResultTypeNames();


  }

  private buildQueryResultTypeNames() {
    if (!this.isSuccess) {
      return [];
    }
    return Object.keys((<QueryResult>this.result).results)
      .map(elementTypeName => findType(this.schema, elementTypeName).name);
  }

  private generateRemoteCallMermaid() {
    if (!this.result || this.result.remoteCalls.length === 0) {
      this.remoteCallMermaid = '';
    }

    const remoteCallLines = this._result.remoteCalls.map(remoteCall => {
      const wasSuccessful = remoteCall.resultCode >= 200 && remoteCall.resultCode <= 299;
      let resultMessage = wasSuccessful ? 'Success ' : 'Error ';
      resultMessage += remoteCall.resultCode;
      const indent = '    ';
      const lines = [indent + `Vyne ->> ${remoteCall.service}: ${remoteCall.operation} (${remoteCall.method})`,
        indent + `${remoteCall.service} ->> Vyne: ${resultMessage} (${remoteCall.durationMs}ms)`
      ].join('\n');
      return lines;

    }).join('\n');

    this.remoteCallMermaid = 'sequenceDiagram\n' + remoteCallLines;
  }

  hasNestedChild = (_: number, nodeData: ProfilerOperation) => nodeData.children.length > 0;
  private _getChildren = (node: ProfilerOperation) => node.children;

  get isSuccess(): Boolean {
    return this.result && this.isQueryResult(this.result) && this.result.fullyResolved;
  }

  get isQueryHistoryPage(): Boolean {
    if (this.router.url.includes('query-history')) {
      return true;
    }
  }

  get isUnsuccessfulSearch(): Boolean {
    return this.result && this.isQueryResult(this.result) && !this.result.fullyResolved;
  }

  get isVerboseResult(): Boolean {
    const queryResult = <QueryResult>this.result;
    return queryResult && queryResult.resultMode === ResultMode.VERBOSE;
  }

  get isError(): Boolean {
    return this.result && !this.isQueryResult(this.result);
  }

  private isQueryResult(result: QueryResult | QueryFailure): result is QueryResult {
    if (!this.result) {
      return false;
    }
    return (<QueryResult>result).results !== undefined;
  }
  public downloadQueryHistory(fileType: DownloadFileType) {
   const queryResponseId = (<QueryResult>this.result).queryResponseId;
    this.fileService.exportQueryHistory(queryResponseId, fileType).subscribe(response => {
      const blob: Blob = new Blob([response], {type: `text/${fileType}; charset=utf-8`});
      fileSaver.saveAs(blob, `query-${new Date().getTime()}.${fileType}`);
    });
  }

  ngOnInit() {
    this.nestedTreeControl = new NestedTreeControl<ProfilerOperation>(this._getChildren);
    // this._result = new QueryFailure("It's all gone horribly wrong", serverResponse.profilerOperation)
    // this.updateDataSources();

  }

  instanceClicked(instance: InstanceLike) {
    const instanceTypeName = typeName(instance);
    const selectedTypeInstanceType = findType(this.schema, instanceTypeName);

    this.instanceSelected.emit(new InstanceSelectedEvent(instance, selectedTypeInstanceType));
  }
}
