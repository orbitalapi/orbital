import { TuiIslandDirective } from "@taiga-ui/legacy";
import {NgModule} from '@angular/core';
import {CommonModule} from '@angular/common';
import {MarkdownComponent, MarkdownModule} from 'ngx-markdown';
import {SchemaMemberTypeExplorerComponent} from 'src/app/schema-member-type-explorer/schema-member-type-explorer.component';
import {AngularSplitModule} from 'angular-split';
import {SchemaMemberTreeComponent} from 'src/app/schema-member-type-explorer/schema-member-tree.component';
import { TuiTree, TuiBadge, TuiTabs } from '@taiga-ui/kit';
import { TuiNotification, TuiGroup, TuiButton } from '@taiga-ui/core';
import {TypeViewerModule} from 'src/app/type-viewer/type-viewer.module';
import {FormsModule} from "@angular/forms";
import {CodeEditorModule} from "../code-editor/code-editor.module";
import {CodeViewerModule} from "../code-viewer/code-viewer.module";
import { OperationViewComponent } from '../operation-view/operation-view.component';
import { SchemaDiagramModule } from '../schema-diagram/schema-diagram.module';
import markedAlert from "marked-alert";
import {
  CaptureLocalNavigationDirective
} from "src/app/markdown-utils/capture-local-navigation.directive";
import {InlineRunQueryButtonComponent} from "../inline-run-query-button/inline-run-query-button.component";
import {SvgIconComponent} from "../svg-icon/svg-icon.component";


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
    TuiBadge,
    ...TuiTree,
    TuiButton,
    TypeViewerModule,
    TuiNotification,
    TuiGroup,
    FormsModule,
    CodeEditorModule,
    CodeViewerModule,
    TuiIslandDirective,
    ...TuiTabs,
    SchemaDiagramModule,
    OperationViewComponent,
    InlineRunQueryButtonComponent,
    SvgIconComponent,
    MarkdownModule.forRoot({
      markedExtensions: [markedAlert()]
    }),
    CaptureLocalNavigationDirective,
  ]
})
export class SchemaMemberTypeExplorerModule {
}
