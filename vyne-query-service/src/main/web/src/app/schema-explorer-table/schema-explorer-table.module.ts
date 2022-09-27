import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { SchemaExplorerTableComponent } from 'src/app/schema-explorer-table/schema-explorer-table.component';
import { AngularSplitModule } from 'angular-split';
import { SchemaEntryTableComponent } from 'src/app/schema-explorer-table/schema-entry-table.component';
import { TuiAccordionModule, TuiBadgeModule, TuiTreeModule } from '@taiga-ui/kit';
import { TuiButtonModule } from '@taiga-ui/core';
import { TypeViewerModule } from 'src/app/type-viewer/type-viewer.module';
import { OperationViewModule } from 'src/app/operation-view/operation-view.module';


@NgModule({
  exports: [
    SchemaExplorerTableComponent
  ],
  declarations: [
    SchemaExplorerTableComponent,
    SchemaEntryTableComponent
  ],
  imports: [
    CommonModule,
    AngularSplitModule,
    TuiAccordionModule,
    TuiBadgeModule,
    TuiTreeModule,
    TuiButtonModule,
    TypeViewerModule,
    OperationViewModule
  ]
})
export class SchemaExplorerTableModule {
}
