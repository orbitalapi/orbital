import { moduleMetadata, storiesOf } from '@storybook/angular';
import { CommonModule } from '@angular/common';
import { BrowserModule } from '@angular/platform-browser';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { SchemaDiagramModule } from './schema-diagram.module';
import { FILMS_SCHEMA } from './films-schema';

storiesOf('Schema Diagram', module)
  .addDecorator(
    moduleMetadata({
      declarations: [],
      imports: [CommonModule, BrowserModule, BrowserAnimationsModule, SchemaDiagramModule]
    })
  )
  .add('show a model', () => {
    return {
      template: `<div style="padding: 40px">
<app-schema-diagram [schema]="schema" [displayedMembers]="types"></app-schema-diagram>
    </div>`,
      props: {
        schema: FILMS_SCHEMA,
        types: ['film.Film']
      }
    };
  })
  .add('show a query service', () => {
  return {
    template: `<div style="padding: 40px">
<app-schema-diagram [schema]="schema" [displayedMembers]="types"></app-schema-diagram>
    </div>`,
    props: {
      schema: FILMS_SCHEMA,
      types: ['actor.ActorService']
    }
  };
})  .add('show an api service', () => {
  return {
    template: `<div style="padding: 40px">
<app-schema-diagram [schema]="schema" [displayedMembers]="types"></app-schema-diagram>
    </div>`,
    props: {
      schema: FILMS_SCHEMA,
      types: ['io.vyne.films.idlookup.IdLookupService']
    }
  };
}).add('show a kafka service', () => {
  return {
    template: `<div style="padding: 40px">
<app-schema-diagram [schema]="schema" [displayedMembers]="types"></app-schema-diagram>
    </div>`,
    props: {
      schema: FILMS_SCHEMA,
      types: ['io.vyne.films.announcements.KafkaService']
    }
  };
});
