import {Component, Input, OnInit} from '@angular/core';
import {ProfilerOperation, QueryResult} from "../../services/query.service";
import {QueryFailure} from "../query-wizard.component";
import {MatTreeNestedDataSource} from "@angular/material";
import {NestedTreeControl} from "@angular/cdk/tree";
import {TypedInstance} from "../../services/types.service";

@ Component({
  selector: 'query-result-display',
  templateUrl: './result-display.component.html',
  styleUrls: ['./result-display.component.scss']
})
export class ResultDisplayComponent implements OnInit {

  nestedTreeControl: NestedTreeControl<ProfilerOperation>;
  nestedDataSource: MatTreeNestedDataSource<ProfilerOperation> = new MatTreeNestedDataSource<ProfilerOperation>();

  objectKeys = Object.keys;
  objectValues = Object.values;

  hasChildren(node: ProfilerOperation) {
    return node.children && node.children.length > 0;
  }

  private _result: QueryResult | QueryFailure;

  get result(): QueryResult | QueryFailure {
    return this._result;
  }

  get queryResultValues(): TypedInstance[] {
    if (!this.isSuccess) return [];
    return Object.values((<QueryResult>this.result).results)
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

const serverResponse = {
  "message": "The search failed with an exception: Failed load invoke GET to http://martypitt-XPS-15-9560:9102clients/{io.vyne.ClientId} - received <500 Internal Server Error,{timestamp=1534835727050, status=500, error=Internal Server Error, exception=java.lang.IllegalStateException, message=No client mapped for id java.lang, path=/clients/java.lang.IllegalArgumentException:%20Source%20map%20does%20not%20contain%20an%20attribute%20clientId},{Content-Type=[application/json;charset=UTF-8], Transfer-Encoding=[chunked], Date=[Tue, 21 Aug 2018 07:15:27 GMT], Connection=[close]}>",
  "profilerOperation": {
    "componentName": "io.osmosis.polymer.query.QueryProfiler",
    "operationName": "Root",
    "path": "/",
    "context": {},
    "result": null,
    "name": "io.osmosis.polymer.query.QueryProfiler:Root",
    "fullPath": "//io.osmosis.polymer.query.QueryProfiler:Root",
    "id": "229ec27d-1369-4b73-b48e-1bdb37e54a74",
    "children": [{
      "componentName": "DefaultQueryEngine",
      "operationName": "Invoke queryStrategy class io.osmosis.polymer.query.ModelsScanStrategy",
      "path": "//io.osmosis.polymer.query.QueryProfiler:Root",
      "context": {"searchTarget": ["io.vyne.ClientJurisdiction"]},
      "result": {
        "startTime": 1534835727040,
        "endTime": 1534835727040,
        "value": {"matchedNodes": {}, "additionalData": []},
        "duration": 0
      },
      "name": "DefaultQueryEngine:Invoke queryStrategy class io.osmosis.polymer.query.ModelsScanStrategy",
      "fullPath": "//io.osmosis.polymer.query.QueryProfiler:Root/DefaultQueryEngine:Invoke queryStrategy class io.osmosis.polymer.query.ModelsScanStrategy",
      "id": "9ca20219-3dc2-4585-8c33-87fd3d3d5959",
      "children": [{
        "componentName": "ModelsScanStrategy",
        "operationName": "scan for matches",
        "path": "//io.osmosis.polymer.query.QueryProfiler:Root/DefaultQueryEngine:Invoke queryStrategy class io.osmosis.polymer.query.ModelsScanStrategy",
        "context": {},
        "result": {
          "startTime": 1534835727040,
          "endTime": 1534835727040,
          "value": {"matchedNodes": {}, "additionalData": []},
          "duration": 0
        },
        "name": "ModelsScanStrategy:scan for matches",
        "fullPath": "//io.osmosis.polymer.query.QueryProfiler:Root/DefaultQueryEngine:Invoke queryStrategy class io.osmosis.polymer.query.ModelsScanStrategy/ModelsScanStrategy:scan for matches",
        "id": "c9b18a23-d277-4871-bbad-685de6e1e941",
        "children": [],
        "description": "ModelsScanStrategy.scan for matches",
        "duration": 0
      }],
      "description": "DefaultQueryEngine.Invoke queryStrategy class io.osmosis.polymer.query.ModelsScanStrategy",
      "duration": 0
    }, {
      "componentName": "DefaultQueryEngine",
      "operationName": "Invoke queryStrategy class io.osmosis.polymer.query.HipsterDiscoverGraphQueryStrategy",
      "path": "//io.osmosis.polymer.query.QueryProfiler:Root",
      "context": {"searchTarget": ["io.vyne.ClientJurisdiction"]},
      "result": null,
      "name": "DefaultQueryEngine:Invoke queryStrategy class io.osmosis.polymer.query.HipsterDiscoverGraphQueryStrategy",
      "fullPath": "//io.osmosis.polymer.query.QueryProfiler:Root/DefaultQueryEngine:Invoke queryStrategy class io.osmosis.polymer.query.HipsterDiscoverGraphQueryStrategy",
      "id": "32c392eb-cf14-4f93-a422-62338f185218",
      "children": [{
        "componentName": "HipsterDiscoverGraphQueryStrategy",
        "operationName": "Searching for path Type_instance(io.vyne.tradeCompliance.aggregator.TradeRequest) -> Type(io.vyne.ClientJurisdiction)",
        "path": "//io.osmosis.polymer.query.QueryProfiler:Root/DefaultQueryEngine:Invoke queryStrategy class io.osmosis.polymer.query.HipsterDiscoverGraphQueryStrategy",
        "context": {
          "Current graph state": ["Parameter(param/io.vyne.Username) -[Is parameter on]-> Operation(io.vyne.demos.tradeCompliance.services.TraderService@@getTrader)", "Member(io.vyne.Price/currency) -[Is an attribute of]-> Type(io.vyne.Price)", "Member(io.vyne.Price/currency) -[Is type of]-> Type(io.vyne.Currency)", "Parameter(param/io.vyne.tradeCompliance.aggregator.TradeRequest) -[Is parameter on]-> Operation(io.vyne.tradeCompliance.aggregator.TradeComplianceEvaluator@@evaluate)", "Member(io.vyne.tradeCompliance.aggregator.TradeComplianceResult/status) -[Is an attribute of]-> Type(io.vyne.tradeCompliance.aggregator.TradeComplianceResult)", "Member(io.vyne.tradeCompliance.aggregator.TradeComplianceResult/status) -[Is type of]-> Type(io.vyne.tradeCompliance.TradeComplianceStatus)", "Member(io.vyne.demos.tradeCompliance.services.TraderService/traders) -[Is an attribute of]-> Type(io.vyne.demos.tradeCompliance.services.TraderService)", "Member(io.vyne.demos.tradeCompliance.services.TraderService/traders) -[Is type of]-> Type(io.vyne.demos.tradeCompliance.services.Map)", "Member(io.vyne.tradeCompliance.rules.RuleEvaluationResult/status) -[Is an attribute of]-> Type(io.vyne.tradeCompliance.rules.RuleEvaluationResult)", "Member(io.vyne.tradeCompliance.rules.RuleEvaluationResult/status) -[Is type of]-> Type(io.vyne.RuleEvaluationStatus)", "Type_instance(lang.taxi.String) -[can populate]-> Parameter(param/lang.taxi.String)", "Type_instance(lang.taxi.String) -[Is instance of]-> Type(lang.taxi.String)", "Type_instance(io.vyne.RuleEvaluationStatus) -[can populate]-> Parameter(param/io.vyne.RuleEvaluationStatus)", "Type_instance(io.vyne.RuleEvaluationStatus) -[Is instance of]-> Type(io.vyne.RuleEvaluationStatus)", "Member(io.vyne.tradeCompliance.aggregator.RuleEvaluationResults/ruleId) -[Is an attribute of]-> Type(io.vyne.tradeCompliance.aggregator.RuleEvaluationResults)", "Member(io.vyne.tradeCompliance.aggregator.RuleEvaluationResults/ruleId) -[Is type of]-> Type(lang.taxi.String)", "Operation(io.vyne.tradeCompliance.aggregator.TradeComplianceEvaluator@@evaluate) -[Requires parameter]-> Parameter(param/io.vyne.tradeCompliance.aggregator.TradeRequest)", "Operation(io.vyne.tradeCompliance.aggregator.TradeComplianceEvaluator@@evaluate) -[provides]-> Type_instance(io.vyne.tradeCompliance.aggregator.TradeComplianceResult)", "Provided_instance_member(io.vyne.demos.tradeCompliance.services.User/username) -[Is an attribute of]-> Type_instance(io.vyne.Username)", "Provided_instance_member(io.vyne.tradeCompliance.aggregator.TradeRequest/price) -[Is an attribute of]-> Type_instance(io.vyne.Price)", "Member(io.vyne.tradeCompliance.aggregator.TradeRequest/notional) -[Is an attribute of]-> Type(io.vyne.tradeCompliance.aggregator.TradeRequest)", "Member(io.vyne.tradeCompliance.aggregator.TradeRequest/notional) -[Is type of]-> Type(io.vyne.TradeNotional)", "Member(io.vyne.demos.tradeCompliance.services.Client/id) -[Is an attribute of]-> Type(io.vyne.demos.tradeCompliance.services.Client)", "Member(io.vyne.demos.tradeCompliance.services.Client/id) -[Is type of]-> Type(io.vyne.ClientId)", "Member(io.vyne.tradeCompliance.aggregator.RuleEvaluationResults/status) -[Is an attribute of]-> Type(io.vyne.tradeCompliance.aggregator.RuleEvaluationResults)", "Member(io.vyne.tradeCompliance.aggregator.RuleEvaluationResults/status) -[Is type of]-> Type(io.vyne.RuleEvaluationStatus)", "Type_instance(io.vyne.tradeCompliance.aggregator.TradeRequest) -[Is instance of]-> Type(io.vyne.tradeCompliance.aggregator.TradeRequest)", "Type_instance(io.vyne.tradeCompliance.aggregator.TradeRequest) -[can populate]-> Parameter(param/io.vyne.tradeCompliance.aggregator.TradeRequest)", "Type_instance(io.vyne.tradeCompliance.aggregator.TradeRequest) -[Instance has attribute]-> Provided_instance_member(io.vyne.tradeCompliance.aggregator.TradeRequest/notional)", "Type_instance(io.vyne.tradeCompliance.aggregator.TradeRequest) -[Instance has attribute]-> Provided_instance_member(io.vyne.tradeCompliance.aggregator.TradeRequest/clientId)", "Type_instance(io.vyne.tradeCompliance.aggregator.TradeRequest) -[Instance has attribute]-> Provided_instance_member(io.vyne.tradeCompliance.aggregator.TradeRequest/traderId)", "Type_instance(io.vyne.tradeCompliance.aggregator.TradeRequest) -[Instance has attribute]-> Provided_instance_member(io.vyne.tradeCompliance.aggregator.TradeRequest/price)", "Type_instance(io.vyne.ClientId) -[can populate]-> Parameter(param/io.vyne.ClientId)", "Type_instance(io.vyne.ClientId) -[Is instance of]-> Type(io.vyne.ClientId)", "Operation(io.vyne.demos.tradeCompliance.services.TraderService@@getTrader) -[Requires parameter]-> Parameter(param/io.vyne.Username)", "Operation(io.vyne.demos.tradeCompliance.services.TraderService@@getTrader) -[provides]-> Type_instance(io.vyne.demos.tradeCompliance.services.User)", "Type_instance(io.vyne.tradeCompliance.aggregator.RuleEvaluationResults) -[can populate]-> Parameter(param/io.vyne.tradeCompliance.aggregator.RuleEvaluationResults)", "Type_instance(io.vyne.tradeCompliance.aggregator.RuleEvaluationResults) -[Is instance of]-> Type(io.vyne.tradeCompliance.aggregator.RuleEvaluationResults)", "Member(io.vyne.tradeCompliance.rules.RuleEvaluationResult/ruleId) -[Is an attribute of]-> Type(io.vyne.tradeCompliance.rules.RuleEvaluationResult)", "Member(io.vyne.tradeCompliance.rules.RuleEvaluationResult/ruleId) -[Is type of]-> Type(lang.taxi.String)", "Provided_instance_member(io.vyne.demos.tradeCompliance.services.Client/name) -[Is an attribute of]-> Type_instance(lang.taxi.String)", "Provided_instance_member(io.vyne.demos.tradeCompliance.services.User/jurisdiction) -[Is an attribute of]-> Type_instance(io.vyne.tradeCompliance.CountryCode)", "Member(io.vyne.tradeCompliance.rules.RuleEvaluationResult/message) -[Is an attribute of]-> Type(io.vyne.tradeCompliance.rules.RuleEvaluationResult)", "Member(io.vyne.tradeCompliance.rules.RuleEvaluationResult/message) -[Is type of]-> Type(lang.taxi.String)", "Member(io.vyne.Money/currency) -[Is an attribute of]-> Type(io.vyne.Money)", "Member(io.vyne.Money/currency) -[Is type of]-> Type(io.vyne.Currency)", "Type(java.util.RuleEvaluationResult) -[Has attribute]-> Member(java.util.RuleEvaluationResult/ruleId)", "Type(java.util.RuleEvaluationResult) -[Has attribute]-> Member(java.util.RuleEvaluationResult/status)", "Type(java.util.RuleEvaluationResult) -[Has attribute]-> Member(java.util.RuleEvaluationResult/message)", "Member(io.vyne.tradeCompliance.rules.JurisdictionRuleRequest/traderJurisdiction) -[Is an attribute of]-> Type(io.vyne.tradeCompliance.rules.JurisdictionRuleRequest)", "Member(io.vyne.tradeCompliance.rules.JurisdictionRuleRequest/traderJurisdiction) -[Is type of]-> Type(io.vyne.TraderJurisdiction)", "Type(io.vyne.tradeCompliance.aggregator.TradeRequest) -[Has attribute]-> Member(io.vyne.tradeCompliance.aggregator.TradeRequest/notional)", "Type(io.vyne.tradeCompliance.aggregator.TradeRequest) -[Has attribute]-> Member(io.vyne.tradeCompliance.aggregator.TradeRequest/clientId)", "Type(io.vyne.tradeCompliance.aggregator.TradeRequest) -[Has attribute]-> Member(io.vyne.tradeCompliance.aggregator.TradeRequest/traderId)", "Type(io.vyne.tradeCompliance.aggregator.TradeRequest) -[Has attribute]-> Member(io.vyne.tradeCompliance.aggregator.TradeRequest/price)", "Provided_instance_member(io.vyne.tradeCompliance.aggregator.TradeRequest/notional) -[Is an attribute of]-> Type_instance(io.vyne.TradeNotional)", "Type_instance(io.vyne.TradeNotional) -[can populate]-> Parameter(param/io.vyne.TradeNotional)", "Type_instance(io.vyne.TradeNotional) -[Is instance of]-> Type(io.vyne.TradeNotional)", "Provided_instance_member(io.vyne.tradeCompliance.rules.RuleEvaluationResult/message) -[Is an attribute of]-> Type_instance(lang.taxi.String)", "Member(io.vyne.demos.tradeCompliance.services.Client/jurisdiction) -[Is an attribute of]-> Type(io.vyne.demos.tradeCompliance.services.Client)", "Member(io.vyne.demos.tradeCompliance.services.Client/jurisdiction) -[Is type of]-> Type(io.vyne.ClientJurisdiction)", "Provided_instance_member(io.vyne.tradeCompliance.rules.RuleEvaluationResult/ruleId) -[Is an attribute of]-> Type_instance(lang.taxi.String)", "Provided_instance_member(io.vyne.tradeCompliance.rules.RuleEvaluationResult/status) -[Is an attribute of]-> Type_instance(io.vyne.RuleEvaluationStatus)", "Type_instance(io.vyne.tradeCompliance.aggregator.TradeComplianceResult) -[Is instance of]-> Type(io.vyne.tradeCompliance.aggregator.TradeComplianceResult)", "Type_instance(io.vyne.tradeCompliance.aggregator.TradeComplianceResult) -[can populate]-> Parameter(param/io.vyne.tradeCompliance.aggregator.TradeComplianceResult)", "Type_instance(io.vyne.tradeCompliance.aggregator.TradeComplianceResult) -[Instance has attribute]-> Provided_instance_member(io.vyne.tradeCompliance.aggregator.TradeComplianceResult/status)", "Type_instance(io.vyne.tradeCompliance.aggregator.TradeComplianceResult) -[Instance has attribute]-> Provided_instance_member(io.vyne.tradeCompliance.aggregator.TradeComplianceResult/results)", "Type(io.vyne.tradeCompliance.rules.JurisdictionRuleRequest) -[Has attribute]-> Member(io.vyne.tradeCompliance.rules.JurisdictionRuleRequest/clientJurisdiction)", "Type(io.vyne.tradeCompliance.rules.JurisdictionRuleRequest) -[Has attribute]-> Member(io.vyne.tradeCompliance.rules.JurisdictionRuleRequest/traderJurisdiction)", "Type(io.vyne.tradeCompliance.rules.RuleEvaluationResult) -[Has attribute]-> Member(io.vyne.tradeCompliance.rules.RuleEvaluationResult/ruleId)", "Type(io.vyne.tradeCompliance.rules.RuleEvaluationResult) -[Has attribute]-> Member(io.vyne.tradeCompliance.rules.RuleEvaluationResult/status)", "Type(io.vyne.tradeCompliance.rules.RuleEvaluationResult) -[Has attribute]-> Member(io.vyne.tradeCompliance.rules.RuleEvaluationResult/message)", "Member(io.vyne.tradeCompliance.aggregator.TradeRequest/clientId) -[Is an attribute of]-> Type(io.vyne.tradeCompliance.aggregator.TradeRequest)", "Member(io.vyne.tradeCompliance.aggregator.TradeRequest/clientId) -[Is type of]-> Type(io.vyne.ClientId)", "Type(io.vyne.Price) -[Has attribute]-> Member(io.vyne.Price/currency)", "Type(io.vyne.Price) -[Has attribute]-> Member(io.vyne.Price/value)", "Operation(io.vyne.demos.tradeCompliance.services.ClientService@@getClient) -[Requires parameter]-> Parameter(param/io.vyne.ClientId)", "Operation(io.vyne.demos.tradeCompliance.services.ClientService@@getClient) -[provides]-> Type_instance(io.vyne.demos.tradeCompliance.services.Client)", "Type(io.vyne.tradeCompliance.TraderMaxTradeValue) -[Has attribute]-> Member(io.vyne.tradeCompliance.TraderMaxTradeValue/currency)", "Type(io.vyne.tradeCompliance.TraderMaxTradeValue) -[Has attribute]-> Member(io.vyne.tradeCompliance.TraderMaxTradeValue/value)", "Type_instance(io.vyne.demos.tradeCompliance.services.User) -[Is instance of]-> Type(io.vyne.demos.tradeCompliance.services.User)", "Type_instance(io.vyne.demos.tradeCompliance.services.User) -[can populate]-> Parameter(param/io.vyne.demos.tradeCompliance.services.User)", "Type_instance(io.vyne.demos.tradeCompliance.services.User) -[Instance has attribute]-> Provided_instance_member(io.vyne.demos.tradeCompliance.services.User/username)", "Type_instance(io.vyne.demos.tradeCompliance.services.User) -[Instance has attribute]-> Provided_instance_member(io.vyne.demos.tradeCompliance.services.User/jurisdiction)", "Provided_instance_member(io.vyne.demos.tradeCompliance.services.Client/id) -[Is an attribute of]-> Type_instance(io.vyne.ClientId)", "Member(io.vyne.demos.tradeCompliance.services.Client/name) -[Is an attribute of]-> Type(io.vyne.demos.tradeCompliance.services.Client)", "Member(io.vyne.demos.tradeCompliance.services.Client/name) -[Is type of]-> Type(lang.taxi.String)", "Provided_instance_member(io.vyne.demos.tradeCompliance.services.Client/jurisdiction) -[Is an attribute of]-> Type_instance(io.vyne.ClientJurisdiction)", "Type(io.vyne.tradeCompliance.aggregator.RuleEvaluationResults) -[Has attribute]-> Member(io.vyne.tradeCompliance.aggregator.RuleEvaluationResults/ruleId)", "Type(io.vyne.tradeCompliance.aggregator.RuleEvaluationResults) -[Has attribute]-> Member(io.vyne.tradeCompliance.aggregator.RuleEvaluationResults/status)", "Type(io.vyne.tradeCompliance.aggregator.RuleEvaluationResults) -[Has attribute]-> Member(io.vyne.tradeCompliance.aggregator.RuleEvaluationResults/message)", "Type_instance(io.vyne.demos.tradeCompliance.services.Client) -[Is instance of]-> Type(io.vyne.demos.tradeCompliance.services.Client)", "Type_instance(io.vyne.demos.tradeCompliance.services.Client) -[can populate]-> Parameter(param/io.vyne.demos.tradeCompliance.services.Client)", "Type_instance(io.vyne.demos.tradeCompliance.services.Client) -[Instance has attribute]-> Provided_instance_member(io.vyne.demos.tradeCompliance.services.Client/id)", "Type_instance(io.vyne.demos.tradeCompliance.services.Client) -[Instance has attribute]-> Provided_instance_member(io.vyne.demos.tradeCompliance.services.Client/name)", "Type_instance(io.vyne.demos.tradeCompliance.services.Client) -[Instance has attribute]-> Provided_instance_member(io.vyne.demos.tradeCompliance.services.Client/jurisdiction)", "Member(io.vyne.tradeCompliance.rules.TradeValueRuleRequest/tradeValue) -[Is an attribute of]-> Type(io.vyne.tradeCompliance.rules.TradeValueRuleRequest)", "Member(io.vyne.tradeCompliance.rules.TradeValueRuleRequest/tradeValue) -[Is type of]-> Type(io.vyne.tradeCompliance.TradeValue)", "Member(io.vyne.Price/value) -[Is an attribute of]-> Type(io.vyne.Price)", "Member(io.vyne.Price/value) -[Is type of]-> Type(io.vyne.MoneyAmount)", "Provided_instance_member(io.vyne.tradeCompliance.aggregator.TradeRequest/clientId) -[Is an attribute of]-> Type_instance(io.vyne.ClientId)", "Member(io.vyne.demos.tradeCompliance.services.User/jurisdiction) -[Is an attribute of]-> Type(io.vyne.demos.tradeCompliance.services.User)", "Member(io.vyne.demos.tradeCompliance.services.User/jurisdiction) -[Is type of]-> Type(io.vyne.tradeCompliance.CountryCode)", "Member(java.util.RuleEvaluationResult/status) -[Is an attribute of]-> Type(java.util.RuleEvaluationResult)", "Member(java.util.RuleEvaluationResult/status) -[Is type of]-> Type(io.vyne.RuleEvaluationStatus)", "Type_instance(io.vyne.Username) -[can populate]-> Parameter(param/io.vyne.Username)", "Type_instance(io.vyne.Username) -[Is instance of]-> Type(io.vyne.Username)", "Member(io.vyne.tradeCompliance.TradeValue/currency) -[Is an attribute of]-> Type(io.vyne.tradeCompliance.TradeValue)", "Member(io.vyne.tradeCompliance.TradeValue/currency) -[Is type of]-> Type(io.vyne.Currency)", "Type_instance(io.vyne.tradeCompliance.TradeComplianceStatus) -[can populate]-> Parameter(param/io.vyne.tradeCompliance.TradeComplianceStatus)", "Type_instance(io.vyne.tradeCompliance.TradeComplianceStatus) -[Is instance of]-> Type(io.vyne.tradeCompliance.TradeComplianceStatus)", "Parameter(param/io.vyne.tradeCompliance.rules.NotionalLimitRuleRequest) -[Is parameter on]-> Operation(io.vyne.tradeCompliance.rules.NotionalLimitRuleService@@evaluate)", "Operation(io.vyne.tradeCompliance.rules.NotionalLimitRuleService@@evaluate) -[Requires parameter]-> Parameter(param/io.vyne.tradeCompliance.rules.NotionalLimitRuleRequest)", "Operation(io.vyne.tradeCompliance.rules.NotionalLimitRuleService@@evaluate) -[provides]-> Type_instance(io.vyne.tradeCompliance.rules.RuleEvaluationResult)", "Type_instance(io.vyne.ClientJurisdiction) -[can populate]-> Parameter(param/io.vyne.ClientJurisdiction)", "Type_instance(io.vyne.ClientJurisdiction) -[Is instance of]-> Type(io.vyne.ClientJurisdiction)", "Member(java.util.RuleEvaluationResult/ruleId) -[Is an attribute of]-> Type(java.util.RuleEvaluationResult)", "Member(java.util.RuleEvaluationResult/ruleId) -[Is type of]-> Type(lang.taxi.String)", "Type(io.vyne.demos.tradeCompliance.services.TraderService) -[Has attribute]-> Member(io.vyne.demos.tradeCompliance.services.TraderService/traders)", "Member(io.vyne.tradeCompliance.rules.JurisdictionRuleRequest/clientJurisdiction) -[Is an attribute of]-> Type(io.vyne.tradeCompliance.rules.JurisdictionRuleRequest)", "Member(io.vyne.tradeCompliance.rules.JurisdictionRuleRequest/clientJurisdiction) -[Is type of]-> Type(io.vyne.ClientJurisdiction)", "Type(io.vyne.Money) -[Has attribute]-> Member(io.vyne.Money/currency)", "Type(io.vyne.Money) -[Has attribute]-> Member(io.vyne.Money/value)", "Member(io.vyne.demos.tradeCompliance.services.ClientService/clients) -[Is an attribute of]-> Type(io.vyne.demos.tradeCompliance.services.ClientService)", "Member(io.vyne.demos.tradeCompliance.services.ClientService/clients) -[Is type of]-> Type(io.vyne.demos.tradeCompliance.services.Map)", "Member(io.vyne.tradeCompliance.aggregator.TradeRequest/price) -[Is an attribute of]-> Type(io.vyne.tradeCompliance.aggregator.TradeRequest)", "Member(io.vyne.tradeCompliance.aggregator.TradeRequest/price) -[Is type of]-> Type(io.vyne.Price)", "Member(java.util.RuleEvaluationResult/message) -[Is an attribute of]-> Type(java.util.RuleEvaluationResult)", "Member(java.util.RuleEvaluationResult/message) -[Is type of]-> Type(lang.taxi.String)", "Member(io.vyne.tradeCompliance.aggregator.RuleEvaluationResults/message) -[Is an attribute of]-> Type(io.vyne.tradeCompliance.aggregator.RuleEvaluationResults)", "Member(io.vyne.tradeCompliance.aggregator.RuleEvaluationResults/message) -[Is type of]-> Type(lang.taxi.String)", "Type(io.vyne.tradeCompliance.rules.NotionalLimitRuleRequest) -[Has attribute]-> Member(io.vyne.tradeCompliance.rules.NotionalLimitRuleRequest/notional)", "Member(io.vyne.tradeCompliance.rules.TradeValueRuleRequest/traderLimit) -[Is an attribute of]-> Type(io.vyne.tradeCompliance.rules.TradeValueRuleRequest)", "Member(io.vyne.tradeCompliance.rules.TradeValueRuleRequest/traderLimit) -[Is type of]-> Type(io.vyne.tradeCompliance.TraderMaxTradeValue)", "Member(io.vyne.tradeCompliance.TraderMaxTradeValue/value) -[Is an attribute of]-> Type(io.vyne.tradeCompliance.TraderMaxTradeValue)", "Member(io.vyne.tradeCompliance.TraderMaxTradeValue/value) -[Is type of]-> Type(io.vyne.MoneyAmount)", "Provided_instance_member(io.vyne.tradeCompliance.aggregator.TradeComplianceResult/status) -[Is an attribute of]-> Type_instance(io.vyne.tradeCompliance.TradeComplianceStatus)", "Provided_instance_member(io.vyne.tradeCompliance.aggregator.TradeRequest/traderId) -[Is an attribute of]-> Type_instance(io.vyne.Username)", "Parameter(param/io.vyne.ClientId) -[Is parameter on]-> Operation(io.vyne.demos.tradeCompliance.services.ClientService@@getClient)", "Type(io.vyne.tradeCompliance.rules.TradeValueRuleRequest) -[Has attribute]-> Member(io.vyne.tradeCompliance.rules.TradeValueRuleRequest/tradeValue)", "Type(io.vyne.tradeCompliance.rules.TradeValueRuleRequest) -[Has attribute]-> Member(io.vyne.tradeCompliance.rules.TradeValueRuleRequest/traderLimit)", "Member(io.vyne.tradeCompliance.aggregator.TradeComplianceResult/results) -[Is an attribute of]-> Type(io.vyne.tradeCompliance.aggregator.TradeComplianceResult)", "Member(io.vyne.tradeCompliance.aggregator.TradeComplianceResult/results) -[Is type of]-> Type(io.vyne.tradeCompliance.aggregator.RuleEvaluationResults)", "Parameter(param/io.vyne.tradeCompliance.rules.JurisdictionRuleRequest) -[Is parameter on]-> Operation(io.vyne.tradeCompliance.rules.JurisdictionRuleService@@evaluate)", "Provided_instance_member(io.vyne.tradeCompliance.aggregator.TradeComplianceResult/results) -[Is an attribute of]-> Type_instance(io.vyne.tradeCompliance.aggregator.RuleEvaluationResults)", "Operation(io.vyne.tradeCompliance.rules.TradeValueRuleService@@evaluate) -[Requires parameter]-> Parameter(param/io.vyne.tradeCompliance.rules.TradeValueRuleRequest)", "Operation(io.vyne.tradeCompliance.rules.TradeValueRuleService@@evaluate) -[provides]-> Type_instance(io.vyne.tradeCompliance.rules.RuleEvaluationResult)", "Member(io.vyne.demos.tradeCompliance.services.User/username) -[Is an attribute of]-> Type(io.vyne.demos.tradeCompliance.services.User)", "Member(io.vyne.demos.tradeCompliance.services.User/username) -[Is type of]-> Type(io.vyne.Username)", "Type(io.vyne.demos.tradeCompliance.services.Client) -[Has attribute]-> Member(io.vyne.demos.tradeCompliance.services.Client/id)", "Type(io.vyne.demos.tradeCompliance.services.Client) -[Has attribute]-> Member(io.vyne.demos.tradeCompliance.services.Client/name)", "Type(io.vyne.demos.tradeCompliance.services.Client) -[Has attribute]-> Member(io.vyne.demos.tradeCompliance.services.Client/jurisdiction)", "Member(io.vyne.Money/value) -[Is an attribute of]-> Type(io.vyne.Money)", "Member(io.vyne.Money/value) -[Is type of]-> Type(io.vyne.MoneyAmount)", "Type(io.vyne.tradeCompliance.TradeValue) -[Has attribute]-> Member(io.vyne.tradeCompliance.TradeValue/currency)", "Type(io.vyne.tradeCompliance.TradeValue) -[Has attribute]-> Member(io.vyne.tradeCompliance.TradeValue/value)", "Type(io.vyne.demos.tradeCompliance.services.ClientService) -[Has attribute]-> Member(io.vyne.demos.tradeCompliance.services.ClientService/clients)", "Member(io.vyne.tradeCompliance.TradeValue/value) -[Is an attribute of]-> Type(io.vyne.tradeCompliance.TradeValue)", "Member(io.vyne.tradeCompliance.TradeValue/value) -[Is type of]-> Type(io.vyne.MoneyAmount)", "Type_instance(io.vyne.tradeCompliance.rules.RuleEvaluationResult) -[Is instance of]-> Type(io.vyne.tradeCompliance.rules.RuleEvaluationResult)", "Type_instance(io.vyne.tradeCompliance.rules.RuleEvaluationResult) -[can populate]-> Parameter(param/io.vyne.tradeCompliance.rules.RuleEvaluationResult)", "Type_instance(io.vyne.tradeCompliance.rules.RuleEvaluationResult) -[Instance has attribute]-> Provided_instance_member(io.vyne.tradeCompliance.rules.RuleEvaluationResult/ruleId)", "Type_instance(io.vyne.tradeCompliance.rules.RuleEvaluationResult) -[Instance has attribute]-> Provided_instance_member(io.vyne.tradeCompliance.rules.RuleEvaluationResult/status)", "Type_instance(io.vyne.tradeCompliance.rules.RuleEvaluationResult) -[Instance has attribute]-> Provided_instance_member(io.vyne.tradeCompliance.rules.RuleEvaluationResult/message)", "Parameter(param/io.vyne.tradeCompliance.rules.TradeValueRuleRequest) -[Is parameter on]-> Operation(io.vyne.tradeCompliance.rules.TradeValueRuleService@@evaluate)", "Member(io.vyne.tradeCompliance.TraderMaxTradeValue/currency) -[Is an attribute of]-> Type(io.vyne.tradeCompliance.TraderMaxTradeValue)", "Member(io.vyne.tradeCompliance.TraderMaxTradeValue/currency) -[Is type of]-> Type(io.vyne.Currency)", "Type_instance(io.vyne.tradeCompliance.CountryCode) -[can populate]-> Parameter(param/io.vyne.tradeCompliance.CountryCode)", "Type_instance(io.vyne.tradeCompliance.CountryCode) -[Is instance of]-> Type(io.vyne.tradeCompliance.CountryCode)", "Type_instance(io.vyne.Price) -[can populate]-> Parameter(param/io.vyne.Price)", "Type_instance(io.vyne.Price) -[Is instance of]-> Type(io.vyne.Price)", "Type(io.vyne.demos.tradeCompliance.services.User) -[Has attribute]-> Member(io.vyne.demos.tradeCompliance.services.User/username)", "Type(io.vyne.demos.tradeCompliance.services.User) -[Has attribute]-> Member(io.vyne.demos.tradeCompliance.services.User/jurisdiction)", "Member(io.vyne.tradeCompliance.aggregator.TradeRequest/traderId) -[Is an attribute of]-> Type(io.vyne.tradeCompliance.aggregator.TradeRequest)", "Member(io.vyne.tradeCompliance.aggregator.TradeRequest/traderId) -[Is type of]-> Type(io.vyne.Username)", "Operation(io.vyne.tradeCompliance.rules.JurisdictionRuleService@@evaluate) -[Requires parameter]-> Parameter(param/io.vyne.tradeCompliance.rules.JurisdictionRuleRequest)", "Operation(io.vyne.tradeCompliance.rules.JurisdictionRuleService@@evaluate) -[provides]-> Type_instance(io.vyne.tradeCompliance.rules.RuleEvaluationResult)", "Member(io.vyne.tradeCompliance.rules.NotionalLimitRuleRequest/notional) -[Is an attribute of]-> Type(io.vyne.tradeCompliance.rules.NotionalLimitRuleRequest)", "Member(io.vyne.tradeCompliance.rules.NotionalLimitRuleRequest/notional) -[Is type of]-> Type(lang.taxi.Decimal)", "Type(io.vyne.tradeCompliance.aggregator.TradeComplianceResult) -[Has attribute]-> Member(io.vyne.tradeCompliance.aggregator.TradeComplianceResult/status)", "Type(io.vyne.tradeCompliance.aggregator.TradeComplianceResult) -[Has attribute]-> Member(io.vyne.tradeCompliance.aggregator.TradeComplianceResult/results)"],
          "discoveredPath": ["io.vyne.tradeCompliance.aggregator.TradeRequest -[Instance has attribute]-> io.vyne.tradeCompliance.aggregator.TradeRequest/clientId", "io.vyne.tradeCompliance.aggregator.TradeRequest/clientId -[Is an attribute of]-> io.vyne.ClientId", "io.vyne.ClientId -[can populate]-> param/io.vyne.ClientId", "param/io.vyne.ClientId -[Is parameter on]-> io.vyne.demos.tradeCompliance.services.ClientService@@getClient", "io.vyne.demos.tradeCompliance.services.ClientService@@getClient -[provides]-> io.vyne.demos.tradeCompliance.services.Client", "io.vyne.demos.tradeCompliance.services.Client -[Is instance of]-> io.vyne.demos.tradeCompliance.services.Client", "io.vyne.demos.tradeCompliance.services.Client -[Has attribute]-> io.vyne.demos.tradeCompliance.services.Client/jurisdiction", "io.vyne.demos.tradeCompliance.services.Client/jurisdiction -[Is type of]-> io.vyne.ClientJurisdiction"]
        },
        "result": null,
        "name": "HipsterDiscoverGraphQueryStrategy:Searching for path Type_instance(io.vyne.tradeCompliance.aggregator.TradeRequest) -> Type(io.vyne.ClientJurisdiction)",
        "fullPath": "//io.osmosis.polymer.query.QueryProfiler:Root/DefaultQueryEngine:Invoke queryStrategy class io.osmosis.polymer.query.HipsterDiscoverGraphQueryStrategy/HipsterDiscoverGraphQueryStrategy:Searching for path Type_instance(io.vyne.tradeCompliance.aggregator.TradeRequest) -> Type(io.vyne.ClientJurisdiction)",
        "id": "1f3b99cb-0864-4bf5-988b-621991af60be",
        "children": [{
          "componentName": "EdgeNavigator",
          "operationName": "Evaluating Type_instance(io.vyne.tradeCompliance.aggregator.TradeRequest) -[Instance has attribute]-> Provided_instance_member(io.vyne.tradeCompliance.aggregator.TradeRequest/clientId) with evaluator InstanceHasAttributeEdgeEvaluator",
          "path": "//io.osmosis.polymer.query.QueryProfiler:Root/DefaultQueryEngine:Invoke queryStrategy class io.osmosis.polymer.query.HipsterDiscoverGraphQueryStrategy/HipsterDiscoverGraphQueryStrategy:Searching for path Type_instance(io.vyne.tradeCompliance.aggregator.TradeRequest) -> Type(io.vyne.ClientJurisdiction)",
          "context": {},
          "result": {
            "startTime": 1534835727044,
            "endTime": 1534835727044,
            "value": {
              "edge": {
                "start": {
                  "value": "io.vyne.tradeCompliance.aggregator.TradeRequest",
                  "elementType": "TYPE_INSTANCE",
                  "instanceValue": null
                },
                "relationship": "INSTANCE_HAS_ATTRIBUTE",
                "endNode": {
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
                "edgeValue": "INSTANCE_HAS_ATTRIBUTE",
                "type": "DIRECTED"
              },
              "result": {
                "value": "io.vyne.tradeCompliance.aggregator.TradeRequest/clientId",
                "elementType": "PROVIDED_INSTANCE_MEMBER",
                "instanceValue": null
              },
              "error": null,
              "wasSuccessful": true,
              "elements": [{
                "value": "io.vyne.tradeCompliance.aggregator.TradeRequest",
                "elementType": "TYPE_INSTANCE",
                "instanceValue": null
              }, {
                "value": "io.vyne.tradeCompliance.aggregator.TradeRequest/clientId",
                "elementType": "PROVIDED_INSTANCE_MEMBER",
                "instanceValue": null
              }]
            },
            "duration": 0
          },
          "name": "EdgeNavigator:Evaluating Type_instance(io.vyne.tradeCompliance.aggregator.TradeRequest) -[Instance has attribute]-> Provided_instance_member(io.vyne.tradeCompliance.aggregator.TradeRequest/clientId) with evaluator InstanceHasAttributeEdgeEvaluator",
          "fullPath": "//io.osmosis.polymer.query.QueryProfiler:Root/DefaultQueryEngine:Invoke queryStrategy class io.osmosis.polymer.query.HipsterDiscoverGraphQueryStrategy/HipsterDiscoverGraphQueryStrategy:Searching for path Type_instance(io.vyne.tradeCompliance.aggregator.TradeRequest) -> Type(io.vyne.ClientJurisdiction)/EdgeNavigator:Evaluating Type_instance(io.vyne.tradeCompliance.aggregator.TradeRequest) -[Instance has attribute]-> Provided_instance_member(io.vyne.tradeCompliance.aggregator.TradeRequest/clientId) with evaluator InstanceHasAttributeEdgeEvaluator",
          "id": "97eb83b1-1140-4071-b485-4b55fc21fde4",
          "children": [],
          "description": "EdgeNavigator.Evaluating Type_instance(io.vyne.tradeCompliance.aggregator.TradeRequest) -[Instance has attribute]-> Provided_instance_member(io.vyne.tradeCompliance.aggregator.TradeRequest/clientId) with evaluator InstanceHasAttributeEdgeEvaluator",
          "duration": 0
        }, {
          "componentName": "EdgeNavigator",
          "operationName": "Evaluating Provided_instance_member(io.vyne.tradeCompliance.aggregator.TradeRequest/clientId) -[Is an attribute of]-> Type_instance(io.vyne.ClientId) with evaluator AttributeOfEdgeEvaluator",
          "path": "//io.osmosis.polymer.query.QueryProfiler:Root/DefaultQueryEngine:Invoke queryStrategy class io.osmosis.polymer.query.HipsterDiscoverGraphQueryStrategy/HipsterDiscoverGraphQueryStrategy:Searching for path Type_instance(io.vyne.tradeCompliance.aggregator.TradeRequest) -> Type(io.vyne.ClientJurisdiction)",
          "context": {},
          "result": {
            "startTime": 1534835727044,
            "endTime": 1534835727044,
            "value": {
              "edge": {
                "start": {
                  "value": "io.vyne.tradeCompliance.aggregator.TradeRequest/clientId",
                  "elementType": "PROVIDED_INSTANCE_MEMBER",
                  "instanceValue": null
                },
                "relationship": "IS_ATTRIBUTE_OF",
                "endNode": {"value": "io.vyne.ClientId", "elementType": "TYPE_INSTANCE", "instanceValue": null},
                "vertex1": {
                  "value": "io.vyne.tradeCompliance.aggregator.TradeRequest/clientId",
                  "elementType": "PROVIDED_INSTANCE_MEMBER",
                  "instanceValue": null
                },
                "vertex2": {"value": "io.vyne.ClientId", "elementType": "TYPE_INSTANCE", "instanceValue": null},
                "edgeValue": "IS_ATTRIBUTE_OF",
                "type": "DIRECTED"
              },
              "result": {"value": "io.vyne.ClientId", "elementType": "TYPE_INSTANCE", "instanceValue": null},
              "error": null,
              "wasSuccessful": true,
              "elements": [{
                "value": "io.vyne.tradeCompliance.aggregator.TradeRequest/clientId",
                "elementType": "PROVIDED_INSTANCE_MEMBER",
                "instanceValue": null
              }, {"value": "io.vyne.ClientId", "elementType": "TYPE_INSTANCE", "instanceValue": null}]
            },
            "duration": 0
          },
          "name": "EdgeNavigator:Evaluating Provided_instance_member(io.vyne.tradeCompliance.aggregator.TradeRequest/clientId) -[Is an attribute of]-> Type_instance(io.vyne.ClientId) with evaluator AttributeOfEdgeEvaluator",
          "fullPath": "//io.osmosis.polymer.query.QueryProfiler:Root/DefaultQueryEngine:Invoke queryStrategy class io.osmosis.polymer.query.HipsterDiscoverGraphQueryStrategy/HipsterDiscoverGraphQueryStrategy:Searching for path Type_instance(io.vyne.tradeCompliance.aggregator.TradeRequest) -> Type(io.vyne.ClientJurisdiction)/EdgeNavigator:Evaluating Provided_instance_member(io.vyne.tradeCompliance.aggregator.TradeRequest/clientId) -[Is an attribute of]-> Type_instance(io.vyne.ClientId) with evaluator AttributeOfEdgeEvaluator",
          "id": "59c8ad3e-af68-406b-887f-21c8b75c20c2",
          "children": [],
          "description": "EdgeNavigator.Evaluating Provided_instance_member(io.vyne.tradeCompliance.aggregator.TradeRequest/clientId) -[Is an attribute of]-> Type_instance(io.vyne.ClientId) with evaluator AttributeOfEdgeEvaluator",
          "duration": 0
        }, {
          "componentName": "EdgeNavigator",
          "operationName": "Evaluating Type_instance(io.vyne.ClientId) -[can populate]-> Parameter(param/io.vyne.ClientId) with evaluator CanPopulateEdgeEvaluator",
          "path": "//io.osmosis.polymer.query.QueryProfiler:Root/DefaultQueryEngine:Invoke queryStrategy class io.osmosis.polymer.query.HipsterDiscoverGraphQueryStrategy/HipsterDiscoverGraphQueryStrategy:Searching for path Type_instance(io.vyne.tradeCompliance.aggregator.TradeRequest) -> Type(io.vyne.ClientJurisdiction)",
          "context": {},
          "result": {
            "startTime": 1534835727044,
            "endTime": 1534835727044,
            "value": {
              "edge": {
                "start": {
                  "value": "io.vyne.ClientId",
                  "elementType": "TYPE_INSTANCE",
                  "instanceValue": null
                },
                "relationship": "CAN_POPULATE",
                "endNode": {"value": "param/io.vyne.ClientId", "elementType": "PARAMETER", "instanceValue": null},
                "vertex1": {"value": "io.vyne.ClientId", "elementType": "TYPE_INSTANCE", "instanceValue": null},
                "vertex2": {"value": "param/io.vyne.ClientId", "elementType": "PARAMETER", "instanceValue": null},
                "edgeValue": "CAN_POPULATE",
                "type": "DIRECTED"
              },
              "result": {"value": "param/io.vyne.ClientId", "elementType": "PARAMETER", "instanceValue": null},
              "error": null,
              "wasSuccessful": true,
              "elements": [{
                "value": "io.vyne.ClientId",
                "elementType": "TYPE_INSTANCE",
                "instanceValue": null
              }, {"value": "param/io.vyne.ClientId", "elementType": "PARAMETER", "instanceValue": null}]
            },
            "duration": 0
          },
          "name": "EdgeNavigator:Evaluating Type_instance(io.vyne.ClientId) -[can populate]-> Parameter(param/io.vyne.ClientId) with evaluator CanPopulateEdgeEvaluator",
          "fullPath": "//io.osmosis.polymer.query.QueryProfiler:Root/DefaultQueryEngine:Invoke queryStrategy class io.osmosis.polymer.query.HipsterDiscoverGraphQueryStrategy/HipsterDiscoverGraphQueryStrategy:Searching for path Type_instance(io.vyne.tradeCompliance.aggregator.TradeRequest) -> Type(io.vyne.ClientJurisdiction)/EdgeNavigator:Evaluating Type_instance(io.vyne.ClientId) -[can populate]-> Parameter(param/io.vyne.ClientId) with evaluator CanPopulateEdgeEvaluator",
          "id": "1fd17a31-d8a7-418c-8156-46c56e91395a",
          "children": [],
          "description": "EdgeNavigator.Evaluating Type_instance(io.vyne.ClientId) -[can populate]-> Parameter(param/io.vyne.ClientId) with evaluator CanPopulateEdgeEvaluator",
          "duration": 0
        }, {
          "componentName": "EdgeNavigator",
          "operationName": "Evaluating Parameter(param/io.vyne.ClientId) -[Is parameter on]-> Operation(io.vyne.demos.tradeCompliance.services.ClientService@@getClient) with evaluator OperationParameterEdgeEvaluator",
          "path": "//io.osmosis.polymer.query.QueryProfiler:Root/DefaultQueryEngine:Invoke queryStrategy class io.osmosis.polymer.query.HipsterDiscoverGraphQueryStrategy/HipsterDiscoverGraphQueryStrategy:Searching for path Type_instance(io.vyne.tradeCompliance.aggregator.TradeRequest) -> Type(io.vyne.ClientJurisdiction)",
          "context": {},
          "result": {
            "startTime": 1534835727045,
            "endTime": 1534835727045,
            "value": {
              "edge": {
                "start": {
                  "value": "param/io.vyne.ClientId",
                  "elementType": "PARAMETER",
                  "instanceValue": null
                },
                "relationship": "IS_PARAMETER_ON",
                "endNode": {
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
                "edgeValue": "IS_PARAMETER_ON",
                "type": "DIRECTED"
              },
              "result": {
                "value": "io.vyne.demos.tradeCompliance.services.ClientService@@getClient",
                "elementType": "OPERATION",
                "instanceValue": null
              },
              "error": null,
              "wasSuccessful": true,
              "elements": [{
                "value": "param/io.vyne.ClientId",
                "elementType": "PARAMETER",
                "instanceValue": null
              }, {
                "value": "io.vyne.demos.tradeCompliance.services.ClientService@@getClient",
                "elementType": "OPERATION",
                "instanceValue": null
              }]
            },
            "duration": 0
          },
          "name": "EdgeNavigator:Evaluating Parameter(param/io.vyne.ClientId) -[Is parameter on]-> Operation(io.vyne.demos.tradeCompliance.services.ClientService@@getClient) with evaluator OperationParameterEdgeEvaluator",
          "fullPath": "//io.osmosis.polymer.query.QueryProfiler:Root/DefaultQueryEngine:Invoke queryStrategy class io.osmosis.polymer.query.HipsterDiscoverGraphQueryStrategy/HipsterDiscoverGraphQueryStrategy:Searching for path Type_instance(io.vyne.tradeCompliance.aggregator.TradeRequest) -> Type(io.vyne.ClientJurisdiction)/EdgeNavigator:Evaluating Parameter(param/io.vyne.ClientId) -[Is parameter on]-> Operation(io.vyne.demos.tradeCompliance.services.ClientService@@getClient) with evaluator OperationParameterEdgeEvaluator",
          "id": "e85e9793-eed6-4fc7-ab33-73bcb4d246d2",
          "children": [],
          "description": "EdgeNavigator.Evaluating Parameter(param/io.vyne.ClientId) -[Is parameter on]-> Operation(io.vyne.demos.tradeCompliance.services.ClientService@@getClient) with evaluator OperationParameterEdgeEvaluator",
          "duration": 0
        }, {
          "componentName": "EdgeNavigator",
          "operationName": "Evaluating Operation(io.vyne.demos.tradeCompliance.services.ClientService@@getClient) -[provides]-> Type_instance(io.vyne.demos.tradeCompliance.services.Client) with evaluator OperationInvocationEvaluator",
          "path": "//io.osmosis.polymer.query.QueryProfiler:Root/DefaultQueryEngine:Invoke queryStrategy class io.osmosis.polymer.query.HipsterDiscoverGraphQueryStrategy/HipsterDiscoverGraphQueryStrategy:Searching for path Type_instance(io.vyne.tradeCompliance.aggregator.TradeRequest) -> Type(io.vyne.ClientJurisdiction)",
          "context": {},
          "result": null,
          "name": "EdgeNavigator:Evaluating Operation(io.vyne.demos.tradeCompliance.services.ClientService@@getClient) -[provides]-> Type_instance(io.vyne.demos.tradeCompliance.services.Client) with evaluator OperationInvocationEvaluator",
          "fullPath": "//io.osmosis.polymer.query.QueryProfiler:Root/DefaultQueryEngine:Invoke queryStrategy class io.osmosis.polymer.query.HipsterDiscoverGraphQueryStrategy/HipsterDiscoverGraphQueryStrategy:Searching for path Type_instance(io.vyne.tradeCompliance.aggregator.TradeRequest) -> Type(io.vyne.ClientJurisdiction)/EdgeNavigator:Evaluating Operation(io.vyne.demos.tradeCompliance.services.ClientService@@getClient) -[provides]-> Type_instance(io.vyne.demos.tradeCompliance.services.Client) with evaluator OperationInvocationEvaluator",
          "id": "05fd06e8-fbcd-4532-bcd8-4b38b60d436a",
          "children": [{
            "componentName": "RestTemplateInvoker",
            "operationName": "Invoke HTTP Operation",
            "path": "//io.osmosis.polymer.query.QueryProfiler:Root/DefaultQueryEngine:Invoke queryStrategy class io.osmosis.polymer.query.HipsterDiscoverGraphQueryStrategy/HipsterDiscoverGraphQueryStrategy:Searching for path Type_instance(io.vyne.tradeCompliance.aggregator.TradeRequest) -> Type(io.vyne.ClientJurisdiction)/EdgeNavigator:Evaluating Operation(io.vyne.demos.tradeCompliance.services.ClientService@@getClient) -[provides]-> Type_instance(io.vyne.demos.tradeCompliance.services.Client) with evaluator OperationInvocationEvaluator",
            "context": {
              "Abosulte Url": "http://martypitt-XPS-15-9560:9102clients/{io.vyne.ClientId}",
              "requestBody": {"headers": {}, "body": null}
            },
            "result": {
              "startTime": 1534835727045,
              "endTime": 1534835727051,
              "value": {
                "headers": {
                  "Content-Type": ["application/json;charset=UTF-8"],
                  "Transfer-Encoding": ["chunked"],
                  "Date": ["Tue, 21 Aug 2018 07:15:27 GMT"],
                  "Connection": ["close"]
                },
                "body": {
                  "timestamp": 1534835727050,
                  "status": 500,
                  "error": "Internal Server Error",
                  "exception": "java.lang.IllegalStateException",
                  "message": "No client mapped for id java.lang",
                  "path": "/clients/java.lang.IllegalArgumentException:%20Source%20map%20does%20not%20contain%20an%20attribute%20clientId"
                },
                "statusCode": "INTERNAL_SERVER_ERROR",
                "statusCodeValue": 500
              },
              "duration": 6
            },
            "name": "RestTemplateInvoker:Invoke HTTP Operation",
            "fullPath": "//io.osmosis.polymer.query.QueryProfiler:Root/DefaultQueryEngine:Invoke queryStrategy class io.osmosis.polymer.query.HipsterDiscoverGraphQueryStrategy/HipsterDiscoverGraphQueryStrategy:Searching for path Type_instance(io.vyne.tradeCompliance.aggregator.TradeRequest) -> Type(io.vyne.ClientJurisdiction)/EdgeNavigator:Evaluating Operation(io.vyne.demos.tradeCompliance.services.ClientService@@getClient) -[provides]-> Type_instance(io.vyne.demos.tradeCompliance.services.Client) with evaluator OperationInvocationEvaluator/RestTemplateInvoker:Invoke HTTP Operation",
            "id": "a68a2f15-ac0a-42aa-9f87-5123c7544fac",
            "children": [],
            "description": "RestTemplateInvoker.Invoke HTTP Operation",
            "duration": 6
          }],
          "description": "EdgeNavigator.Evaluating Operation(io.vyne.demos.tradeCompliance.services.ClientService@@getClient) -[provides]-> Type_instance(io.vyne.demos.tradeCompliance.services.Client) with evaluator OperationInvocationEvaluator",
          "duration": 11
        }],
        "description": "HipsterDiscoverGraphQueryStrategy.Searching for path Type_instance(io.vyne.tradeCompliance.aggregator.TradeRequest) -> Type(io.vyne.ClientJurisdiction)",
        "duration": 13
      }],
      "description": "DefaultQueryEngine.Invoke queryStrategy class io.osmosis.polymer.query.HipsterDiscoverGraphQueryStrategy",
      "duration": 16
    }],
    "description": "io.osmosis.polymer.query.QueryProfiler.Root",
    "duration": 16
  }
}
