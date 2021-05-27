import {NgModule} from '@angular/core';

import {QualityCardComponent} from './quality-card.component';
import {MatCardModule} from '@angular/material/card';
import {LineChartModule} from '@swimlane/ngx-charts';
import {CommonModule} from '@angular/common';
import {QualityScoreComponent} from './quality-score.component';
import {MatIconModule} from '@angular/material/icon';
import {BrowserAnimationsModule} from '@angular/platform-browser/animations';
import { AttributeScoreTableComponent } from './attribute-score-table.component';
import { RuleScoreTableComponent } from './rule-score-table.component';
import {MatButtonModule} from '@angular/material/button';
import { QualityCardContainerComponent } from './quality-card-container.component';

@NgModule({
  imports: [
    BrowserAnimationsModule,
    MatCardModule,
    LineChartModule,
    CommonModule,
    MatIconModule,
    MatButtonModule,
  ],
  exports: [QualityCardComponent, QualityScoreComponent, QualityCardContainerComponent],
  declarations: [QualityCardComponent, QualityScoreComponent, AttributeScoreTableComponent, RuleScoreTableComponent, QualityCardContainerComponent],
  providers: [],
})
export class QualityCardModule {
}
