import {moduleMetadata, storiesOf} from '@storybook/angular';
import {CommonModule} from '@angular/common';
import {BrowserModule} from '@angular/platform-browser';
import {ResultsTableModule} from '../results-table/results-table.module';
import {SchemaDisplayTableModule} from './schema-display-table.module';
import {testSchema} from '../object-view/test-schema';
import {findType} from '../services/schema';
import {ordersSchema} from './orders-schema';

const schema: any = ordersSchema;

const customerType = findType(schema, 'OrderTransaction');
storiesOf('Schema display table', module)
  .addDecorator(
    moduleMetadata({
      declarations: [],
      imports: [CommonModule, BrowserModule, SchemaDisplayTableModule]
    })
  )
  .add('default', () => {
    return {
      template: `<div style="padding: 40px; width: 100%; height: 250px; background: #f5f6fa" >
<app-schema-display-table [type]="customerType" [schema]="schema"></app-schema-display-table>
</div>`,
      props: {
        schema: schema,
        customerType
      }
    };
  });
