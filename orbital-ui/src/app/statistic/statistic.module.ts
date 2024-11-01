import { TuiBadge } from "@taiga-ui/kit";
import {NgModule} from '@angular/core';
import {CommonModule} from '@angular/common';
import {StatisticComponent} from './statistic.component';

@NgModule({
  declarations: [StatisticComponent],
  exports: [StatisticComponent],
    imports: [
        CommonModule,
        TuiBadge
    ]
})
export class StatisticModule {
}
