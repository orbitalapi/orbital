import {NgModule} from '@angular/core';
import {CommonModule} from '@angular/common';
import {BrowserModule} from '@angular/platform-browser';
import {BrowserAnimationsModule} from '@angular/platform-browser/animations';
import {TypeLinkGraphModule} from './type-link-graph/type-link-graph.module';
import {AttributeTableModule} from './attribute-table/attribute-table.module';
import {ContentsTableModule} from './contents-table/contents-table.module';
import {DescriptionEditorModule} from './description-editor/description-editor.module';
import {EnumTableModule} from './enums-table/enum-table.module';
import {TypeViewerComponent} from './type-viewer.component';
import {MatToolbarModule} from '@angular/material/toolbar';
import {SearchModule} from '../search/search.module';
import {CodeViewerModule} from '../code-viewer/code-viewer.module';
import {PolicyManagerModule} from '../policy-manager/policy-manager.module';
import {TocHostDirective} from './toc-host.directive';
import {TypeViewerContainerComponent} from './type-viewer-container.component';
import {InheritanceGraphModule} from '../inheritence-graph/inheritance-graph.module';


@NgModule({
  imports: [
    SearchModule,
    MatToolbarModule,
    CommonModule,
    BrowserModule,
    BrowserAnimationsModule,
    AttributeTableModule,
    ContentsTableModule,
    DescriptionEditorModule,
    EnumTableModule,
    TypeLinkGraphModule,
    CodeViewerModule,
    PolicyManagerModule,
    InheritanceGraphModule
  ],
  declarations: [
    TocHostDirective,
    TypeViewerComponent,
    TypeViewerContainerComponent
  ],
  exports: [
    TypeViewerComponent
  ]
})
export class TypeViewerModule {
}
