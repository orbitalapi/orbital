import {Component, OnInit, ViewContainerRef} from '@angular/core';
import {ICellEditorAngularComp, INoRowsOverlayAngularComp} from 'ag-grid-angular';
import {IAfterGuiAttachedParams, ICellEditorParams, ICellRendererParams, INoRowsOverlayParams} from 'ag-grid-community';
import {debug} from 'util';
import {findType, Schema, Type} from '../services/schema';
import {TableColumn} from './db-importer.service';

@Component({
  selector: 'app-type-selector-cell-editor',
  template: `
    <div class="cell-editor">
      <app-type-autocomplete [schema]="schema"
                             placeholder="Select a type"
                             hint="Start typing to see available types"
                             [(selectedType)]="selectedType"></app-type-autocomplete>
      <button mat-raised-button>Create new type</button>
    </div>
  `,
  styleUrls: ['./type-selector-cell-editor.component.scss']
})
export class TypeSelectorCellEditorComponent implements ICellEditorAngularComp {
  schema: Schema;

  selectedType: Type;

  agInit(params: ICellEditorParams): void {
    this.schema = (params as any).schema();
    this.selectedType = findType(this.schema, (params.data as TableColumn).taxiType.parameterizedName);
  }

  isPopup(): boolean {
    return true;
  }

  getValue(): any {
    return (this.selectedType) ? this.selectedType.name : null;
  }

  // agInit(params: ICellRendererParams): void {

  // }


}
