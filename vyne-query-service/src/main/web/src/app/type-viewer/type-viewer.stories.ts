import {moduleMetadata, storiesOf} from '@storybook/angular';
import {APP_BASE_HREF, CommonModule} from '@angular/common';
import {BrowserModule} from '@angular/platform-browser';
import {RouterTestingModule} from '@angular/router/testing';
import {AttributeTableComponent} from './attribute-table/attribute-table.component';
import {TypeViewerComponent} from './type-viewer.component';
import {MatToolbarModule} from '@angular/material/toolbar';
import {ContentsTableComponent} from './contents-table/contents-table.component';
import {TocHostDirective} from './toc-host.directive';
import {TypeViewerModule} from './type-viewer.module';
import {fqn} from '../services/schema';

const type = {
  'name': {
    'fullyQualifiedName': fqn('demo.Customer')
  },
  'attributes': {
    'email': {
      'type': fqn('demo.CustomerEmailAddress'),
      'collection': false,
      'fullyQualifiedName': 'demo.CustomerEmailAddress',
      'modifiers': [], 'accessor': null, 'constraints': []
    },
    'id': {
      'type': fqn('demo.CustomerId'), 'collection': false, 'fullyQualifiedName': 'demo.CustomerId'
      , 'modifiers': [], 'accessor': null, 'constraints': []
    },
    'name': {
      'type': fqn('demo.CustomerName'), 'collection': false, 'fullyQualifiedName': 'demo.CustomerName'
      , 'modifiers': [], 'accessor': null, 'constraints': []
    },
    'postcode': {
      'type': fqn('demo.Postcode'), 'collection': false, 'fullyQualifiedName': 'demo.Postcode'
      , 'modifiers': [], 'accessor': null, 'constraints': []
    }
  },
  'modifiers': [],
  'aliasForType': null,
  'inherits': [],
  'enumValues': [],
  'sources': [{
    'origin': 'customer-service:0.1.0',
    'language': 'Taxi',
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

storiesOf('TypeViewer', module
)
  .addDecorator(
    moduleMetadata({
      declarations: [],
      providers: [{provide: APP_BASE_HREF, useValue: '/'}],
      imports: [CommonModule, BrowserModule, RouterTestingModule, MatToolbarModule, TypeViewerModule]
    })
  ).add('default', () => {
  return {
    template: `<app-type-viewer [type]="type"></app-type-viewer>`,
    props: {
      type
    }
  };
});

