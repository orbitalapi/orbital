import { NgModule } from '@angular/core';
import { InlineQueryRunnerComponent } from './inline-query-runner.component';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatExpansionModule } from '@angular/material/expansion';
import { CommonModule } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { VyneServicesModule } from '../services/vyne-services.module';
import { TabbedResultsViewModule } from '../tabbed-results-view/tabbed-results-view.module';

@NgModule({
  imports: [
    CommonModule, MatProgressBarModule, MatExpansionModule, MatButtonModule,
    VyneServicesModule, TabbedResultsViewModule
  ],
  exports: [InlineQueryRunnerComponent],
  declarations: [InlineQueryRunnerComponent],
  providers: [],
})
export class InlineQueryRunnerModule {
}
