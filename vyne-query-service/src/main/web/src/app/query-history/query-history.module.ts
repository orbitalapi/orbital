import {NgModule} from '@angular/core';
import {QueryHistoryComponent} from './query-history.component';
import {BrowserModule} from '@angular/platform-browser';
import {CommonModule} from '@angular/common';
import {SearchModule} from '../search/search.module';
import {MatButtonModule} from '@angular/material/button';
import {ResultViewerModule} from '../query-wizard/result-display/result-viewer.module';
import {MomentModule} from 'ngx-moment';
import {MatToolbarModule} from '@angular/material/toolbar';
import { VyneqlRecordComponent } from './vyneql-record.component';
import { RestfulRecordComponent } from './restful-record.component';

@NgModule({
  imports: [
    BrowserModule,
    CommonModule,
    SearchModule,
    MatButtonModule,
    ResultViewerModule,
    MomentModule,
    MatToolbarModule
  ],
  exports: [QueryHistoryComponent],
  declarations: [QueryHistoryComponent, VyneqlRecordComponent, RestfulRecordComponent],
  providers: [],
})
export class QueryHistoryModule {
}
