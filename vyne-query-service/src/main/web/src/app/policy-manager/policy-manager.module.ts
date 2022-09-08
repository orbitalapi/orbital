import {NgModule} from '@angular/core';
import {CommonModule} from '@angular/common';
import {MatTabsModule} from '@angular/material/tabs';
import {FormsModule, ReactiveFormsModule} from '@angular/forms';
import {MatSelectModule} from '@angular/material/select';
import {MatChipsModule} from '@angular/material/chips';
import {MatIconModule} from '@angular/material/icon';
import {MatProgressBarModule} from '@angular/material/progress-bar';
import {MatAutocompleteModule} from '@angular/material/autocomplete';
import {MatInputModule} from '@angular/material/input';
import {MatButtonModule} from '@angular/material/button';
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
import {MatSnackBarModule} from '@angular/material/snack-bar';

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


