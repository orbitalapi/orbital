import {NgModule} from '@angular/core';

import {TypeEditorComponent} from './type-editor.component';
import {MatCardModule} from '@angular/material/card';
import {MatFormFieldModule} from '@angular/material/form-field';
import {CommonModule} from '@angular/common';
import {FormsModule, ReactiveFormsModule} from '@angular/forms';
import {TypeAutocompleteModule} from '../type-autocomplete/type-autocomplete.module';
import {MatInputModule} from '@angular/material/input';
import {MatButtonModule} from '@angular/material/button';
import {DescriptionEditorModule} from '../type-viewer/description-editor/description-editor.module';
import { TypeEditorCardComponent } from './type-editor-card.component';
import { TypeEditorPopupComponent } from './type-editor-popup.component';

@NgModule({
  imports: [
    MatCardModule,
    MatFormFieldModule,
    CommonModule,
    FormsModule,
    ReactiveFormsModule,
    TypeAutocompleteModule,
    MatInputModule,
    MatButtonModule,
    DescriptionEditorModule
  ],
  exports: [TypeEditorComponent, TypeEditorCardComponent, TypeEditorPopupComponent],
  declarations: [TypeEditorComponent, TypeEditorCardComponent, TypeEditorPopupComponent],
  providers: [],
  entryComponents: [TypeEditorPopupComponent]
})
export class TypedEditorModule {
}
