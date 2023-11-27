import {CommonModule} from '@angular/common';
import {NgModule} from '@angular/core';
import {AgGridModule} from 'ag-grid-angular';
import {ResultsTableComponent} from './results-table.component';
import {TypeInfoHeaderComponent} from './type-info-header.component';
import {MatLegacyTooltipModule as MatTooltipModule} from '@angular/material/legacy-tooltip';

@NgModule({
    imports: [CommonModule,
        AgGridModule,
        MatTooltipModule
    ],
    exports: [ResultsTableComponent],
    declarations: [ResultsTableComponent, TypeInfoHeaderComponent],
    providers: []
})
export class ResultsTableModule {
}
