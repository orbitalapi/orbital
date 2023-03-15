import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ObjectViewComponent } from './object-view.component';
import { MatButtonModule } from '@angular/material/button';
import { MatMenuModule } from '@angular/material/menu';
import { MatRadioModule } from '@angular/material/radio';
import { ObjectViewContainerComponent } from './object-view-container.component';
import { ResultsTableModule } from '../results-table/results-table.module';
import { MatButtonToggleModule } from '@angular/material/button-toggle';
import { FormsModule } from '@angular/forms';
import { TuiPaginationModule, TuiTreeModule } from '@taiga-ui/kit';
import { ScrollingModule } from '@angular/cdk/scrolling';
import { ScrollingModule as ExperimentalScrollingModule } from '@angular/cdk-experimental/scrolling';
import { JsonViewerModule } from 'src/app/json-viewer/json-viewer.module';

@NgModule({
  imports: [CommonModule,
    MatButtonModule,
    MatButtonToggleModule,
    MatMenuModule,
    MatRadioModule,
    ResultsTableModule, FormsModule, TuiTreeModule, ScrollingModule, ExperimentalScrollingModule, TuiPaginationModule, JsonViewerModule],
  exports: [ObjectViewComponent, ObjectViewContainerComponent],
  declarations: [ObjectViewComponent, ObjectViewContainerComponent],
  providers: [],
})
export class ObjectViewModule {
}
