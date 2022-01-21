import {NgModule} from '@angular/core';

import {AuthManagerComponent} from './auth-manager.component';
import {HeaderBarModule} from '../header-bar/header-bar.module';
import { TokenListComponent } from './token-list.component';
import {CommonModule} from '@angular/common';
import {MatButtonModule} from '@angular/material/button';
import {MatDialogModule} from '@angular/material/dialog';
import { NewTokenPanelComponent } from './new-token-panel.component';
import {MatCardModule} from '@angular/material/card';
import {TypeAutocompleteModule} from '../type-autocomplete/type-autocomplete.module';
import {FormsModule, ReactiveFormsModule} from '@angular/forms';
import {MatFormFieldModule} from '@angular/material/form-field';
import {MatSelectModule} from '@angular/material/select';
import {MatInputModule} from '@angular/material/input';
import {MatSnackBarModule} from '@angular/material/snack-bar';
import {TuiButtonModule} from '@taiga-ui/core';

@NgModule({
    imports: [
        HeaderBarModule,
        CommonModule,
        MatButtonModule,
        MatDialogModule,
        MatCardModule,
        TypeAutocompleteModule,
        ReactiveFormsModule,
        FormsModule,
        MatFormFieldModule,
        MatSelectModule,
        MatInputModule,
        MatSnackBarModule,
        TuiButtonModule
    ],
  exports: [AuthManagerComponent],
  declarations: [AuthManagerComponent, TokenListComponent, NewTokenPanelComponent],
  providers: [],
  entryComponents: [NewTokenPanelComponent]
})
export class AuthManagerModule {
}
