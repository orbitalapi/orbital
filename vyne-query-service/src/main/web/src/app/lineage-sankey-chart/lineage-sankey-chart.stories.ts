import {moduleMetadata, storiesOf} from '@storybook/angular';
import {CommonModule} from '@angular/common';
import {BrowserModule} from '@angular/platform-browser';
import {BrowserAnimationsModule} from '@angular/platform-browser/animations';
import {LineageSankeyChartModule} from './lineage-sankey-chart.module';
import {lineageSankeyChartData} from './lineage-sankey-chart.data';
import {LineageDisplayModule} from '../lineage-display/lineage-display.module';

storiesOf('Lineage sankey chart', module)
  .addDecorator(
    moduleMetadata({
      declarations: [],
      imports: [CommonModule, BrowserModule, BrowserAnimationsModule, LineageSankeyChartModule, LineageDisplayModule]
    })
  )
  .add('default', () => {
    return {
      template: `<div style="padding: 40px">
<app-lineage-sankey-chart [rows]="chartData"></app-lineage-sankey-chart>
    </div>`,
      props: {
        chartData: lineageSankeyChartData
      }
    };
  })
  .add('lineage chart', () => {
    return {
      template: `<div style="padding: 40px">
<app-query-lineage [rows]="chartData"></app-query-lineage>
    </div>`,
      props: {
        chartData: lineageSankeyChartData
      }
    };
  });


