import { moduleMetadata } from "@storybook/angular";
import { ObjectViewComponent } from "./object-view.component";
import { CommonModule } from "@angular/common";
import { BrowserModule } from "@angular/platform-browser";
import {
  findType,
  Schema,
  TypeCollection,
  TypedInstance,
  TypeNamedInstance,
} from "../services/schema";
import { testSchema } from "./test-schema";
import { ObjectViewModule } from "./object-view.module";
import { TuiRootModule } from "@taiga-ui/core";

const schema = testSchema;
const typeNamedInstance: TypeNamedInstance = {
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

const typedInstance: TypedInstance = Object.freeze({
  type: findType(schema as TypeCollection, "demo.Customer"),
  value: {
    id: 1,
    name: "Jimmy",
    email: "jimmy@demo.com",
    postcode: "SW11",
  },
});
const nestedTypedInstance: TypedInstance = Object.freeze({
  type: findType(schema as TypeCollection, "demo.CustomerBalance"),
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
    balances: [
      {
        balance: 300,
        cardNumber: "123455677",
        currencyUnit: "GBP",
      },
      {
        balance: 300,
        cardNumber: "123455677",
        currencyUnit: "GBP",
      },
    ],
  },
});

export default {
  title: "Object Viewer",

  decorators: [
    moduleMetadata({
      declarations: [],
      imports: [CommonModule, BrowserModule, ObjectViewModule, TuiRootModule],
    }),
  ],
};

export const ContainerView = () => {
  return {
    template: `<div style="padding: 40px">
    <app-object-view-container [schema]="schema" [instance]="typedInstance" style="display: block; height: 300px"></app-object-view-container>
    </div>`,
    props: {
      schema,
      typeNamedInstance,
      typedInstance,
    },
  };
};

ContainerView.story = {
  name: "container view",
};

export const Default = () => {
  return {
    template: `
<tui-root>
<div style="padding: 40px">
    <app-object-view [schema]="schema" [instance]="typedInstance" [type]="type"></app-object-view>
    <hr>
    <app-object-view [schema]="schema" [instance]="typeNamedInstance" [type]="type"></app-object-view>
    </div>
</tui-root>`,
    props: {
      schema,
      type: typedInstance.type,
      typeNamedInstance,
      typedInstance,
    },
  };
};

Default.story = {
  name: "default",
};

export const Collections = () => {
  return {
    template: `
<tui-root>
<div style="padding: 40px; height: 800px;">
    <app-object-view [schema]="schema" [instance]="typedInstanceArray" [type]="type"></app-object-view>
<!--    <hr>-->
<!--    <app-object-view [schema]="schema" [instance]="typeNamedInstanceArray" [type]="type"></app-object-view>-->
    </div>
</tui-root>`,
    props: {
      schema,
      type: typedInstance.type,
      typeNamedInstanceArray: [typeNamedInstance, typeNamedInstance],
      typedInstanceArray: [typedInstance, typedInstance],
    },
  };
};

Collections.story = {
  name: "collections",
};

export const VeryLargeCollection = () => {
  return {
    template: `
<tui-root>
<div style="padding: 40px; height: 300px;">
    <app-object-view [schema]="schema" [instance]="typedInstanceArray" [type]="type"></app-object-view>
    </div>
</tui-root>`,
    props: {
      schema,
      type: nestedTypedInstance.type,
      typedInstanceArray: Array(500).fill(nestedTypedInstance),
    },
  };
};

VeryLargeCollection.story = {
  name: "very large collection",
};

export const WithNested = () => {
  return {
    template: `
<tui-root>
<div style="padding: 40px; height: 300px;">
    <app-object-view [schema]="schema" [instance]="typedInstanceArray" [type]="type"></app-object-view>
    </div>
</tui-root>`,
    props: {
      schema,
      type: nestedTypedInstance.type,
      typeNamedInstanceArray: [nestedTypedInstance, nestedTypedInstance],
      typedInstanceArray: [nestedTypedInstance, nestedTypedInstance],
    },
  };
};

WithNested.story = {
  name: "with nested",
};
