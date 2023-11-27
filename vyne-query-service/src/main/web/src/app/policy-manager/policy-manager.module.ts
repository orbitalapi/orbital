import {NgModule} from '@angular/core';
import {CommonModule} from '@angular/common';
import {MatLegacyTabsModule as MatTabsModule} from '@angular/material/legacy-tabs';
import {FormsModule, ReactiveFormsModule} from '@angular/forms';
import {MatLegacySelectModule as MatSelectModule} from '@angular/material/legacy-select';
import {MatLegacyChipsModule as MatChipsModule} from '@angular/material/legacy-chips';
import {MatIconModule} from '@angular/material/icon';
import {MatLegacyProgressBarModule as MatProgressBarModule} from '@angular/material/legacy-progress-bar';
import {MatLegacyAutocompleteModule as MatAutocompleteModule} from '@angular/material/legacy-autocomplete';
import {MatLegacyInputModule as MatInputModule} from '@angular/material/legacy-input';
import {MatLegacyButtonModule as MatButtonModule} from '@angular/material/legacy-button';
import {CaseConditionEditorComponent} from './case-condition-editor.component';
import {ElseEditorComponent} from './else-editor.component';
import {EqualsEditorComponent} from './equals-editor.component';
import {InstructionSelectorComponent} from './instruction-selector.component';
import {MultivalueEditorComponent} from './multivalue-editor.component';
import {PolicyEditorComponent} from './policy-editor.component';
import {PolicyManagerComponent} from './policy-manager.component';
import {StatementDisplayComponent} from './statement-display.component';
import {StatementEditorComponent} from './statement-editor.component';
import {TypeAutocompleteModule} from '../type-autocomplete/type-autocomplete.module';
import {VyneServicesModule} from '../services/vyne-services.module';
import {PolicyManagerContainerComponent} from './policy-manager-container.component';
import {MatLegacySnackBarModule as MatSnackBarModule} from '@angular/material/legacy-snack-bar';

@NgModule({
  imports: [
    CommonModule,
    MatTabsModule, FormsModule, ReactiveFormsModule, MatSelectModule,
    MatChipsModule, MatIconModule, MatProgressBarModule, MatAutocompleteModule, MatInputModule,
    MatButtonModule,
    MatSnackBarModule,
    TypeAutocompleteModule,
    VyneServicesModule
  ],
  exports: [CaseConditionEditorComponent, ElseEditorComponent,
    EqualsEditorComponent, InstructionSelectorComponent, MultivalueEditorComponent,
    PolicyEditorComponent, PolicyManagerComponent, StatementDisplayComponent, StatementEditorComponent,
    PolicyManagerContainerComponent
    ],
  declarations: [CaseConditionEditorComponent, ElseEditorComponent,
    EqualsEditorComponent, InstructionSelectorComponent, MultivalueEditorComponent,
    PolicyManagerContainerComponent,
    PolicyEditorComponent, PolicyManagerComponent, StatementDisplayComponent, StatementEditorComponent,
    ],
  providers: [],
})
export class PolicyManagerModule {
}


