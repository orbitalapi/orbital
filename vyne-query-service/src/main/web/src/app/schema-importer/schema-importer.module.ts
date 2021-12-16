import {NgModule} from '@angular/core';
import {CommonModule} from '@angular/common';
import {SchemaImporterComponent} from './schema-importer.component';
import {
  TuiAccordionModule,
  TuiBadgeModule,
  TuiInputModule,
  TuiTagModule,
  TuiTextAreaModule,
  TuiTreeModule
} from '@taiga-ui/kit';
import { SchemaMemberListComponent } from './schema-member-list.component';
import { ModelMemberComponent } from './model-member.component';
import { ModelDisplayComponent } from './model-display.component';
import {FormsModule} from '@angular/forms';
import {TuiButtonModule, TuiHintModule, TuiTextfieldControllerModule} from '@taiga-ui/core';
import { ModelMemberTreeNodeComponent } from './model-member-tree-node.component';
import { TypeSearchComponent } from './type-search/type-search.component';
import {TypeSearchResultComponent} from './type-search/type-search-result.component';
import { TypeSearchResultDocsComponent } from './type-search/type-search-result-docs.component';
import {TypeViewerModule} from '../type-viewer/type-viewer.module';


@NgModule({
  exports: [SchemaImporterComponent, ModelDisplayComponent,
    TypeSearchComponent, TypeSearchResultComponent],
  declarations: [SchemaImporterComponent,
    SchemaMemberListComponent, ModelMemberComponent,
    ModelDisplayComponent, ModelMemberTreeNodeComponent,
    TypeSearchComponent, TypeSearchResultComponent, TypeSearchResultDocsComponent],
  imports: [
    CommonModule,
    TuiTreeModule,
    TuiAccordionModule,
    TuiBadgeModule,
    TuiTextAreaModule,
    TuiTextfieldControllerModule,
    FormsModule,
    TuiTagModule,
    TuiHintModule,
    TuiButtonModule,
    TuiInputModule,
    TypeViewerModule
  ]
})
export class SchemaImporterModule {
}
