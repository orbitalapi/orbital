import {NgModule} from '@angular/core';

import {ServiceStatsComponent} from './service-stats.component';
import {RouterModule} from '@angular/router';
import {CommonModule} from '@angular/common';

@NgModule({
  imports: [
    RouterModule,
    CommonModule
  ],
  exports: [ServiceStatsComponent],
  declarations: [ServiceStatsComponent],
  providers: [],
})
export class ServiceStatsModule {
}
