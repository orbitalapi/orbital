import {moduleMetadata, storiesOf} from '@storybook/angular';
import {CommonModule} from '@angular/common';
import {BrowserModule} from '@angular/platform-browser';
import {BrowserAnimationsModule} from '@angular/platform-browser/animations';
import {schemaWithNestedTypes} from '../../schema-importer/schema-importer.data';
import { TypeViewerModule } from '../type-viewer.module';
import { VERY_LARGE_SCHEMA } from './very-large-schema';

storiesOf('Model tree list', module)
  .addDecorator(
    moduleMetadata({
      declarations: [],
      imports: [CommonModule, BrowserModule, BrowserAnimationsModule, TypeViewerModule]
    })
  )
  .add('model display', () => {
    return {
      template: `
        <app-model-attribute-tree-list [model]="model" [schema]="schema" [editable]="true" [anonymousTypes]="anonymousTypes"></app-model-attribute-tree-list>
      `,
      props: {
        schema: schemaWithNestedTypes,
        model: schemaWithNestedTypes.types.find(t => t.name.fullyQualifiedName === 'io.vyne.demo.Person'),
        anonymousTypes: []
      }
    }
  })
  .add('large model display', () => {
    return {
      template: `
        <app-model-attribute-tree-list [model]="model" [schema]="schema" [editable]="true" [anonymousTypes]="anonymousTypes"></app-model-attribute-tree-list>
      `,
      props: {
        schema: VERY_LARGE_SCHEMA,
        model: VERY_LARGE_SCHEMA.types.find(t => t.name.fullyQualifiedName === 'com.ultumus.uatapi.indexComposition.definitions.Composition'),
        anonymousTypes: []
      }
    }
  })