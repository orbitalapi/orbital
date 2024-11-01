import { NgModule } from '@angular/core';
import { CodeViewerComponent } from './code-viewer.component';
import { ErrorListComponent } from './error-list.component';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AngularSplitModule } from 'angular-split';
import { CodeEditorModule } from '../code-editor/code-editor.module';
import { FileTreeComponent } from './file-tree.component';
import { TuiIcon, TuiButton } from "@taiga-ui/core";
import { TuiTree, TuiBadge } from "@taiga-ui/kit";
import { FileTreeFolderNodeComponent } from './file-tree-folder-node.component';
import {CompilationMessageListModule} from "../compilation-message-list/compilation-message-list.module";

@NgModule({
  // FormsModule is required for ngModel binding.
  imports: [CommonModule, FormsModule, AngularSplitModule, CodeEditorModule, TuiIcon, ...TuiTree, TuiButton, CompilationMessageListModule, TuiBadge],
  exports: [CodeViewerComponent, ErrorListComponent],
  declarations: [CodeViewerComponent, ErrorListComponent, FileTreeComponent, FileTreeFolderNodeComponent],
  providers: [],
})
export class CodeViewerModule {
}


