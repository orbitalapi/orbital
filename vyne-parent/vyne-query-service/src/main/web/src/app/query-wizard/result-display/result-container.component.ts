import {Component, Input, OnInit} from '@angular/core';
import {ProfilerOperation, QueryResult} from "../../services/query.service";
import {QueryFailure} from "../query-wizard.component";
import {MatTreeNestedDataSource} from "@angular/material";
import {NestedTreeControl} from "@angular/cdk/tree";
import {QualifiedName, Schema, Type, TypesService} from "../../services/types.service";

@ Component({
  selector: 'query-result-container',
  templateUrl: './result-container.component.html',
  styleUrls: ['./result-container.component.scss']
})
export class ResultContainerComponent implements OnInit {


  constructor(private typeService: TypesService) {
    typeService.getTypes().subscribe(schema => this.schema = schema)
  }

  nestedTreeControl: NestedTreeControl<ProfilerOperation>;
  nestedDataSource: MatTreeNestedDataSource<ProfilerOperation> = new MatTreeNestedDataSource<ProfilerOperation>();

  objectKeys = Object.keys;
  objectValues = Object.values;

  schema: Schema;

  remoteCallMermaid: string = "";

  get showMermaid(): boolean {
    return this.remoteCallMermaid.length > 0;
  }

  hasChildren(node: ProfilerOperation) {
    return node.children && node.children.length > 0;
  }

  private _result: QueryResult | QueryFailure;

  get result(): QueryResult | QueryFailure {
    return this._result;
  }

  get unmatchedNodes(): string {
    let queryResult = <QueryResult> this.result;
    return queryResult.unmatchedNodes.map(qn => qn.name).join(", ")
  }

  get queryResultTypeNames(): QualifiedName[] {
    if (!this.isSuccess) return [];
    return Object.keys((<QueryResult>this.result).results)
      .map(typeName => {
        let parts = typeName.split(".");
        return {
          name: parts[parts.length - 1],
          fullyQualifiedName: typeName
        }
      })
  }

  getResultForTypeName(typeName: QualifiedName): any {
    return (<QueryResult>this.result).results[typeName.fullyQualifiedName]
  }

  getTypeByName(typeName: QualifiedName): Type {
    return this.schema.types.find(type => type.name.fullyQualifiedName == typeName.fullyQualifiedName)
  }

  @Input()
  set result(value: QueryResult | QueryFailure) {
    this._result = value;
    this.updateDataSources();
  }

  get iconClass(): string {
    if (!this.result) return "";
    return (this.isSuccess) ? "check_circle" : "error"
  }

  private updateDataSources() {
    if (!this.result) return;
    this.nestedTreeControl = new NestedTreeControl<ProfilerOperation>(this._getChildren);
    if (this.isSuccess) {
      this.nestedDataSource.data = [(<QueryResult>this.result).profilerOperation]
    } else {

      this.nestedDataSource.data = [(<QueryFailure>this.result).profilerOperation]
    }

    this.generateRemoteCallMermaid()
  }

  private generateRemoteCallMermaid() {
    if (!this.result || this.result.remoteCalls.length == 0) {
      this.remoteCallMermaid = "";
    }

    let remoteCallLines = this._result.remoteCalls.map(remoteCall => {
      let wasSuccessful = remoteCall.resultCode >= 200 && remoteCall.resultCode <= 299;
      let resultMessage = wasSuccessful ? "Success " : "Error ";
      resultMessage += remoteCall.resultCode;
      let indent = "    ";
      let lines = [indent + `Vyne ->> ${remoteCall.service.name}: ${remoteCall.operation} (${remoteCall.method})`,
        indent + `${remoteCall.service.name} ->> Vyne: ${resultMessage} (${remoteCall.durationMs}ms)`
      ].join("\n");
      return lines;

    }).join("\n");

    this.remoteCallMermaid = "sequenceDiagram\n" + remoteCallLines
  }

  hasNestedChild = (_: number, nodeData: ProfilerOperation) => nodeData.children.length > 0;
  private _getChildren = (node: ProfilerOperation) => node.children;

  get isSuccess(): Boolean {
    return this.result && this.isQueryResult(this.result) && this.result.fullyResolved
  }

  get isUnsuccessfulSearch(): Boolean {
    return this.result && this.isQueryResult(this.result) && !this.result.fullyResolved
  }

  get isError(): Boolean {
    return this.result && !this.isQueryResult(this.result)
  }

  private isQueryResult(result: QueryResult | QueryFailure): result is QueryResult {
    if (!this.result) return false;
    return (<QueryResult>result).results !== undefined
  }


  ngOnInit() {
    this.nestedTreeControl = new NestedTreeControl<ProfilerOperation>(this._getChildren);
    // this._result = new QueryFailure("It's all gone horribly wrong", serverResponse.profilerOperation)
    // this.updateDataSources();

  }
}
