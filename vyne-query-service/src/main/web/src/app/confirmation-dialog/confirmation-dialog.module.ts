import { NgModule } from '@angular/core';
import { ConfirmationDialogComponent } from 'src/app/confirmation-dialog/confirmation-dialog.component';
import { MatLegacyDialogModule as MatDialogModule } from '@angular/material/legacy-dialog';
import { MatLegacyButtonModule as MatButtonModule } from '@angular/material/legacy-button';

@NgModule({
    imports: [
        MatDialogModule,
        MatButtonModule
    ],
    exports: [
        ConfirmationDialogComponent
    ],
    declarations: [
        ConfirmationDialogComponent
    ],
    providers: []
})
export class ConfirmationDialogModule {
}
