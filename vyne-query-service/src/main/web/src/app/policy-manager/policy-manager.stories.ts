import {storiesOf, moduleMetadata} from '@storybook/angular';
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
import {CovalentHighlightModule} from '@covalent/highlight';
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
    'namespace': 'demo',
    'parameterizedName': 'demo.Customer',
    'name': 'Customer'
  },
  'attributes': {
    'email': {
      'type': {
        'name': {
          'fullyQualifiedName': 'demo.CustomerEmailAddress',
          'parameters': [],
          'namespace': 'demo',
          'parameterizedName': 'demo.CustomerEmailAddress',
          'name': 'CustomerEmailAddress'
        }, 'collection': false, 'fullyQualifiedName': 'demo.CustomerEmailAddress', isCollection: false,
        constraints: [],

      }, 'modifiers': [], 'accessor': null, 'constraints': []
    },
    'id': {
      'type': {
        'name': {
          'fullyQualifiedName': 'demo.CustomerId',
          'parameters': [],
          'namespace': 'demo',
          'parameterizedName': 'demo.CustomerId',
          'name': 'CustomerId'
        }, 'collection': false, 'fullyQualifiedName': 'demo.CustomerId', isCollection: false,
        constraints: [],

      }, 'modifiers': [], 'accessor': null, 'constraints': []
    },
    'name': {
      'type': {
        'name': {
          'fullyQualifiedName': 'demo.CustomerName',
          'parameters': [],
          'namespace': 'demo',
          'parameterizedName': 'demo.CustomerName',
          'name': 'CustomerName'
        }, 'collection': false, 'fullyQualifiedName': 'demo.CustomerName', isCollection: false,
        constraints: [],

      }, 'modifiers': [], 'accessor': null, 'constraints': []
    },
    'postcode': {
      'type': {
        'name': {
          'fullyQualifiedName': 'demo.Postcode',
          'parameters': [],
          'namespace': 'demo',
          'parameterizedName': 'demo.Postcode',
          'name': 'Postcode'
        }, 'collection': false, 'fullyQualifiedName': 'demo.Postcode', isCollection: false,
        constraints: [],
      }, 'modifiers': [], 'accessor': null, 'constraints': []
    }
  },
  'modifiers': [],
  'aliasForType': null,
  'inherits': [],
  'enumValues': [],
  'sources': [{
    'origin': 'customer-service:0.1.0',
    'language': 'Taxi',
    // tslint:disable-next-line:max-line-length
    'content': 'type Customer {\\n      email : CustomerEmailAddress\\n      id : CustomerId\\n      name : CustomerName\\n      postcode : Postcode\\n   }'
  }],
  'typeParameters': [],
  'isTypeAlias': false,
  'isScalar': false,
  'isParameterType': false,
  'isClosed': false,
  'inheritanceGraph': [],
  'closed': false,
  'fullyQualifiedName': 'demo.Customer',
  'memberQualifiedName': {
    'fullyQualifiedName': 'demo.Customer',
    'parameters': [],
    'namespace': 'demo',
    'parameterizedName': 'demo.Customer',
    'name': 'Customer'
  },
  'parameterType': false,
  'typeAlias': false,
  'scalar': false,
  'primitive': false
} as Type;

const schema: Schema = {
  types: [type],
  operations: [],
  services: []
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

