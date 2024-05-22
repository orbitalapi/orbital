import { moduleMetadata } from "@storybook/angular";
import { APP_BASE_HREF, CommonModule } from "@angular/common";
import { BrowserModule } from "@angular/platform-browser";
import { RouterTestingModule } from "@angular/router/testing";
import { AttributeTableComponent } from "./attribute-table/attribute-table.component";
import { TypeViewerComponent } from "./type-viewer.component";
import { MatToolbarModule } from "@angular/material/toolbar";
import { ContentsTableComponent } from "./contents-table/contents-table.component";
import { TocHostDirective } from "./toc-host.directive";
import { TypeViewerModule } from "./type-viewer.module";
import { findType, fqn, Metadata, Schema } from "../services/schema";
import {
  DATA_OWNER_FQN,
  DATA_OWNER_TAG_OWNER_NAME,
} from "../data-catalog/data-catalog.models";
import { testSchema } from "../object-view/test-schema";

const type = {
  name: {
    fullyQualifiedName: fqn("demo.Customer"),
  },
  attributes: {
    email: {
      type: fqn("demo.CustomerEmailAddress"),
      collection: false,
      fullyQualifiedName: "demo.CustomerEmailAddress",
      modifiers: [],
      accessor: null,
      constraints: [],
    },
    id: {
      type: fqn("demo.CustomerId"),
      collection: false,
      fullyQualifiedName: "demo.CustomerId",
      modifiers: [],
      accessor: null,
      constraints: [],
    },
    name: {
      type: fqn("demo.CustomerName"),
      collection: false,
      fullyQualifiedName: "demo.CustomerName",
      modifiers: [],
      accessor: null,
      constraints: [],
    },
    postcode: {
      type: fqn("demo.Postcode"),
      collection: false,
      fullyQualifiedName: "demo.Postcode",
      modifiers: [],
      accessor: null,
      constraints: [],
    },
  },
  modifiers: [],
  aliasForType: null,
  inherits: [],
  enumValues: [],
  sources: [
    {
      origin: "customer-service:0.1.0",
      language: "Taxi",
      content:
        "type Customer {\n      email : CustomerEmailAddress\n      id : CustomerId\n      name : CustomerName\n      postcode : Postcode\n   }",
    },
  ],
  typeParameters: [],
  isTypeAlias: false,
  isScalar: false,
  isParameterType: false,
  isClosed: false,
  inheritanceGraph: [],
  closed: false,
  primitive: false,
  fullyQualifiedName: "demo.Customer",
  memberQualifiedName: {
    fullyQualifiedName: "demo.Customer",
    parameters: [],
    name: "Customer",
    namespace: "demo",
    parameterizedName: "demo.Customer",
  },
  parameterType: false,
  scalar: false,
  typeAlias: false,
};

export default {
  title: "TypeViewer",

  decorators: [
    moduleMetadata({
      declarations: [],
      providers: [{ provide: APP_BASE_HREF, useValue: "/" }],
      imports: [
        CommonModule,
        BrowserModule,
        RouterTestingModule,
        MatToolbarModule,
        TypeViewerModule,
      ],
    }),
  ],
};

export const Default = () => {
  return {
    template: `<app-type-viewer [type]="type"></app-type-viewer>`,
    props: {
      type,
    },
  };
};

Default.story = {
  name: "default",
};

export const TagsSection = () => {
  return {
    template: `
<div>
<app-tags-section [metadata]="metadataWithOwner"></app-tags-section>
<hr />
<app-tags-section [metadata]="metadataWithoutOwner"></app-tags-section>
<hr />
<app-tags-section [metadata]="emptyMetadata"></app-tags-section>
</div>
`,
    props: {
      metadataWithOwner: [
        {
          name: fqn(DATA_OWNER_FQN),
          params: {
            name: "Jimmy Pitt",
          },
        },
        {
          name: fqn("io.vyne.Gdpr"),
          params: {},
        },
        {
          name: fqn("io.vyne.Sensitive"),
          params: {},
        },
      ] as Metadata[],
      metadataWithoutOwner: [
        {
          name: fqn("io.vyne.Gdpr"),
          params: {},
        },
        {
          name: fqn("io.vyne.Sensitive"),
          params: {},
        },
      ] as Metadata[],
      emptyMetadata: [] as Metadata[],
    },
  };
};

TagsSection.story = {
  name: "tags section",
};

export const TagEditor = () => {
  return {
    template: `<div style="background-color: #F5F7F9; padding-left: 100px;">
    <app-edit-tags-panel [availableTags]="availableTags" [selectedTags]="selectedTags"></app-edit-tags-panel>
</div>`,
    props: {
      availableTags: [
        fqn("com.foo.bar.Gdpr"),
        fqn("com.foo.bar.Sensitive"),
        fqn("com.foo.bar.MaterialImpact"),
      ],
      selectedTags: [fqn("com.foo.bar.Gdpr")],
    },
  };
};

TagEditor.story = {
  name: "tag editor",
};

export const InheritsFrom = () => {
  return {
    template: `<div style="background-color: #F5F7F9; padding-left: 100px;">
<h3>Primitive type</h3>
<app-inherits-from [type]="primitiveType" [editable]="false"></app-inherits-from>
<h3>Primitive type (editable)</h3>
<app-inherits-from [type]="primitiveType" [editable]="true"></app-inherits-from>
<h3>Semantic type</h3>
<app-inherits-from [type]="semanticType" [editable]="false"></app-inherits-from>
<h3>Semantic type (editable)</h3>
<app-inherits-from [type]="semanticType" [editable]="true"></app-inherits-from>
<h3>Inherits from sSemantic type</h3>
<app-inherits-from [type]="inheritsFromSemanticType" [editable]="false"></app-inherits-from>
<h3>Inherits from semantic type (editable)</h3>
<app-inherits-from [type]="inheritsFromSemanticType" [editable]="true"></app-inherits-from>
</div>`,
    props: {
      primitiveType: findType(testSchema, "lang.taxi.String"),
      semanticType: findType(testSchema, "demo.CustomerEmailAddress"),
      inheritsFromSemanticType: findType(
        testSchema,
        "demo.CustomerWorkEmailAddress",
      ),
    },
  };
};

InheritsFrom.story = {
  name: "inherits from",
};
