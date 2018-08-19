import {BrowserModule} from '@angular/platform-browser';
import {NgModule} from '@angular/core';

import {CovalentDynamicFormsModule} from '@covalent/dynamic-forms';

import {AppComponent} from './app.component';
import {BrowserAnimationsModule} from '@angular/platform-browser/animations';
import {VyneComponent} from './vyne/vyne.component';
import {LayoutModule} from '@angular/cdk/layout';
import {
  MatButtonModule,
  MatExpansionModule,
  MatIconModule,
  MatListModule,
  MatSidenavModule,
  MatToolbarModule,
  MatCardModule, MatFormFieldModule, MatAutocompleteModule, MatInputModule,
} from '@angular/material';
import {TypeListComponent} from './type-list/type-list.component';
import {CommonModule} from "@angular/common";
import {RouterModule} from "@angular/router";
import {HttpClientModule} from "@angular/common/http";
import {QueryWizardComponent} from './query-wizard/query-wizard.component';
import {TypesService} from "./services/types.service";
import {FormsModule, ReactiveFormsModule} from "@angular/forms";
import {QueryService} from "./services/query.service";


const appRoutes = [
  {path: '', redirectTo: 'type-explorer', pathMatch: 'full'},
  {path: 'type-explorer', component: TypeListComponent},
  {path: 'query-wizard', component: QueryWizardComponent},
];

@NgModule({
  declarations: [
    AppComponent,
    VyneComponent,
    TypeListComponent,
    QueryWizardComponent
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

    CovalentDynamicFormsModule,


  ],
  providers: [TypesService, QueryService],
  bootstrap: [AppComponent]
})
export class AppModule {
}
