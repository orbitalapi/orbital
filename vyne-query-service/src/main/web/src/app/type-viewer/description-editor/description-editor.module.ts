import { NgModule } from '@angular/core';
import { DescriptionEditorComponent } from './description-editor.component';
import { DescriptionEditorContainerComponent } from './description-editor-container.component';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { CommonModule } from '@angular/common';
import { MarkdownModule } from 'ngx-markdown';
import { MatSnackBarModule } from '@angular/material/snack-bar';
import { VyneServicesModule } from '../../services/vyne-services.module';
import { RouterModule } from '@angular/router';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { ChangesetNameDialogComponent } from '../../changeset-name-dialog/changeset-name-dialog.component';
import { TuiInputModule, TuiTextAreaModule } from '@taiga-ui/kit';
import { TuiButtonModule } from '@taiga-ui/core';

@NgModule({
  imports: [
    CommonModule,
    MatButtonModule,
    MatIconModule,
    MatSnackBarModule,
    MarkdownModule.forRoot(),
    VyneServicesModule,
    RouterModule,
    FormsModule,
    ReactiveFormsModule,
    TuiTextAreaModule,
    TuiInputModule,
    TuiButtonModule,
  ],
  exports: [DescriptionEditorContainerComponent, DescriptionEditorComponent, ChangesetNameDialogComponent],
  declarations: [DescriptionEditorComponent, DescriptionEditorContainerComponent, ChangesetNameDialogComponent],
  providers: [],
})
export class DescriptionEditorModule {
}
