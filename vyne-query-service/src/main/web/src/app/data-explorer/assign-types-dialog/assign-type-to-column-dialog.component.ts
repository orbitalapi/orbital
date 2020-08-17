import {Component, EventEmitter, Inject, Input, Output} from '@angular/core';
import {MAT_DIALOG_DATA, MatDialogRef} from '@angular/material/dialog';
import {Schema, SchemaMember, Type, VersionedSource} from '../../services/schema';
import {buildInheritable} from "../../inheritence-graph/inheritance-graph.component";

@Component({
  selector: 'app-assign-type-to-column-dialog',
  templateUrl: './assign-type-to-column-dialog.component.html',
  styleUrls: ['./assign-type-to-column-dialog.component.scss']
})
export class AssignTypeToColumnDialogComponent {
  @Output()
  selectedTypeChanged = new EventEmitter<Type>();
  schema: Schema;
  targetType: Type;
  sources: VersionedSource[];
  inheritanceView;

  constructor(
    private dialogRef: MatDialogRef<AssignTypeToColumnDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: any) {
    this.schema = data.schema;
  }
  onTypeChange(event) {
    this.targetType = event;
    this.inheritanceView = buildInheritable(this.targetType, this.schema);
  }
  save() {
    this.dialogRef.close(this.targetType);
  }

  close() {
    this.dialogRef.close();
  }
}
