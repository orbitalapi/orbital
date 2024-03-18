import {NgModule} from '@angular/core';
import {CommonModule} from '@angular/common';
import {SchemaMemberTypeExplorerComponent} from 'src/app/schema-member-type-explorer/schema-member-type-explorer.component';
import {AngularSplitModule} from 'angular-split';
import {SchemaMemberTreeComponent} from 'src/app/schema-member-type-explorer/schema-member-tree.component';
import {TuiBadgeModule, TuiIslandModule, TuiTabsModule, TuiTreeModule} from '@taiga-ui/kit';
import {TuiButtonModule, TuiGroupModule, TuiNotificationModule} from '@taiga-ui/core';
import {TypeViewerModule} from 'src/app/type-viewer/type-viewer.module';
import {OperationViewModule} from 'src/app/operation-view/operation-view.module';
import {FormsModule} from "@angular/forms";
import {CodeEditorModule} from "../code-editor/code-editor.module";
import {CodeViewerModule} from "../code-viewer/code-viewer.module";
import { SchemaDiagramModule } from '../schema-diagram/schema-diagram.module';


@NgModule({
  exports: [
    SchemaMemberTypeExplorerComponent
  ],
  declarations: [
    SchemaMemberTypeExplorerComponent,
    SchemaMemberTreeComponent
  ],
    imports: [
        CommonModule,
        AngularSplitModule,
        TuiBadgeModule,
        TuiTreeModule,
        TuiButtonModule,
        TypeViewerModule,
        OperationViewModule,
        TuiNotificationModule,
        TuiGroupModule,
        FormsModule,
        CodeEditorModule,
        CodeViewerModule,
        TuiIslandModule,
        TuiTabsModule,
        SchemaDiagramModule
    ]
})
export class SchemaMemberTypeExplorerModule {
}
