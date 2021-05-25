import {NgModule} from '@angular/core';

import {TypeEditorComponent} from './type-editor.component';
import {MatCardModule} from '@angular/material/card';
import {MatFormFieldModule} from '@angular/material/form-field';
import {CommonModule} from '@angular/common';
import {FormsModule} from '@angular/forms';
import {TypeAutocompleteModule} from '../type-autocomplete/type-autocomplete.module';
import {MatInputModule} from '@angular/material/input';
import {MatButtonModule} from '@angular/material/button';
import {DescriptionEditorModule} from '../type-viewer/description-editor/description-editor.module';

@NgModule({
    imports: [
        MatCardModule,
        MatFormFieldModule,
        CommonModule,
        FormsModule,
        TypeAutocompleteModule,
        MatInputModule,
        MatButtonModule,
        DescriptionEditorModule
    ],
  exports: [TypeEditorComponent],
  declarations: [TypeEditorComponent],
  providers: [],
})
export class TypedEditorModule {
}
