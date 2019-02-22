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
import {CommonModule} from "@angular/common";
import {RouterModule} from "@angular/router";
import {HttpClientModule} from "@angular/common/http";
import {QueryWizardComponent} from './query-wizard/query-wizard.component';
import {TypesService} from "./services/types.service";
import {FormsModule, ReactiveFormsModule} from "@angular/forms";
import {QueryService} from "./services/query.service";
import {ProfileGraphComponent} from './query-wizard/result-display/profile-graph.component';
import {NgxGraphModule} from '@swimlane/ngx-graph';
import {CovalentJsonFormatterModule} from '@covalent/core/json-formatter';
import {PropertyViewComponent} from './type-list/property-view.component';
import {CdkTableModule} from "@angular/cdk/table";
import {SourceViewComponent} from './type-list/source-view.component';
import {TypeLinksComponent} from './type-list/type-links.component';
import {ResultViewerComponent} from './query-wizard/result-display/result-viewer.component';
import {ResultContainerComponent} from "./query-wizard/result-display/result-container.component";
import {QueryHistoryComponent} from './query-history/query-history.component';
import {MomentModule} from "angular2-moment";
import {ParameterViewComponent} from './type-list/parameter-view.component';
import {TaxiViewerModule} from "./query-wizard/taxi-viewer/taxi-viewer.module";
import {SchemaExplorerComponent} from './schema-explorer/schema-explorer.component';
import {NewSchemaWizardComponent} from './schema-explorer/new-schema-wizard/new-schema-wizard.component';
import {FactEditorComponent} from './query-wizard/fact-editor/fact-editor.component';
import {TypeAutocompleteComponent} from './query-wizard/type-autocomplete.component';
import {PolicyManagerComponent} from './policy-manager/policy-manager.component';
import { PolicyEditorComponent } from './policy-manager/policy-editor.component';
import { CaseConditionEditorComponent } from './policy-manager/case-condition-editor.component';
import { MultivalueEditorComponent } from './policy-manager/multivalue-editor.component';
import {EqualsEditorComponent} from "./policy-manager/equals-editor.component";
import { StatementEditorComponent } from './policy-manager/statement-editor.component';
import { ElseEditorComponent } from './policy-manager/else-editor.component';
import { InstructionSelectorComponent } from './policy-manager/instruction-selector.component';
import { StatementDisplayComponent } from './policy-manager/statement-display.component';
import {VyneQueryViewerComponent} from "./query-wizard/taxi-viewer/vyne-query-viewer/vyne-query-viewer.component";

const appRoutes = [
  {path: '', redirectTo: 'type-explorer', pathMatch: 'full'},
  {path: 'type-explorer', component: TypeListComponent},
  {path: 'query-wizard', component: QueryWizardComponent},
  {path: 'schema-explorer', component: SchemaExplorerComponent},
  {path: 'schema-explorer/import', component: NewSchemaWizardComponent},
  {path: 'result-explorer', component: ProfileGraphComponent},
  {path: 'query-history', component: QueryHistoryComponent}
];

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
    PolicyManagerComponent,
    PolicyEditorComponent,
    CaseConditionEditorComponent,
    EqualsEditorComponent,
    MultivalueEditorComponent,
    StatementEditorComponent,
    ElseEditorComponent,
    InstructionSelectorComponent,
    StatementDisplayComponent
  ],
  imports: [
    RouterModule.forRoot(
      appRoutes,
      {useHash: true}
    ),

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

    MomentModule,

    TaxiViewerModule

  ],
  providers: [TypesService, QueryService],
  bootstrap: [AppComponent]
})
export class AppModule {
}
