import {NgModule} from '@angular/core';
import {CommonModule} from '@angular/common';
import {ObjectViewComponent} from './object-view.component';
import {MatButtonModule} from '@angular/material/button';
import {MatMenuModule} from '@angular/material/menu';
import {MatRadioModule} from '@angular/material/radio';
import {ObjectViewContainerComponent} from './object-view-container.component';
import {ResultsTableModule} from '../results-table/results-table.module';
import {MatButtonToggleModule} from '@angular/material/button-toggle';
import {FormsModule} from '@angular/forms';
import {TuiPaginationModule, TuiTreeModule} from '@taiga-ui/kit';
import { ScrollingModule } from '@angular/cdk/scrolling';
import { ScrollingModule as ExperimentalScrollingModule} from '@angular/cdk-experimental/scrolling';
@NgModule({
    imports: [CommonModule,
        MatButtonModule,
        MatButtonToggleModule,
        MatMenuModule,
        MatRadioModule,
        ResultsTableModule, FormsModule, TuiTreeModule, ScrollingModule, ExperimentalScrollingModule, TuiPaginationModule],
  exports: [ObjectViewComponent, ObjectViewContainerComponent],
  declarations: [ObjectViewComponent, ObjectViewContainerComponent],
  providers: [],
})
export class ObjectViewModule {
}
