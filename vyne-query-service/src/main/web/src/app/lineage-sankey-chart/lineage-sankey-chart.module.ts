import {NgModule} from '@angular/core';

import {LineageSankeyChartComponent} from './lineage-sankey-chart.component';
import {GoogleChartsModule} from 'angular-google-charts';

@NgModule({
  imports: [GoogleChartsModule],
  exports: [LineageSankeyChartComponent],
  declarations: [LineageSankeyChartComponent],
  providers: [],
})
export class LineageSankeyChartModule {
}
