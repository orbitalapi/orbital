import {moduleMetadata, storiesOf} from '@storybook/angular';
import {ObjectViewComponent} from './object-view.component';
import {CommonModule} from '@angular/common';
import {BrowserModule} from '@angular/platform-browser';
import {findType, Schema, TypeCollection, TypedInstance} from '../services/schema';
import {TypeNamedInstance} from '../services/query.service';
import {testSchema} from './test-schema';

const schema = testSchema;
const typeNamedInstance: TypeNamedInstance = {
  'typeName': 'demo.Customer',
  'value': {
    'id': {'typeName': 'demo.CustomerId', 'value': 1},
    'name': {'typeName': 'demo.CustomerName', 'value': 'Jimmy'},
    'email': {'typeName': 'demo.CustomerEmailAddress', 'value': 'jimmy@demo.com'},
    'postcode': {'typeName': 'demo.Postcode', 'value': 'SW11'},
    'balance': {
      'typeName': 'demo.RewardsAccountBalance', value: {
        'balance': {'typeName': 'demo.RewardsBalance', value: 300},
        'cardNumber': {'typeName': 'demo.RewardsCardNumber', value: '1234-5678-3002-2003'},
        'currencyUnit': {'typeName': 'demo.CurrencyUnit', value: 'GBP'}
      }
    }
  }
};

const typedInstance: TypedInstance = {
  type: findType(schema as TypeCollection, 'demo.Customer'),
  value: {
    id: 1,
    name: 'Jimmy',
    email: 'jimmy@demo.com',
    postcode: 'SW11',
    balance: {
      balance: 300,
      cardNumber: '123455677',
      currencyUnit: 'GBP'
    }
  }
};

storiesOf('Object Viewer', module)
  .addDecorator(
    moduleMetadata({
      declarations: [ObjectViewComponent],
      imports: [CommonModule, BrowserModule]
    })
  ).add('default', () => {
  return {
    template: `<div style="padding: 40px">
    <app-object-view [schema]="schema" [instance]="typedInstance"></app-object-view>
    <hr>
    <app-object-view [schema]="schema" [instance]="typeNamedInstance"></app-object-view>
    </div>`,
    props: {
      schema,
      typeNamedInstance,
      typedInstance
    }
  };
});
