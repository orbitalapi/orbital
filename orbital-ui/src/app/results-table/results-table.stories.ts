import {HttpClientModule} from '@angular/common/http';
import {importProvidersFrom} from '@angular/core';
import {applicationConfig, moduleMetadata} from '@storybook/angular';
import { CommonModule } from "@angular/common";
import {of} from 'rxjs';
import {Environment, ENVIRONMENT} from '../services/environment';
import {QueryService} from '../services/query.service';
import {
  findType,
  TypeCollection,
  TypedInstance,
  TypeNamedInstance,
} from "../services/schema";
import { testSchema } from "../object-view/test-schema";
import { ResultsTableModule } from "./results-table.module";

const schema = testSchema;
const typeNamedInstance: TypeNamedInstance = {
  typeName: "demo.Customer",
  value: {
    id: { typeName: "demo.CustomerId", value: "1812" },
    name: { typeName: "demo.CustomerName", value: "Jimmy" },
    email: { typeName: "demo.CustomerEmailAddress", value: "jimmy@demo.com" },
    postcode: { typeName: "demo.Postcode", value: "SW11" },
    date: { typeName: "demo.Date", value: "2007-09-10T16:46:03.905Z" },
    balance: {
      typeName: "demo.RewardsAccountBalance",
      value: {
        balance: { typeName: "demo.RewardsBalance", value: 300 },
        cardNumber: {
          typeName: "demo.RewardsCardNumber",
          value: "1234-5678-3002-2003",
        },
        currencyUnit: { typeName: "demo.CurrencyUnit", value: "GBP" },
      },
    },
  },
};

const typeWithLineage: TypeNamedInstance = {
  typeName: "demo.Customer",
  value: {
    id: { typeName: "demo.CustomerId", value: 1 },
    name: { typeName: "demo.CustomerName", value: "Jimmy" },
    email: { typeName: "demo.CustomerEmailAddress", value: "jimmy@demo.com" },
    postcode: { typeName: "demo.Postcode", value: "SW11" },
    balance: {
      typeName: "demo.RewardsAccountBalance",
      value: {
        balance: { typeName: "demo.RewardsBalance", value: 300 },
        cardNumber: {
          typeName: "demo.RewardsCardNumber",
          value: "1234-5678-3002-2003",
        },
        currencyUnit: { typeName: "demo.CurrencyUnit", value: "GBP" },
      },
    },
  },
};

const typedInstance: TypedInstance = {
  type: findType(schema as TypeCollection, "demo.Customer"),
  value: {
    id: 1,
    name: "Jimmy",
    email: "jimmy@demo.com",
    postcode: "SW11",
    balance: {
      balance: 300,
      cardNumber: "123455677",
      currencyUnit: "GBP",
    },
  },
};

export default {
  title: "Result table",
  decorators: [
    moduleMetadata({
      declarations: [],
      imports: [CommonModule, ResultsTableModule],
    }),
    applicationConfig({
      providers: [
        {
          provide: ENVIRONMENT,
          useValue: {
            serverUrl: "http://localhost:9022",
            production: false,
          } as Environment,
        },
        QueryService,
        importProvidersFrom(
          HttpClientModule,
        )
      ]
    })
  ],
};

export const Default = () => {
  return {
    template: `<div style="padding: 40px; width: 100%; height: 250px" >
    <app-results-table  [type]="type" [schema]="schema" [instances$]="typedInstance"></app-results-table>
    <hr>
    <app-results-table  [type]="type" [schema]="schema" [instances$]="typeNamedInstance"></app-results-table>
    </div>`,
    props: {
      schema,
      type: typedInstance.type,
      typeNamedInstance: of(typeNamedInstance),
      typedInstance: of(typedInstance),
    },
  };
};

Default.story = {
  name: "default",
};

export const Collections = () => {
  return {
    template: `<div style="padding: 40px; width: 100%; height: 250px" >
    <app-results-table [schema]="schema"
        [instances$]="typedInstanceArray"
        [type]="type"></app-results-table>
    <hr>
    <app-results-table [schema]="schema"
                        [type]="type"
                        [instances$]="typeNamedInstanceArray"></app-results-table>
    </div>`,
    props: {
      type: typedInstance.type,
      schema,
      typeNamedInstanceArray: of([typeNamedInstance, typeNamedInstance]),
      typedInstanceArray: of([typedInstance, typedInstance]),
    },
  };
};

Collections.story = {
  name: "collections",
};
