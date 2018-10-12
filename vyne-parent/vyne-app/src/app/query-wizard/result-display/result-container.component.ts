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

  typeValueResponse = Object.values(simpleResponse.results)[0];
  objectValueResponse = Object.values(objectResponse.results)[0];

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

const simpleLightweightResponse = {
  "results": {"io.vyne.ClientJurisdiction": "GBP"},
  "unmatchedNodes": [],
  "path": null,
  "profilerOperation": {
    "componentName": "io.osmosis.polymer.query.QueryProfiler",
    "operationName": "Root",
    "type": "ROOT",
    "path": "/",
    "context": {},
    "remoteCalls": [],
    "result": null,
    "name": "io.osmosis.polymer.query.QueryProfiler:Root",
    "fullPath": "//io.osmosis.polymer.query.QueryProfiler:Root",
    "id": "8e90e19a-796f-4c2b-90d3-a4adebcdd2ed",
    "children": [{
      "componentName": "DefaultQueryEngine",
      "operationName": "Query with ModelsScanStrategy",
      "type": "GRAPH_TRAVERSAL",
      "path": "//io.osmosis.polymer.query.QueryProfiler:Root",
      "context": {"Search target": ["io.vyne.ClientJurisdiction"]},
      "remoteCalls": [],
      "result": {
        "startTime": 1535452739976,
        "endTime": 1535452739976,
        "value": {"matchedNodes": {}, "additionalData": []},
        "duration": 0
      },
      "name": "DefaultQueryEngine:Query with ModelsScanStrategy",
      "fullPath": "//io.osmosis.polymer.query.QueryProfiler:Root/DefaultQueryEngine:Query with ModelsScanStrategy",
      "id": "8314b6f4-891f-48a0-b539-82fabbc4406d",
      "children": [{
        "componentName": "ModelsScanStrategy",
        "operationName": "scan for matches",
        "type": "LOOKUP",
        "path": "//io.osmosis.polymer.query.QueryProfiler:Root/DefaultQueryEngine:Query with ModelsScanStrategy",
        "context": {},
        "remoteCalls": [],
        "result": {
          "startTime": 1535452739976,
          "endTime": 1535452739976,
          "value": {"matchedNodes": {}, "additionalData": []},
          "duration": 0
        },
        "name": "ModelsScanStrategy:scan for matches",
        "fullPath": "//io.osmosis.polymer.query.QueryProfiler:Root/DefaultQueryEngine:Query with ModelsScanStrategy/ModelsScanStrategy:scan for matches",
        "id": "3abf926e-f228-483f-816a-ce7e1de57009",
        "children": [],
        "duration": 0,
        "description": "ModelsScanStrategy.scan for matches"
      }],
      "duration": 0,
      "description": "DefaultQueryEngine.Query with ModelsScanStrategy"
    }, {
      "componentName": "DefaultQueryEngine",
      "operationName": "Query with HipsterDiscoverGraphQueryStrategy",
      "type": "GRAPH_TRAVERSAL",
      "path": "//io.osmosis.polymer.query.QueryProfiler:Root",
      "context": {"Search target": ["io.vyne.ClientJurisdiction"]},
      "remoteCalls": [],
      "result": {
        "startTime": 1535452739976,
        "endTime": 1535452739988,
        "value": {
          "matchedNodes": {
            "QuerySpecTypeNode(type=Type(name=io.vyne.ClientJurisdiction, attributes={}, modifiers=[PRIMITIVE], aliasForType=lang.taxi.String, inherits=[], sources=[SourceCode(origin=Unknown, language=Taxi, content=)]), children=[], mode=DISCOVER)": {
              "type": {
                "name": {
                  "fullyQualifiedName": "io.vyne.ClientJurisdiction",
                  "name": "ClientJurisdiction"
                },
                "attributes": {},
                "modifiers": ["PRIMITIVE"],
                "aliasForType": {"fullyQualifiedName": "lang.taxi.String", "name": "String"},
                "inherits": [],
                "sources": [{"origin": "Unknown", "language": "Taxi", "content": ""}],
                "inheritanceGraph": [],
                "fullyQualifiedName": "io.vyne.ClientJurisdiction",
                "typeAlias": true,
                "parameterType": false,
                "scalar": true
              }, "value": "GBP"
            }
          }, "additionalData": []
        },
        "duration": 12
      },
      "name": "DefaultQueryEngine:Query with HipsterDiscoverGraphQueryStrategy",
      "fullPath": "//io.osmosis.polymer.query.QueryProfiler:Root/DefaultQueryEngine:Query with HipsterDiscoverGraphQueryStrategy",
      "id": "7a4617f6-a915-44a5-91d3-0398134988e1",
      "children": [{
        "componentName": "HipsterDiscoverGraphQueryStrategy",
        "operationName": "Searching for path TradeRequest -> ClientJurisdiction",
        "type": "GRAPH_TRAVERSAL",
        "path": "//io.osmosis.polymer.query.QueryProfiler:Root/DefaultQueryEngine:Query with HipsterDiscoverGraphQueryStrategy",
        "context": {
          "Current graph": ["Type(io.vyne.tradeCompliance.rules.JurisdictionRuleResponse) -[Has attribute]-> Member(io.vyne.tradeCompliance.rules.JurisdictionRuleResponse/message)", "Type(io.vyne.tradeCompliance.rules.JurisdictionRuleResponse) -[Has attribute]-> Member(io.vyne.tradeCompliance.rules.JurisdictionRuleResponse/ruleId)", "Type(io.vyne.tradeCompliance.rules.JurisdictionRuleResponse) -[Has attribute]-> Member(io.vyne.tradeCompliance.rules.JurisdictionRuleResponse/status)", "Parameter(param/io.vyne.tradeCompliance.Currency) -[Is parameter on]-> Operation(io.vyne.demos.tradeCompliance.services.RateConverterService@@convert)", "Parameter(param/io.vyne.tradeCompliance.Currency) -[Is parameter on]-> Parameter(param/io.vyne.tradeCompliance.TradeValue)", "Member(io.vyne.tradeCompliance.TradeValue/currency) -[Is an attribute of]-> Type(io.vyne.tradeCompliance.TradeValue)", "Member(io.vyne.tradeCompliance.TradeValue/currency) -[Is type of]-> Type(io.vyne.tradeCompliance.Currency)", "Type_instance(io.vyne.demos.tradeCompliance.services.Client) -[Is instanceOfType of]-> Type(io.vyne.demos.tradeCompliance.services.Client)", "Type_instance(io.vyne.demos.tradeCompliance.services.Client) -[can populate]-> Parameter(param/io.vyne.demos.tradeCompliance.services.Client)", "Type_instance(io.vyne.demos.tradeCompliance.services.Client) -[Instance has attribute]-> Provided_instance_member(io.vyne.demos.tradeCompliance.services.Client/id)", "Type_instance(io.vyne.demos.tradeCompliance.services.Client) -[Instance has attribute]-> Provided_instance_member(io.vyne.demos.tradeCompliance.services.Client/jurisdiction)", "Type_instance(io.vyne.demos.tradeCompliance.services.Client) -[Instance has attribute]-> Provided_instance_member(io.vyne.demos.tradeCompliance.services.Client/name)", "Provided_instance_member(io.vyne.demos.tradeCompliance.services.Client/name) -[Is an attribute of]-> Type_instance(lang.taxi.String)", "Type(io.vyne.tradeCompliance.rules.NotionalLimitRuleRequest) -[Has attribute]-> Member(io.vyne.tradeCompliance.rules.NotionalLimitRuleRequest/notional)", "Member(java.util.RuleEvaluationResult/status) -[Is an attribute of]-> Type(java.util.RuleEvaluationResult)", "Member(java.util.RuleEvaluationResult/status) -[Is type of]-> Type(java.util.RagStatus)", "Member(io.vyne.demos.tradeCompliance.services.TradeValueRequest/notional) -[Is an attribute of]-> Type(io.vyne.demos.tradeCompliance.services.TradeValueRequest)", "Member(io.vyne.demos.tradeCompliance.services.TradeValueRequest/notional) -[Is type of]-> Type(io.vyne.TradeNotional)", "Member(io.vyne.Price/value) -[Is an attribute of]-> Type(io.vyne.Price)", "Member(io.vyne.Price/value) -[Is type of]-> Type(io.vyne.tradeCompliance.MoneyAmount)", "Provided_instance_member(io.vyne.demos.tradeCompliance.services.Trader/username) -[Is an attribute of]-> Type_instance(io.vyne.Username)", "Member(io.vyne.tradeCompliance.TraderMaxTradeValue/value) -[Is an attribute of]-> Type(io.vyne.tradeCompliance.TraderMaxTradeValue)", "Member(io.vyne.tradeCompliance.TraderMaxTradeValue/value) -[Is type of]-> Type(io.vyne.tradeCompliance.MoneyAmount)", "Operation(io.vyne.demos.tradeCompliance.services.TraderService@@getTrader) -[Requires parameter]-> Parameter(param/io.vyne.Username)", "Operation(io.vyne.demos.tradeCompliance.services.TraderService@@getTrader) -[provides]-> Type_instance(io.vyne.demos.tradeCompliance.services.Trader)", "Member(io.vyne.tradeCompliance.rules.JurisdictionRuleRequest/clientJurisdiction) -[Is an attribute of]-> Type(io.vyne.tradeCompliance.rules.JurisdictionRuleRequest)", "Member(io.vyne.tradeCompliance.rules.JurisdictionRuleRequest/clientJurisdiction) -[Is type of]-> Type(io.vyne.ClientJurisdiction)", "Member(io.vyne.tradeCompliance.rules.NotionalLimitRuleResponse/ruleId) -[Is an attribute of]-> Type(io.vyne.tradeCompliance.rules.NotionalLimitRuleResponse)", "Member(io.vyne.tradeCompliance.rules.NotionalLimitRuleResponse/ruleId) -[Is type of]-> Type(lang.taxi.String)", "Type(io.vyne.tradeCompliance.rules.TradeValueRuleResponse) -[Has attribute]-> Member(io.vyne.tradeCompliance.rules.TradeValueRuleResponse/message)", "Type(io.vyne.tradeCompliance.rules.TradeValueRuleResponse) -[Has attribute]-> Member(io.vyne.tradeCompliance.rules.TradeValueRuleResponse/ruleId)", "Type(io.vyne.tradeCompliance.rules.TradeValueRuleResponse) -[Has attribute]-> Member(io.vyne.tradeCompliance.rules.TradeValueRuleResponse/status)", "Parameter(param/io.vyne.TradeNotional) -[Is parameter on]-> Parameter(param/io.vyne.tradeCompliance.rules.NotionalLimitRuleRequest)", "Parameter(param/io.vyne.TradeNotional) -[Is parameter on]-> Parameter(param/io.vyne.demos.tradeCompliance.services.TradeValueRequest)", "Provided_instance_member(io.vyne.demos.tradeCompliance.services.Trader/maxValue) -[Is an attribute of]-> Type_instance(io.vyne.tradeCompliance.TraderMaxTradeValue)", "Member(io.vyne.demos.tradeCompliance.services.Trader/maxValue) -[Is an attribute of]-> Type(io.vyne.demos.tradeCompliance.services.Trader)", "Member(io.vyne.demos.tradeCompliance.services.Trader/maxValue) -[Is type of]-> Type(io.vyne.tradeCompliance.TraderMaxTradeValue)", "Member(io.vyne.tradeCompliance.rules.TradeValueRuleRequest/tradeValue) -[Is an attribute of]-> Type(io.vyne.tradeCompliance.rules.TradeValueRuleRequest)", "Member(io.vyne.tradeCompliance.rules.TradeValueRuleRequest/tradeValue) -[Is type of]-> Type(io.vyne.tradeCompliance.TradeValue)", "Member(io.vyne.tradeCompliance.rules.JurisdictionRuleResponse/message) -[Is an attribute of]-> Type(io.vyne.tradeCompliance.rules.JurisdictionRuleResponse)", "Member(io.vyne.tradeCompliance.rules.JurisdictionRuleResponse/message) -[Is type of]-> Type(lang.taxi.String)", "Member(io.vyne.tradeCompliance.aggregator.TradeRequest/traderId) -[Is an attribute of]-> Type(io.vyne.tradeCompliance.aggregator.TradeRequest)", "Member(io.vyne.tradeCompliance.aggregator.TradeRequest/traderId) -[Is type of]-> Type(io.vyne.Username)", "Type_instance(io.vyne.Price) -[can populate]-> Parameter(param/io.vyne.Price)", "Type_instance(io.vyne.Price) -[Is instanceOfType of]-> Type(io.vyne.Price)", "Provided_instance_member(io.vyne.tradeCompliance.rules.TradeValueRuleResponse/message) -[Is an attribute of]-> Type_instance(lang.taxi.String)", "Type(io.vyne.tradeCompliance.aggregator.TradeComplianceResult) -[Has attribute]-> Member(io.vyne.tradeCompliance.aggregator.TradeComplianceResult/results)", "Type(io.vyne.tradeCompliance.aggregator.TradeComplianceResult) -[Has attribute]-> Member(io.vyne.tradeCompliance.aggregator.TradeComplianceResult/status)", "Type(io.vyne.tradeCompliance.TradeValue) -[Has attribute]-> Member(io.vyne.tradeCompliance.TradeValue/currency)", "Type(io.vyne.tradeCompliance.TradeValue) -[Has attribute]-> Member(io.vyne.tradeCompliance.TradeValue/value)", "Member(io.vyne.demos.tradeCompliance.services.Trader/username) -[Is an attribute of]-> Type(io.vyne.demos.tradeCompliance.services.Trader)", "Member(io.vyne.demos.tradeCompliance.services.Trader/username) -[Is type of]-> Type(io.vyne.Username)", "Member(io.vyne.tradeCompliance.aggregator.RuleEvaluationResults/message) -[Is an attribute of]-> Type(io.vyne.tradeCompliance.aggregator.RuleEvaluationResults)", "Member(io.vyne.tradeCompliance.aggregator.RuleEvaluationResults/message) -[Is type of]-> Type(lang.taxi.String)", "Member(io.vyne.tradeCompliance.aggregator.TradeRequest/price) -[Is an attribute of]-> Type(io.vyne.tradeCompliance.aggregator.TradeRequest)", "Member(io.vyne.tradeCompliance.aggregator.TradeRequest/price) -[Is type of]-> Type(io.vyne.Price)", "Operation(io.vyne.tradeCompliance.rules.JurisdictionRuleService@@evaluate) -[Requires parameter]-> Parameter(param/io.vyne.tradeCompliance.rules.JurisdictionRuleRequest)", "Operation(io.vyne.tradeCompliance.rules.JurisdictionRuleService@@evaluate) -[provides]-> Type_instance(io.vyne.tradeCompliance.rules.JurisdictionRuleResponse)", "Member(io.vyne.tradeCompliance.rules.JurisdictionRuleResponse/ruleId) -[Is an attribute of]-> Type(io.vyne.tradeCompliance.rules.JurisdictionRuleResponse)", "Member(io.vyne.tradeCompliance.rules.JurisdictionRuleResponse/ruleId) -[Is type of]-> Type(lang.taxi.String)", "Member(io.vyne.tradeCompliance.TradeValue/value) -[Is an attribute of]-> Type(io.vyne.tradeCompliance.TradeValue)", "Member(io.vyne.tradeCompliance.TradeValue/value) -[Is type of]-> Type(io.vyne.tradeCompliance.MoneyAmount)", "Member(java.util.RuleEvaluationResult/ruleId) -[Is an attribute of]-> Type(java.util.RuleEvaluationResult)", "Member(java.util.RuleEvaluationResult/ruleId) -[Is type of]-> Type(lang.taxi.String)", "Provided_instance_member(io.vyne.tradeCompliance.aggregator.TradeComplianceResult/results) -[Is an attribute of]-> Type_instance(io.vyne.tradeCompliance.aggregator.RuleEvaluationResults)", "Type_instance(io.vyne.ClientJurisdiction) -[can populate]-> Parameter(param/io.vyne.ClientJurisdiction)", "Type_instance(io.vyne.ClientJurisdiction) -[Is instanceOfType of]-> Type(io.vyne.ClientJurisdiction)", "Member(java.util.RuleEvaluationResult/message) -[Is an attribute of]-> Type(java.util.RuleEvaluationResult)", "Member(java.util.RuleEvaluationResult/message) -[Is type of]-> Type(lang.taxi.String)", "Member(io.vyne.tradeCompliance.rules.TradeValueRuleRequest/traderLimit) -[Is an attribute of]-> Type(io.vyne.tradeCompliance.rules.TradeValueRuleRequest)", "Member(io.vyne.tradeCompliance.rules.TradeValueRuleRequest/traderLimit) -[Is type of]-> Type(io.vyne.tradeCompliance.TraderMaxTradeValue)", "Parameter(param/io.vyne.tradeCompliance.rules.JurisdictionRuleRequest) -[Is parameter on]-> Operation(io.vyne.tradeCompliance.rules.JurisdictionRuleService@@evaluate)", "Member(io.vyne.tradeCompliance.aggregator.TradeComplianceResult/results) -[Is an attribute of]-> Type(io.vyne.tradeCompliance.aggregator.TradeComplianceResult)", "Member(io.vyne.tradeCompliance.aggregator.TradeComplianceResult/results) -[Is type of]-> Type(io.vyne.tradeCompliance.aggregator.RuleEvaluationResults)", "Type(io.vyne.demos.tradeCompliance.services.Trader) -[Has attribute]-> Member(io.vyne.demos.tradeCompliance.services.Trader/jurisdiction)", "Type(io.vyne.demos.tradeCompliance.services.Trader) -[Has attribute]-> Member(io.vyne.demos.tradeCompliance.services.Trader/maxValue)", "Type(io.vyne.demos.tradeCompliance.services.Trader) -[Has attribute]-> Member(io.vyne.demos.tradeCompliance.services.Trader/username)", "Parameter(param/io.vyne.ClientJurisdiction) -[Is parameter on]-> Parameter(param/io.vyne.tradeCompliance.rules.JurisdictionRuleRequest)", "Parameter(param/io.vyne.tradeCompliance.aggregator.TradeRequest) -[Is parameter on]-> Operation(io.vyne.tradeCompliance.aggregator.TradeComplianceEvaluator@@evaluate)", "Parameter(param/io.vyne.tradeCompliance.TraderMaxTradeValue) -[Is parameter on]-> Parameter(param/io.vyne.tradeCompliance.rules.TradeValueRuleRequest)", "Provided_instance_member(io.vyne.tradeCompliance.TradeValue/currency) -[Is an attribute of]-> Type_instance(io.vyne.tradeCompliance.Currency)", "Provided_instance_member(io.vyne.tradeCompliance.aggregator.TradeRequest/price) -[Is an attribute of]-> Type_instance(io.vyne.Price)", "Member(io.vyne.tradeCompliance.rules.JurisdictionRuleRequest/traderJurisdiction) -[Is an attribute of]-> Type(io.vyne.tradeCompliance.rules.JurisdictionRuleRequest)", "Member(io.vyne.tradeCompliance.rules.JurisdictionRuleRequest/traderJurisdiction) -[Is type of]-> Type(io.vyne.TraderJurisdiction)", "Provided_instance_member(io.vyne.tradeCompliance.rules.JurisdictionRuleResponse/ruleId) -[Is an attribute of]-> Type_instance(lang.taxi.String)", "Member(io.vyne.tradeCompliance.aggregator.TradeRequest/clientId) -[Is an attribute of]-> Type(io.vyne.tradeCompliance.aggregator.TradeRequest)", "Member(io.vyne.tradeCompliance.aggregator.TradeRequest/clientId) -[Is type of]-> Type(io.vyne.ClientId)", "Parameter(param/io.vyne.tradeCompliance.rules.NotionalLimitRuleRequest) -[Is parameter on]-> Operation(io.vyne.tradeCompliance.rules.NotionalLimitRuleService@@evaluate)", "Type_instance(io.vyne.ClientId) -[can populate]-> Parameter(param/io.vyne.ClientId)", "Type_instance(io.vyne.ClientId) -[Is instanceOfType of]-> Type(io.vyne.ClientId)", "Provided_instance_member(io.vyne.tradeCompliance.aggregator.TradeRequest/traderId) -[Is an attribute of]-> Type_instance(io.vyne.Username)", "Type_instance(io.vyne.tradeCompliance.aggregator.TradeComplianceResult) -[Is instanceOfType of]-> Type(io.vyne.tradeCompliance.aggregator.TradeComplianceResult)", "Type_instance(io.vyne.tradeCompliance.aggregator.TradeComplianceResult) -[can populate]-> Parameter(param/io.vyne.tradeCompliance.aggregator.TradeComplianceResult)", "Type_instance(io.vyne.tradeCompliance.aggregator.TradeComplianceResult) -[Instance has attribute]-> Provided_instance_member(io.vyne.tradeCompliance.aggregator.TradeComplianceResult/results)", "Type_instance(io.vyne.tradeCompliance.aggregator.TradeComplianceResult) -[Instance has attribute]-> Provided_instance_member(io.vyne.tradeCompliance.aggregator.TradeComplianceResult/status)", "Parameter(param/io.vyne.demos.tradeCompliance.services.TradeValueRequest) -[Is parameter on]-> Operation(io.vyne.demos.tradeCompliance.services.TradeValueService@@calculateValue)", "Member(io.vyne.Money/currency) -[Is an attribute of]-> Type(io.vyne.Money)", "Member(io.vyne.Money/currency) -[Is type of]-> Type(io.vyne.tradeCompliance.Currency)", "Provided_instance_member(io.vyne.tradeCompliance.rules.JurisdictionRuleResponse/status) -[Is an attribute of]-> Type_instance(io.vyne.tradeCompliance.rules.RagStatus)", "Member(io.vyne.demos.tradeCompliance.services.Trader/jurisdiction) -[Is an attribute of]-> Type(io.vyne.demos.tradeCompliance.services.Trader)", "Member(io.vyne.demos.tradeCompliance.services.Trader/jurisdiction) -[Is type of]-> Type(io.vyne.TraderJurisdiction)", "Member(io.vyne.demos.tradeCompliance.services.Client/name) -[Is an attribute of]-> Type(io.vyne.demos.tradeCompliance.services.Client)", "Member(io.vyne.demos.tradeCompliance.services.Client/name) -[Is type of]-> Type(lang.taxi.String)", "Operation(io.vyne.tradeCompliance.aggregator.TradeComplianceEvaluator@@evaluate) -[Requires parameter]-> Parameter(param/io.vyne.tradeCompliance.aggregator.TradeRequest)", "Operation(io.vyne.tradeCompliance.aggregator.TradeComplianceEvaluator@@evaluate) -[provides]-> Type_instance(io.vyne.tradeCompliance.aggregator.TradeComplianceResult)", "Parameter(param/io.vyne.TraderJurisdiction) -[Is parameter on]-> Parameter(param/io.vyne.tradeCompliance.rules.JurisdictionRuleRequest)", "Type_instance(io.vyne.tradeCompliance.TradeComplianceStatus) -[can populate]-> Parameter(param/io.vyne.tradeCompliance.TradeComplianceStatus)", "Type_instance(io.vyne.tradeCompliance.TradeComplianceStatus) -[Is instanceOfType of]-> Type(io.vyne.tradeCompliance.TradeComplianceStatus)", "Member(io.vyne.tradeCompliance.rules.TradeValueRuleResponse/status) -[Is an attribute of]-> Type(io.vyne.tradeCompliance.rules.TradeValueRuleResponse)", "Member(io.vyne.tradeCompliance.rules.TradeValueRuleResponse/status) -[Is type of]-> Type(io.vyne.tradeCompliance.rules.RagStatus)", "Member(io.vyne.demos.tradeCompliance.services.Client/jurisdiction) -[Is an attribute of]-> Type(io.vyne.demos.tradeCompliance.services.Client)", "Member(io.vyne.demos.tradeCompliance.services.Client/jurisdiction) -[Is type of]-> Type(io.vyne.ClientJurisdiction)", "Type(io.vyne.tradeCompliance.aggregator.TradeRequest) -[Has attribute]-> Member(io.vyne.tradeCompliance.aggregator.TradeRequest/clientId)", "Type(io.vyne.tradeCompliance.aggregator.TradeRequest) -[Has attribute]-> Member(io.vyne.tradeCompliance.aggregator.TradeRequest/notional)", "Type(io.vyne.tradeCompliance.aggregator.TradeRequest) -[Has attribute]-> Member(io.vyne.tradeCompliance.aggregator.TradeRequest/price)", "Type(io.vyne.tradeCompliance.aggregator.TradeRequest) -[Has attribute]-> Member(io.vyne.tradeCompliance.aggregator.TradeRequest/traderId)", "Type_instance(io.vyne.tradeCompliance.TradeValue) -[Is instanceOfType of]-> Type(io.vyne.tradeCompliance.TradeValue)", "Type_instance(io.vyne.tradeCompliance.TradeValue) -[can populate]-> Parameter(param/io.vyne.tradeCompliance.TradeValue)", "Type_instance(io.vyne.tradeCompliance.TradeValue) -[Instance has attribute]-> Provided_instance_member(io.vyne.tradeCompliance.TradeValue/currency)", "Type_instance(io.vyne.tradeCompliance.TradeValue) -[Instance has attribute]-> Provided_instance_member(io.vyne.tradeCompliance.TradeValue/value)", "Parameter(param/io.vyne.Price) -[Is parameter on]-> Parameter(param/io.vyne.demos.tradeCompliance.services.TradeValueRequest)", "Type_instance(io.vyne.TradeNotional) -[can populate]-> Parameter(param/io.vyne.TradeNotional)", "Type_instance(io.vyne.TradeNotional) -[Is instanceOfType of]-> Type(io.vyne.TradeNotional)", "Member(io.vyne.tradeCompliance.rules.NotionalLimitRuleResponse/status) -[Is an attribute of]-> Type(io.vyne.tradeCompliance.rules.NotionalLimitRuleResponse)", "Member(io.vyne.tradeCompliance.rules.NotionalLimitRuleResponse/status) -[Is type of]-> Type(io.vyne.tradeCompliance.rules.RagStatus)", "Type(io.vyne.demos.tradeCompliance.services.TradeValueRequest) -[Has attribute]-> Member(io.vyne.demos.tradeCompliance.services.TradeValueRequest/notional)", "Type(io.vyne.demos.tradeCompliance.services.TradeValueRequest) -[Has attribute]-> Member(io.vyne.demos.tradeCompliance.services.TradeValueRequest/price)", "Type_instance(io.vyne.tradeCompliance.rules.RagStatus) -[can populate]-> Parameter(param/io.vyne.tradeCompliance.rules.RagStatus)", "Type_instance(io.vyne.tradeCompliance.rules.RagStatus) -[Is instanceOfType of]-> Type(io.vyne.tradeCompliance.rules.RagStatus)", "Type_instance(io.vyne.Username) -[can populate]-> Parameter(param/io.vyne.Username)", "Type_instance(io.vyne.Username) -[Is instanceOfType of]-> Type(io.vyne.Username)", "Type_instance(io.vyne.tradeCompliance.rules.NotionalLimitRuleResponse) -[Is instanceOfType of]-> Type(io.vyne.tradeCompliance.rules.NotionalLimitRuleResponse)", "Type_instance(io.vyne.tradeCompliance.rules.NotionalLimitRuleResponse) -[can populate]-> Parameter(param/io.vyne.tradeCompliance.rules.NotionalLimitRuleResponse)", "Type_instance(io.vyne.tradeCompliance.rules.NotionalLimitRuleResponse) -[can populate]-> Parameter(param/io.vyne.tradeCompliance.rules.RuleEvaluationResult)", "Type_instance(io.vyne.tradeCompliance.rules.NotionalLimitRuleResponse) -[Instance has attribute]-> Provided_instance_member(io.vyne.tradeCompliance.rules.NotionalLimitRuleResponse/message)", "Type_instance(io.vyne.tradeCompliance.rules.NotionalLimitRuleResponse) -[Instance has attribute]-> Provided_instance_member(io.vyne.tradeCompliance.rules.NotionalLimitRuleResponse/ruleId)", "Type_instance(io.vyne.tradeCompliance.rules.NotionalLimitRuleResponse) -[Instance has attribute]-> Provided_instance_member(io.vyne.tradeCompliance.rules.NotionalLimitRuleResponse/status)", "Provided_instance_member(io.vyne.tradeCompliance.rules.NotionalLimitRuleResponse/message) -[Is an attribute of]-> Type_instance(lang.taxi.String)", "Operation(io.vyne.tradeCompliance.rules.TradeValueRuleService@@evaluate) -[Requires parameter]-> Parameter(param/io.vyne.tradeCompliance.rules.TradeValueRuleRequest)", "Operation(io.vyne.tradeCompliance.rules.TradeValueRuleService@@evaluate) -[provides]-> Type_instance(io.vyne.tradeCompliance.rules.TradeValueRuleResponse)", "Type_instance(io.vyne.tradeCompliance.MoneyAmount) -[can populate]-> Parameter(param/io.vyne.tradeCompliance.MoneyAmount)", "Type_instance(io.vyne.tradeCompliance.MoneyAmount) -[Is instanceOfType of]-> Type(io.vyne.tradeCompliance.MoneyAmount)", "Member(io.vyne.tradeCompliance.aggregator.RuleEvaluationResults/status) -[Is an attribute of]-> Type(io.vyne.tradeCompliance.aggregator.RuleEvaluationResults)", "Member(io.vyne.tradeCompliance.aggregator.RuleEvaluationResults/status) -[Is type of]-> Type(java.util.RagStatus)", "Member(io.vyne.tradeCompliance.rules.RuleEvaluationResult/status) -[Is an attribute of]-> Type(io.vyne.tradeCompliance.rules.RuleEvaluationResult)", "Member(io.vyne.tradeCompliance.rules.RuleEvaluationResult/status) -[Is type of]-> Type(io.vyne.tradeCompliance.rules.RagStatus)", "Provided_instance_member(io.vyne.tradeCompliance.aggregator.TradeComplianceResult/status) -[Is an attribute of]-> Type_instance(io.vyne.tradeCompliance.TradeComplianceStatus)", "Member(io.vyne.demos.tradeCompliance.services.Client/id) -[Is an attribute of]-> Type(io.vyne.demos.tradeCompliance.services.Client)", "Member(io.vyne.demos.tradeCompliance.services.Client/id) -[Is type of]-> Type(io.vyne.ClientId)", "Parameter(param/io.vyne.ClientId) -[Is parameter on]-> Operation(io.vyne.demos.tradeCompliance.services.ClientService@@getClient)", "Type(io.vyne.demos.tradeCompliance.services.Client) -[Has attribute]-> Member(io.vyne.demos.tradeCompliance.services.Client/id)", "Type(io.vyne.demos.tradeCompliance.services.Client) -[Has attribute]-> Member(io.vyne.demos.tradeCompliance.services.Client/jurisdiction)", "Type(io.vyne.demos.tradeCompliance.services.Client) -[Has attribute]-> Member(io.vyne.demos.tradeCompliance.services.Client/name)", "Type(io.vyne.Price) -[Has attribute]-> Member(io.vyne.Price/currency)", "Type(io.vyne.Price) -[Has attribute]-> Member(io.vyne.Price/value)", "Parameter(param/io.vyne.tradeCompliance.MoneyAmount) -[Is parameter on]-> Parameter(param/io.vyne.tradeCompliance.TradeValue)", "Member(io.vyne.tradeCompliance.rules.TradeValueRuleResponse/ruleId) -[Is an attribute of]-> Type(io.vyne.tradeCompliance.rules.TradeValueRuleResponse)", "Member(io.vyne.tradeCompliance.rules.TradeValueRuleResponse/ruleId) -[Is type of]-> Type(lang.taxi.String)", "Type_instance(lang.taxi.String) -[can populate]-> Parameter(param/lang.taxi.String)", "Type_instance(lang.taxi.String) -[Is instanceOfType of]-> Type(lang.taxi.String)", "Parameter(param/io.vyne.tradeCompliance.rules.TradeValueRuleRequest) -[Is parameter on]-> Operation(io.vyne.tradeCompliance.rules.TradeValueRuleService@@evaluate)", "Provided_instance_member(io.vyne.tradeCompliance.aggregator.TradeRequest/notional) -[Is an attribute of]-> Type_instance(io.vyne.TradeNotional)", "Member(io.vyne.tradeCompliance.TraderMaxTradeValue/currency) -[Is an attribute of]-> Type(io.vyne.tradeCompliance.TraderMaxTradeValue)", "Member(io.vyne.tradeCompliance.TraderMaxTradeValue/currency) -[Is type of]-> Type(io.vyne.tradeCompliance.Currency)", "Type_instance(io.vyne.tradeCompliance.aggregator.TradeRequest) -[Is instanceOfType of]-> Type(io.vyne.tradeCompliance.aggregator.TradeRequest)", "Type_instance(io.vyne.tradeCompliance.aggregator.TradeRequest) -[can populate]-> Parameter(param/io.vyne.tradeCompliance.aggregator.TradeRequest)", "Type_instance(io.vyne.tradeCompliance.aggregator.TradeRequest) -[Instance has attribute]-> Provided_instance_member(io.vyne.tradeCompliance.aggregator.TradeRequest/clientId)", "Type_instance(io.vyne.tradeCompliance.aggregator.TradeRequest) -[Instance has attribute]-> Provided_instance_member(io.vyne.tradeCompliance.aggregator.TradeRequest/notional)", "Type_instance(io.vyne.tradeCompliance.aggregator.TradeRequest) -[Instance has attribute]-> Provided_instance_member(io.vyne.tradeCompliance.aggregator.TradeRequest/price)", "Type_instance(io.vyne.tradeCompliance.aggregator.TradeRequest) -[Instance has attribute]-> Provided_instance_member(io.vyne.tradeCompliance.aggregator.TradeRequest/traderId)", "Provided_instance_member(io.vyne.demos.tradeCompliance.services.Trader/jurisdiction) -[Is an attribute of]-> Type_instance(io.vyne.TraderJurisdiction)", "Type(io.vyne.tradeCompliance.rules.JurisdictionRuleRequest) -[Has attribute]-> Member(io.vyne.tradeCompliance.rules.JurisdictionRuleRequest/clientJurisdiction)", "Type(io.vyne.tradeCompliance.rules.JurisdictionRuleRequest) -[Has attribute]-> Member(io.vyne.tradeCompliance.rules.JurisdictionRuleRequest/traderJurisdiction)", "Type_instance(io.vyne.tradeCompliance.Currency) -[can populate]-> Parameter(param/io.vyne.tradeCompliance.Currency)", "Type_instance(io.vyne.tradeCompliance.Currency) -[Is instanceOfType of]-> Type(io.vyne.tradeCompliance.Currency)", "Type_instance(io.vyne.tradeCompliance.rules.TradeValueRuleResponse) -[Is instanceOfType of]-> Type(io.vyne.tradeCompliance.rules.TradeValueRuleResponse)", "Type_instance(io.vyne.tradeCompliance.rules.TradeValueRuleResponse) -[can populate]-> Parameter(param/io.vyne.tradeCompliance.rules.TradeValueRuleResponse)", "Type_instance(io.vyne.tradeCompliance.rules.TradeValueRuleResponse) -[can populate]-> Parameter(param/io.vyne.tradeCompliance.rules.RuleEvaluationResult)", "Type_instance(io.vyne.tradeCompliance.rules.TradeValueRuleResponse) -[Instance has attribute]-> Provided_instance_member(io.vyne.tradeCompliance.rules.TradeValueRuleResponse/message)", "Type_instance(io.vyne.tradeCompliance.rules.TradeValueRuleResponse) -[Instance has attribute]-> Provided_instance_member(io.vyne.tradeCompliance.rules.TradeValueRuleResponse/ruleId)", "Type_instance(io.vyne.tradeCompliance.rules.TradeValueRuleResponse) -[Instance has attribute]-> Provided_instance_member(io.vyne.tradeCompliance.rules.TradeValueRuleResponse/status)", "Member(io.vyne.tradeCompliance.aggregator.TradeRequest/notional) -[Is an attribute of]-> Type(io.vyne.tradeCompliance.aggregator.TradeRequest)", "Member(io.vyne.tradeCompliance.aggregator.TradeRequest/notional) -[Is type of]-> Type(io.vyne.TradeNotional)", "Provided_instance_member(io.vyne.tradeCompliance.rules.TradeValueRuleResponse/ruleId) -[Is an attribute of]-> Type_instance(lang.taxi.String)", "Member(io.vyne.tradeCompliance.aggregator.TradeComplianceResult/status) -[Is an attribute of]-> Type(io.vyne.tradeCompliance.aggregator.TradeComplianceResult)", "Member(io.vyne.tradeCompliance.aggregator.TradeComplianceResult/status) -[Is type of]-> Type(io.vyne.tradeCompliance.TradeComplianceStatus)", "Member(io.vyne.tradeCompliance.rules.RuleEvaluationResult/message) -[Is an attribute of]-> Type(io.vyne.tradeCompliance.rules.RuleEvaluationResult)", "Member(io.vyne.tradeCompliance.rules.RuleEvaluationResult/message) -[Is type of]-> Type(lang.taxi.String)", "Member(io.vyne.tradeCompliance.rules.RuleEvaluationResult/ruleId) -[Is an attribute of]-> Type(io.vyne.tradeCompliance.rules.RuleEvaluationResult)", "Member(io.vyne.tradeCompliance.rules.RuleEvaluationResult/ruleId) -[Is type of]-> Type(lang.taxi.String)", "Type(java.util.RuleEvaluationResult) -[Has attribute]-> Member(java.util.RuleEvaluationResult/message)", "Type(java.util.RuleEvaluationResult) -[Has attribute]-> Member(java.util.RuleEvaluationResult/ruleId)", "Type(java.util.RuleEvaluationResult) -[Has attribute]-> Member(java.util.RuleEvaluationResult/status)", "Type(io.vyne.tradeCompliance.rules.NotionalLimitRuleResponse) -[Has attribute]-> Member(io.vyne.tradeCompliance.rules.NotionalLimitRuleResponse/message)", "Type(io.vyne.tradeCompliance.rules.NotionalLimitRuleResponse) -[Has attribute]-> Member(io.vyne.tradeCompliance.rules.NotionalLimitRuleResponse/ruleId)", "Type(io.vyne.tradeCompliance.rules.NotionalLimitRuleResponse) -[Has attribute]-> Member(io.vyne.tradeCompliance.rules.NotionalLimitRuleResponse/status)", "Member(io.vyne.Price/currency) -[Is an attribute of]-> Type(io.vyne.Price)", "Member(io.vyne.Price/currency) -[Is type of]-> Type(io.vyne.tradeCompliance.Currency)", "Provided_instance_member(io.vyne.tradeCompliance.TradeValue/value) -[Is an attribute of]-> Type_instance(io.vyne.tradeCompliance.MoneyAmount)", "Operation(io.vyne.tradeCompliance.rules.NotionalLimitRuleService@@evaluate) -[Requires parameter]-> Parameter(param/io.vyne.tradeCompliance.rules.NotionalLimitRuleRequest)", "Operation(io.vyne.tradeCompliance.rules.NotionalLimitRuleService@@evaluate) -[provides]-> Type_instance(io.vyne.tradeCompliance.rules.NotionalLimitRuleResponse)", "Type(io.vyne.tradeCompliance.TraderMaxTradeValue) -[Has attribute]-> Member(io.vyne.tradeCompliance.TraderMaxTradeValue/currency)", "Type(io.vyne.tradeCompliance.TraderMaxTradeValue) -[Has attribute]-> Member(io.vyne.tradeCompliance.TraderMaxTradeValue/value)", "Type(io.vyne.tradeCompliance.rules.RuleEvaluationResult) -[Has attribute]-> Member(io.vyne.tradeCompliance.rules.RuleEvaluationResult/message)", "Type(io.vyne.tradeCompliance.rules.RuleEvaluationResult) -[Has attribute]-> Member(io.vyne.tradeCompliance.rules.RuleEvaluationResult/ruleId)", "Type(io.vyne.tradeCompliance.rules.RuleEvaluationResult) -[Has attribute]-> Member(io.vyne.tradeCompliance.rules.RuleEvaluationResult/status)", "Member(io.vyne.tradeCompliance.rules.TradeValueRuleResponse/message) -[Is an attribute of]-> Type(io.vyne.tradeCompliance.rules.TradeValueRuleResponse)", "Member(io.vyne.tradeCompliance.rules.TradeValueRuleResponse/message) -[Is type of]-> Type(lang.taxi.String)", "Member(io.vyne.demos.tradeCompliance.services.TradeValueRequest/price) -[Is an attribute of]-> Type(io.vyne.demos.tradeCompliance.services.TradeValueRequest)", "Member(io.vyne.demos.tradeCompliance.services.TradeValueRequest/price) -[Is type of]-> Type(io.vyne.Price)", "Operation(io.vyne.demos.tradeCompliance.services.ClientService@@getClient) -[Requires parameter]-> Parameter(param/io.vyne.ClientId)", "Operation(io.vyne.demos.tradeCompliance.services.ClientService@@getClient) -[provides]-> Type_instance(io.vyne.demos.tradeCompliance.services.Client)", "Type_instance(io.vyne.tradeCompliance.aggregator.RuleEvaluationResults) -[can populate]-> Parameter(param/io.vyne.tradeCompliance.aggregator.RuleEvaluationResults)", "Type_instance(io.vyne.tradeCompliance.aggregator.RuleEvaluationResults) -[Is instanceOfType of]-> Type(io.vyne.tradeCompliance.aggregator.RuleEvaluationResults)", "Provided_instance_member(io.vyne.tradeCompliance.rules.TradeValueRuleResponse/status) -[Is an attribute of]-> Type_instance(io.vyne.tradeCompliance.rules.RagStatus)", "Type(io.vyne.Money) -[Has attribute]-> Member(io.vyne.Money/currency)", "Type(io.vyne.Money) -[Has attribute]-> Member(io.vyne.Money/value)", "Provided_instance_member(io.vyne.demos.tradeCompliance.services.Client/jurisdiction) -[Is an attribute of]-> Type_instance(io.vyne.ClientJurisdiction)", "Provided_instance_member(io.vyne.tradeCompliance.rules.NotionalLimitRuleResponse/ruleId) -[Is an attribute of]-> Type_instance(lang.taxi.String)", "Provided_instance_member(io.vyne.demos.tradeCompliance.services.Client/id) -[Is an attribute of]-> Type_instance(io.vyne.ClientId)", "Parameter(param/io.vyne.Username) -[Is parameter on]-> Operation(io.vyne.demos.tradeCompliance.services.TraderService@@getTrader)", "Member(io.vyne.tradeCompliance.rules.JurisdictionRuleResponse/status) -[Is an attribute of]-> Type(io.vyne.tradeCompliance.rules.JurisdictionRuleResponse)", "Member(io.vyne.tradeCompliance.rules.JurisdictionRuleResponse/status) -[Is type of]-> Type(io.vyne.tradeCompliance.rules.RagStatus)", "Type(io.vyne.tradeCompliance.rules.TradeValueRuleRequest) -[Has attribute]-> Member(io.vyne.tradeCompliance.rules.TradeValueRuleRequest/tradeValue)", "Type(io.vyne.tradeCompliance.rules.TradeValueRuleRequest) -[Has attribute]-> Member(io.vyne.tradeCompliance.rules.TradeValueRuleRequest/traderLimit)", "Operation(io.vyne.demos.tradeCompliance.services.RateConverterService@@convert) -[Requires parameter]-> Parameter(param/io.vyne.tradeCompliance.Currency)", "Operation(io.vyne.demos.tradeCompliance.services.RateConverterService@@convert) -[Requires parameter]-> Parameter(param/io.vyne.tradeCompliance.TradeValue)", "Operation(io.vyne.demos.tradeCompliance.services.RateConverterService@@convert) -[provides]-> Type_instance(io.vyne.tradeCompliance.TradeValue)", "Member(io.vyne.tradeCompliance.aggregator.RuleEvaluationResults/ruleId) -[Is an attribute of]-> Type(io.vyne.tradeCompliance.aggregator.RuleEvaluationResults)", "Member(io.vyne.tradeCompliance.aggregator.RuleEvaluationResults/ruleId) -[Is type of]-> Type(lang.taxi.String)", "Type_instance(io.vyne.demos.tradeCompliance.services.Trader) -[Is instanceOfType of]-> Type(io.vyne.demos.tradeCompliance.services.Trader)", "Type_instance(io.vyne.demos.tradeCompliance.services.Trader) -[can populate]-> Parameter(param/io.vyne.demos.tradeCompliance.services.Trader)", "Type_instance(io.vyne.demos.tradeCompliance.services.Trader) -[Instance has attribute]-> Provided_instance_member(io.vyne.demos.tradeCompliance.services.Trader/jurisdiction)", "Type_instance(io.vyne.demos.tradeCompliance.services.Trader) -[Instance has attribute]-> Provided_instance_member(io.vyne.demos.tradeCompliance.services.Trader/maxValue)", "Type_instance(io.vyne.demos.tradeCompliance.services.Trader) -[Instance has attribute]-> Provided_instance_member(io.vyne.demos.tradeCompliance.services.Trader/username)", "Member(io.vyne.tradeCompliance.rules.NotionalLimitRuleRequest/notional) -[Is an attribute of]-> Type(io.vyne.tradeCompliance.rules.NotionalLimitRuleRequest)", "Member(io.vyne.tradeCompliance.rules.NotionalLimitRuleRequest/notional) -[Is type of]-> Type(io.vyne.TradeNotional)", "Type_instance(io.vyne.tradeCompliance.TraderMaxTradeValue) -[can populate]-> Parameter(param/io.vyne.tradeCompliance.TraderMaxTradeValue)", "Type_instance(io.vyne.tradeCompliance.TraderMaxTradeValue) -[Is instanceOfType of]-> Type(io.vyne.tradeCompliance.TraderMaxTradeValue)", "Parameter(param/io.vyne.tradeCompliance.TradeValue) -[Is parameter on]-> Parameter(param/io.vyne.tradeCompliance.rules.TradeValueRuleRequest)", "Parameter(param/io.vyne.tradeCompliance.TradeValue) -[Is parameter on]-> Operation(io.vyne.demos.tradeCompliance.services.RateConverterService@@convert)", "Provided_instance_member(io.vyne.tradeCompliance.aggregator.TradeRequest/clientId) -[Is an attribute of]-> Type_instance(io.vyne.ClientId)", "Provided_instance_member(io.vyne.tradeCompliance.rules.JurisdictionRuleResponse/message) -[Is an attribute of]-> Type_instance(lang.taxi.String)", "Provided_instance_member(io.vyne.tradeCompliance.rules.NotionalLimitRuleResponse/status) -[Is an attribute of]-> Type_instance(io.vyne.tradeCompliance.rules.RagStatus)", "Type_instance(io.vyne.tradeCompliance.rules.JurisdictionRuleResponse) -[Is instanceOfType of]-> Type(io.vyne.tradeCompliance.rules.JurisdictionRuleResponse)", "Type_instance(io.vyne.tradeCompliance.rules.JurisdictionRuleResponse) -[can populate]-> Parameter(param/io.vyne.tradeCompliance.rules.JurisdictionRuleResponse)", "Type_instance(io.vyne.tradeCompliance.rules.JurisdictionRuleResponse) -[can populate]-> Parameter(param/io.vyne.tradeCompliance.rules.RuleEvaluationResult)", "Type_instance(io.vyne.tradeCompliance.rules.JurisdictionRuleResponse) -[Instance has attribute]-> Provided_instance_member(io.vyne.tradeCompliance.rules.JurisdictionRuleResponse/message)", "Type_instance(io.vyne.tradeCompliance.rules.JurisdictionRuleResponse) -[Instance has attribute]-> Provided_instance_member(io.vyne.tradeCompliance.rules.JurisdictionRuleResponse/ruleId)", "Type_instance(io.vyne.tradeCompliance.rules.JurisdictionRuleResponse) -[Instance has attribute]-> Provided_instance_member(io.vyne.tradeCompliance.rules.JurisdictionRuleResponse/status)", "Type(io.vyne.tradeCompliance.aggregator.RuleEvaluationResults) -[Has attribute]-> Member(io.vyne.tradeCompliance.aggregator.RuleEvaluationResults/message)", "Type(io.vyne.tradeCompliance.aggregator.RuleEvaluationResults) -[Has attribute]-> Member(io.vyne.tradeCompliance.aggregator.RuleEvaluationResults/ruleId)", "Type(io.vyne.tradeCompliance.aggregator.RuleEvaluationResults) -[Has attribute]-> Member(io.vyne.tradeCompliance.aggregator.RuleEvaluationResults/status)", "Member(io.vyne.Money/value) -[Is an attribute of]-> Type(io.vyne.Money)", "Member(io.vyne.Money/value) -[Is type of]-> Type(io.vyne.tradeCompliance.MoneyAmount)", "Member(io.vyne.tradeCompliance.rules.NotionalLimitRuleResponse/message) -[Is an attribute of]-> Type(io.vyne.tradeCompliance.rules.NotionalLimitRuleResponse)", "Member(io.vyne.tradeCompliance.rules.NotionalLimitRuleResponse/message) -[Is type of]-> Type(lang.taxi.String)", "Operation(io.vyne.demos.tradeCompliance.services.TradeValueService@@calculateValue) -[Requires parameter]-> Parameter(param/io.vyne.demos.tradeCompliance.services.TradeValueRequest)", "Operation(io.vyne.demos.tradeCompliance.services.TradeValueService@@calculateValue) -[provides]-> Type_instance(io.vyne.tradeCompliance.TradeValue)", "Type_instance(io.vyne.TraderJurisdiction) -[can populate]-> Parameter(param/io.vyne.TraderJurisdiction)", "Type_instance(io.vyne.TraderJurisdiction) -[Is instanceOfType of]-> Type(io.vyne.TraderJurisdiction)"],
          "Discovered path": ["io.vyne.tradeCompliance.aggregator.TradeRequest -[Instance has attribute]-> io.vyne.tradeCompliance.aggregator.TradeRequest/clientId", "io.vyne.tradeCompliance.aggregator.TradeRequest/clientId -[Is an attribute of]-> io.vyne.ClientId", "io.vyne.ClientId -[can populate]-> param/io.vyne.ClientId", "param/io.vyne.ClientId -[Is parameter on]-> io.vyne.demos.tradeCompliance.services.ClientService@@getClient", "io.vyne.demos.tradeCompliance.services.ClientService@@getClient -[provides]-> io.vyne.demos.tradeCompliance.services.Client", "io.vyne.demos.tradeCompliance.services.Client -[Is instanceOfType of]-> io.vyne.demos.tradeCompliance.services.Client", "io.vyne.demos.tradeCompliance.services.Client -[Has attribute]-> io.vyne.demos.tradeCompliance.services.Client/jurisdiction", "io.vyne.demos.tradeCompliance.services.Client/jurisdiction -[Is type of]-> io.vyne.ClientJurisdiction"]
        },
        "remoteCalls": [],
        "result": {
          "startTime": 1535452739982,
          "endTime": 1535452739988,
          "value": {
            "first": {
              "type": {
                "name": {
                  "fullyQualifiedName": "io.vyne.ClientJurisdiction",
                  "name": "ClientJurisdiction"
                },
                "attributes": {},
                "modifiers": ["PRIMITIVE"],
                "aliasForType": {"fullyQualifiedName": "lang.taxi.String", "name": "String"},
                "inherits": [],
                "sources": [{"origin": "Unknown", "language": "Taxi", "content": ""}],
                "inheritanceGraph": [],
                "fullyQualifiedName": "io.vyne.ClientJurisdiction",
                "typeAlias": true,
                "parameterType": false,
                "scalar": true
              }, "value": "GBP"
            },
            "second": {
              "start": {
                "fullyQualifiedName": "io.vyne.tradeCompliance.aggregator.TradeRequest",
                "name": "TradeRequest"
              },
              "target": {"fullyQualifiedName": "io.vyne.ClientJurisdiction", "name": "ClientJurisdiction"},
              "links": [{
                "start": {
                  "fullyQualifiedName": "io.vyne.tradeCompliance.aggregator.TradeRequest",
                  "name": "TradeRequest"
                },
                "relationship": "INSTANCE_HAS_ATTRIBUTE",
                "end": {
                  "fullyQualifiedName": "io.vyne.tradeCompliance.aggregator.TradeRequest/clientId",
                  "name": "TradeRequest/clientId"
                },
                "cost": 1
              }, {
                "start": {
                  "fullyQualifiedName": "io.vyne.tradeCompliance.aggregator.TradeRequest/clientId",
                  "name": "TradeRequest/clientId"
                },
                "relationship": "IS_ATTRIBUTE_OF",
                "end": {"fullyQualifiedName": "io.vyne.ClientId", "name": "ClientId"},
                "cost": 2
              }, {
                "start": {"fullyQualifiedName": "io.vyne.ClientId", "name": "ClientId"},
                "relationship": "CAN_POPULATE",
                "end": {"fullyQualifiedName": "param/io.vyne.ClientId", "name": "ClientId"},
                "cost": 3
              }, {
                "start": {"fullyQualifiedName": "param/io.vyne.ClientId", "name": "ClientId"},
                "relationship": "IS_PARAMETER_ON",
                "end": {
                  "fullyQualifiedName": "io.vyne.demos.tradeCompliance.services.ClientService@@getClient",
                  "name": "ClientService@@getClient"
                },
                "cost": 4
              }, {
                "start": {
                  "fullyQualifiedName": "io.vyne.demos.tradeCompliance.services.ClientService@@getClient",
                  "name": "ClientService@@getClient"
                },
                "relationship": "PROVIDES",
                "end": {"fullyQualifiedName": "io.vyne.demos.tradeCompliance.services.Client", "name": "Client"},
                "cost": 5
              }, {
                "start": {"fullyQualifiedName": "io.vyne.demos.tradeCompliance.services.Client", "name": "Client"},
                "relationship": "IS_INSTANCE_OF",
                "end": {"fullyQualifiedName": "io.vyne.demos.tradeCompliance.services.Client", "name": "Client"},
                "cost": 6
              }, {
                "start": {"fullyQualifiedName": "io.vyne.demos.tradeCompliance.services.Client", "name": "Client"},
                "relationship": "HAS_ATTRIBUTE",
                "end": {
                  "fullyQualifiedName": "io.vyne.demos.tradeCompliance.services.Client/jurisdiction",
                  "name": "Client/jurisdiction"
                },
                "cost": 7
              }, {
                "start": {
                  "fullyQualifiedName": "io.vyne.demos.tradeCompliance.services.Client/jurisdiction",
                  "name": "Client/jurisdiction"
                },
                "relationship": "IS_TYPE_OF",
                "end": {"fullyQualifiedName": "io.vyne.ClientJurisdiction", "name": "ClientJurisdiction"},
                "cost": 8
              }],
              "exists": true,
              "description": "io.vyne.tradeCompliance.aggregator.TradeRequest -[Instance has attribute]-> io.vyne.tradeCompliance.aggregator.TradeRequest/clientId, io.vyne.tradeCompliance.aggregator.TradeRequest/clientId -[Is an attribute of]-> io.vyne.ClientId, io.vyne.ClientId -[can populate]-> param/io.vyne.ClientId, param/io.vyne.ClientId -[Is parameter on]-> io.vyne.demos.tradeCompliance.services.ClientService@@getClient, io.vyne.demos.tradeCompliance.services.ClientService@@getClient -[provides]-> io.vyne.demos.tradeCompliance.services.Client, io.vyne.demos.tradeCompliance.services.Client -[Is instanceOfType of]-> io.vyne.demos.tradeCompliance.services.Client, io.vyne.demos.tradeCompliance.services.Client -[Has attribute]-> io.vyne.demos.tradeCompliance.services.Client/jurisdiction, io.vyne.demos.tradeCompliance.services.Client/jurisdiction -[Is type of]-> io.vyne.ClientJurisdiction"
            }
          },
          "duration": 6
        },
        "name": "HipsterDiscoverGraphQueryStrategy:Searching for path TradeRequest -> ClientJurisdiction",
        "fullPath": "//io.osmosis.polymer.query.QueryProfiler:Root/DefaultQueryEngine:Query with HipsterDiscoverGraphQueryStrategy/HipsterDiscoverGraphQueryStrategy:Searching for path TradeRequest -> ClientJurisdiction",
        "id": "3c4904b2-a296-40bc-b930-7731187d405e",
        "children": [{
          "componentName": "EdgeNavigator",
          "operationName": "Evaluating Type_instance(io.vyne.tradeCompliance.aggregator.TradeRequest) -[Instance has attribute]-> Provided_instance_member(io.vyne.tradeCompliance.aggregator.TradeRequest/clientId) with evaluator InstanceHasAttributeEdgeEvaluator",
          "type": "GRAPH_TRAVERSAL",
          "path": "//io.osmosis.polymer.query.QueryProfiler:Root/DefaultQueryEngine:Query with HipsterDiscoverGraphQueryStrategy/HipsterDiscoverGraphQueryStrategy:Searching for path TradeRequest -> ClientJurisdiction",
          "context": {},
          "remoteCalls": [],
          "result": {
            "startTime": 1535452739983,
            "endTime": 1535452739983,
            "value": {
              "edge": {
                "relationship": "INSTANCE_HAS_ATTRIBUTE",
                "target": {
                  "value": "io.vyne.tradeCompliance.aggregator.TradeRequest/clientId",
                  "elementType": "PROVIDED_INSTANCE_MEMBER",
                  "instanceValue": null
                },
                "vertex1": {
                  "value": "io.vyne.tradeCompliance.aggregator.TradeRequest",
                  "elementType": "TYPE_INSTANCE",
                  "instanceValue": null
                },
                "vertex2": {
                  "value": "io.vyne.tradeCompliance.aggregator.TradeRequest/clientId",
                  "elementType": "PROVIDED_INSTANCE_MEMBER",
                  "instanceValue": null
                },
                "previousValue": {
                  "clientId": {
                    "type": {
                      "name": {
                        "fullyQualifiedName": "io.vyne.ClientId",
                        "name": "ClientId"
                      },
                      "attributes": {},
                      "modifiers": ["PRIMITIVE"],
                      "aliasForType": {"fullyQualifiedName": "lang.taxi.String", "name": "String"},
                      "inherits": [],
                      "sources": [{"origin": "Unknown", "language": "Taxi", "content": ""}],
                      "inheritanceGraph": [],
                      "fullyQualifiedName": "io.vyne.ClientId",
                      "typeAlias": true,
                      "parameterType": false,
                      "scalar": true
                    }, "value": "kevin"
                  },
                  "notional": {
                    "type": {
                      "name": {
                        "fullyQualifiedName": "io.vyne.TradeNotional",
                        "name": "TradeNotional"
                      },
                      "attributes": {},
                      "modifiers": ["PRIMITIVE"],
                      "aliasForType": {"fullyQualifiedName": "lang.taxi.Decimal", "name": "Decimal"},
                      "inherits": [],
                      "sources": [{"origin": "Unknown", "language": "Taxi", "content": ""}],
                      "inheritanceGraph": [],
                      "fullyQualifiedName": "io.vyne.TradeNotional",
                      "typeAlias": true,
                      "parameterType": false,
                      "scalar": true
                    }, "value": "1000"
                  },
                  "price": {
                    "currency": {
                      "type": {
                        "name": {
                          "fullyQualifiedName": "io.vyne.tradeCompliance.Currency",
                          "name": "Currency"
                        },
                        "attributes": {},
                        "modifiers": ["PRIMITIVE"],
                        "aliasForType": {"fullyQualifiedName": "lang.taxi.String", "name": "String"},
                        "inherits": [],
                        "sources": [{"origin": "Unknown", "language": "Taxi", "content": ""}],
                        "inheritanceGraph": [],
                        "fullyQualifiedName": "io.vyne.tradeCompliance.Currency",
                        "typeAlias": true,
                        "parameterType": false,
                        "scalar": true
                      }, "value": "GBP"
                    },
                    "value": {
                      "type": {
                        "name": {
                          "fullyQualifiedName": "io.vyne.tradeCompliance.MoneyAmount",
                          "name": "MoneyAmount"
                        },
                        "attributes": {},
                        "modifiers": ["PRIMITIVE"],
                        "aliasForType": {"fullyQualifiedName": "lang.taxi.Decimal", "name": "Decimal"},
                        "inherits": [],
                        "sources": [{"origin": "Unknown", "language": "Taxi", "content": ""}],
                        "inheritanceGraph": [],
                        "fullyQualifiedName": "io.vyne.tradeCompliance.MoneyAmount",
                        "typeAlias": true,
                        "parameterType": false,
                        "scalar": true
                      }, "value": "1.2"
                    }
                  },
                  "traderId": {
                    "type": {
                      "name": {"fullyQualifiedName": "io.vyne.Username", "name": "Username"},
                      "attributes": {},
                      "modifiers": ["PRIMITIVE"],
                      "aliasForType": {"fullyQualifiedName": "lang.taxi.String", "name": "String"},
                      "inherits": [],
                      "sources": [{"origin": "Unknown", "language": "Taxi", "content": ""}],
                      "inheritanceGraph": [],
                      "fullyQualifiedName": "io.vyne.Username",
                      "typeAlias": true,
                      "parameterType": false,
                      "scalar": true
                    }, "value": "jimmy"
                  }
                },
                "description": "Type_instance(io.vyne.tradeCompliance.aggregator.TradeRequest) -[Instance has attribute]-> Provided_instance_member(io.vyne.tradeCompliance.aggregator.TradeRequest/clientId)"
              },
              "resultGraphElement": {
                "value": "io.vyne.tradeCompliance.aggregator.TradeRequest/clientId",
                "elementType": "PROVIDED_INSTANCE_MEMBER",
                "instanceValue": null
              },
              "resultValue": {
                "type": {
                  "name": {"fullyQualifiedName": "io.vyne.ClientId", "name": "ClientId"},
                  "attributes": {},
                  "modifiers": ["PRIMITIVE"],
                  "aliasForType": {"fullyQualifiedName": "lang.taxi.String", "name": "String"},
                  "inherits": [],
                  "sources": [{"origin": "Unknown", "language": "Taxi", "content": ""}],
                  "inheritanceGraph": [],
                  "fullyQualifiedName": "io.vyne.ClientId",
                  "typeAlias": true,
                  "parameterType": false,
                  "scalar": true
                }, "value": "kevin"
              },
              "error": null,
              "element": {
                "value": "io.vyne.tradeCompliance.aggregator.TradeRequest/clientId",
                "elementType": "PROVIDED_INSTANCE_MEMBER",
                "instanceValue": null
              },
              "wasSuccessful": true
            },
            "duration": 0
          },
          "name": "EdgeNavigator:Evaluating Type_instance(io.vyne.tradeCompliance.aggregator.TradeRequest) -[Instance has attribute]-> Provided_instance_member(io.vyne.tradeCompliance.aggregator.TradeRequest/clientId) with evaluator InstanceHasAttributeEdgeEvaluator",
          "fullPath": "//io.osmosis.polymer.query.QueryProfiler:Root/DefaultQueryEngine:Query with HipsterDiscoverGraphQueryStrategy/HipsterDiscoverGraphQueryStrategy:Searching for path TradeRequest -> ClientJurisdiction/EdgeNavigator:Evaluating Type_instance(io.vyne.tradeCompliance.aggregator.TradeRequest) -[Instance has attribute]-> Provided_instance_member(io.vyne.tradeCompliance.aggregator.TradeRequest/clientId) with evaluator InstanceHasAttributeEdgeEvaluator",
          "id": "ba2c2b0e-7bbf-489a-8cad-2c4cce45287d",
          "children": [],
          "duration": 0,
          "description": "EdgeNavigator.Evaluating Type_instance(io.vyne.tradeCompliance.aggregator.TradeRequest) -[Instance has attribute]-> Provided_instance_member(io.vyne.tradeCompliance.aggregator.TradeRequest/clientId) with evaluator InstanceHasAttributeEdgeEvaluator"
        }, {
          "componentName": "EdgeNavigator",
          "operationName": "Evaluating Provided_instance_member(io.vyne.tradeCompliance.aggregator.TradeRequest/clientId) -[Is an attribute of]-> Type_instance(io.vyne.ClientId) with evaluator AttributeOfEdgeEvaluator",
          "type": "GRAPH_TRAVERSAL",
          "path": "//io.osmosis.polymer.query.QueryProfiler:Root/DefaultQueryEngine:Query with HipsterDiscoverGraphQueryStrategy/HipsterDiscoverGraphQueryStrategy:Searching for path TradeRequest -> ClientJurisdiction",
          "context": {},
          "remoteCalls": [],
          "result": {
            "startTime": 1535452739983,
            "endTime": 1535452739983,
            "value": {
              "edge": {
                "relationship": "IS_ATTRIBUTE_OF",
                "target": {"value": "io.vyne.ClientId", "elementType": "TYPE_INSTANCE", "instanceValue": null},
                "vertex1": {
                  "value": "io.vyne.tradeCompliance.aggregator.TradeRequest/clientId",
                  "elementType": "PROVIDED_INSTANCE_MEMBER",
                  "instanceValue": null
                },
                "vertex2": {"value": "io.vyne.ClientId", "elementType": "TYPE_INSTANCE", "instanceValue": null},
                "previousValue": {
                  "type": {
                    "name": {"fullyQualifiedName": "io.vyne.ClientId", "name": "ClientId"},
                    "attributes": {},
                    "modifiers": ["PRIMITIVE"],
                    "aliasForType": {"fullyQualifiedName": "lang.taxi.String", "name": "String"},
                    "inherits": [],
                    "sources": [{"origin": "Unknown", "language": "Taxi", "content": ""}],
                    "inheritanceGraph": [],
                    "fullyQualifiedName": "io.vyne.ClientId",
                    "typeAlias": true,
                    "parameterType": false,
                    "scalar": true
                  }, "value": "kevin"
                },
                "description": "Provided_instance_member(io.vyne.tradeCompliance.aggregator.TradeRequest/clientId) -[Is an attribute of]-> Type_instance(io.vyne.ClientId)"
              },
              "resultGraphElement": {
                "value": "io.vyne.ClientId",
                "elementType": "TYPE_INSTANCE",
                "instanceValue": null
              },
              "resultValue": {
                "type": {
                  "name": {"fullyQualifiedName": "io.vyne.ClientId", "name": "ClientId"},
                  "attributes": {},
                  "modifiers": ["PRIMITIVE"],
                  "aliasForType": {"fullyQualifiedName": "lang.taxi.String", "name": "String"},
                  "inherits": [],
                  "sources": [{"origin": "Unknown", "language": "Taxi", "content": ""}],
                  "inheritanceGraph": [],
                  "fullyQualifiedName": "io.vyne.ClientId",
                  "typeAlias": true,
                  "parameterType": false,
                  "scalar": true
                }, "value": "kevin"
              },
              "error": null,
              "element": {"value": "io.vyne.ClientId", "elementType": "TYPE_INSTANCE", "instanceValue": null},
              "wasSuccessful": true
            },
            "duration": 0
          },
          "name": "EdgeNavigator:Evaluating Provided_instance_member(io.vyne.tradeCompliance.aggregator.TradeRequest/clientId) -[Is an attribute of]-> Type_instance(io.vyne.ClientId) with evaluator AttributeOfEdgeEvaluator",
          "fullPath": "//io.osmosis.polymer.query.QueryProfiler:Root/DefaultQueryEngine:Query with HipsterDiscoverGraphQueryStrategy/HipsterDiscoverGraphQueryStrategy:Searching for path TradeRequest -> ClientJurisdiction/EdgeNavigator:Evaluating Provided_instance_member(io.vyne.tradeCompliance.aggregator.TradeRequest/clientId) -[Is an attribute of]-> Type_instance(io.vyne.ClientId) with evaluator AttributeOfEdgeEvaluator",
          "id": "cffa43d2-c839-4e9b-8740-53207d3a6b5f",
          "children": [],
          "duration": 0,
          "description": "EdgeNavigator.Evaluating Provided_instance_member(io.vyne.tradeCompliance.aggregator.TradeRequest/clientId) -[Is an attribute of]-> Type_instance(io.vyne.ClientId) with evaluator AttributeOfEdgeEvaluator"
        }, {
          "componentName": "EdgeNavigator",
          "operationName": "Evaluating Type_instance(io.vyne.ClientId) -[can populate]-> Parameter(param/io.vyne.ClientId) with evaluator CanPopulateEdgeEvaluator",
          "type": "GRAPH_TRAVERSAL",
          "path": "//io.osmosis.polymer.query.QueryProfiler:Root/DefaultQueryEngine:Query with HipsterDiscoverGraphQueryStrategy/HipsterDiscoverGraphQueryStrategy:Searching for path TradeRequest -> ClientJurisdiction",
          "context": {},
          "remoteCalls": [],
          "result": {
            "startTime": 1535452739983,
            "endTime": 1535452739983,
            "value": {
              "edge": {
                "relationship": "CAN_POPULATE",
                "target": {"value": "param/io.vyne.ClientId", "elementType": "PARAMETER", "instanceValue": null},
                "vertex1": {"value": "io.vyne.ClientId", "elementType": "TYPE_INSTANCE", "instanceValue": null},
                "vertex2": {"value": "param/io.vyne.ClientId", "elementType": "PARAMETER", "instanceValue": null},
                "previousValue": {
                  "type": {
                    "name": {"fullyQualifiedName": "io.vyne.ClientId", "name": "ClientId"},
                    "attributes": {},
                    "modifiers": ["PRIMITIVE"],
                    "aliasForType": {"fullyQualifiedName": "lang.taxi.String", "name": "String"},
                    "inherits": [],
                    "sources": [{"origin": "Unknown", "language": "Taxi", "content": ""}],
                    "inheritanceGraph": [],
                    "fullyQualifiedName": "io.vyne.ClientId",
                    "typeAlias": true,
                    "parameterType": false,
                    "scalar": true
                  }, "value": "kevin"
                },
                "description": "Type_instance(io.vyne.ClientId) -[can populate]-> Parameter(param/io.vyne.ClientId)"
              },
              "resultGraphElement": {
                "value": "param/io.vyne.ClientId",
                "elementType": "PARAMETER",
                "instanceValue": null
              },
              "resultValue": {
                "type": {
                  "name": {"fullyQualifiedName": "io.vyne.ClientId", "name": "ClientId"},
                  "attributes": {},
                  "modifiers": ["PRIMITIVE"],
                  "aliasForType": {"fullyQualifiedName": "lang.taxi.String", "name": "String"},
                  "inherits": [],
                  "sources": [{"origin": "Unknown", "language": "Taxi", "content": ""}],
                  "inheritanceGraph": [],
                  "fullyQualifiedName": "io.vyne.ClientId",
                  "typeAlias": true,
                  "parameterType": false,
                  "scalar": true
                }, "value": "kevin"
              },
              "error": null,
              "element": {"value": "param/io.vyne.ClientId", "elementType": "PARAMETER", "instanceValue": null},
              "wasSuccessful": true
            },
            "duration": 0
          },
          "name": "EdgeNavigator:Evaluating Type_instance(io.vyne.ClientId) -[can populate]-> Parameter(param/io.vyne.ClientId) with evaluator CanPopulateEdgeEvaluator",
          "fullPath": "//io.osmosis.polymer.query.QueryProfiler:Root/DefaultQueryEngine:Query with HipsterDiscoverGraphQueryStrategy/HipsterDiscoverGraphQueryStrategy:Searching for path TradeRequest -> ClientJurisdiction/EdgeNavigator:Evaluating Type_instance(io.vyne.ClientId) -[can populate]-> Parameter(param/io.vyne.ClientId) with evaluator CanPopulateEdgeEvaluator",
          "id": "e4dea843-cfab-4600-a653-71c0e49e77dd",
          "children": [],
          "duration": 0,
          "description": "EdgeNavigator.Evaluating Type_instance(io.vyne.ClientId) -[can populate]-> Parameter(param/io.vyne.ClientId) with evaluator CanPopulateEdgeEvaluator"
        }, {
          "componentName": "EdgeNavigator",
          "operationName": "Evaluating Parameter(param/io.vyne.ClientId) -[Is parameter on]-> Operation(io.vyne.demos.tradeCompliance.services.ClientService@@getClient) with evaluator OperationParameterEdgeEvaluator",
          "type": "GRAPH_TRAVERSAL",
          "path": "//io.osmosis.polymer.query.QueryProfiler:Root/DefaultQueryEngine:Query with HipsterDiscoverGraphQueryStrategy/HipsterDiscoverGraphQueryStrategy:Searching for path TradeRequest -> ClientJurisdiction",
          "context": {},
          "remoteCalls": [],
          "result": {
            "startTime": 1535452739983,
            "endTime": 1535452739983,
            "value": {
              "edge": {
                "relationship": "IS_PARAMETER_ON",
                "target": {
                  "value": "io.vyne.demos.tradeCompliance.services.ClientService@@getClient",
                  "elementType": "OPERATION",
                  "instanceValue": null
                },
                "vertex1": {"value": "param/io.vyne.ClientId", "elementType": "PARAMETER", "instanceValue": null},
                "vertex2": {
                  "value": "io.vyne.demos.tradeCompliance.services.ClientService@@getClient",
                  "elementType": "OPERATION",
                  "instanceValue": null
                },
                "previousValue": {
                  "type": {
                    "name": {"fullyQualifiedName": "io.vyne.ClientId", "name": "ClientId"},
                    "attributes": {},
                    "modifiers": ["PRIMITIVE"],
                    "aliasForType": {"fullyQualifiedName": "lang.taxi.String", "name": "String"},
                    "inherits": [],
                    "sources": [{"origin": "Unknown", "language": "Taxi", "content": ""}],
                    "inheritanceGraph": [],
                    "fullyQualifiedName": "io.vyne.ClientId",
                    "typeAlias": true,
                    "parameterType": false,
                    "scalar": true
                  }, "value": "kevin"
                },
                "description": "Parameter(param/io.vyne.ClientId) -[Is parameter on]-> Operation(io.vyne.demos.tradeCompliance.services.ClientService@@getClient)"
              },
              "resultGraphElement": {
                "value": "io.vyne.demos.tradeCompliance.services.ClientService@@getClient",
                "elementType": "OPERATION",
                "instanceValue": null
              },
              "resultValue": {
                "type": {
                  "name": {"fullyQualifiedName": "io.vyne.ClientId", "name": "ClientId"},
                  "attributes": {},
                  "modifiers": ["PRIMITIVE"],
                  "aliasForType": {"fullyQualifiedName": "lang.taxi.String", "name": "String"},
                  "inherits": [],
                  "sources": [{"origin": "Unknown", "language": "Taxi", "content": ""}],
                  "inheritanceGraph": [],
                  "fullyQualifiedName": "io.vyne.ClientId",
                  "typeAlias": true,
                  "parameterType": false,
                  "scalar": true
                }, "value": "kevin"
              },
              "error": null,
              "element": {
                "value": "io.vyne.demos.tradeCompliance.services.ClientService@@getClient",
                "elementType": "OPERATION",
                "instanceValue": null
              },
              "wasSuccessful": true
            },
            "duration": 0
          },
          "name": "EdgeNavigator:Evaluating Parameter(param/io.vyne.ClientId) -[Is parameter on]-> Operation(io.vyne.demos.tradeCompliance.services.ClientService@@getClient) with evaluator OperationParameterEdgeEvaluator",
          "fullPath": "//io.osmosis.polymer.query.QueryProfiler:Root/DefaultQueryEngine:Query with HipsterDiscoverGraphQueryStrategy/HipsterDiscoverGraphQueryStrategy:Searching for path TradeRequest -> ClientJurisdiction/EdgeNavigator:Evaluating Parameter(param/io.vyne.ClientId) -[Is parameter on]-> Operation(io.vyne.demos.tradeCompliance.services.ClientService@@getClient) with evaluator OperationParameterEdgeEvaluator",
          "id": "270a0359-75f0-4b30-a631-875ca0037545",
          "children": [],
          "duration": 0,
          "description": "EdgeNavigator.Evaluating Parameter(param/io.vyne.ClientId) -[Is parameter on]-> Operation(io.vyne.demos.tradeCompliance.services.ClientService@@getClient) with evaluator OperationParameterEdgeEvaluator"
        }, {
          "componentName": "EdgeNavigator",
          "operationName": "Evaluating Operation(io.vyne.demos.tradeCompliance.services.ClientService@@getClient) -[provides]-> Type_instance(io.vyne.demos.tradeCompliance.services.Client) with evaluator OperationInvocationEvaluator",
          "type": "GRAPH_TRAVERSAL",
          "path": "//io.osmosis.polymer.query.QueryProfiler:Root/DefaultQueryEngine:Query with HipsterDiscoverGraphQueryStrategy/HipsterDiscoverGraphQueryStrategy:Searching for path TradeRequest -> ClientJurisdiction",
          "context": {},
          "remoteCalls": [],
          "result": {
            "startTime": 1535452739983,
            "endTime": 1535452739987,
            "value": {
              "edge": {
                "relationship": "PROVIDES",
                "target": {
                  "value": "io.vyne.demos.tradeCompliance.services.Client",
                  "elementType": "TYPE_INSTANCE",
                  "instanceValue": null
                },
                "vertex1": {
                  "value": "io.vyne.demos.tradeCompliance.services.ClientService@@getClient",
                  "elementType": "OPERATION",
                  "instanceValue": null
                },
                "vertex2": {
                  "value": "io.vyne.demos.tradeCompliance.services.Client",
                  "elementType": "TYPE_INSTANCE",
                  "instanceValue": null
                },
                "previousValue": {
                  "type": {
                    "name": {"fullyQualifiedName": "io.vyne.ClientId", "name": "ClientId"},
                    "attributes": {},
                    "modifiers": ["PRIMITIVE"],
                    "aliasForType": {"fullyQualifiedName": "lang.taxi.String", "name": "String"},
                    "inherits": [],
                    "sources": [{"origin": "Unknown", "language": "Taxi", "content": ""}],
                    "inheritanceGraph": [],
                    "fullyQualifiedName": "io.vyne.ClientId",
                    "typeAlias": true,
                    "parameterType": false,
                    "scalar": true
                  }, "value": "kevin"
                },
                "description": "Operation(io.vyne.demos.tradeCompliance.services.ClientService@@getClient) -[provides]-> Type_instance(io.vyne.demos.tradeCompliance.services.Client)"
              },
              "resultGraphElement": {
                "value": "io.vyne.demos.tradeCompliance.services.Client",
                "elementType": "TYPE_INSTANCE",
                "instanceValue": null
              },
              "resultValue": {
                "id": {
                  "type": {
                    "name": {"fullyQualifiedName": "io.vyne.ClientId", "name": "ClientId"},
                    "attributes": {},
                    "modifiers": ["PRIMITIVE"],
                    "aliasForType": {"fullyQualifiedName": "lang.taxi.String", "name": "String"},
                    "inherits": [],
                    "sources": [{"origin": "Unknown", "language": "Taxi", "content": ""}],
                    "inheritanceGraph": [],
                    "fullyQualifiedName": "io.vyne.ClientId",
                    "typeAlias": true,
                    "parameterType": false,
                    "scalar": true
                  }, "value": "kevin"
                },
                "name": {
                  "type": {
                    "name": {"fullyQualifiedName": "lang.taxi.String", "name": "String"},
                    "attributes": {},
                    "modifiers": ["PRIMITIVE"],
                    "aliasForType": null,
                    "inherits": [],
                    "sources": [{"origin": "Unknown", "language": "Taxi", "content": ""}],
                    "inheritanceGraph": [],
                    "fullyQualifiedName": "lang.taxi.String",
                    "typeAlias": false,
                    "parameterType": false,
                    "scalar": true
                  }, "value": "Kevin Smith"
                },
                "jurisdiction": {
                  "type": {
                    "name": {
                      "fullyQualifiedName": "io.vyne.ClientJurisdiction",
                      "name": "ClientJurisdiction"
                    },
                    "attributes": {},
                    "modifiers": ["PRIMITIVE"],
                    "aliasForType": {"fullyQualifiedName": "lang.taxi.String", "name": "String"},
                    "inherits": [],
                    "sources": [{"origin": "Unknown", "language": "Taxi", "content": ""}],
                    "inheritanceGraph": [],
                    "fullyQualifiedName": "io.vyne.ClientJurisdiction",
                    "typeAlias": true,
                    "parameterType": false,
                    "scalar": true
                  }, "value": "GBP"
                }
              },
              "error": null,
              "element": {
                "value": "io.vyne.demos.tradeCompliance.services.Client",
                "elementType": "TYPE_INSTANCE",
                "instanceValue": null
              },
              "wasSuccessful": true
            },
            "duration": 4
          },
          "name": "EdgeNavigator:Evaluating Operation(io.vyne.demos.tradeCompliance.services.ClientService@@getClient) -[provides]-> Type_instance(io.vyne.demos.tradeCompliance.services.Client) with evaluator OperationInvocationEvaluator",
          "fullPath": "//io.osmosis.polymer.query.QueryProfiler:Root/DefaultQueryEngine:Query with HipsterDiscoverGraphQueryStrategy/HipsterDiscoverGraphQueryStrategy:Searching for path TradeRequest -> ClientJurisdiction/EdgeNavigator:Evaluating Operation(io.vyne.demos.tradeCompliance.services.ClientService@@getClient) -[provides]-> Type_instance(io.vyne.demos.tradeCompliance.services.Client) with evaluator OperationInvocationEvaluator",
          "id": "5afe3e22-0177-4bbb-8832-8b5608578bea",
          "children": [{
            "componentName": "RestTemplateInvoker",
            "operationName": "Invoke HTTP Operation",
            "type": "REMOTE_CALL",
            "path": "//io.osmosis.polymer.query.QueryProfiler:Root/DefaultQueryEngine:Query with HipsterDiscoverGraphQueryStrategy/HipsterDiscoverGraphQueryStrategy:Searching for path TradeRequest -> ClientJurisdiction/EdgeNavigator:Evaluating Operation(io.vyne.demos.tradeCompliance.services.ClientService@@getClient) -[provides]-> Type_instance(io.vyne.demos.tradeCompliance.services.Client) with evaluator OperationInvocationEvaluator",
            "context": {
              "Absolute Url": "http://192.168.5.73:9102clients/{io.vyne.ClientId}",
              "Service": {
                "name": {
                  "fullyQualifiedName": "io.vyne.demos.tradeCompliance.services.ClientService",
                  "name": "ClientService"
                },
                "operations": [{
                  "name": "getClient",
                  "parameters": [{
                    "type": {
                      "name": {"fullyQualifiedName": "io.vyne.ClientId", "name": "ClientId"},
                      "attributes": {},
                      "modifiers": ["PRIMITIVE"],
                      "aliasForType": {"fullyQualifiedName": "lang.taxi.String", "name": "String"},
                      "inherits": [],
                      "sources": [{"origin": "Unknown", "language": "Taxi", "content": ""}],
                      "inheritanceGraph": [],
                      "fullyQualifiedName": "io.vyne.ClientId",
                      "typeAlias": true,
                      "parameterType": false,
                      "scalar": true
                    }, "name": null, "metadata": [], "constraints": []
                  }],
                  "returnType": {
                    "name": {
                      "fullyQualifiedName": "io.vyne.demos.tradeCompliance.services.Client",
                      "name": "Client"
                    },
                    "attributes": {
                      "id": {
                        "name": {"fullyQualifiedName": "io.vyne.ClientId", "name": "ClientId"},
                        "fullyQualifiedName": "io.vyne.ClientId",
                        "constraints": [],
                        "collection": false
                      },
                      "jurisdiction": {
                        "name": {
                          "fullyQualifiedName": "io.vyne.ClientJurisdiction",
                          "name": "ClientJurisdiction"
                        }, "fullyQualifiedName": "io.vyne.ClientJurisdiction", "constraints": [], "collection": false
                      },
                      "name": {
                        "name": {"fullyQualifiedName": "lang.taxi.String", "name": "String"},
                        "fullyQualifiedName": "lang.taxi.String",
                        "constraints": [],
                        "collection": false
                      }
                    },
                    "modifiers": [],
                    "aliasForType": null,
                    "inherits": [],
                    "sources": [{
                      "origin": "<unknown>",
                      "language": "Taxi",
                      "content": "type Client {\n        id : io.vyne.ClientId\n        jurisdiction : io.vyne.ClientJurisdiction\n        name : String\n    }"
                    }],
                    "inheritanceGraph": [],
                    "fullyQualifiedName": "io.vyne.demos.tradeCompliance.services.Client",
                    "typeAlias": false,
                    "parameterType": false,
                    "scalar": false
                  },
                  "metadata": [{
                    "name": {"fullyQualifiedName": "HttpOperation", "name": "HttpOperation"},
                    "params": {"method": "GET", "url": "/clients/{io.vyne.ClientId}"}
                  }],
                  "contract": {
                    "returnType": {
                      "name": {
                        "fullyQualifiedName": "io.vyne.demos.tradeCompliance.services.Client",
                        "name": "Client"
                      },
                      "attributes": {
                        "id": {
                          "name": {"fullyQualifiedName": "io.vyne.ClientId", "name": "ClientId"},
                          "fullyQualifiedName": "io.vyne.ClientId",
                          "constraints": [],
                          "collection": false
                        },
                        "jurisdiction": {
                          "name": {
                            "fullyQualifiedName": "io.vyne.ClientJurisdiction",
                            "name": "ClientJurisdiction"
                          }, "fullyQualifiedName": "io.vyne.ClientJurisdiction", "constraints": [], "collection": false
                        },
                        "name": {
                          "name": {"fullyQualifiedName": "lang.taxi.String", "name": "String"},
                          "fullyQualifiedName": "lang.taxi.String",
                          "constraints": [],
                          "collection": false
                        }
                      },
                      "modifiers": [],
                      "aliasForType": null,
                      "inherits": [],
                      "sources": [{
                        "origin": "<unknown>",
                        "language": "Taxi",
                        "content": "type Client {\n        id : io.vyne.ClientId\n        jurisdiction : io.vyne.ClientJurisdiction\n        name : String\n    }"
                      }],
                      "inheritanceGraph": [],
                      "fullyQualifiedName": "io.vyne.demos.tradeCompliance.services.Client",
                      "typeAlias": false,
                      "parameterType": false,
                      "scalar": false
                    }, "constraints": []
                  }
                }],
                "metadata": [{
                  "name": {
                    "fullyQualifiedName": "ServiceDiscoveryClient",
                    "name": "ServiceDiscoveryClient"
                  }, "params": {"serviceName": "services"}
                }],
                "sourceCode": [{
                  "origin": "<unknown>",
                  "language": "Taxi",
                  "content": "@ServiceDiscoveryClient(serviceName = \"services\")\n    service ClientService {\n        @HttpOperation(method = \"GET\" , url = \"/clients/{io.vyne.ClientId}\")\n        operation getClient(  io.vyne.ClientId ) : Client\n    }"
                }],
                "qualifiedName": "io.vyne.demos.tradeCompliance.services.ClientService"
              },
              "Operation": {
                "name": "getClient",
                "parameters": [{
                  "type": {
                    "name": {"fullyQualifiedName": "io.vyne.ClientId", "name": "ClientId"},
                    "attributes": {},
                    "modifiers": ["PRIMITIVE"],
                    "aliasForType": {"fullyQualifiedName": "lang.taxi.String", "name": "String"},
                    "inherits": [],
                    "sources": [{"origin": "Unknown", "language": "Taxi", "content": ""}],
                    "inheritanceGraph": [],
                    "fullyQualifiedName": "io.vyne.ClientId",
                    "typeAlias": true,
                    "parameterType": false,
                    "scalar": true
                  }, "name": null, "metadata": [], "constraints": []
                }],
                "returnType": {
                  "name": {
                    "fullyQualifiedName": "io.vyne.demos.tradeCompliance.services.Client",
                    "name": "Client"
                  },
                  "attributes": {
                    "id": {
                      "name": {"fullyQualifiedName": "io.vyne.ClientId", "name": "ClientId"},
                      "fullyQualifiedName": "io.vyne.ClientId",
                      "constraints": [],
                      "collection": false
                    },
                    "jurisdiction": {
                      "name": {
                        "fullyQualifiedName": "io.vyne.ClientJurisdiction",
                        "name": "ClientJurisdiction"
                      }, "fullyQualifiedName": "io.vyne.ClientJurisdiction", "constraints": [], "collection": false
                    },
                    "name": {
                      "name": {"fullyQualifiedName": "lang.taxi.String", "name": "String"},
                      "fullyQualifiedName": "lang.taxi.String",
                      "constraints": [],
                      "collection": false
                    }
                  },
                  "modifiers": [],
                  "aliasForType": null,
                  "inherits": [],
                  "sources": [{
                    "origin": "<unknown>",
                    "language": "Taxi",
                    "content": "type Client {\n        id : io.vyne.ClientId\n        jurisdiction : io.vyne.ClientJurisdiction\n        name : String\n    }"
                  }],
                  "inheritanceGraph": [],
                  "fullyQualifiedName": "io.vyne.demos.tradeCompliance.services.Client",
                  "typeAlias": false,
                  "parameterType": false,
                  "scalar": false
                },
                "metadata": [{
                  "name": {"fullyQualifiedName": "HttpOperation", "name": "HttpOperation"},
                  "params": {"method": "GET", "url": "/clients/{io.vyne.ClientId}"}
                }],
                "contract": {
                  "returnType": {
                    "name": {
                      "fullyQualifiedName": "io.vyne.demos.tradeCompliance.services.Client",
                      "name": "Client"
                    },
                    "attributes": {
                      "id": {
                        "name": {"fullyQualifiedName": "io.vyne.ClientId", "name": "ClientId"},
                        "fullyQualifiedName": "io.vyne.ClientId",
                        "constraints": [],
                        "collection": false
                      },
                      "jurisdiction": {
                        "name": {
                          "fullyQualifiedName": "io.vyne.ClientJurisdiction",
                          "name": "ClientJurisdiction"
                        }, "fullyQualifiedName": "io.vyne.ClientJurisdiction", "constraints": [], "collection": false
                      },
                      "name": {
                        "name": {"fullyQualifiedName": "lang.taxi.String", "name": "String"},
                        "fullyQualifiedName": "lang.taxi.String",
                        "constraints": [],
                        "collection": false
                      }
                    },
                    "modifiers": [],
                    "aliasForType": null,
                    "inherits": [],
                    "sources": [{
                      "origin": "<unknown>",
                      "language": "Taxi",
                      "content": "type Client {\n        id : io.vyne.ClientId\n        jurisdiction : io.vyne.ClientJurisdiction\n        name : String\n    }"
                    }],
                    "inheritanceGraph": [],
                    "fullyQualifiedName": "io.vyne.demos.tradeCompliance.services.Client",
                    "typeAlias": false,
                    "parameterType": false,
                    "scalar": false
                  }, "constraints": []
                }
              }
            },
            "remoteCalls": [{
              "service": {
                "fullyQualifiedName": "io.vyne.demos.tradeCompliance.services.ClientService",
                "name": "ClientService"
              },
              "operation": "getClient",
              "method": "GET",
              "requestBody": null,
              "resultCode": 200,
              "durationMs": 3,
              "response": {"id": "kevin", "name": "Kevin Smith", "jurisdiction": "GBP"}
            }],
            "result": {
              "startTime": 1535452739983,
              "endTime": 1535452739987,
              "value": {
                "id": {
                  "type": {
                    "name": {"fullyQualifiedName": "io.vyne.ClientId", "name": "ClientId"},
                    "attributes": {},
                    "modifiers": ["PRIMITIVE"],
                    "aliasForType": {"fullyQualifiedName": "lang.taxi.String", "name": "String"},
                    "inherits": [],
                    "sources": [{"origin": "Unknown", "language": "Taxi", "content": ""}],
                    "inheritanceGraph": [],
                    "fullyQualifiedName": "io.vyne.ClientId",
                    "typeAlias": true,
                    "parameterType": false,
                    "scalar": true
                  }, "value": "kevin"
                },
                "name": {
                  "type": {
                    "name": {"fullyQualifiedName": "lang.taxi.String", "name": "String"},
                    "attributes": {},
                    "modifiers": ["PRIMITIVE"],
                    "aliasForType": null,
                    "inherits": [],
                    "sources": [{"origin": "Unknown", "language": "Taxi", "content": ""}],
                    "inheritanceGraph": [],
                    "fullyQualifiedName": "lang.taxi.String",
                    "typeAlias": false,
                    "parameterType": false,
                    "scalar": true
                  }, "value": "Kevin Smith"
                },
                "jurisdiction": {
                  "type": {
                    "name": {
                      "fullyQualifiedName": "io.vyne.ClientJurisdiction",
                      "name": "ClientJurisdiction"
                    },
                    "attributes": {},
                    "modifiers": ["PRIMITIVE"],
                    "aliasForType": {"fullyQualifiedName": "lang.taxi.String", "name": "String"},
                    "inherits": [],
                    "sources": [{"origin": "Unknown", "language": "Taxi", "content": ""}],
                    "inheritanceGraph": [],
                    "fullyQualifiedName": "io.vyne.ClientJurisdiction",
                    "typeAlias": true,
                    "parameterType": false,
                    "scalar": true
                  }, "value": "GBP"
                }
              },
              "duration": 4
            },
            "name": "RestTemplateInvoker:Invoke HTTP Operation",
            "fullPath": "//io.osmosis.polymer.query.QueryProfiler:Root/DefaultQueryEngine:Query with HipsterDiscoverGraphQueryStrategy/HipsterDiscoverGraphQueryStrategy:Searching for path TradeRequest -> ClientJurisdiction/EdgeNavigator:Evaluating Operation(io.vyne.demos.tradeCompliance.services.ClientService@@getClient) -[provides]-> Type_instance(io.vyne.demos.tradeCompliance.services.Client) with evaluator OperationInvocationEvaluator/RestTemplateInvoker:Invoke HTTP Operation",
            "id": "47b85d98-48c9-4509-8aec-2aecd058ab25",
            "children": [],
            "duration": 4,
            "description": "RestTemplateInvoker.Invoke HTTP Operation"
          }],
          "duration": 4,
          "description": "EdgeNavigator.Evaluating Operation(io.vyne.demos.tradeCompliance.services.ClientService@@getClient) -[provides]-> Type_instance(io.vyne.demos.tradeCompliance.services.Client) with evaluator OperationInvocationEvaluator"
        }, {
          "componentName": "EdgeNavigator",
          "operationName": "Evaluating Type_instance(io.vyne.demos.tradeCompliance.services.Client) -[Is instanceOfType of]-> Type(io.vyne.demos.tradeCompliance.services.Client) with evaluator IsInstanceOfEdgeEvaluator",
          "type": "GRAPH_TRAVERSAL",
          "path": "//io.osmosis.polymer.query.QueryProfiler:Root/DefaultQueryEngine:Query with HipsterDiscoverGraphQueryStrategy/HipsterDiscoverGraphQueryStrategy:Searching for path TradeRequest -> ClientJurisdiction",
          "context": {},
          "remoteCalls": [],
          "result": {
            "startTime": 1535452739987,
            "endTime": 1535452739987,
            "value": {
              "edge": {
                "relationship": "IS_INSTANCE_OF",
                "target": {
                  "value": "io.vyne.demos.tradeCompliance.services.Client",
                  "elementType": "TYPE",
                  "instanceValue": null
                },
                "vertex1": {
                  "value": "io.vyne.demos.tradeCompliance.services.Client",
                  "elementType": "TYPE_INSTANCE",
                  "instanceValue": null
                },
                "vertex2": {
                  "value": "io.vyne.demos.tradeCompliance.services.Client",
                  "elementType": "TYPE",
                  "instanceValue": null
                },
                "previousValue": {
                  "id": {
                    "type": {
                      "name": {
                        "fullyQualifiedName": "io.vyne.ClientId",
                        "name": "ClientId"
                      },
                      "attributes": {},
                      "modifiers": ["PRIMITIVE"],
                      "aliasForType": {"fullyQualifiedName": "lang.taxi.String", "name": "String"},
                      "inherits": [],
                      "sources": [{"origin": "Unknown", "language": "Taxi", "content": ""}],
                      "inheritanceGraph": [],
                      "fullyQualifiedName": "io.vyne.ClientId",
                      "typeAlias": true,
                      "parameterType": false,
                      "scalar": true
                    }, "value": "kevin"
                  },
                  "name": {
                    "type": {
                      "name": {"fullyQualifiedName": "lang.taxi.String", "name": "String"},
                      "attributes": {},
                      "modifiers": ["PRIMITIVE"],
                      "aliasForType": null,
                      "inherits": [],
                      "sources": [{"origin": "Unknown", "language": "Taxi", "content": ""}],
                      "inheritanceGraph": [],
                      "fullyQualifiedName": "lang.taxi.String",
                      "typeAlias": false,
                      "parameterType": false,
                      "scalar": true
                    }, "value": "Kevin Smith"
                  },
                  "jurisdiction": {
                    "type": {
                      "name": {
                        "fullyQualifiedName": "io.vyne.ClientJurisdiction",
                        "name": "ClientJurisdiction"
                      },
                      "attributes": {},
                      "modifiers": ["PRIMITIVE"],
                      "aliasForType": {"fullyQualifiedName": "lang.taxi.String", "name": "String"},
                      "inherits": [],
                      "sources": [{"origin": "Unknown", "language": "Taxi", "content": ""}],
                      "inheritanceGraph": [],
                      "fullyQualifiedName": "io.vyne.ClientJurisdiction",
                      "typeAlias": true,
                      "parameterType": false,
                      "scalar": true
                    }, "value": "GBP"
                  }
                },
                "description": "Type_instance(io.vyne.demos.tradeCompliance.services.Client) -[Is instanceOfType of]-> Type(io.vyne.demos.tradeCompliance.services.Client)"
              },
              "resultGraphElement": {
                "value": "io.vyne.demos.tradeCompliance.services.Client",
                "elementType": "TYPE",
                "instanceValue": null
              },
              "resultValue": {
                "id": {
                  "type": {
                    "name": {"fullyQualifiedName": "io.vyne.ClientId", "name": "ClientId"},
                    "attributes": {},
                    "modifiers": ["PRIMITIVE"],
                    "aliasForType": {"fullyQualifiedName": "lang.taxi.String", "name": "String"},
                    "inherits": [],
                    "sources": [{"origin": "Unknown", "language": "Taxi", "content": ""}],
                    "inheritanceGraph": [],
                    "fullyQualifiedName": "io.vyne.ClientId",
                    "typeAlias": true,
                    "parameterType": false,
                    "scalar": true
                  }, "value": "kevin"
                },
                "name": {
                  "type": {
                    "name": {"fullyQualifiedName": "lang.taxi.String", "name": "String"},
                    "attributes": {},
                    "modifiers": ["PRIMITIVE"],
                    "aliasForType": null,
                    "inherits": [],
                    "sources": [{"origin": "Unknown", "language": "Taxi", "content": ""}],
                    "inheritanceGraph": [],
                    "fullyQualifiedName": "lang.taxi.String",
                    "typeAlias": false,
                    "parameterType": false,
                    "scalar": true
                  }, "value": "Kevin Smith"
                },
                "jurisdiction": {
                  "type": {
                    "name": {
                      "fullyQualifiedName": "io.vyne.ClientJurisdiction",
                      "name": "ClientJurisdiction"
                    },
                    "attributes": {},
                    "modifiers": ["PRIMITIVE"],
                    "aliasForType": {"fullyQualifiedName": "lang.taxi.String", "name": "String"},
                    "inherits": [],
                    "sources": [{"origin": "Unknown", "language": "Taxi", "content": ""}],
                    "inheritanceGraph": [],
                    "fullyQualifiedName": "io.vyne.ClientJurisdiction",
                    "typeAlias": true,
                    "parameterType": false,
                    "scalar": true
                  }, "value": "GBP"
                }
              },
              "error": null,
              "element": {
                "value": "io.vyne.demos.tradeCompliance.services.Client",
                "elementType": "TYPE",
                "instanceValue": null
              },
              "wasSuccessful": true
            },
            "duration": 0
          },
          "name": "EdgeNavigator:Evaluating Type_instance(io.vyne.demos.tradeCompliance.services.Client) -[Is instanceOfType of]-> Type(io.vyne.demos.tradeCompliance.services.Client) with evaluator IsInstanceOfEdgeEvaluator",
          "fullPath": "//io.osmosis.polymer.query.QueryProfiler:Root/DefaultQueryEngine:Query with HipsterDiscoverGraphQueryStrategy/HipsterDiscoverGraphQueryStrategy:Searching for path TradeRequest -> ClientJurisdiction/EdgeNavigator:Evaluating Type_instance(io.vyne.demos.tradeCompliance.services.Client) -[Is instanceOfType of]-> Type(io.vyne.demos.tradeCompliance.services.Client) with evaluator IsInstanceOfEdgeEvaluator",
          "id": "77b39bf9-ce88-47f3-be90-e3c42ac10a45",
          "children": [],
          "duration": 0,
          "description": "EdgeNavigator.Evaluating Type_instance(io.vyne.demos.tradeCompliance.services.Client) -[Is instanceOfType of]-> Type(io.vyne.demos.tradeCompliance.services.Client) with evaluator IsInstanceOfEdgeEvaluator"
        }, {
          "componentName": "EdgeNavigator",
          "operationName": "Evaluating Type(io.vyne.demos.tradeCompliance.services.Client) -[Has attribute]-> Member(io.vyne.demos.tradeCompliance.services.Client/jurisdiction) with evaluator HasAttributeEdgeEvaluator",
          "type": "GRAPH_TRAVERSAL",
          "path": "//io.osmosis.polymer.query.QueryProfiler:Root/DefaultQueryEngine:Query with HipsterDiscoverGraphQueryStrategy/HipsterDiscoverGraphQueryStrategy:Searching for path TradeRequest -> ClientJurisdiction",
          "context": {},
          "remoteCalls": [],
          "result": {
            "startTime": 1535452739987,
            "endTime": 1535452739987,
            "value": {
              "edge": {
                "relationship": "HAS_ATTRIBUTE",
                "target": {
                  "value": "io.vyne.demos.tradeCompliance.services.Client/jurisdiction",
                  "elementType": "MEMBER",
                  "instanceValue": null
                },
                "vertex1": {
                  "value": "io.vyne.demos.tradeCompliance.services.Client",
                  "elementType": "TYPE",
                  "instanceValue": null
                },
                "vertex2": {
                  "value": "io.vyne.demos.tradeCompliance.services.Client/jurisdiction",
                  "elementType": "MEMBER",
                  "instanceValue": null
                },
                "previousValue": {
                  "id": {
                    "type": {
                      "name": {
                        "fullyQualifiedName": "io.vyne.ClientId",
                        "name": "ClientId"
                      },
                      "attributes": {},
                      "modifiers": ["PRIMITIVE"],
                      "aliasForType": {"fullyQualifiedName": "lang.taxi.String", "name": "String"},
                      "inherits": [],
                      "sources": [{"origin": "Unknown", "language": "Taxi", "content": ""}],
                      "inheritanceGraph": [],
                      "fullyQualifiedName": "io.vyne.ClientId",
                      "typeAlias": true,
                      "parameterType": false,
                      "scalar": true
                    }, "value": "kevin"
                  },
                  "name": {
                    "type": {
                      "name": {"fullyQualifiedName": "lang.taxi.String", "name": "String"},
                      "attributes": {},
                      "modifiers": ["PRIMITIVE"],
                      "aliasForType": null,
                      "inherits": [],
                      "sources": [{"origin": "Unknown", "language": "Taxi", "content": ""}],
                      "inheritanceGraph": [],
                      "fullyQualifiedName": "lang.taxi.String",
                      "typeAlias": false,
                      "parameterType": false,
                      "scalar": true
                    }, "value": "Kevin Smith"
                  },
                  "jurisdiction": {
                    "type": {
                      "name": {
                        "fullyQualifiedName": "io.vyne.ClientJurisdiction",
                        "name": "ClientJurisdiction"
                      },
                      "attributes": {},
                      "modifiers": ["PRIMITIVE"],
                      "aliasForType": {"fullyQualifiedName": "lang.taxi.String", "name": "String"},
                      "inherits": [],
                      "sources": [{"origin": "Unknown", "language": "Taxi", "content": ""}],
                      "inheritanceGraph": [],
                      "fullyQualifiedName": "io.vyne.ClientJurisdiction",
                      "typeAlias": true,
                      "parameterType": false,
                      "scalar": true
                    }, "value": "GBP"
                  }
                },
                "description": "Type(io.vyne.demos.tradeCompliance.services.Client) -[Has attribute]-> Member(io.vyne.demos.tradeCompliance.services.Client/jurisdiction)"
              },
              "resultGraphElement": {
                "value": "io.vyne.demos.tradeCompliance.services.Client/jurisdiction",
                "elementType": "MEMBER",
                "instanceValue": null
              },
              "resultValue": {
                "type": {
                  "name": {
                    "fullyQualifiedName": "io.vyne.ClientJurisdiction",
                    "name": "ClientJurisdiction"
                  },
                  "attributes": {},
                  "modifiers": ["PRIMITIVE"],
                  "aliasForType": {"fullyQualifiedName": "lang.taxi.String", "name": "String"},
                  "inherits": [],
                  "sources": [{"origin": "Unknown", "language": "Taxi", "content": ""}],
                  "inheritanceGraph": [],
                  "fullyQualifiedName": "io.vyne.ClientJurisdiction",
                  "typeAlias": true,
                  "parameterType": false,
                  "scalar": true
                }, "value": "GBP"
              },
              "error": null,
              "element": {
                "value": "io.vyne.demos.tradeCompliance.services.Client/jurisdiction",
                "elementType": "MEMBER",
                "instanceValue": null
              },
              "wasSuccessful": true
            },
            "duration": 0
          },
          "name": "EdgeNavigator:Evaluating Type(io.vyne.demos.tradeCompliance.services.Client) -[Has attribute]-> Member(io.vyne.demos.tradeCompliance.services.Client/jurisdiction) with evaluator HasAttributeEdgeEvaluator",
          "fullPath": "//io.osmosis.polymer.query.QueryProfiler:Root/DefaultQueryEngine:Query with HipsterDiscoverGraphQueryStrategy/HipsterDiscoverGraphQueryStrategy:Searching for path TradeRequest -> ClientJurisdiction/EdgeNavigator:Evaluating Type(io.vyne.demos.tradeCompliance.services.Client) -[Has attribute]-> Member(io.vyne.demos.tradeCompliance.services.Client/jurisdiction) with evaluator HasAttributeEdgeEvaluator",
          "id": "4f270209-0832-4c65-b3a7-c40b7ae3c0f9",
          "children": [],
          "duration": 0,
          "description": "EdgeNavigator.Evaluating Type(io.vyne.demos.tradeCompliance.services.Client) -[Has attribute]-> Member(io.vyne.demos.tradeCompliance.services.Client/jurisdiction) with evaluator HasAttributeEdgeEvaluator"
        }, {
          "componentName": "EdgeNavigator",
          "operationName": "Evaluating Member(io.vyne.demos.tradeCompliance.services.Client/jurisdiction) -[Is type of]-> Type(io.vyne.ClientJurisdiction) with evaluator IsTypeOfEdgeEvaluator",
          "type": "GRAPH_TRAVERSAL",
          "path": "//io.osmosis.polymer.query.QueryProfiler:Root/DefaultQueryEngine:Query with HipsterDiscoverGraphQueryStrategy/HipsterDiscoverGraphQueryStrategy:Searching for path TradeRequest -> ClientJurisdiction",
          "context": {},
          "remoteCalls": [],
          "result": {
            "startTime": 1535452739988,
            "endTime": 1535452739988,
            "value": {
              "edge": {
                "relationship": "IS_TYPE_OF",
                "target": {"value": "io.vyne.ClientJurisdiction", "elementType": "TYPE", "instanceValue": null},
                "vertex1": {
                  "value": "io.vyne.demos.tradeCompliance.services.Client/jurisdiction",
                  "elementType": "MEMBER",
                  "instanceValue": null
                },
                "vertex2": {"value": "io.vyne.ClientJurisdiction", "elementType": "TYPE", "instanceValue": null},
                "previousValue": {
                  "type": {
                    "name": {
                      "fullyQualifiedName": "io.vyne.ClientJurisdiction",
                      "name": "ClientJurisdiction"
                    },
                    "attributes": {},
                    "modifiers": ["PRIMITIVE"],
                    "aliasForType": {"fullyQualifiedName": "lang.taxi.String", "name": "String"},
                    "inherits": [],
                    "sources": [{"origin": "Unknown", "language": "Taxi", "content": ""}],
                    "inheritanceGraph": [],
                    "fullyQualifiedName": "io.vyne.ClientJurisdiction",
                    "typeAlias": true,
                    "parameterType": false,
                    "scalar": true
                  }, "value": "GBP"
                },
                "description": "Member(io.vyne.demos.tradeCompliance.services.Client/jurisdiction) -[Is type of]-> Type(io.vyne.ClientJurisdiction)"
              },
              "resultGraphElement": {
                "value": "io.vyne.ClientJurisdiction",
                "elementType": "TYPE",
                "instanceValue": null
              },
              "resultValue": {
                "type": {
                  "name": {
                    "fullyQualifiedName": "io.vyne.ClientJurisdiction",
                    "name": "ClientJurisdiction"
                  },
                  "attributes": {},
                  "modifiers": ["PRIMITIVE"],
                  "aliasForType": {"fullyQualifiedName": "lang.taxi.String", "name": "String"},
                  "inherits": [],
                  "sources": [{"origin": "Unknown", "language": "Taxi", "content": ""}],
                  "inheritanceGraph": [],
                  "fullyQualifiedName": "io.vyne.ClientJurisdiction",
                  "typeAlias": true,
                  "parameterType": false,
                  "scalar": true
                }, "value": "GBP"
              },
              "error": null,
              "element": {"value": "io.vyne.ClientJurisdiction", "elementType": "TYPE", "instanceValue": null},
              "wasSuccessful": true
            },
            "duration": 0
          },
          "name": "EdgeNavigator:Evaluating Member(io.vyne.demos.tradeCompliance.services.Client/jurisdiction) -[Is type of]-> Type(io.vyne.ClientJurisdiction) with evaluator IsTypeOfEdgeEvaluator",
          "fullPath": "//io.osmosis.polymer.query.QueryProfiler:Root/DefaultQueryEngine:Query with HipsterDiscoverGraphQueryStrategy/HipsterDiscoverGraphQueryStrategy:Searching for path TradeRequest -> ClientJurisdiction/EdgeNavigator:Evaluating Member(io.vyne.demos.tradeCompliance.services.Client/jurisdiction) -[Is type of]-> Type(io.vyne.ClientJurisdiction) with evaluator IsTypeOfEdgeEvaluator",
          "id": "078507db-faa8-4625-930c-0dcc997daa1c",
          "children": [],
          "duration": 0,
          "description": "EdgeNavigator.Evaluating Member(io.vyne.demos.tradeCompliance.services.Client/jurisdiction) -[Is type of]-> Type(io.vyne.ClientJurisdiction) with evaluator IsTypeOfEdgeEvaluator"
        }],
        "duration": 6,
        "description": "HipsterDiscoverGraphQueryStrategy.Searching for path TradeRequest -> ClientJurisdiction"
      }],
      "duration": 12,
      "description": "DefaultQueryEngine.Query with HipsterDiscoverGraphQueryStrategy"
    }],
    "duration": 13,
    "description": "io.osmosis.polymer.query.QueryProfiler.Root"
  },
  "fullyResolved": true,
  "remoteCalls": [{
    "service": {
      "fullyQualifiedName": "io.vyne.demos.tradeCompliance.services.ClientService",
      "name": "ClientService"
    },
    "operation": "getClient",
    "method": "GET",
    "requestBody": null,
    "resultCode": 200,
    "durationMs": 3,
    "response": {"id": "kevin", "name": "Kevin Smith", "jurisdiction": "GBP"}
  }]
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
