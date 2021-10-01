import {Component, OnInit, ViewContainerRef} from '@angular/core';
import {ICellEditorAngularComp, INoRowsOverlayAngularComp} from 'ag-grid-angular';
import {IAfterGuiAttachedParams, ICellEditorParams, ICellRendererParams, INoRowsOverlayParams} from 'ag-grid-community';
import {debug} from 'util';
import {findType, Schema, Type} from '../services/schema';
import {TableColumn} from './db-importer.service';
import {MatDialog, MatDialogRef} from '@angular/material/dialog';
import {TypeEditorComponent} from '../type-editor/type-editor.component';
import {TypeEditorPopupComponent} from '../type-editor/type-editor-popup.component';

@Component({
  selector: 'app-type-selector-cell-editor',
  template: `
    <div class="cell-editor">
      <app-type-autocomplete [schema]="schema"
                             placeholder="Select a type"
                             hint="Start typing to see available types"
                             [(selectedType)]="selectedType"></app-type-autocomplete>
      <button mat-raised-button (click)="createNewType()">Create new type</button>
    </div>
  `,
  styleUrls: ['./type-selector-cell-editor.component.scss']
})
export class TypeSelectorCellEditorComponent implements ICellEditorAngularComp {
  schema: Schema;

  selectedType: Type;
  private diaglogRef: MatDialogRef<TypeEditorPopupComponent>;

  constructor(private dialog: MatDialog) {
  }

  agInit(params: ICellEditorParams): void {
    this.schema = (params as any).schema();
    const tableColumn = params.data as TableColumn;
    this.selectedType = tableColumn.taxiType ? findType(this.schema, tableColumn.taxiType.parameterizedName) : null;
  }

  isPopup(): boolean {
    return true;
  }

  getValue(): any {
    return (this.selectedType) ? this.selectedType.name : null;
  }

  // agInit(params: ICellRendererParams): void {

  // }


  createNewType() {
    this.diaglogRef = this.dialog.open(TypeEditorPopupComponent, {
      width: '1000px'
    });
    this.diaglogRef.afterClosed().subscribe(event => {

    });
  }
}
