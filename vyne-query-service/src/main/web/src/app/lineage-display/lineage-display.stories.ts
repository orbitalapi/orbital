import {moduleMetadata, storiesOf} from '@storybook/angular';
import {ObjectViewComponent} from '../object-view/object-view.component';
import {CommonModule} from '@angular/common';
import {BrowserModule} from '@angular/platform-browser';
import {LineageDisplayModule} from './lineage-display.module';
import {
  LINEAGE_GRAPH, LINEAGE_GRAPH_WITH_COMPLEX_EXPRESSION,
  LINEAGE_GRAPH_WITH_EVALUATED_EXPRESSION,
  LINEAGE_GRAPH_WITH_FAILED_EXPRESSION
} from './lineage-data';
import {QueryService} from '../services/query.service';
import {lineageSankeyChartData} from './lineage-sankey-chart.data';

class MockQueryService implements Partial<QueryService> {

}

storiesOf('Lineage display', module)
  .addDecorator(
    moduleMetadata({
      declarations: [],
      providers: [{provide: QueryService, useClass: MockQueryService}],
      imports: [CommonModule, BrowserModule, LineageDisplayModule]
    })
  ).add('default', () => {
  return {
    template: `<div style="padding: 40px">
    <app-lineage-display
    [dataSource]="lineageGraph"
    [instance]="typeNamedInstance"
    ></app-lineage-display>
    </div>`,
    props: {
      lineageGraph: LINEAGE_GRAPH,
      typeNamedInstance: {
        'typeName': 'demo.RewardsBalance',
        'value': 2300
      }
    }
  };
})
  .add('evaluated expression', () => {
    return {
      template: `<div style="padding: 40px">
    <app-lineage-display
    [dataSource]="lineageGraph"
    [instance]="typeNamedInstance"
    ></app-lineage-display>
    </div>`,
      props: {
        lineageGraph: LINEAGE_GRAPH_WITH_EVALUATED_EXPRESSION,
        typeNamedInstance: {
          'typeName': 'demo.RewardsBalance',
          'value': 2300
        }
      }
    };

  })
  .add('failed expression', () => {
    return {
      template: `<div style="padding: 40px">
    <app-lineage-display
    [dataSource]="lineageGraph"
    [instance]="typeNamedInstance"
    ></app-lineage-display>
    </div>`,
      props: {
        lineageGraph: LINEAGE_GRAPH_WITH_FAILED_EXPRESSION,
        typeNamedInstance: {
          'typeName': 'demo.RewardsBalance',
          'value': 2300
        }
      }
    };

  })
  .add('complex expression', () => {
    return {
      template: `<div style="padding: 40px">
    <app-lineage-display
    [dataSource]="lineageGraph"
    [instance]="typeNamedInstance"
    ></app-lineage-display>
    </div>`,
      props: {
        lineageGraph: JSON.parse(JSON.stringify(LINEAGE_GRAPH_WITH_COMPLEX_EXPRESSION)),
        typeNamedInstance: {
          'typeName': 'demo.RewardsBalance',
          'value': 2300
        }
      }
    };

  })
  .add('query lineage chart', () => {
    return {
      template: `<div style="padding: 40px">
<app-query-lineage [rows]="chartData"></app-query-lineage>
    </div>`,
      props: {
        chartData: lineageSankeyChartData
      }
    };
  });
