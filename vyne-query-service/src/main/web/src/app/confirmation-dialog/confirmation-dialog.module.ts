import { NgModule } from '@angular/core';
import { ConfirmationDialogComponent } from 'src/app/confirmation-dialog/confirmation-dialog.component';
import { MatDialogModule } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';

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
  providers: [],
  entryComponents: [ConfirmationDialogComponent]
})
export class ConfirmationDialogModule {
}
