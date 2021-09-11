import {NgModule} from '@angular/core';
import {BrowserModule} from '@angular/platform-browser';
import {CommonModule} from '@angular/common';
import {TypedInstancePanelContainerComponent} from './typed-instance-panel-container.component';
import {TypedInstancePanelComponent} from './typed-instance-panel.component';
import {DescriptionEditorModule} from '../type-viewer/description-editor/description-editor.module';
import {EnumTableModule} from '../type-viewer/enums-table/enum-table.module';
import {AttributeTableModule} from '../type-viewer/attribute-table/attribute-table.module';
import {InlineQueryRunnerModule} from '../inline-query-runner/inline-query-runner.module';
import {InheritanceGraphModule} from '../inheritence-graph/inheritance-graph.module';
import {LineageDisplayModule} from '../lineage-display/lineage-display.module';
import {MatButtonModule} from '@angular/material/button';
import {MatIconModule} from '@angular/material/icon';


@NgModule({
  imports: [
    BrowserModule,
    CommonModule,
    DescriptionEditorModule,
    EnumTableModule,
    AttributeTableModule,
    InlineQueryRunnerModule,
    InheritanceGraphModule,
    LineageDisplayModule,
    MatButtonModule,
    MatIconModule
  ],
    exports: [TypedInstancePanelContainerComponent, TypedInstancePanelComponent],
  declarations: [
    TypedInstancePanelComponent,
    TypedInstancePanelContainerComponent
  ],
  providers: [],
})
export class TypedInstancePanelModule {
}
