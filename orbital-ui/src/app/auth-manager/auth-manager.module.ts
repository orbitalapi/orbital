import { NgModule } from '@angular/core';

import { AuthManagerComponent } from './auth-manager.component';
import { HeaderBarModule } from '../header-bar/header-bar.module';
import { TokenListComponent } from './token-list.component';
import { CommonModule } from '@angular/common';
import { MatLegacyButtonModule as MatButtonModule } from '@angular/material/legacy-button';
import { MatLegacyDialogModule as MatDialogModule } from '@angular/material/legacy-dialog';
import { NewTokenPanelComponent } from './new-token-panel.component';
import { MatLegacyCardModule as MatCardModule } from '@angular/material/legacy-card';
import { TypeAutocompleteModule } from '../type-autocomplete/type-autocomplete.module';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { MatLegacyFormFieldModule as MatFormFieldModule } from '@angular/material/legacy-form-field';
import { MatLegacySelectModule as MatSelectModule } from '@angular/material/legacy-select';
import { MatLegacyInputModule as MatInputModule } from '@angular/material/legacy-input';
import { MatLegacySnackBarModule as MatSnackBarModule } from '@angular/material/legacy-snack-bar';
import { TuiButtonModule } from '@taiga-ui/core';
import { RouterModule } from '@angular/router';
import { AuthManagerService } from 'src/app/auth-manager/auth-manager.service';
import { HeaderComponentLayoutModule } from 'src/app/header-component-layout/header-component-layout.module';
import { AddTokenPanelComponent } from './add-token-panel.component';

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
        TuiButtonModule,
        RouterModule.forChild([
            {
                path: '',
                component: AuthManagerComponent,
            },
        ]),
        HeaderComponentLayoutModule
    ],
    exports: [AuthManagerComponent, TokenListComponent],
    declarations: [AuthManagerComponent, TokenListComponent, NewTokenPanelComponent, AddTokenPanelComponent],
    providers: [AuthManagerService]
})
export class AuthManagerModule {
}
