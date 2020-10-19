import {CovalentHighlightModule} from '@covalent/highlight';
import {BrowserModule} from '@angular/platform-browser';
import {CommonModule} from '@angular/common';
import {NgModule} from '@angular/core';
import {AgGridModule} from 'ag-grid-angular';
import {ResultsTableComponent} from './results-table.component';
import { TypeInfoHeaderComponent } from './type-info-header.component';
import {MatTooltipModule} from '@angular/material/tooltip';
import {BrowserAnimationsModule} from '@angular/platform-browser/animations';

@NgModule({
  imports: [BrowserModule, CommonModule,
    AgGridModule.withComponents(),
    BrowserAnimationsModule,
    MatTooltipModule
  ],
  exports: [ResultsTableComponent],
  declarations: [ResultsTableComponent, TypeInfoHeaderComponent],
  providers: [],
  entryComponents: [TypeInfoHeaderComponent]
})
export class ResultsTableModule {
}
