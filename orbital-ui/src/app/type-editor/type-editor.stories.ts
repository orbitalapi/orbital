import { moduleMetadata } from "@storybook/angular";
import { testSchema } from "../object-view/test-schema";
import { TypedEditorModule } from "./type-editor.module";

export default {
  title: "Type Editor",

  decorators: [
    moduleMetadata({
      declarations: [],
      imports: [TypedEditorModule],
    }),
  ],
};

export const Default = () => {
  return {
    template: `<div style="padding: 40px; width: 60rem;">
<app-type-editor [schema]="schema"></app-type-editor>
</div>`,
    props: {
      schema: testSchema,
    },
  };
};

export const Card = () => {
  return {
    template: `<div style="padding: 40px; width: 60rem;">
<app-type-editor-card [schema]="schema"></app-type-editor-card>
</div>`,
    props: {
      schema: testSchema,
    },
  };
};

export const Popup = () => {
  return {
    template: `<div style="padding: 40px; width: 60rem;">
<app-type-editor-popup [schema]="schema"></app-type-editor-popup>
</div>`,
    props: {
      schema: testSchema,
    },
  };
};
