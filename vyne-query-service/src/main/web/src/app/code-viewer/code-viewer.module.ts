import {NgModule} from '@angular/core';
import {CodeViewerComponent} from './code-viewer.component';
import {ErrorListComponent} from './error-list.component';
import {BrowserModule} from '@angular/platform-browser';
import {CommonModule} from '@angular/common';
import {MonacoEditorModule} from '@materia-ui/ngx-monaco-editor';
import {FormsModule} from '@angular/forms';
import {AngularSplitModule} from "angular-split";

@NgModule({
  // FormsModule is required for ngModel binding.
    imports: [BrowserModule, CommonModule, MonacoEditorModule, FormsModule, AngularSplitModule],
  exports: [CodeViewerComponent, ErrorListComponent],
  declarations: [CodeViewerComponent, ErrorListComponent],
  providers: [],
})
export class CodeViewerModule {
}


