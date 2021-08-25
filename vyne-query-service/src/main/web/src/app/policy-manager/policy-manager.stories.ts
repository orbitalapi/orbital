/* tslint:disable:max-line-length */
import {moduleMetadata, storiesOf} from '@storybook/angular';
import {PolicyEditorComponent} from './policy-editor.component';
import {CaseConditionEditorComponent} from './case-condition-editor.component';
import {ElseEditorComponent} from './else-editor.component';
import {EqualsEditorComponent} from './equals-editor.component';
import {InstructionSelectorComponent} from './instruction-selector.component';
import {MultivalueEditorComponent} from './multivalue-editor.component';
import {PolicyManagerComponent} from './policy-manager.component';
import {StatementDisplayComponent} from './statement-display.component';
import {StatementEditorComponent} from './statement-editor.component';
import {CommonModule} from '@angular/common';
import {MatTabsModule} from '@angular/material/tabs';
import {BrowserAnimationsModule} from '@angular/platform-browser/animations';
import {BrowserModule} from '@angular/platform-browser';
import {MatSelectModule} from '@angular/material/select';
import {TypeAutocompleteComponent} from '../type-autocomplete/type-autocomplete.component';
import {MatChipsModule} from '@angular/material/chips';
import {MatIconModule} from '@angular/material/icon';
import {MatProgressBarModule} from '@angular/material/progress-bar';
import {FormsModule, ReactiveFormsModule} from '@angular/forms';
import {MatAutocompleteModule} from '@angular/material/autocomplete';
import {Policy} from './policies';
import {Schema, Type} from '../services/schema';
import {MatInputModule} from '@angular/material/input';
import {MatButtonModule} from '@angular/material/button';


const type = {
  'name': {
    'fullyQualifiedName': 'demo.Customer',
    'parameters': [],
    'name': 'Customer',
    'shortDisplayName': 'Customer',
    'longDisplayName': 'demo.Customer',
    'namespace': 'demo',
    'parameterizedName': 'demo.Customer'
  },
  'attributes': {
    'email': {
      'type': {
        'fullyQualifiedName': 'demo.CustomerEmailAddress',
        'parameters': [],
        'name': 'CustomerEmailAddress',
        'shortDisplayName': 'CustomerEmailAddress',
        'longDisplayName': 'demo.CustomerEmailAddress',
        'namespace': 'demo',
        'parameterizedName': 'demo.CustomerEmailAddress'
      },
      'modifiers': [],
      'accessor': null,
      'readCondition': null,
      'typeDoc': null,
      'constraints': []
    },
    'id': {
      'type': {
        'fullyQualifiedName': 'demo.CustomerId',
        'parameters': [],
        'name': 'CustomerId',
        'shortDisplayName': 'CustomerId',
        'longDisplayName': 'demo.CustomerId',
        'namespace': 'demo',
        'parameterizedName': 'demo.CustomerId'
      },
      'modifiers': [],
      'accessor': null,
      'readCondition': null,
      'typeDoc': null,
      'constraints': []
    },
    'name': {
      'type': {
        'fullyQualifiedName': 'demo.CustomerName',
        'parameters': [],
        'name': 'CustomerName',
        'shortDisplayName': 'CustomerName',
        'longDisplayName': 'demo.CustomerName',
        'namespace': 'demo',
        'parameterizedName': 'demo.CustomerName'
      },
      'modifiers': [],
      'accessor': null,
      'readCondition': null,
      'typeDoc': null,
      'constraints': []
    },
    'postcode': {
      'type': {
        'fullyQualifiedName': 'demo.Postcode',
        'parameters': [],
        'name': 'Postcode',
        'shortDisplayName': 'Postcode',
        'longDisplayName': 'demo.Postcode',
        'namespace': 'demo',
        'parameterizedName': 'demo.Postcode'
      },
      'modifiers': [],
      'accessor': null,
      'readCondition': null,
      'typeDoc': null,
      'constraints': []
    }
  },
  'modifiers': [],
  'metadata': [],
  'aliasForType': null,
  'inheritsFrom': [],
  'enumValues': [],
  'sources': [
    {
      'name': 'customer-service',
      'version': '0.0.0',
      'content': 'type Customer {\n      email : CustomerEmailAddress\n      id : CustomerId\n      name : CustomerName\n      postcode : Postcode\n   }',
    }
  ],
  'typeParameters': [],
  'typeDoc': '',
  'isTypeAlias': false,
  'format': null,
  'hasFormat': false,
  'isParameterType': false,
  'isClosed': false,
  'isPrimitive': false,
  'basePrimitiveTypeName' : null,
  'fullyQualifiedName': 'demo.Customer',
  'memberQualifiedName': {
    'fullyQualifiedName': 'demo.Customer',
    'parameters': [],
    'name': 'Customer',
    'shortDisplayName': 'Customer',
    'longDisplayName': 'demo.Customer',
    'namespace': 'demo',
    'parameterizedName': 'demo.Customer'
  },
  'isCollection': false,
  'underlyingTypeParameters': [],
  'collectionType': null,
  'isScalar': false
} as Type;

const schema: Schema = {
  types: [type],
  operations: [],
  services: [],
  members: []
};

const emptyPolicy = Policy.createNew(type);

storiesOf('PolicyManager', module)
  .addDecorator(
    moduleMetadata({
      declarations: [CaseConditionEditorComponent, ElseEditorComponent,
        EqualsEditorComponent, InstructionSelectorComponent, MultivalueEditorComponent,
        PolicyEditorComponent, PolicyManagerComponent, StatementDisplayComponent, StatementEditorComponent,
        TypeAutocompleteComponent,
      ],
      imports: [CommonModule, BrowserModule, MatTabsModule, FormsModule, ReactiveFormsModule, BrowserAnimationsModule, MatSelectModule,
        MatChipsModule, MatIconModule, MatProgressBarModule, MatAutocompleteModule, MatInputModule,
        MatButtonModule]
    })
  ).add('empty state', () => {
  return {
    template: `<div style="padding: 40px"><app-policy-manager [targetType]="type"></app-policy-manager></div>`,
    props: {
      type
    }
  };
})
  .add('new policy', () => {
    return {
      template: `<div style="padding: 40px"><app-policy-manager [targetType]="type" [policy]="policy" [schema]="schema"></app-policy-manager></div>`,
      props: {
        type,
        schema,
        policy: emptyPolicy
      }
    };
  })
;

