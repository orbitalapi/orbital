import {moduleMetadata, storiesOf} from '@storybook/angular';
import {APP_BASE_HREF, CommonModule} from '@angular/common';
import {BrowserModule} from '@angular/platform-browser';
import {RouterTestingModule} from '@angular/router/testing';
import {AttributeTableComponent} from './attribute-table/attribute-table.component';
import {TypeViewerComponent} from './type-viewer.component';
import {MatToolbarModule} from "@angular/material/toolbar";
import {ContentsTableComponent} from "./contents-table/contents-table.component";
import {TocHostDirective} from "./toc-host.directive";

const type = {
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

storiesOf('TypeViewer', module)
  .addDecorator(
    moduleMetadata({
      declarations: [AttributeTableComponent, TypeViewerComponent, ContentsTableComponent, TocHostDirective],
      providers: [{provide: APP_BASE_HREF, useValue: '/'}],
      imports: [CommonModule, BrowserModule, RouterTestingModule, MatToolbarModule]
    })
  ).add('default', () => {
  return {
    template: `<app-type-viewer [type]="type"></app-type-viewer>`,
    props: {
      type
    }
  };
});

