import {Component, OnInit} from '@angular/core';
import {ICellEditorAngularComp, ICellRendererAngularComp} from 'ag-grid-angular';
import {ICellEditorParams, ICellRendererParams} from 'ag-grid-community';
import {MatCheckboxChange} from '@angular/material/checkbox';

@Component({
  selector: 'app-checkbox-cell-editor',
  template: `
    <mat-checkbox [(ngModel)]="checked" [disabled]="!editable"></mat-checkbox>
  `,
  styleUrls: ['./checkbox-cell-editor.component.scss']
})
export class CheckboxCellEditorComponent implements ICellRendererAngularComp, ICellEditorAngularComp {
  checked: boolean;
  editable: boolean;
  agInit(params: ICellRendererParams | ICellEditorParams): void {
    this.checked = params.value;
    this.editable = (typeof params.colDef.editable === 'boolean') ? !!params.colDef.editable : (params.colDef as any).editable() as boolean;
  }

  getValue(): any {
    return this.checked;
  }

  refresh(params: any): boolean {
    return false;
  }
}
