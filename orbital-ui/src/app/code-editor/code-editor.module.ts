import {NgModule} from '@angular/core';
import {CommonModule} from '@angular/common';
import {FormsModule} from '@angular/forms';
import {CodeEditorComponent} from './code-editor.component';
import {VyneServicesModule} from "../services/vyne-services.module";

@NgModule({
  // FormsModule is required for ngModel binding.
  imports: [CommonModule, FormsModule, VyneServicesModule],
  exports: [CodeEditorComponent],
  declarations: [CodeEditorComponent],
  providers: [],
})
export class CodeEditorModule {
}


