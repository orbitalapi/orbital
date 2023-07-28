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
    TruncatePipeModule
  ]
})
export class QueryHistoryPanelModule { }
