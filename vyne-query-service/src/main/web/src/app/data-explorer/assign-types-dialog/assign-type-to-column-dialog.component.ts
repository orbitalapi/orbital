import {Component, EventEmitter, Inject, Output} from '@angular/core';
import {MAT_DIALOG_DATA, MatDialogRef} from '@angular/material/dialog';
import {Schema, Type} from '../../services/schema';

@Component({
  selector: 'app-assign-type-to-column-dialog',
  templateUrl: './assign-type-to-column-dialog.component.html',
  styleUrls: ['./assign-type-to-column-dialog.component.scss']
})
export class AssignTypeToColumnDialogComponent {
  @Output()
  selectedTypeChanged = new EventEmitter<Type>();
  schema: Schema;
  targetTypes: Type;

  constructor(
    private dialogRef: MatDialogRef<AssignTypeToColumnDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: any) {
      this.schema = data.schema;
    }

  save() {
    this.dialogRef.close(this.targetTypes);
  }

  close() {
    this.dialogRef.close();
  }
}
