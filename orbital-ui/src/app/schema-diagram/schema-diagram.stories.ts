import { TuiRoot } from "@taiga-ui/core";
import {provideAnimations} from '@angular/platform-browser/animations';
import {applicationConfig, moduleMetadata} from '@storybook/angular';
import {CommonModule} from '@angular/common';
import {of} from 'rxjs';
import {SchemaDiagramModule} from './schema-diagram.module';
import {FILMS_SCHEMA} from './films-schema';

export default {
  title: "Schema Diagram",

  decorators: [
    moduleMetadata({
      declarations: [],
      imports: [
        CommonModule,
        TuiRoot,
        SchemaDiagramModule,
      ],
    }),
    applicationConfig({
      providers: [provideAnimations()],
    }),
  ],
};

export const ShowAModel = () => {
  return {
    template: `<tui-root>
        <div style="padding: 20px; width: 1000px; height: 1200px;">
            <app-schema-diagram [schema$]="schema" [displayedMembers]="displayedMembers"></app-schema-diagram>
        </div>
    </tui-root>`,
    props: {
      schema: of(FILMS_SCHEMA),
      displayedMembers: ["film.Film"],
    },
  };
};
ShowAModel.storyName = "show a model";

export const ShowAType = () => {
  return {
    template: `<tui-root>
        <div style="padding: 20px; width: 1200px; height: 1200px;">
            <app-schema-diagram [schema$]="schema" [displayedMembers]="displayedMembers"></app-schema-diagram>
        </div>
    </tui-root>`,
    props: {
      schema: of(FILMS_SCHEMA),
      displayedMembers: ["taxi.http.PathVariable"],
    },
  };
};
ShowAType.storyName = "show a type";

export const ShowAKafkaService = () => {
  return {
    template: `<tui-root>
        <div style="padding: 20px; width: 1200px; height: 1200px;">
            <app-schema-diagram [schema$]="schema" [displayedMembers]="displayedMembers"></app-schema-diagram>
        </div>
    </tui-root>`,
    props: {
      schema: of(FILMS_SCHEMA),
      displayedMembers: ["io.vyne.demos.films.KafkaPublisher"],
    },
  };
};
ShowAKafkaService.storyName = "show a kafka service";

export const ShowAllServices = () => {
  return {
    template: `<tui-root>
        <div style="padding: 20px; width: 1200px; height: 1200px;">
            <app-schema-diagram [schema$]="schema" [displayedMembers]="displayedMembers"></app-schema-diagram>
        </div>
    </tui-root>`,
    props: {
      schema: of(FILMS_SCHEMA),
      displayedMembers: 'services'
    },
  };
};
ShowAllServices.storyName = "show all services";

export const ShowEverything = () => {
  return {
    template: `<tui-root>
        <div style="padding: 20px; width: 1200px; height: 1200px;">
            <app-schema-diagram [schema$]="schema" [displayedMembers]="displayedMembers"></app-schema-diagram>
        </div>
    </tui-root>`,
    props: {
      schema: of(FILMS_SCHEMA),
      displayedMembers: 'everything'
    },
  };
};
ShowEverything.storyName = "show everything";
