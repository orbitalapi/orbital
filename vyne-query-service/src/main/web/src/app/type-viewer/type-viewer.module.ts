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
import {HeaderBarModule} from '../header-bar/header-bar.module';
import {SimpleCodeViewerModule} from '../simple-code-viewer/simple-code-viewer.module';
import {UsagesTableComponent} from './usages-table/usages-table.component';
import {OperationBadgeModule} from '../operation-badge/operation-badge.module';
import {TagsSectionComponent} from './tags-section/tags-section.component';
import {MatIconModule} from '@angular/material/icon';
import {EditTagsPanelComponent} from './tags-section/edit-tags-panel.component';
import {NgSelectModule} from '@ng-select/ng-select';
import {MatButtonModule} from '@angular/material/button';
import {FormsModule} from '@angular/forms';
import {EditTagsPanelContainerComponent} from './tags-section/edit-tags-panel-container.component';
import { EditOwnerPanelContainerComponent } from './tags-section/edit-owner-panel-container.component';
import { EditOwnerPanelComponent } from './tags-section/edit-owner-panel.component';
import {LineageGraphComponent} from './lineage-graph/lineage-graph.component';
import { LineageGraphContainerComponent } from './lineage-graph/lineage-graph-container.component';
import {LineageGraphModule} from './lineage-graph/lineage-graph.module';


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
    InheritanceGraphModule,
    HeaderBarModule,
    SimpleCodeViewerModule,
    OperationBadgeModule,
    MatIconModule,
    NgSelectModule,
    MatButtonModule,
    FormsModule,
    LineageGraphModule
  ],
  declarations: [
    TocHostDirective,
    TypeViewerComponent,
    TypeViewerContainerComponent,
    UsagesTableComponent,
    TagsSectionComponent,
    EditTagsPanelComponent,
    EditTagsPanelContainerComponent,
    EditOwnerPanelContainerComponent,
    EditOwnerPanelComponent,
  ],
  exports: [
    TagsSectionComponent,
    TypeViewerComponent,
    EditTagsPanelComponent,
    EditOwnerPanelComponent,
    EditTagsPanelContainerComponent
  ],
  entryComponents: [
    EditTagsPanelContainerComponent,
    EditOwnerPanelContainerComponent
  ]
})
export class TypeViewerModule {
}
