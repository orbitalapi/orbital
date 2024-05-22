import { moduleMetadata } from "@storybook/angular";
import { CommonModule } from "@angular/common";
import { BrowserModule } from "@angular/platform-browser";
import { BrowserAnimationsModule } from "@angular/platform-browser/animations";
import { of } from "rxjs";
import { searchResults } from "../../search/search-result-list/search-result.stories";
import { schemaWithNestedTypes } from "../../data-source-import/data-source-import.data";
import { OperationQueryResult } from "../../services/types.service";
import { SearchResultDocs } from "./type-search.component";
import { TypeViewerModule } from "../type-viewer.module";

export default {
  title: "Type search panel",

  decorators: [
    moduleMetadata({
      declarations: [],
      imports: [
        CommonModule,
        BrowserModule,
        BrowserAnimationsModule,
        TypeViewerModule,
      ],
    }),
  ],
};

export const TypeSearch = () => {
  return {
    template: `
<div style="padding: 40px">
      <app-type-search
      [searchResults]="searchResults"
      [searchResultDocs]="searchResultsDocs"

       (searchResultHighlighted)="onSearchResultHighlighted"></app-type-search>
      </div>
`,
    props: {
      searchResults: of(searchResults),
      searchResultsDocs: {
        type: schemaWithNestedTypes.types.find(
          (t) => t.name.fullyQualifiedName === "io.vyne.demo.Person",
        ),
        typeUsages: {
          typeName: "io.vyne.demo.Person",
          results: [],
        } as OperationQueryResult,
      } as SearchResultDocs,
    },
  };
};

TypeSearch.story = {
  name: "type search",
};

export const InProgress = () => {
  return {
    template: `
<div style="padding: 40px">
      <app-type-search
      [searchResults]="searchResults"
      [searchResultDocs]="searchResultsDocs"
        [loading]="true"
       (searchResultHighlighted)="onSearchResultHighlighted"></app-type-search>
      </div>
`,
    props: {
      searchResults: of(searchResults),
      searchResultsDocs: {
        type: schemaWithNestedTypes.types.find(
          (t) => t.name.fullyQualifiedName === "io.vyne.demo.Person",
        ),
        typeUsages: {
          typeName: "io.vyne.demo.Person",
          results: [],
        } as OperationQueryResult,
      } as SearchResultDocs,
    },
  };
};

InProgress.story = {
  name: "in progress",
};
