import {NgModule} from '@angular/core';
import {DescriptionEditorComponent} from './description-editor.component';
import {DescriptionEditorContainerComponent} from './description-editor-container.component';
import {MatButtonModule} from '@angular/material/button';
import {MatIconModule} from '@angular/material/icon';
import {CommonModule} from '@angular/common';
import {MarkdownModule} from 'ngx-markdown';
import {MatSnackBarModule} from '@angular/material/snack-bar';
import {VyneServicesModule} from '../../services/vyne-services.module';
import {RouterModule} from '@angular/router';

@NgModule({
  imports: [CommonModule, MatButtonModule, MatIconModule,
    MatSnackBarModule,
    MarkdownModule.forRoot(),
    VyneServicesModule,
    RouterModule
  ],
  exports: [DescriptionEditorContainerComponent, DescriptionEditorComponent],
  declarations: [DescriptionEditorComponent, DescriptionEditorContainerComponent],
  providers: [],
})
export class DescriptionEditorModule {
}
