import {NgModule} from '@angular/core';
import {BrowserModule} from '@angular/platform-browser';
import {CommonModule} from '@angular/common';
import {MonacoEditorModule} from '@materia-ui/ngx-monaco-editor';
import {FormsModule} from '@angular/forms';
import { CodeViewerComponent } from '../code-viewer/code-viewer.component';

@NgModule({
  // FormsModule is required for ngModel binding.
  imports: [BrowserModule, CommonModule, MonacoEditorModule, FormsModule],
  exports: [CodeViewerComponent],
  declarations: [CodeViewerComponent],
  providers: [],
})
export class CodeEditorModule {
}


