import { moduleMetadata } from "@storybook/angular";
import { AttributeTableComponent } from "./attribute-table.component";
import { APP_BASE_HREF, CommonModule } from "@angular/common";
import { BrowserModule } from "@angular/platform-browser";
import { RouterTestingModule } from "@angular/router/testing";
import { findType } from "../../services/schema";
import { testSchema } from "../../object-view/test-schema";
import { schemaWithNestedTypes } from "../../data-source-import/data-source-import.data";

const type = findType(testSchema as any, "demo.Customer");
const nestedType = findType(schemaWithNestedTypes, "io.vyne.demo.Person");

export default {
  title: "AttributeTable",

  decorators: [
    moduleMetadata({
      declarations: [AttributeTableComponent],
      providers: [{ provide: APP_BASE_HREF, useValue: "/" }],
      imports: [CommonModule, BrowserModule, RouterTestingModule],
    }),
  ],
};

export const Default = () => {
  return {
    template: `<app-attribute-table [type]="type"></app-attribute-table> `,
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
    template: `<app-attribute-table [type]="nestedType"></app-attribute-table> `,
    props: {
      nestedType,
    },
  };
};

WithNestedAttributes.story = {
  name: "with nested attributes",
};
