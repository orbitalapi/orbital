import {Component, Inject, OnInit} from '@angular/core';
import {MAT_DIALOG_DATA, MatDialogRef} from '@angular/material/dialog';
import {ExportFileService} from '../services/export.file.service';
import {CsvOptions} from '../services/types.service';
import {Type} from '../services/schema';

@Component({
  selector: 'app-test-spec-form',
  template: `
    <h2>
      Download a test spec
    </h2>
    <p>
      This lets you download the output of your parsed content as a test case than can by run automatically using Vynes
      testing tools.
    </p>
    <mat-form-field appearance="outline">
      <mat-label>Test case name</mat-label>
      <input matInput placeholder="Placeholder" [(ngModel)]="testSpecName">
      <mat-hint>Giving the test case a meaningful name helps explain what the test is asserting</mat-hint>
    </mat-form-field>
    <div class="button-row">
      <button mat-stroked-button (click)="onCancelClicked()">Cancel</button>
      <div class="spacer"></div>
      <button mat-raised-button color="primary" [disabled]="!hasName" (click)="onDownloadClicked()">Download</button>
    </div>
  `,
  styleUrls: ['./test-spec-form.component.scss']
})
export class TestSpecFormComponent {

  constructor(public dialogRef: MatDialogRef<TestSpecFormComponent>) {
  }

  testSpecName: string;

  get hasName(): boolean {
    return this.testSpecName && this.testSpecName.length > 0;
  }

  onDownloadClicked() {
    this.dialogRef.close(this.testSpecName);
  }

  onCancelClicked() {
    this.dialogRef.close(null);
  }
}
