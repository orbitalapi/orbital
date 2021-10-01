import {BrowserModule} from '@angular/platform-browser';
import {ApplicationRef, DoBootstrap, NgModule, Optional} from '@angular/core';

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
import {CaskViewerModule} from './cask-viewer/cask-viewer.module';
import {CaskViewerComponent} from './cask-viewer/cask-viewer.component';
import {QueryPanelComponent} from './query-panel/query-panel.component';
import {QueryPanelModule} from './query-panel/query-panel.module';
import {MatNativeDateModule} from '@angular/material/core';
import {ServiceViewContainerComponent} from './service-view/service-view-container.component';
import {ServiceViewModule} from './service-view/service-view.module';
import {OperationViewModule} from './operation-view/operation-view.module';
import {OperationViewContainerComponent} from './operation-view/operation-view-container.component';
import {AuthModule} from './auth/auth.module';
import {AuthService} from './auth/auth.service';
import {AuthManagerComponent} from './auth-mananger/auth-manager.component';
import {AuthManagerModule} from './auth-mananger/auth-manager.module';
import {ConfirmationDialogComponent} from './confirmation-dialog/confirmation-dialog.component';
import {MatDialogModule} from '@angular/material/dialog';
import {MatButtonModule} from '@angular/material/button';
import {ConnectionManagerModule} from './connection-manager/connection-manager.module';
import {DbConnectionEditorModule} from './db-connection-editor/db-connection-editor.module';
import {ConnectionListComponent} from './connection-manager/connection-list.component';
import {TableSelectorContainerComponent} from './db-connection-editor/table-selector-container.component';
import {ConnectionManagerComponent} from './connection-manager/connection-manager.component';
import {TableImporterContainerComponent} from './db-connection-editor/table-importer-container.component';
import {PipelineBuilderContainerComponent} from './pipelines/pipeline-builder/pipeline-builder-container.component';
import {PipelineViewContainerComponent} from './pipelines/pipeline-view/pipeline-view-container.component';
import {DbConnectionWizardComponent} from './db-connection-editor/db-connection-wizard.component';
import {PipelineManagerComponent} from './pipelines/pipeline-manager/pipeline-manager.component';
import {PipelineListComponent} from './pipelines/pipeline-list/pipeline-list.component';

export const routerModule = RouterModule.forRoot(
  [
    {path: '', redirectTo: 'catalog', pathMatch: 'full'},
    {path: 'types', redirectTo: 'catalog'},
    {path: 'catalogue', redirectTo: 'catalog'},
    {path: 'types/:typeName', redirectTo: 'catalog/:typeName'},
    {path: 'catalogue/:typeName', redirectTo: 'catalog/:typeName'},
    {path: 'catalog/:typeName', component: TypeViewerContainerComponent},
    {path: 'catalog', component: TypeListComponent},
    {path: 'services/:serviceName', component: ServiceViewContainerComponent},
    {path: 'services/:serviceName/:operationName', component: OperationViewContainerComponent},
    {path: 'query-wizard', component: QueryPanelComponent},
    {path: 'data-explorer', component: DataExplorerComponent},
    {path: 'schema-explorer', component: SchemaExplorerComponent},
    {path: 'schema-explorer/import', component: NewSchemaWizardComponent},
    {path: 'query-history', component: QueryHistoryComponent},
    {path: 'cask-viewer', component: CaskViewerComponent},
    {path: 'query-history/:queryResponseId', component: QueryHistoryComponent},
    {
      path: 'connection-manager', component: ConnectionManagerComponent, children: [
        {
          path: '', component: ConnectionListComponent
        },
        {
          path: 'new/database', component: DbConnectionWizardComponent
        },
        {
          path: 'jdbc/:connectionName', component: TableSelectorContainerComponent
        },
        {
          path: 'jdbc/:connectionName/:schemaName/:tableName', component: TableImporterContainerComponent
        }
      ]
    },
    {path: 'authentication-manager', component: AuthManagerComponent},
    {
      path: 'pipeline-manager', component: PipelineManagerComponent, children: [
        {path: '', component: PipelineListComponent},
        {path: 'new', component: PipelineBuilderContainerComponent},
        {path: ':pipelineId', component: PipelineViewContainerComponent}
      ]
    }
  ],
  {useHash: false, anchorScrolling: 'enabled', scrollPositionRestoration: 'disabled'}
);

const oauth2OidcModule = [AuthModule];


/*
if (!environment.secure) {
  oauth2OidcModule = [];
}
*/


@NgModule({
  declarations: [
    AppComponent,
    ConfirmationDialogComponent,
  ],
  imports: [
    routerModule,

    BrowserModule,
    BrowserAnimationsModule,
    CommonModule,
    LayoutModule,

    HttpClientModule,
    MatNativeDateModule,

    MatButtonModule,
    MatDialogModule,

    MarkdownModule.forRoot(),

    AuthManagerModule,
    CaskViewerModule,
    TypeViewerModule,
    ConnectionManagerModule,
    DbConnectionEditorModule,
    NgSelectModule,
    TypeAutocompleteModule,
    PipelinesModule,
    DataExplorerModule,
    SearchModule,
    SchemaExplorerModule,
    ServiceViewModule,
    OperationViewModule,
    CodeViewerModule,
    QueryPanelModule,
    QueryHistoryModule,
    TypeListModule,
    VyneModule,
    ...oauth2OidcModule,
  ],
  providers: [
    TypesService,
    QueryService,
    SearchService,
  ],
  entryComponents: [AppComponent, ConfirmationDialogComponent]
})
export class AppModule implements DoBootstrap {
  constructor(@Optional() private authService: AuthService) {
  }

  ngDoBootstrap(appRef: ApplicationRef): void {
    this.authService.bootstrapAuthService()
      .then(() => {
        appRef.bootstrap(AppComponent);
      })
      .catch(error => {
        console.error(`[ngDoBootstrap] Problem while authService.bootstrapAuthService(): ${JSON.stringify(error)}`, error);
      });
  }
}
