import {Component, OnInit, ViewContainerRef} from '@angular/core';
import {ICellEditorAngularComp, INoRowsOverlayAngularComp} from 'ag-grid-angular';
import {IAfterGuiAttachedParams, ICellEditorParams, ICellRendererParams, INoRowsOverlayParams} from 'ag-grid-community';
import {debug, isNullOrUndefined} from 'util';
import {findType, Schema, Type} from '../services/schema';
import {ColumnMapping, JdbcColumn} from './db-importer.service';
import {MatDialog, MatDialogRef} from '@angular/material/dialog';
import {TypeEditorComponent} from '../type-editor/type-editor.component';
import {TypeEditorPopupComponent} from '../type-editor/type-editor-popup.component';
import {TaxiSubmissionResult, TypesService} from '../services/types.service';

@Component({
  selector: 'app-type-selector-cell-editor',
  template: `
    <div class="cell-editor">
      <app-type-autocomplete [schema]="schema"
                             placeholder="Select a type"
                             hint="Start typing to see available types"
                             (selectedTypeChange)="onSelectedTypeChanged($event)"
      ></app-type-autocomplete>
      <button mat-raised-button (click)="createNewType()">Create new type</button>
    </div>
  `,
  styleUrls: ['./type-selector-cell-editor.component.scss']
})
export class TypeSelectorCellEditorComponent implements ICellEditorAngularComp {
  schema: Schema;

  selectedType: Type;
  private diaglogRef: MatDialogRef<TypeEditorPopupComponent>;
  private stopEditing: (suppressNavigateAfterEdit?: boolean) => void;

  constructor(private dialog: MatDialog, private typeService: TypesService) {
    typeService.getTypes()
      .subscribe(schema => this.schema = schema);
  }

  agInit(params: ICellEditorParams): void {
    const tableColumn = params.data as ColumnMapping;
    this.stopEditing = params.stopEditing;
    this.selectedType = (tableColumn.typeSpec && tableColumn.typeSpec.typeName) ?
      findType(this.schema, tableColumn.typeSpec.typeName.parameterizedName) : null;
  }

  isPopup(): boolean {
    return true;
  }

  getValue(): any {
    return (this.selectedType) ? this.selectedType.name : null;
  }

  createNewType() {
    this.diaglogRef = this.dialog.open(TypeEditorPopupComponent, {
      width: '1000px'
    });
    this.diaglogRef.afterClosed().subscribe((event: TaxiSubmissionResult | null) => {
      if (!isNullOrUndefined(event)) {
        if (event.types.length !== 1) {
          console.error('Expected a single type back from type creation, but found ' + event.types.length);
        } else {
          this.selectedType = event.types[0];
          this.stopEditing(false);
        }
      }
    });
  }

  onSelectedTypeChanged($event: Type) {
    this.selectedType = $event;
    this.stopEditing(false);
  }
}
