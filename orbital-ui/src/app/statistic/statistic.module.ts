import {NgModule} from '@angular/core';
import {CommonModule} from '@angular/common';
import {StatisticComponent} from './statistic.component';
import {TuiBadgeModule} from "@taiga-ui/kit";


@NgModule({
  declarations: [StatisticComponent],
  exports: [StatisticComponent],
    imports: [
        CommonModule,
        TuiBadgeModule
    ]
})
export class StatisticModule {
}
