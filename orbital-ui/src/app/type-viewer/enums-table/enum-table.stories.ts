import { moduleMetadata } from "@storybook/angular";
import { APP_BASE_HREF, CommonModule } from "@angular/common";
import { RouterTestingModule } from "@angular/router/testing";
import { findType } from "../../services/schema";
import { testSchema } from "../../object-view/test-schema";
import { schemaWithNestedTypes } from "../../data-source-import/data-source-import.data";
import {EnumTableComponent} from './enum-table.component';

const type = findType(testSchema as any, "demo.CurrencyUnit");
const nestedType = findType(schemaWithNestedTypes, "io.vyne.demo.Person");

export default {
  title: "EnumTable",

  decorators: [
    moduleMetadata({
      declarations: [EnumTableComponent],
      providers: [{ provide: APP_BASE_HREF, useValue: "/" }],
      imports: [CommonModule, RouterTestingModule],
    }),
  ],
};

export const Default = () => {
  return {
    template: `<app-enum-table [type]="type"></app-enum-table> `,
    props: {
      type,
    },
  };
};

Default.story = {
  name: "default",
};

export const WithNestedAttributes = () => {
  return {
    template: `<app-enum-table [type]="nestedType"></app-enum-table> `,
    props: {
      nestedType,
    },
  };
};

WithNestedAttributes.story = {
  name: "with nested attributes",
};
