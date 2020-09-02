import {Component} from '@angular/core';
import {IHeaderAngularComp} from 'ag-grid-angular';
import {IHeaderParams} from 'ag-grid-community';
import {CustomCsvTableHeaderService} from '../services/custom-csv-table-header.service';

export interface CustomHeader {
  typeName: string;
  shouldDisplayAddButtons: boolean;
  shouldDisplayBadges: boolean;
  fieldName: string;
}

@Component({
  template: `
    <div class="ag-cell-label-container" role="presentation">
      <span ref="eMenu" class="ag-header-icon ag-header-cell-menu-button"></span>
      <div style="display:grid">
        <span>{{headerComponentParameters.fieldName}}</span>
        <div [ngClass]="(headerComponentParameters.shouldDisplayBadges)?'badge-visible':'badge-hidden'">
          <span class="mono-badge">{{headerComponentParameters.typeName}}
            <span mat-icon-button class="clear-button" (click)="removeType(headerComponentParameters.fieldName)">
                 <img class="clear-icon" src="assets/img/clear-cross-circle.svg">
            </span>
          </span>
        </div>
        <div [ngClass]="(headerComponentParameters.shouldDisplayAddButtons)?'badge-visible':'badge-hidden'">
          <span class="add-type-badge" (click)="addType(headerComponentParameters.fieldName)">Add Type</span>
        </div>
      </div>
    </div>`,
  styleUrls: ['./csv-viewer.component.scss'],
})
export class GridHeaderActionsComponent implements IHeaderAngularComp {
  constructor(private customCsvTableHeaderService: CustomCsvTableHeaderService) {
  }


  public headerComponentParameters: CustomHeader;

  agInit(headerParams: IHeaderParams): void {
    this.headerComponentParameters = headerParams.column.getColDef().headerComponentParams;
  }

  public addType(fieldName: string): void {
    this.customCsvTableHeaderService.sendFieldName(fieldName);
  }

  public removeType(fieldName: string): void {
    this.customCsvTableHeaderService.sendTypeToRemove(fieldName);
  }
}
