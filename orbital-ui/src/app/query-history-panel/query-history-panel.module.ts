import { TuiProgress } from "@taiga-ui/kit";
import { TuiNotification } from "@taiga-ui/core";
import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { QueryHistoryPanelComponent } from './query-history-panel.component';
import { QueryHistoryEntryComponent } from './query-history-entry.component';
import {TruncatePipeModule} from "../truncate-pipe/truncate-pipe.module";

@NgModule({
  declarations: [
    QueryHistoryPanelComponent,
    QueryHistoryEntryComponent
  ],
  exports: [
    QueryHistoryPanelComponent
  ],
  imports: [
    CommonModule,
    TruncatePipeModule,
    TuiNotification,
    ...TuiProgress
  ]
})
export class QueryHistoryPanelModule { }
