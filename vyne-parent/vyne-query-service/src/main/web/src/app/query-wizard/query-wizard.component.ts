import {Component, OnInit} from '@angular/core';
import {ActivatedRoute} from "@angular/router";
import {Field, Modifier, Schema, Type, TypeReference} from "../services/schema";
import {TypesService} from "../services/types.service";
import {map} from "rxjs/operators";
import {
  ITdDynamicElementConfig,
  TdDynamicElement,
  TdDynamicFormsComponent,
  TdDynamicType
} from '@covalent/dynamic-forms';
import {FormControl} from "@angular/forms";
import {
  Fact,
  ProfilerOperation,
  Query,
  QueryMode,
  QueryResult,
  QueryService,
  RemoteCall
} from "../services/query.service";
import {HttpErrorResponse} from "@angular/common/http";

@Component({
  selector: 'query-wizard',
  templateUrl: './query-wizard.component.html',
  styleUrls: ['./query-wizard.component.scss'],
})
export class QueryWizardComponent implements OnInit {
  schema: Schema;
  queryMode = new FormControl();

  targetType: Type;

  constructor(private route: ActivatedRoute,
              private typesService: TypesService,
              private queryService: QueryService) {
  }

  forms: FactForm[] = [];
  facts: Fact[] = [];

  private subscribedDynamicForms: TdDynamicFormsComponent[] = [];

  lastQueryResult: QueryResult | QueryFailure;
  addingNewFact: boolean = false;

  ngOnInit() {
    this.typesService.getTypes()
      .subscribe(next => this.schema = next);
    this.route.queryParamMap
      .subscribe(params => {
          params.getAll("types")
            .forEach(type => this.appendEmptyType(type))
        }
      );

    this.queryMode.setValue(QueryMode.DISCOVER)
  }

  // Dirty hack to capture the forms generated dynamically, so we can listen for
  // form events
  getAndRegisterElements(factForm: FactForm, component: TdDynamicFormsComponent) {
    if (this.subscribedDynamicForms.indexOf(component) == -1) {
      component.form.valueChanges.subscribe(valueChangedEvent => {
        this.updateFact(factForm, component.value);
      });
      this.subscribedDynamicForms.push(component)
    }

    return factForm.elements;
  }

  removeFact(factForm: FactForm) {
    const index = this.forms.indexOf(factForm);
    this.forms.splice(index, 1);
    this.facts = this.buildFacts();
  }

  updateFact(formSpec: FactForm, value) {
    formSpec.value = value;

    // let fullyQualifiedName = formSpec.type.name.fullyQualifiedName;
    // TODO: Add a test for this.
    // The issue I found was that when the type passed in to the
    // query builder is a simple type (ie., CustomerNumber),
    // then the request was being built incorrectly, as
    // CustomerNumber : { CustomerNumber : 1 }
    // instead of :
    // CustomerNumber : 1
    // This whole thing is a smell, and need to get some decent
    // tests around this.
    // if (formSpec.type.scalar) {
    //   let unwrappedValue = Object.values(value)[0];
    //   this.facts[fullyQualifiedName] = unwrappedValue;
    // } else {
    //   let nestedValue = this.nest(value);
    //   this.facts[fullyQualifiedName] = nestedValue;
    // }

    this.facts = this.buildFacts();
  }

  private buildFacts(): Fact[] {
    // const facts = {};
    const facts = this.forms
      .filter(formSpec => formSpec.value)
      .map(formSpec => {
        let fullyQualifiedName = formSpec.type.name.fullyQualifiedName;
        if (formSpec.type.scalar) {
          let unwrappedValue = Object.values(formSpec.value)[0];
          return new Fact(fullyQualifiedName, unwrappedValue);
          // facts[fullyQualifiedName] = unwrappedValue;
        } else {
          let nestedValue = this.nest(formSpec.value);
          return new Fact(fullyQualifiedName, nestedValue);
          // facts[fullyQualifiedName] = nestedValue;
        }
      });
    return facts;
  }


  submitQuery() {
    this.lastQueryResult = null;
    // let facts = this.buildFacts();
    // let factList: Fact[] = Object.keys(facts).map(key => new Fact(key, facts[key]));
    const factList = this.buildFacts();
    let query = new Query(
      this.targetType.name.fullyQualifiedName,
      // this.targetTypeInput.value,
      factList,
      this.queryMode.value
    );
    this.queryService.submitQuery(query)
      .subscribe(result => {
        this.lastQueryResult = result
      }, error => {
        let errorResponse = error as HttpErrorResponse;
        if (errorResponse.error && (errorResponse.error as any).hasOwnProperty('profilerOperation')) {
          this.lastQueryResult = new QueryFailure(errorResponse.error.message, errorResponse.error.profilerOperation, errorResponse.error.remoteCalls)
        } else {
          // There was an unhandled error...
          console.error("An unhandled error occurred:");
          console.error(JSON.stringify(error));
        }
      })
  }

  // Convert a property of "foo.bar = 123" to an object with nested properties
  private nest(value: any): any {
    let result = {};
    Object.keys(value).forEach(attriubteName => {
      this.setValue(result, attriubteName, value[attriubteName]);
    });
    return result;
  }

  private setValue(target: any, path: string, value: any) {
    if (path.indexOf('.') === -1) {
      target[path] = value
    } else {
      let pathParts = path.split('.');
      let thisPart = pathParts.splice(0, 1)[0];
      if (!target.hasOwnProperty(thisPart)) {
        target[thisPart] = {};
      }
      this.setValue(target[thisPart], pathParts.join("."), value);
    }
  }

  private appendEmptyType(typeName: string) {
    this.typesService.getTypes()
      .pipe(
        map(schema => {
          let type = schema.types.find(type => type.name.fullyQualifiedName == typeName);
          return this.buildTypeForm(type, schema)
        }),
      ).subscribe(form => this.forms.push(form))
  }

  private buildTypeForm(type: Type, schema: Schema): FactForm {
    let elements = this.getElementsForType(type, schema);
    return new FactForm(
      elements,
      type
    )
  }

  private getElementsForType(type: Type, schema: Schema, prefix: string[] = [], fieldName: string = null): ITdDynamicElementConfig[] {
    if (type.scalar) {
      // suspect this is a smell I'm doing something wrong.
      // If the original root type was scalar, when we won't have a prefix, so
      // just use the name directly.
      // Otherwise, if we've navigated into this attribute, use the prefix, which is actually
      // the full path.
      let name = (prefix.length == 0) ? type.name.name : prefix.join(".");
      let label = (fieldName) ? `${fieldName} (${type.name.name})` : type.name.name
      return [{
        name: name,
        label: label,
        ...this.getInputControlForType(type)
      }];
    } else {
      let elements: ITdDynamicElementConfig[] = [];
      Object.keys(type.attributes).forEach(attributeName => {
        let attributeTypeRef = type.attributes[attributeName];
        let attributeType = schema.types.find(type => type.name.fullyQualifiedName == attributeTypeRef.type.fullyQualifiedName);
        let newPrefix = prefix.concat([attributeName]);
        elements = elements.concat(this.getElementsForType(attributeType, schema, newPrefix, attributeName));
      });
      return elements;
    }
  }

  private getInputControlForType(type: Type): any {
    if (!type.scalar) {
      throw new Error("Can only get inputs for scalar types");
    }
    // TODO : Aliases could be nested ... follow the chain
    let targetType = (type.aliasForType) ? type.aliasForType.fullyQualifiedName : type.name.fullyQualifiedName
    if (type.modifiers.indexOf(Modifier.ENUM) != -1) {
      return {
        type: TdDynamicElement.Select,
        selections: type.enumValues
      }
    }

    let control: any;
    switch (targetType) {
      case "lang.taxi.String" :
        control = {type: TdDynamicElement.Input};
        break;
      case "lang.taxi.Decimal" :
        control = {type: TdDynamicType.Number};
        break;
      case "lang.taxi.Int" :
        control = {type: TdDynamicType.Number};
        break;
      case "lang.taxi.Boolean" :
        control = {type: TdDynamicElement.Checkbox};
        break;
      default:
        debugger;
    }
    return control;
  }


  addNewFact() {
    this.addingNewFact = true;
  }

  onNewFactTypeSelected(type: Type) {
    this.appendEmptyType(type.name.fullyQualifiedName);
    this.addingNewFact = false;
  }
}

export class QueryFailure {
  constructor(readonly message: string, readonly profilerOperation: ProfilerOperation, readonly remoteCalls: RemoteCall[]) {
  }
}


export class FactForm {
  constructor(
    readonly elements: ITdDynamicElementConfig[],
    readonly  type: Type
  ) {

  }

  value: any

}

const stubbedResponse = {
  "results": {
    "io.vyne.demos.marketing.shop.AvailableRewards": {
      "rewards": [{
        "name": "Weekend at the spa",
        "priceInGbp": 300
      }, {"name": "Night at the moview", "priceInGbp": 20}, {"name": "Bottle of wine", "priceInGbp": 10}]
    }
  },
  "unmatchedNodes": [],
  "path": null,
  "duration": 166,
  "remoteCalls": [{
    "service": {
      "fullyQualifiedName": "io.vyne.demos.rewards.CustomerService",
      "parameters": [],
      "parameterizedName": "io.vyne.demos.rewards.CustomerService",
      "namespace": "io.vyne.demos.rewards",
      "name": "CustomerService"
    },
    "addresss": "http://192.168.5.73:9200/customers/email/jimmy@demo.com",
    "operation": "getCustomerByEmail",
    "responseTypeName": {
      "fullyQualifiedName": "demo.Customer",
      "parameters": [],
      "parameterizedName": "demo.Customer",
      "namespace": "demo",
      "name": "Customer"
    },
    "method": "GET",
    "requestBody": null,
    "resultCode": 200,
    "durationMs": 11,
    "response": {"id": 1, "name": "Jimmy", "email": "jimmy@demo.com"}
  }, {
    "service": {
      "fullyQualifiedName": "io.vyne.demos.marketing.MarketingService",
      "parameters": [],
      "parameterizedName": "io.vyne.demos.marketing.MarketingService",
      "namespace": "io.vyne.demos.marketing",
      "name": "MarketingService"
    },
    "addresss": "http://192.168.5.73:9201/marketing/1",
    "operation": "getMarketingDetailsForCustomer",
    "responseTypeName": {
      "fullyQualifiedName": "io.vyne.demos.marketing.CustomerMarketingRecord",
      "parameters": [],
      "parameterizedName": "io.vyne.demos.marketing.CustomerMarketingRecord",
      "namespace": "io.vyne.demos.marketing",
      "name": "CustomerMarketingRecord"
    },
    "method": "GET",
    "requestBody": null,
    "resultCode": 200,
    "durationMs": 10,
    "response": {"id": 1, "rewardsCardNumber": "4005-2003-2330-1002"}
  }, {
    "service": {
      "fullyQualifiedName": "io.vyne.demos.rewards.balances.RewardsBalanceService",
      "parameters": [],
      "parameterizedName": "io.vyne.demos.rewards.balances.RewardsBalanceService",
      "namespace": "io.vyne.demos.rewards.balances",
      "name": "RewardsBalanceService"
    },
    "addresss": "http://192.168.5.73:9202/balances/4005-2003-2330-1002",
    "operation": "getRewardsBalance",
    "responseTypeName": {
      "fullyQualifiedName": "demo.RewardsAccountBalance",
      "parameters": [],
      "parameterizedName": "demo.RewardsAccountBalance",
      "namespace": "demo",
      "name": "RewardsAccountBalance"
    },
    "method": "GET",
    "requestBody": null,
    "resultCode": 200,
    "durationMs": 7,
    "response": {"cardNumber": "4005-2003-2330-1002", "balance": 2300, "currencyUnit": "POINTS"}
  }, {
    "service": {
      "fullyQualifiedName": "io.vyne.demos.rewards.balances.RewardsBalanceService",
      "parameters": [],
      "parameterizedName": "io.vyne.demos.rewards.balances.RewardsBalanceService",
      "namespace": "io.vyne.demos.rewards.balances",
      "name": "RewardsBalanceService"
    },
    "addresss": "http://192.168.5.73:9202/balances/POINTS",
    "operation": "convert",
    "responseTypeName": {
      "fullyQualifiedName": "demo.RewardsAccountBalance",
      "parameters": [],
      "parameterizedName": "demo.RewardsAccountBalance",
      "namespace": "demo",
      "name": "RewardsAccountBalance"
    },
    "method": "POST",
    "requestBody": {"cardNumber": "4005-2003-2330-1002", "balance": 2300, "currencyUnit": "POINTS"},
    "resultCode": 200,
    "durationMs": 16,
    "response": {"cardNumber": "4005-2003-2330-1002", "balance": 1150, "currencyUnit": "POINTS"}
  }, {
    "service": {
      "fullyQualifiedName": "io.vyne.demos.marketing.shop.RewardsShopService",
      "parameters": [],
      "parameterizedName": "io.vyne.demos.marketing.shop.RewardsShopService",
      "namespace": "io.vyne.demos.marketing.shop",
      "name": "RewardsShopService"
    },
    "addresss": "http://192.168.5.73:9205/shop",
    "operation": "getAvailableRewards",
    "responseTypeName": {
      "fullyQualifiedName": "io.vyne.demos.marketing.shop.AvailableRewards",
      "parameters": [],
      "parameterizedName": "io.vyne.demos.marketing.shop.AvailableRewards",
      "namespace": "io.vyne.demos.marketing.shop",
      "name": "AvailableRewards"
    },
    "method": "POST",
    "requestBody": {"cardNumber": "4005-2003-2330-1002", "balance": 1150, "currencyUnit": "POINTS"},
    "resultCode": 200,
    "durationMs": 10,
    "response": {
      "rewards": [{"name": "Weekend at the spa", "priceInGbp": 300}, {
        "name": "Night at the moview",
        "priceInGbp": 20
      }, {"name": "Bottle of wine", "priceInGbp": 10}]
    }
  }],
  "vyneCost": 38,
  "fullyResolved": true,
  "timings": {"LOOKUP": 0, "GRAPH_TRAVERSAL": 27, "GRAPH_BUILDING": 11, "REMOTE_CALL": 127, "ROOT": 4}
}
