import {CovalentHighlightModule} from '@covalent/highlight';
import {BrowserModule} from '@angular/platform-browser';
import {CommonModule} from '@angular/common';
import {NgModule} from '@angular/core';
import {AgGridModule} from 'ag-grid-angular';
import {ResultsTableComponent} from './results-table.component';

@NgModule({
  imports: [BrowserModule, CommonModule,
    AgGridModule.withComponents()
  ],
  exports: [ResultsTableComponent],
  declarations: [ResultsTableComponent],
  providers: []
})
export class ResultsTableModule {
}
