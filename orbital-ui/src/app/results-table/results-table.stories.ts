import {moduleMetadata, storiesOf} from '@storybook/angular';
import {CommonModule} from '@angular/common';
import {BrowserModule} from '@angular/platform-browser';
import {findType, TypeCollection, TypedInstance, TypeNamedInstance} from '../services/schema';
import {testSchema} from '../object-view/test-schema';
import {ResultsTableModule} from './results-table.module';

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

const typeWithLineage: TypeNamedInstance = {
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

storiesOf('Result table', module)
  .addDecorator(
    moduleMetadata({
      declarations: [],
      imports: [CommonModule, BrowserModule, ResultsTableModule]
    })
  ).add('default', () => {
  return {
    template: `<div style="padding: 40px; width: 100%; height: 250px" >
    <app-results-table  [type]="type" [schema]="schema" [instance]="typedInstance"></app-results-table>
    <hr>
    <app-results-table  [type]="type" [schema]="schema" [instance]="typeNamedInstance"></app-results-table>
    </div>`,
    props: {
      schema,
      type: typedInstance.type,
      typeNamedInstance,
      typedInstance
    }
  };
})
  .add('collections', () => {
    return {
      template: `<div style="padding: 40px; width: 100%; height: 250px" >
    <app-results-table [schema]="schema"
        [instance]="typedInstanceArray"
        [type]="type"></app-results-table>
    <hr>
    <app-results-table [schema]="schema"
                        [type]="type"
                        [instance]="typeNamedInstanceArray"></app-results-table>
    </div>`,
      props: {
        type: typedInstance.type,
        schema,
        typeNamedInstanceArray: [typeNamedInstance, typeNamedInstance],
        typedInstanceArray: [typedInstance, typedInstance]
      }
    };
  });
