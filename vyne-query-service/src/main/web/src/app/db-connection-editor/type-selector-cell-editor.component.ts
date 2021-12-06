import {Component, OnInit, ViewContainerRef} from '@angular/core';
import {ICellEditorAngularComp, INoRowsOverlayAngularComp} from 'ag-grid-angular';
import {IAfterGuiAttachedParams, ICellEditorParams, ICellRendererParams, INoRowsOverlayParams} from 'ag-grid-community';
import {debug, isNullOrUndefined} from 'util';
import {findType, QualifiedName, Schema, Type} from '../services/schema';
import {ColumnMapping, JdbcColumn, TypeSpecContainer} from './db-importer.service';
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

  typeSpecContainer: TypeSpecContainer;
  private diaglogRef: MatDialogRef<TypeEditorPopupComponent>;
  private stopEditing: (suppressNavigateAfterEdit?: boolean) => void;

  constructor(private dialog: MatDialog, private typeService: TypesService) {
    typeService.getTypes()
      .subscribe(schema => this.schema = schema);
  }

  agInit(params: ICellEditorParams): void {
    this.typeSpecContainer = params.data as TypeSpecContainer;
    if (isNullOrUndefined(this.typeSpecContainer)) {
      // tslint:disable-next-line:max-line-length
      console.error('It is invalid to pass a null TypeSpecContainer to this component.  The value of the contained TypeSpec may be null, but not the container itself.');
    }
    this.stopEditing = params.stopEditing;
    // this.selectedType = (tableColumn.typeSpec && tableColumn.typeSpec.typeName) ?
    //   findType(this.schema, tableColumn.typeSpec.typeName.parameterizedName) : null;
  }

  isPopup(): boolean {
    return true;
  }

  getValue(): TypeSpecContainer {
    return this.typeSpecContainer;
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
          this.updateTypeSpec(event.types[0].name);
          this.stopEditing(false);
        }
      }
    });
  }

  onSelectedTypeChanged($event: Type) {
    this.updateTypeSpec($event.name);
    this.stopEditing(false);
  }

  private updateTypeSpec(name: QualifiedName) {
    if (isNullOrUndefined(this.typeSpecContainer.typeSpec)) {
      this.typeSpecContainer.typeSpec = {
        typeName: name,
        metadata: [],
        taxi: null
      };
    } else {
      this.typeSpecContainer.typeSpec.typeName = name;
    }
  }
}
