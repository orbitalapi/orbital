import {NgModule} from '@angular/core';

import {QualityCardComponent} from './quality-card.component';
import {MatCardModule} from '@angular/material/card';
import {LineChartModule} from '@swimlane/ngx-charts';
import {CommonModule} from '@angular/common';
import {QualityScoreComponent} from './quality-score.component';
import {MatIconModule} from '@angular/material/icon';

@NgModule({
  imports: [
    MatCardModule,
    LineChartModule,
    CommonModule,
    MatIconModule,
  ],
  exports: [QualityCardComponent, QualityScoreComponent],
  declarations: [QualityCardComponent, QualityScoreComponent],
  providers: [],
})
export class QualityCardModule {
}
