import {NgModule} from '@angular/core';
import {CommonModule} from '@angular/common';
import {SchemaExplorerTableComponent} from 'src/app/schema-explorer-table/schema-explorer-table.component';
import {AngularSplitModule} from 'angular-split';
import {SchemaEntryTableComponent} from 'src/app/schema-explorer-table/schema-entry-table.component';
import {TuiAccordionModule, TuiBadgeModule, TuiRadioBlockModule, TuiTreeModule} from '@taiga-ui/kit';
import {TuiButtonModule, TuiGroupModule, TuiNotificationModule} from '@taiga-ui/core';
import {TypeViewerModule} from 'src/app/type-viewer/type-viewer.module';
import {OperationViewModule} from 'src/app/operation-view/operation-view.module';
import {FormsModule} from "@angular/forms";
import {CodeEditorModule} from "../code-editor/code-editor.module";
import {CodeViewerModule} from "../code-viewer/code-viewer.module";


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
    OperationViewModule,
    TuiNotificationModule,
    TuiRadioBlockModule,
    TuiGroupModule,
    FormsModule,
    CodeEditorModule,
    CodeViewerModule
  ]
})
export class SchemaExplorerTableModule {
}
