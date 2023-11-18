import {NgModule} from '@angular/core';

import {TypeEditorComponent} from './type-editor.component';
import {MatLegacyCardModule as MatCardModule} from '@angular/material/legacy-card';
import {MatLegacyFormFieldModule as MatFormFieldModule} from '@angular/material/legacy-form-field';
import {CommonModule} from '@angular/common';
import {FormsModule, ReactiveFormsModule} from '@angular/forms';
import {TypeAutocompleteModule} from '../type-autocomplete/type-autocomplete.module';
import {MatLegacyInputModule as MatInputModule} from '@angular/material/legacy-input';
import {MatLegacyButtonModule as MatButtonModule} from '@angular/material/legacy-button';
import {DescriptionEditorModule} from '../type-viewer/description-editor/description-editor.module';
import { TypeEditorCardComponent } from './type-editor-card.component';
import { TypeEditorPopupComponent } from './type-editor-popup.component';
import { TypeEditorContainerComponent } from './type-editor-container.component';
import {MatLegacyProgressBarModule as MatProgressBarModule} from '@angular/material/legacy-progress-bar';
import {TuiInputModule} from '@taiga-ui/kit';

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
        DescriptionEditorModule,
        MatProgressBarModule,
        TuiInputModule
    ],
    exports: [TypeEditorComponent, TypeEditorCardComponent, TypeEditorPopupComponent],
    declarations: [TypeEditorComponent, TypeEditorCardComponent, TypeEditorPopupComponent, TypeEditorContainerComponent],
    providers: []
})
export class TypedEditorModule {
}
