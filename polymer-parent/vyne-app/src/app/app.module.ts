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
import {ResultDisplayComponent} from './query-wizard/result-display/result-display.component';
import {ProfileGraphComponent} from './query-wizard/result-display/profile-graph.component';
import {NgxGraphModule} from '@swimlane/ngx-graph';
import {CovalentJsonFormatterModule} from '@covalent/core/json-formatter';
import {PropertyViewComponent} from './type-list/property-view.component';
import {CdkTableModule} from "@angular/cdk/table";
import {SourceViewComponent} from './type-list/source-view.component';
import { TypeLinksComponent } from './type-list/type-links.component';

const appRoutes = [
  {path: '', redirectTo: 'type-explorer', pathMatch: 'full'},
  {path: 'type-explorer', component: TypeListComponent},
  {path: 'query-wizard', component: QueryWizardComponent},
  {path: 'result-explorer', component: ProfileGraphComponent}
];

@NgModule({
  declarations: [
    AppComponent,
    VyneComponent,
    TypeListComponent,
    QueryWizardComponent,
    ResultDisplayComponent,
    ProfileGraphComponent,
    PropertyViewComponent,
    SourceViewComponent,
    TypeLinksComponent
  ],
  imports: [
    RouterModule.forRoot(
      appRoutes,
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
    CovalentHighlightModule

  ],
  providers: [TypesService, QueryService],
  bootstrap: [AppComponent]
})
export class AppModule {
}
