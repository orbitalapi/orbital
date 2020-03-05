import {BrowserModule} from '@angular/platform-browser';
import {NgModule} from '@angular/core';

import {CovalentDynamicFormsModule} from '@covalent/dynamic-forms';
import {CovalentHighlightModule} from '@covalent/highlight';

import {AppComponent} from './app.component';
import {BrowserAnimationsModule} from '@angular/platform-browser/animations';
import {VyneComponent} from './vyne/vyne.component';
import {LayoutModule} from '@angular/cdk/layout';
import {
  MatAutocompleteModule,
  MatButtonModule,
  MatButtonToggleModule,
  MatCardModule, MatChipsModule,
  MatExpansionModule,
  MatFormFieldModule,
  MatIconModule,
  MatInputModule,
  MatListModule,
  MatMenuModule,
  MatProgressBarModule,
  MatSelectModule,
  MatSidenavModule, MatSlideToggleModule,
  MatSnackBarModule,
  MatStepperModule,
  MatTableModule,
  MatTabsModule,
  MatToolbarModule, MatTooltipModule,
  MatTreeModule,
} from '@angular/material';
import {TypeListComponent} from './type-list/type-list.component';
import {CommonModule} from '@angular/common';
import {RouterModule} from '@angular/router';
import {HttpClientModule} from '@angular/common/http';
import {QueryWizardComponent} from './query-wizard/query-wizard.component';
import {TypesService} from './services/types.service';
import {FormsModule, ReactiveFormsModule} from '@angular/forms';
import {QueryService} from './services/query.service';
import {ProfileGraphComponent} from './query-wizard/result-display/profile-graph.component';
import {NgxGraphModule} from '@swimlane/ngx-graph';
import {CovalentJsonFormatterModule} from '@covalent/core/json-formatter';
import {PropertyViewComponent} from './type-list/property-view.component';
import {CdkTableModule} from '@angular/cdk/table';
import {SourceViewComponent} from './type-list/source-view.component';
import {TypeLinksComponent} from './type-list/type-links.component';
import {ResultViewerComponent} from './query-wizard/result-display/result-viewer.component';
import {ResultContainerComponent} from './query-wizard/result-display/result-container.component';
import {QueryHistoryComponent} from './query-history/query-history.component';
import {ParameterViewComponent} from './type-list/parameter-view.component';
import {MomentModule} from 'ngx-moment';
import {TaxiViewerModule} from './query-wizard/taxi-viewer/taxi-viewer.module';
import {SchemaExplorerComponent} from './schema-explorer/schema-explorer.component';
import {NewSchemaWizardComponent} from './schema-explorer/new-schema-wizard/new-schema-wizard.component';
import {FactEditorComponent} from './query-wizard/fact-editor/fact-editor.component';
import {TypeAutocompleteComponent} from './query-wizard/type-autocomplete.component';
import {PolicyManagerComponent} from './policy-manager/policy-manager.component';
import {PolicyEditorComponent} from './policy-manager/policy-editor.component';
import {CaseConditionEditorComponent} from './policy-manager/case-condition-editor.component';
import {MultivalueEditorComponent} from './policy-manager/multivalue-editor.component';
import {EqualsEditorComponent} from './policy-manager/equals-editor.component';
import {StatementEditorComponent} from './policy-manager/statement-editor.component';
import {ElseEditorComponent} from './policy-manager/else-editor.component';
import {InstructionSelectorComponent} from './policy-manager/instruction-selector.component';
import {StatementDisplayComponent} from './policy-manager/statement-display.component';
import {VyneQueryViewerComponent} from './query-wizard/taxi-viewer/vyne-query-viewer/vyne-query-viewer.component';
import {TypeViewerComponent} from './type-viewer/type-viewer.component';
import {AttributeTableComponent} from './type-viewer/attribute-table/attribute-table.component';
import {TypeViewerContainerComponent} from './type-viewer/type-viewer-container.component';
import {TocHostDirective} from './type-viewer/toc-host.directive';
import {ContentsTableComponent} from './type-viewer/contents-table/contents-table.component';
import {TypeLinkGraphComponent} from './type-viewer/type-link-graph/type-link-graph.component';
import {TypeLinkGraphContainerComponent} from './type-viewer/type-link-graph/type-link-graph-container.component';
import { CodeViewerComponent } from './code-viewer/code-viewer.component';
import {HighlightModule} from 'ngx-highlightjs';
import {PolicyManagerContainerComponent} from './policy-manager/policy-manager-container.component';
import { DescriptionEditorComponent } from './type-viewer/description-editor/description-editor.component';
import {DescriptionEditorContainerComponent} from './type-viewer/description-editor/description-editor-container.component';
import { SearchResultComponent } from './search/seach-result/search-result.component';
import { SearchResultListComponent } from './search/search-result-list/search-result-list.component';
import { SearchBarComponent } from './search/search-bar/search-bar.component';
import {NgSelectModule} from '@ng-select/ng-select';
import {SearchBarContainerComponent} from './search/search-bar/search-bar.container.component';
import {SearchService} from './search/search.service';
import { ObjectViewComponent } from './object-view/object-view.component';
import { SchemaEditorComponent } from './schema-editor/schema-editor.component';
import { FileFactSelectorComponent } from './query-wizard/file-fact-selector/file-fact-selector.component';
import {CovalentFileModule} from '@covalent/core';

export const routerModule = RouterModule.forRoot(
  [
    {path: '', redirectTo: 'types', pathMatch: 'full'},
    {path: 'types', component: TypeListComponent},
    {path: 'types/:typeName', component: TypeViewerContainerComponent},
    {path: 'query-wizard', component: QueryWizardComponent},
    {path: 'schema-explorer', component: SchemaExplorerComponent},
    {path: 'schema-explorer/import', component: NewSchemaWizardComponent},
    {path: 'result-explorer', component: ProfileGraphComponent},
    {path: 'query-history', component: QueryHistoryComponent}
  ],
  {useHash: true}
);


@NgModule({
  declarations: [
    AppComponent,
    VyneComponent,
    TypeListComponent,
    QueryWizardComponent,

    ResultContainerComponent,
    ProfileGraphComponent,
    PropertyViewComponent,
    SourceViewComponent,
    TypeLinksComponent,
    ResultViewerComponent,
    QueryHistoryComponent,
    ParameterViewComponent,
    SchemaExplorerComponent,
    NewSchemaWizardComponent,
    FactEditorComponent,
    TypeAutocompleteComponent,
    PolicyManagerContainerComponent,
    PolicyManagerComponent,
    PolicyEditorComponent,
    CaseConditionEditorComponent,
    EqualsEditorComponent,
    MultivalueEditorComponent,
    StatementEditorComponent,
    ElseEditorComponent,
    InstructionSelectorComponent,
    StatementDisplayComponent,
    TypeViewerComponent,
    TypeViewerContainerComponent,
    AttributeTableComponent,
    DescriptionEditorComponent,
    DescriptionEditorContainerComponent,
    TocHostDirective,
    ContentsTableComponent,
    TypeLinkGraphComponent,
    TypeLinkGraphContainerComponent,
    CodeViewerComponent,
    DescriptionEditorComponent,
    SearchResultComponent,
    SearchResultListComponent,
    SearchBarComponent,
    SearchBarContainerComponent,
    ObjectViewComponent,
    SchemaEditorComponent,
    FileFactSelectorComponent
  ],
  imports: [
    routerModule,

    BrowserModule,
    BrowserAnimationsModule,
    CommonModule,
    LayoutModule,
    FormsModule,
    ReactiveFormsModule,

    HttpClientModule,
    NgxGraphModule,

    MatToolbarModule,
    MatButtonModule,
    MatSidenavModule,
    MatIconModule,
    MatListModule,
    MatExpansionModule,
    MatCardModule,
    MatFormFieldModule,
    MatAutocompleteModule,
    MatInputModule,
    MatMenuModule,
    MatChipsModule,
    MatTooltipModule,
    MatSlideToggleModule,
    MatButtonToggleModule,
    MatSnackBarModule,
    MatSelectModule,
    MatStepperModule,
    MatTreeModule,
    MatTabsModule,
    MatTableModule,
    MatProgressBarModule,
    CdkTableModule,

    CovalentDynamicFormsModule,
    CovalentJsonFormatterModule,
    CovalentHighlightModule,
    CovalentFileModule,

    MomentModule,

    TaxiViewerModule,
    NgSelectModule

  ],
  // Not sure why I'm having to do this -- but here we are
  providers: [TypesService, QueryService, SearchService],
  bootstrap: [AppComponent]
})
export class AppModule {
}
