import { NgModule } from '@angular/core';
import { CodeViewerComponent } from './code-viewer.component';
import { ErrorListComponent } from './error-list.component';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AngularSplitModule } from 'angular-split';
import { CodeEditorModule } from '../code-editor/code-editor.module';

@NgModule({
  // FormsModule is required for ngModel binding.
  imports: [CommonModule, FormsModule, AngularSplitModule, CodeEditorModule],
  exports: [CodeViewerComponent, ErrorListComponent],
  declarations: [CodeViewerComponent, ErrorListComponent],
  providers: [],
})
export class CodeViewerModule {
}


