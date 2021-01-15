import {Component, OnInit} from '@angular/core';
import {IHeaderAngularComp} from 'ag-grid-angular';
import {IHeaderParams} from 'ag-grid-community';
import {QualifiedName, Type} from '../services/schema';

@Component({
  selector: 'app-type-info-header',
  template: `
    <div class="ag-cell-label-container" role="presentation">
      <div>
        <div>{{headerData.fieldName}}</div>
        <div class="mono-badge"
             [matTooltip]="headerData.typeName.longDisplayName"
        >{{headerData.typeName.shortDisplayName}}</div>
      </div>
    </div>
  `,
  styleUrls: ['./type-info-header.component.scss']
})
export class TypeInfoHeaderComponent implements IHeaderAngularComp {

  public headerData: TypeInfoHeaderData;

  agInit(params: IHeaderParams): void {
    this.headerData = params.column.getColDef().headerComponentParams;
  }
}

export interface TypeInfoHeaderData {
  typeName: QualifiedName;
  fieldName: string;
  type: Type;
}
