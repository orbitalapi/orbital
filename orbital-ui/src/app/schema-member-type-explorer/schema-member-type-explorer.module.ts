import {NgModule} from '@angular/core';
import {CommonModule} from '@angular/common';
import {MarkdownComponent, MarkdownModule} from 'ngx-markdown';
import {SchemaMemberTypeExplorerComponent} from 'src/app/schema-member-type-explorer/schema-member-type-explorer.component';
import {AngularSplitModule} from 'angular-split';
import {SchemaMemberTreeComponent} from 'src/app/schema-member-type-explorer/schema-member-tree.component';
import {TuiBadgeModule, TuiIslandModule, TuiTabsModule, TuiTreeModule} from '@taiga-ui/kit';
import {TuiButtonModule, TuiGroupModule, TuiNotificationModule} from '@taiga-ui/core';
import {TypeViewerModule} from 'src/app/type-viewer/type-viewer.module';
import {FormsModule} from "@angular/forms";
import {CodeEditorModule} from "../code-editor/code-editor.module";
import {CodeViewerModule} from "../code-viewer/code-viewer.module";
import { OperationViewComponent } from '../operation-view/operation-view.component';
import { SchemaDiagramModule } from '../schema-diagram/schema-diagram.module';
import markedAlert from "marked-alert";


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
    TuiNotificationModule,
    TuiGroupModule,
    FormsModule,
    CodeEditorModule,
    CodeViewerModule,
    TuiIslandModule,
    TuiTabsModule,
    SchemaDiagramModule,
    OperationViewComponent,
    MarkdownModule.forRoot({
      markedExtensions: [markedAlert()]
    }),
  ]
})
export class SchemaMemberTypeExplorerModule {
}
