import {CommonModule} from '@angular/common';
import {NgModule} from '@angular/core';
import {AgGridModule} from 'ag-grid-angular';
import {ResultsTableComponent} from './results-table.component';
import {TypeInfoHeaderComponent} from './type-info-header.component';
import {MatTooltipModule} from '@angular/material/tooltip';

@NgModule({
  imports: [CommonModule,
    AgGridModule.withComponents(),
    MatTooltipModule
  ],
  exports: [ResultsTableComponent],
  declarations: [ResultsTableComponent, TypeInfoHeaderComponent],
  providers: [],
  entryComponents: [TypeInfoHeaderComponent]
})
export class ResultsTableModule {
}
