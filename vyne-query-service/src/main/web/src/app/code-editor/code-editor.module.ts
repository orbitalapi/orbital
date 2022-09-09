import {NgModule} from '@angular/core';
import {BrowserModule} from '@angular/platform-browser';
import {CommonModule} from '@angular/common';
import {FormsModule} from '@angular/forms';
import {CodeEditorComponent} from './code-editor.component';

@NgModule({
  // FormsModule is required for ngModel binding.
  imports: [BrowserModule, CommonModule, FormsModule],
  exports: [CodeEditorComponent],
  declarations: [CodeEditorComponent],
  providers: [],
})
export class CodeEditorModule {
}


