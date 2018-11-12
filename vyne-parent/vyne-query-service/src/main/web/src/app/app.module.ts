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
  MatCardModule,
  MatExpansionModule,
  MatFormFieldModule,
  MatIconModule,
  MatInputModule,
  MatListModule,
  MatSelectModule,
  MatSidenavModule,
  MatTableModule,
  MatTabsModule,
  MatToolbarModule,
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
import {MermaidComponent} from './mermaid/mermaid.component';
import {QueryHistoryComponent} from './query-history/query-history.component';
import {MomentModule} from "angular2-moment";
import { ParameterViewComponent } from './type-list/parameter-view.component';

const appRoutes = [
  {path: '', redirectTo: 'type-explorer', pathMatch: 'full'},
  {path: 'type-explorer', component: TypeListComponent},
  {path: 'query-wizard', component: QueryWizardComponent},
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
    MermaidComponent,
    QueryHistoryComponent,
    ParameterViewComponent
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
    MatSelectModule,
    MatTreeModule,
    MatTabsModule,
    MatTableModule,
    CdkTableModule,

    CovalentDynamicFormsModule,
    CovalentJsonFormatterModule,
    CovalentHighlightModule,

    MomentModule

  ],
  providers: [TypesService, QueryService],
  bootstrap: [AppComponent]
})
export class AppModule {
}
