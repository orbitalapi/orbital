import {Component, EventEmitter, Input, OnInit, Output} from '@angular/core';
import {
  ProfilerOperation,
  QueryHistoryRecord,
  QueryResult,
  ResponseStatus,
  ResultMode
} from '../../services/query.service';
import {QueryFailure} from '../query-wizard/query-wizard.component';
import {MatTreeNestedDataSource} from '@angular/material';
import {NestedTreeControl} from '@angular/cdk/tree';
import {TypesService} from '../../services/types.service';
import {findType, QualifiedName, Schema, Type, TypedInstance} from '../../services/schema';
import {
  InstanceLike,
  InstanceLikeOrCollection,
  typeName,
  UntypedInstance
} from '../../object-view/object-view.component';
import {ExportFileService} from '../../services/export.file.service';
import * as fileSaver from 'file-saver';
import {Router} from '@angular/router';
import {BaseQueryResultComponent} from './BaseQueryResultComponent';

export class InstanceSelectedEvent {
  constructor(public readonly selectedTypeInstance: InstanceLike | UntypedInstance,
              public readonly selectedTypeInstanceType: Type | null,
              public readonly nodeId: string | null
  ) {
  }
}

export enum DownloadFileType {JSON = 'JSON', CSV = 'CSV'}

@Component({
  selector: 'query-result-container',
  templateUrl: './result-container.component.html',
  styleUrls: ['./result-container.component.scss']
})
export class ResultContainerComponent extends BaseQueryResultComponent implements OnInit {

  constructor(typeService: TypesService, private fileService: ExportFileService, private router: Router) {
    super(typeService);
    typeService.getTypes().subscribe(schema => this.schema = schema);

  }


  downloadFileType = DownloadFileType;

  nestedTreeControl: NestedTreeControl<ProfilerOperation>;
  nestedDataSource: MatTreeNestedDataSource<ProfilerOperation> = new MatTreeNestedDataSource<ProfilerOperation>();

  objectKeys = Object.keys;
  objectValues = Object.values;
  @Input()
  activeRecord: QueryHistoryRecord;

  remoteCallMermaid = '';

  get duration(): number {
    return this._result.profilerOperation.duration;
  }

  get showMermaid(): boolean {
    return this.remoteCallMermaid.length > 0;
  }

  hasChildren(node: ProfilerOperation) {
    return node.children && node.children.length > 0;
  }


  get unmatchedNodes(): string {
    const queryResult = <QueryResult>this.result;
    return queryResult.unmatchedNodes ? queryResult.unmatchedNodes.map(qn => qn.longDisplayName).join(', ') : '';
  }


  get iconClass(): string {
    if (!this.result) {
      return '';
    }
    return (this.isSuccess) ? 'check_circle' : 'error';
  }

  protected updateDataSources() {
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

  queryAgain() {
    this.activeRecord && this.router.navigate(['/query-wizard'], {state: {query: this.activeRecord}});
  }
}
