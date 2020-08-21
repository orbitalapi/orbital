import {Component, EventEmitter, Inject, Input, Output} from '@angular/core';
import {MAT_DIALOG_DATA, MatDialogRef} from '@angular/material/dialog';
import {Schema, Type, VersionedSource} from '../../services/schema';
import {buildInheritable, Inheritable} from '../../inheritence-graph/inheritance-graph.component';
import {FormBuilder, FormControl, FormGroup} from '@angular/forms';
import {dateTimeSymbolsExampleTableData} from './date-time-symbols-example-table-data';
import {dateTimeSampleFormatsTableData} from './date-time-sample-formats-table-data';

export interface AssignedTypeData {
  targetType: any;
  format: string;
  inheritedName: string;
}

interface DateTimeFormatsTableColumns {
  symbol: string;
  meaning: string;
  presentation: string;
  examples: any;
}

interface DateTimeSampleFormatsTableColumns {
  format: string;
  example: string;
}

@Component({
  selector: 'app-assign-type-to-column-dialog',
  templateUrl: './assign-type-to-column-dialog.component.html',
  styleUrls: ['./assign-type-to-column-dialog.component.scss']
})
export class AssignTypeToColumnDialogComponent {
  @Input()
  expanded: boolean;
  @Output()
  selectedTypeChanged = new EventEmitter<Type>();
  schema: Schema;
  targetType: Type;
  sources: VersionedSource[];
  inheritanceView: Inheritable;
  formatForm: FormGroup;
  format = new FormControl();
  isInheritedType = false;
  sampleDateTimeFormatsTable: DateTimeFormatsTableColumns[] = dateTimeSymbolsExampleTableData;
  dateTimeSampleFormatsTableData: DateTimeSampleFormatsTableColumns[] = dateTimeSampleFormatsTableData;
  inheritedFrom: string;

  constructor(
    private dialogRef: MatDialogRef<AssignTypeToColumnDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: any, private fb: FormBuilder) {
    this.schema = data.schema;
    this.expanded = false;
    this.formatForm = fb.group({
      typeName: this.format
    });
  }

  onTypeChange(event) {
    this.targetType = event;
    this.inheritanceView = buildInheritable(this.targetType, this.schema);
    if (this.targetType.hasFormat) {
      this.inheritedFrom = this.targetType.inheritsFrom[0].name;
      this.isInheritedType = true;
    }
  }

  save() {
    const postData: AssignedTypeData = {
      targetType: this.targetType,
      format: this.format.value,
      inheritedName: this.inheritedFrom
    };
    this.dialogRef.close(postData);
  }

  close() {
    this.dialogRef.close();
  }

  toggleVisibility() {
    this.expanded = !this.expanded;
  }
}
