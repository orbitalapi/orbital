import { NgModule } from '@angular/core';
import { QueryHistoryComponent } from './query-history.component';
import { CommonModule } from '@angular/common';
import { SearchModule } from '../search/search.module';
import { MatButtonModule } from '@angular/material/button';
import { MomentModule } from 'ngx-moment';
import { MatToolbarModule } from '@angular/material/toolbar';
import { VyneqlRecordComponent } from './vyneql-record.component';
import { RestfulRecordComponent } from './restful-record.component';
import { MatSidenavModule } from '@angular/material/sidenav';
import { TypedInstancePanelModule } from '../typed-instance-panel/typed-instance-panel.module';
import { ScrollingModule } from '@angular/cdk/scrolling';
import { QueryListComponent } from './query-list.component';
import { MatIconModule } from '@angular/material/icon';
import { QueryHistoryCardComponent } from './query-history-card.component';
import { QueryPanelModule } from '../query-panel/query-panel.module';
import { HeaderBarModule } from '../header-bar/header-bar.module';
import { MatDialogModule } from '@angular/material/dialog';
import { ActiveQueryCardComponent } from './active-query-card.component';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { TabbedResultsViewModule } from '../tabbed-results-view/tabbed-results-view.module';
import { AngularSplitModule } from 'angular-split';
import { ExpandingPanelSetModule } from '../expanding-panelset/expanding-panel-set.module';
import { TuiButtonModule } from '@taiga-ui/core';
import { RouterModule } from '@angular/router';
import { ResultsDownloadModule } from 'src/app/results-download/results-download.module';
import { TruncatePipeModule } from 'src/app/truncate-pipe/truncate-pipe.module';

@NgModule({
  imports: [
    TruncatePipeModule,
    CommonModule,
    SearchModule,
    MatButtonModule,
    MomentModule,
    MatToolbarModule,
    MatIconModule,
    MatSidenavModule,
    TypedInstancePanelModule,
    ScrollingModule,
    QueryPanelModule,
    HeaderBarModule,
    MatDialogModule,
    MatProgressBarModule,
    TabbedResultsViewModule,
    AngularSplitModule,
    ExpandingPanelSetModule,
    TuiButtonModule,
    ResultsDownloadModule,
    RouterModule.forChild([
      {
        path: '',
        component: QueryHistoryComponent,
      },
      {
        path: ':queryResponseId',
        component: QueryHistoryComponent,
      },
    ])
  ],
  exports: [QueryHistoryComponent, QueryListComponent],
  declarations: [
    QueryHistoryComponent,
    VyneqlRecordComponent,
    RestfulRecordComponent,
    QueryListComponent,
    QueryHistoryCardComponent,
    ActiveQueryCardComponent],
  providers: [],
})
export class QueryHistoryModule {
}
