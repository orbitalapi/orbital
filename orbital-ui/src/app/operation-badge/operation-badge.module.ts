import {NgModule} from '@angular/core';

import {OperationBadgeComponent} from './operation-badge.component';
import {RouterModule} from '@angular/router';
import {CommonModule} from '@angular/common';

@NgModule({
  imports: [RouterModule, CommonModule],
  exports: [OperationBadgeComponent],
  declarations: [OperationBadgeComponent],
  providers: [],
})
export class OperationBadgeModule {
}
