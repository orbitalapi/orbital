import {moduleMetadata, storiesOf} from '@storybook/angular';
import {AttributeTableComponent} from './attribute-table.component';
import {APP_BASE_HREF, CommonModule} from '@angular/common';
import {BrowserModule} from '@angular/platform-browser';
import {RouterModule} from '@angular/router';
import {routerModule} from '../../app.module';
import {RouterTestingModule} from '@angular/router/testing';
import {findType} from '../../services/schema';
import {testSchema} from '../../object-view/test-schema';
import {schemaWithNestedTypes} from '../../schema-importer/schema-importer.data';

const type = findType(testSchema as any, 'demo.Customer')
const nestedType = findType(schemaWithNestedTypes, 'io.vyne.demo.Person' )
storiesOf('AttributeTable', module)
  .addDecorator(
    moduleMetadata({
      declarations: [AttributeTableComponent],
      providers: [{provide: APP_BASE_HREF, useValue: '/'}],
      imports: [CommonModule, BrowserModule, RouterTestingModule]
    })
  ).add('default', () => {
  return {
    template: `<app-attribute-table [type]="type"></app-attribute-table> `,
    props: {
      type
    }
  };
})
  .add('with nested attributes', () => {
    return {
      template: `<app-attribute-table [type]="nestedType"></app-attribute-table> `,
      props: {
        nestedType
      }
    };
  });

