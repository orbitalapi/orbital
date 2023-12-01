import { Component, Inject, OnInit } from '@angular/core';
import { MAT_LEGACY_DIALOG_DATA as MAT_DIALOG_DATA, MatLegacyDialogRef as MatDialogRef } from '@angular/material/legacy-dialog';


export type ConfirmationAction = 'OK' | 'Cancel';

export class ConfirmationParams {
  constructor(
    readonly title: string,
    readonly message: string,
    readonly okActionLabel: string = 'OK',
    readonly cancelActionLabel: string = 'Cancel'
  ) {
  }
}


@Component({
  selector: 'app-confirmation-dialog',
  template: `
    <h2 mat-dialog-title>{{params.title}}</h2>
    <mat-dialog-content>{{params.message}}
    </mat-dialog-content>
    <mat-dialog-actions>
      <button mat-stroked-button mat-button mat-dialog-close="Cancel">{{ params.cancelActionLabel}}</button>
      <div class="spacer"></div>
      <button mat-flat-button color="primary" mat-dialog-close="OK">{{params.okActionLabel}}</button>
    </mat-dialog-actions>
  `,
  styleUrls: ['./confirmation-dialog.component.scss']
})
export class ConfirmationDialogComponent {

  constructor(
    @Inject(MAT_DIALOG_DATA) public params: ConfirmationParams,
    public dialogRef: MatDialogRef<ConfirmationDialogComponent>,
  ) {
  }

}
