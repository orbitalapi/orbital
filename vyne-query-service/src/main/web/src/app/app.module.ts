import {BrowserModule} from '@angular/platform-browser';
import {NgModule} from '@angular/core';

import {AppComponent} from './app.component';
import {BrowserAnimationsModule} from '@angular/platform-browser/animations';
import {LayoutModule} from '@angular/cdk/layout';
import {CommonModule} from '@angular/common';
import {RouterModule} from '@angular/router';
import {HttpClientModule} from '@angular/common/http';
import {TypesService} from './services/types.service';
import {QueryService} from './services/query.service';
import {QueryHistoryComponent} from './query-history/query-history.component';
import {SchemaExplorerComponent} from './schema-explorer/schema-explorer.component';
import {NewSchemaWizardComponent} from './schema-explorer/new-schema-wizard/new-schema-wizard.component';
import {TypeViewerContainerComponent} from './type-viewer/type-viewer-container.component';
import {NgSelectModule} from '@ng-select/ng-select';
import {TypeAutocompleteModule} from './type-autocomplete/type-autocomplete.module';
import {PipelinesModule} from './pipelines/pipelines.module';
import {MarkdownModule} from 'ngx-markdown';
import {DataExplorerModule} from './data-explorer/data-explorer.module';
import {TypeViewerModule} from './type-viewer/type-viewer.module';
import {SearchModule} from './search/search.module';
import {CodeViewerModule} from './code-viewer/code-viewer.module';
import {SearchService} from './search/search.service';
import {QueryHistoryModule} from './query-history/query-history.module';
import {DataExplorerComponent} from './data-explorer/data-explorer.component';
import {TypeListModule} from './type-list/type-list.module';
import {TypeListComponent} from './type-list/type-list.component';
import {SchemaExplorerModule} from './schema-explorer/schema-explorer.module';
import {VyneModule} from './vyne/vyne.module';
import { CaskViewerModule } from './cask-viewer/cask-viewer.module';
import {CaskViewerComponent} from './cask-viewer/cask-viewer.component';
import {InheritanceGraphComponent} from './inheritence-graph/inheritance-graph.component';
import {QueryHistoryContainerComponent} from './query-history/query-history-container.component';
import { QueryPanelComponent } from './query-panel/query-panel.component';
import { QueryPanelModule } from './query-panel/query-panel.module';
import { ResultsTableComponent } from './results-table/results-table.component';

export const routerModule = RouterModule.forRoot(
  [
    {path: '', redirectTo: 'types', pathMatch: 'full'},
    {path: 'types', component: TypeListComponent},
    {path: 'types/:typeName', component: TypeViewerContainerComponent},
    {path: 'query-wizard', component: QueryPanelComponent},
    {path: 'data-explorer', component: DataExplorerComponent},
    {path: 'schema-explorer', component: SchemaExplorerComponent},
    {path: 'schema-explorer/import', component: NewSchemaWizardComponent},
    {path: 'query-history', component: QueryHistoryComponent},
    {path: 'cask-viewer', component: CaskViewerComponent},
    {path: 'query-history/:queryResponseId', component: QueryHistoryContainerComponent},
  ],
  {useHash: false, anchorScrolling: 'enabled', scrollPositionRestoration: 'disabled'}
);


@NgModule({
  declarations: [
    AppComponent,
  ],
  imports: [
    routerModule,

    BrowserModule,
    BrowserAnimationsModule,
    CommonModule,
    LayoutModule,

    HttpClientModule,

    MarkdownModule.forRoot(),

    CaskViewerModule,
    TypeViewerModule,
    NgSelectModule,
    TypeAutocompleteModule,
    PipelinesModule,
    DataExplorerModule,
    SearchModule,
    SchemaExplorerModule,
    CodeViewerModule,
    QueryPanelModule,
    QueryHistoryModule,
    TypeListModule,
    VyneModule

  ],
  // Not sure why I'm having to do this -- but here we are
  providers: [TypesService, QueryService, SearchService],
  exports: [],
  bootstrap: [AppComponent]
})
export class AppModule {
}
