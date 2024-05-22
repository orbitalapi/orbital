import { moduleMetadata } from "@storybook/angular";
import { CommonModule } from "@angular/common";
import { BrowserModule } from "@angular/platform-browser";
import { BrowserAnimationsModule } from "@angular/platform-browser/animations";
import { SchemaDiagramModule } from "./schema-diagram.module";
import { FILMS_SCHEMA } from "./films-schema";

export default {
  title: "Schema Diagram",

  decorators: [
    moduleMetadata({
      declarations: [],
      imports: [
        CommonModule,
        BrowserModule,
        BrowserAnimationsModule,
        SchemaDiagramModule,
      ],
    }),
  ],
};

export const ShowAModel = () => {
  return {
    template: `<div style="padding: 40px; width: 1200px; height: 1200px;">
<app-schema-diagram [schema]="schema" [displayedMembers]="types"></app-schema-diagram>
    </div>`,
    props: {
      schema: FILMS_SCHEMA,
      types: ["film.Film"],
    },
  };
};

ShowAModel.story = {
  name: "show a model",
};

export const ShowAQueryService = () => {
  return {
    template: `<div style="padding: 40px; width: 1200px; height: 1200px;">
<app-schema-diagram [schema]="schema" [displayedMembers]="types"></app-schema-diagram>
    </div>`,
    props: {
      schema: FILMS_SCHEMA,
      types: ["actor.ActorService"],
    },
  };
};

ShowAQueryService.story = {
  name: "show a query service",
};

export const ShowAnApiService = () => {
  return {
    template: `<div style="padding: 40px; width: 1200px; height: 1200px;">
<app-schema-diagram [schema]="schema" [displayedMembers]="types"></app-schema-diagram>
    </div>`,
    props: {
      schema: FILMS_SCHEMA,
      types: ["io.vyne.films.idlookup.IdLookupService"],
    },
  };
};

ShowAnApiService.story = {
  name: "show an api service",
};

export const ShowAKafkaService = () => {
  return {
    template: `<div style="padding: 40px; width: 1200px; height: 1200px;">
<app-schema-diagram [schema]="schema" [displayedMembers]="types"></app-schema-diagram>
    </div>`,
    props: {
      schema: FILMS_SCHEMA,
      types: ["io.vyne.films.announcements.KafkaService"],
    },
  };
};

ShowAKafkaService.story = {
  name: "show a kafka service",
};

export const ShowAllServices = () => {
  const services = FILMS_SCHEMA.services.map((s) => s.name.fullyQualifiedName);
  return {
    template: `<div style="padding: 40px; width: 1200px; height: 1200px;">
<app-schema-diagram [schema]="schema" [displayedMembers]="types"></app-schema-diagram>
    </div>`,
    props: {
      schema: FILMS_SCHEMA,
      types: services,
    },
  };
};

ShowAllServices.story = {
  name: "show all services",
};
