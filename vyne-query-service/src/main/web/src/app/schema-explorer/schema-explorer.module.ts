import {NgModule} from '@angular/core';
import {SchemaExplorerComponent} from './schema-explorer.component';
import {SearchModule} from '../search/search.module';
import {MatToolbarModule} from '@angular/material/toolbar';
import {BrowserModule} from '@angular/platform-browser';
import {CommonModule} from '@angular/common';
import {CodeViewerModule} from '../code-viewer/code-viewer.module';
import {MatMenuModule} from '@angular/material/menu';
import {MatButtonModule} from '@angular/material/button';
import {MatProgressBarModule} from '@angular/material/progress-bar';
import {MatStepperModule} from '@angular/material/stepper';
import {MatFormFieldModule} from '@angular/material/form-field';
import {MatSelectModule} from '@angular/material/select';
import {ReactiveFormsModule} from '@angular/forms';
import {CovalentHighlightModule} from '@covalent/highlight';
import {MatListModule} from '@angular/material/list';
import {MatIconModule} from '@angular/material/icon';
import {MatInputModule} from '@angular/material/input';
import {HeaderBarModule} from '../header-bar/header-bar.module';
import {RouterModule} from '@angular/router';


@NgModule({
  imports: [
    BrowserModule,
    CommonModule,
    MatMenuModule,
    MatButtonModule,
    SearchModule,
    MatToolbarModule,
    CodeViewerModule,
    MatProgressBarModule,
    MatStepperModule,
    MatFormFieldModule,
    MatSelectModule,
    ReactiveFormsModule,
    CovalentHighlightModule,
    MatListModule,
    MatIconModule,
    MatInputModule,
    HeaderBarModule,
    RouterModule,
  ],
  exports: [SchemaExplorerComponent],
  declarations: [SchemaExplorerComponent],
  providers: [],
})
export class SchemaExplorerModule {
}

