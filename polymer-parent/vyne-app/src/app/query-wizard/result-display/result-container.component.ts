import {Component, Input, OnInit} from '@angular/core';
import {ProfilerOperation, QueryResult} from "../../services/query.service";
import {QueryFailure} from "../query-wizard.component";
import {MatTreeNestedDataSource} from "@angular/material";
import {NestedTreeControl} from "@angular/cdk/tree";
import {QualifiedName, TypedInstance} from "../../services/types.service";

@ Component({
  selector: 'query-result-container',
  templateUrl: './result-container.component.html',
  styleUrls: ['./result-container.component.scss']
})
export class ResultContainerComponent implements OnInit {

  nestedTreeControl: NestedTreeControl<ProfilerOperation>;
  nestedDataSource: MatTreeNestedDataSource<ProfilerOperation> = new MatTreeNestedDataSource<ProfilerOperation>();

  objectKeys = Object.keys;
  objectValues = Object.values;

  typeValueResponse = Object.values(simpleResponse.results)[0];
  objectValueResponse = Object.values(objectResponse.results)[0];

  hasChildren(node: ProfilerOperation) {
    return node.children && node.children.length > 0;
  }

  private _result: QueryResult | QueryFailure;

  get result(): QueryResult | QueryFailure {
    return this._result;
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

  getResultForTypeName(typeName: QualifiedName) {
    return (<QueryResult>this.result).results[typeName.fullyQualifiedName]
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

const simpleResponse = {
  "results": {
    "io.vyne.ClientJurisdiction": {
      "type": {
        "name": {
          "fullyQualifiedName": "io.vyne.ClientJurisdiction",
          "name": "ClientJurisdiction"
        },
        "attributes": {},
        "modifiers": [
          "PRIMITIVE"
        ],
        "aliasForType": {
          "fullyQualifiedName": "lang.taxi.String",
          "name": "String"
        },
        "inherits": [],
        "sources": [
          {
            "origin": "Unknown",
            "language": "Taxi",
            "content": ""
          }
        ],
        "inheritanceGraph": [],
        "fullyQualifiedName": "io.vyne.ClientJurisdiction",
        "typeAlias": true,
        "parameterType": false,
        "scalar": true
      },
      "value": "GBP"
    }
  },
  "unmatchedNodes": [],
  "path": null,
  "profilerOperation": {},
  "fullyResolved": true
};

const objectResponse = {
  "results": {
    "io.vyne.tradeCompliance.TraderMaxTradeValue": {
      "currency": {
        "type": {
          "name": {
            "fullyQualifiedName": "io.vyne.Currency",
            "name": "Currency"
          },
          "attributes": {},
          "modifiers": [
            "PRIMITIVE"
          ],
          "aliasForType": {
            "fullyQualifiedName": "lang.taxi.String",
            "name": "String"
          },
          "inherits": [],
          "sources": [
            {
              "origin": "Unknown",
              "language": "Taxi",
              "content": ""
            }
          ],
          "inheritanceGraph": [],
          "fullyQualifiedName": "io.vyne.Currency",
          "typeAlias": true,
          "parameterType": false,
          "scalar": true
        },
        "value": "USD"
      },
      "value": {
        "type": {
          "name": {
            "fullyQualifiedName": "io.vyne.MoneyAmount",
            "name": "MoneyAmount"
          },
          "attributes": {},
          "modifiers": [
            "PRIMITIVE"
          ],
          "aliasForType": {
            "fullyQualifiedName": "lang.taxi.Decimal",
            "name": "Decimal"
          },
          "inherits": [],
          "sources": [
            {
              "origin": "Unknown",
              "language": "Taxi",
              "content": ""
            }
          ],
          "inheritanceGraph": [],
          "fullyQualifiedName": "io.vyne.MoneyAmount",
          "typeAlias": true,
          "parameterType": false,
          "scalar": true
        },
        "value": 1000000
      }
    }
  },
  "unmatchedNodes": [],
  "path": null,
  "fullyResolved": true
}
