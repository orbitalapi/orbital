import {moduleMetadata, storiesOf} from '@storybook/angular';
import {ContentsTableComponent} from '../contents-table/contents-table.component';
import {CommonModule} from '@angular/common';
import {BrowserModule} from '@angular/platform-browser';
import {TypeLinkGraphComponent} from './type-link-graph.component';
import {SchemaGraph} from '../../services/schema';
import {Observable, of} from 'rxjs';
import {NgxGraphModule} from '@swimlane/ngx-graph';
import {BrowserAnimationsModule} from "@angular/platform-browser/animations";


const linksResponse = {
  'nodes': {
    'OperationiovynedemosrewardsCustomerServicegetCustomerByEmail': {
      'id': 'OperationiovynedemosrewardsCustomerServicegetCustomerByEmail',
      'label': 'CustomerService/getCustomerByEmail()',
      'type': 'OPERATION',
      'nodeId': 'io.vyne.demos.rewards.CustomerService.getCustomerByEmail()'
    },
    'TypedemoCustomer': {'id': 'TypedemoCustomer', 'label': 'Customer', 'type': 'TYPE', 'nodeId': 'demo.Customer'},
    'MemberdemoCustomeremail': {
      'id': 'MemberdemoCustomeremail',
      'label': 'Customer/email',
      'type': 'MEMBER',
      'nodeId': 'demo.Customer:email'
    },
    'MemberdemoCustomerid': {
      'id': 'MemberdemoCustomerid',
      'label': 'Customer/id',
      'type': 'MEMBER',
      'nodeId': 'demo.Customer:id'
    },
    'MemberdemoCustomername': {
      'id': 'MemberdemoCustomername',
      'label': 'Customer/name',
      'type': 'MEMBER',
      'nodeId': 'demo.Customer:name'
    },
    'MemberdemoCustomerpostcode': {
      'id': 'MemberdemoCustomerpostcode',
      'label': 'Customer/postcode',
      'type': 'MEMBER',
      'nodeId': 'demo.Customer:postcode'
    }
  },
  'links': {
    '1264367428': {
      'source': 'OperationiovynedemosrewardsCustomerServicegetCustomerByEmail',
      'target': 'TypedemoCustomer',
      'label': 'provides'
    },
    '-1999251664': {'source': 'TypedemoCustomer', 'target': 'MemberdemoCustomeremail', 'label': 'Has attribute'},
    '-903635269': {'source': 'TypedemoCustomer', 'target': 'MemberdemoCustomerid', 'label': 'Has attribute'},
    '-282193397': {'source': 'TypedemoCustomer', 'target': 'MemberdemoCustomername', 'label': 'Has attribute'},
    '236088265': {'source': 'TypedemoCustomer', 'target': 'MemberdemoCustomerpostcode', 'label': 'Has attribute'}
  }
};

const schemaGraph = SchemaGraph.from(linksResponse.nodes, linksResponse.links);
const source: Observable<SchemaGraph> = of(schemaGraph);

storiesOf('TypeLinkGraph', module)
  .addDecorator(
    moduleMetadata({
      declarations: [TypeLinkGraphComponent],
      imports: [CommonModule, BrowserModule, NgxGraphModule, BrowserAnimationsModule]
    })
  ).add('default', () => {
  return {
    template: `<app-type-link-graph [schemaGraphs]="source"></app-type-link-graph>`,
    props: {
      source
    }
  };
});

