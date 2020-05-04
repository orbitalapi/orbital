import {moduleMetadata, storiesOf} from '@storybook/angular';
import {BrowserModule} from '@angular/platform-browser';
import {RouterModule} from '@angular/router';
import {routerModule} from '../../app.module';
import {RouterTestingModule} from '@angular/router/testing';
import {DescriptionEditorComponent} from './description-editor.component';
import {CommonModule} from '@angular/common';
import {MatButtonModule} from '@angular/material/button';
import {MatIconModule} from '@angular/material/icon';

// tslint:disable-next-line:max-line-length
const typeDoc = 'This is the type description. \nIt contains markdown, including **bold** and *underline* text. \n ```This is a code block```.';
const typeWithoutDoc = {
  'name': {
    'fullyQualifiedName': 'demo.Customer',
    'parameters': [],
    'name': 'Customer',
    'namespace': 'demo',
    'parameterizedName': 'demo.Customer'
  },
  'attributes': {
    'email': {
      'type': {
        'name': {
          'fullyQualifiedName': 'demo.CustomerEmailAddress',
          'parameters': [],
          'name': 'CustomerEmailAddress',
          'namespace': 'demo',
          'parameterizedName': 'demo.CustomerEmailAddress'
        }, 'collection': false, 'fullyQualifiedName': 'demo.CustomerEmailAddress'
      }, 'modifiers': [], 'accessor': null, 'constraints': []
    },
    'id': {
      'type': {
        'name': {
          'fullyQualifiedName': 'demo.CustomerId',
          'parameters': [],
          'name': 'CustomerId',
          'namespace': 'demo',
          'parameterizedName': 'demo.CustomerId'
        }, 'collection': false, 'fullyQualifiedName': 'demo.CustomerId'
      }, 'modifiers': [], 'accessor': null, 'constraints': []
    },
    'name': {
      'type': {
        'name': {
          'fullyQualifiedName': 'demo.CustomerName',
          'parameters': [],
          'name': 'CustomerName',
          'namespace': 'demo',
          'parameterizedName': 'demo.CustomerName'
        }, 'collection': false, 'fullyQualifiedName': 'demo.CustomerName'
      }, 'modifiers': [], 'accessor': null, 'constraints': []
    },
    'postcode': {
      'type': {
        'name': {
          'fullyQualifiedName': 'demo.Postcode',
          'parameters': [],
          'name': 'Postcode',
          'namespace': 'demo',
          'parameterizedName': 'demo.Postcode'
        }, 'collection': false, 'fullyQualifiedName': 'demo.Postcode'
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
    'content': 'type Customer {\n      email : CustomerEmailAddress\n      id : CustomerId\n      name : CustomerName\n      postcode : Postcode\n   }'
  }],
  'typeParameters': [],
  'isTypeAlias': false,
  'isScalar': false,
  'isParameterType': false,
  'isClosed': false,
  'inheritanceGraph': [],
  'closed': false,
  'primitive': false,
  'fullyQualifiedName': 'demo.Customer',
  'memberQualifiedName': {
    'fullyQualifiedName': 'demo.Customer',
    'parameters': [],
    'name': 'Customer',
    'namespace': 'demo',
    'parameterizedName': 'demo.Customer'
  },
  'parameterType': false,
  'scalar': false,
  'typeAlias': false
};

const typeWithDoc = {
  typeDoc: typeDoc,
  ...typeWithoutDoc
};

storiesOf('DescriptionEditor', module)
  .addDecorator(
    moduleMetadata({
      declarations: [DescriptionEditorComponent],
      imports: [CommonModule, BrowserModule, RouterTestingModule, MatButtonModule, MatIconModule]
    })
  ).add('with default docs', () => {
  return {
    template: `<div style="padding: 20px"><app-description-editor [type]="type"></app-description-editor></div>`,
    props: {
      type: typeWithDoc
    }
  };
})
.add('without default docs', () => {
  return {
    template: `<div style="padding: 20px"><app-description-editor [type]="type"></app-description-editor></div>`,
    props: {
      type: typeWithoutDoc
    }
  };
});

